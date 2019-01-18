package net.finmath.xva.initialmargin.simm2.calculation;

import static net.finmath.functions.NormalDistribution.inverseCumulativeDistribution;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import net.finmath.sensitivities.simm2.RiskClass;
import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.xva.initialmargin.simm2.specs.ParameterSet;

public class SimmCurvatureScheme extends SimmBaseScheme {

	public SimmCurvatureScheme(ParameterSet parameter) {
		super(parameter);
	}

	public RandomVariable getTheta(Collection<WeightedSensitivity> singleSensitivities) {
		final RandomVariable denominator = singleSensitivities.stream().
				map(ws -> ws.getWeightedSensitivity().abs()).
				reduce(new Scalar(0.0), RandomVariable::add);

		final RandomVariable numerator = singleSensitivities.stream().
				map(WeightedSensitivity::getWeightedSensitivity).
				reduce(new Scalar(0.0), RandomVariable::add);

		return numerator.div(denominator).cap(0.0);
	}

	public RandomVariable getLambda(RandomVariable theta) {
		return theta.add(1.0).mult(Math.pow(inverseCumulativeDistribution(0.995), 2.0) - 1.0).sub(theta);
	}

	/**
	 * Calculates the weighted sensitivity of a trade.
	 *
	 * @param coordinate The coordinate describing what kind of sensitivity (with respect to which risk factor) x is.
	 * @param x          The value of the sensitivity as a random variable.
	 * @return The {@link WeightedSensitivity} object representing the computation result.
	 */
	private WeightedSensitivity getWeightedSensitivity(SimmCoordinate coordinate, RandomVariable x) {
		return new WeightedSensitivity(coordinate, null, x.mult(parameter.getRiskWeight(coordinate)));
	}

	public BucketResult getBucketAggregation(String bucketName, Map<SimmCoordinate, RandomVariable> gradient) {
		final Set<WeightedSensitivity> weightedSensitivities = gradient.entrySet().stream().map(e -> getWeightedSensitivity(e.getKey(), e.getValue())).collect(Collectors.toSet());

		RandomVariable k = gradient.entrySet().stream().map(e -> getWeightedSensitivity(e.getKey(), e.getValue())).
				flatMap(w -> weightedSensitivities.stream().map(v -> w.getCrossTermWithoutConcentration(v, parameter))).
				reduce(new Scalar(0.0), RandomVariable::add).sqrt();

		return new BucketResult(bucketName, weightedSensitivities, k);
	}

	private RandomVariable getMarginForResidualOrNonResidual(Collection<WeightedSensitivity> singleSensitivities, Collection<BucketResult> buckets, RiskClass riskClass) {
		final RandomVariable lambda = getLambda(getTheta(singleSensitivities));
		final RandomVariable sumCvr = singleSensitivities.stream().
				map(WeightedSensitivity::getWeightedSensitivity).
				reduce(new Scalar(0.0), RandomVariable::add);

		return lambda.mult(getMargin(buckets, riskClass)).add(sumCvr).floor(0.0);
	}

	@Override
	RandomVariable getMargin(Collection<BucketResult> results, RiskClass riskClass) {
		final Map<Boolean, List<BucketResult>> bucketResultsByRes = results.stream().
				collect(Collectors.groupingBy(r -> r.getBucketName().equalsIgnoreCase("residual")));

		final Map<Boolean, List<WeightedSensitivity>> singleSensitivitiesByRes = bucketResultsByRes.entrySet().stream().
				map(e -> Pair.of(e.getKey(),
						e.getValue().stream().flatMap(f -> f.getSingleSensitivities().stream()).
						collect(Collectors.toList()))).collect(Collectors.toMap(Pair::getKey, Pair::getValue));

		return bucketResultsByRes.keySet().stream().
				map(isRes -> getMarginForResidualOrNonResidual(singleSensitivitiesByRes.get(isRes), bucketResultsByRes.get(isRes), riskClass)).
				reduce(new Scalar(0.0), RandomVariable::add);
	}
}
