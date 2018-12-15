package net.finmath.xva.initialmargin.simm2.calculation;

import net.finmath.sensitivities.simm2.RiskClass;
import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.Scalar;
import net.finmath.xva.initialmargin.simm2.specs.ParameterSet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SimmBaseScheme {
	protected final ParameterSet parameter;

	public SimmBaseScheme(ParameterSet parameter) {
		this.parameter = parameter;
	}

	/**
	 * Override this method to adjust the gradient before further calculations.
	 *
	 * @param gradient The input gradient from a {@link SimmBaseScheme#getMargin(RiskClass, Map)} call.
	 * @return Returns a stream of the gradient components used to calculate buckets.
	 */
	protected Stream<Map.Entry<SimmCoordinate, RandomVariableInterface>> streamGradient(Map<SimmCoordinate, RandomVariableInterface> gradient) {
		return gradient.entrySet().stream();
	}

	/**
	 * Calculates the result of a bucket aggregation, i. e. the figures K including the constituents' weighted sensitivities.
	 *
	 * @return A {@link BucketResult} containing the whole thing.
	 */
	protected abstract BucketResult getBucketAggregation(String bucketName, Map<SimmCoordinate, RandomVariableInterface> gradient);

	/**
	 * Calculates the resulting delta margin according to ISDA SIMM v2.0 B.8 (d)
	 *
	 * @param results A collection of per-bucket results.
	 * @return The delta margin.
	 */
	RandomVariableInterface getMargin(Collection<BucketResult> results, RiskClass riskClass) {
		return results.stream().
				flatMap(bK1 -> results.stream().
						map(bK2 -> {

							if (bK1.getBucketName().equalsIgnoreCase(bK2.getBucketName())) {
								return bK1.getK().squared();
							}
							return bK1.getS().mult(bK2.getS()).
									mult(bK1.getG(bK2)).
									mult(parameter.getCrossBucketCorrelation(riskClass, bK1.getBucketName(), bK2.getBucketName()));
						})).
				reduce(new Scalar(0.0), RandomVariableInterface::add).
				sqrt();
	}

	/**
	 * Calculates the resulting delta margin according to ISDA SIMM v2.0 B.8 (d)
	 *
	 * @param gradient A sensitivity gradient in SIMM coordinates.
	 * @return The delta margin.
	 */
	public RandomVariableInterface getMargin(RiskClass riskClass, Map<SimmCoordinate, RandomVariableInterface> gradient) {

		Set<BucketResult> bucketResults = streamGradient(gradient).
				collect(Collectors.groupingBy(e -> e.getKey().getSimmBucket(), Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))).
				entrySet().stream().
				map(bucketWS -> getBucketAggregation(bucketWS.getKey(), bucketWS.getValue())).
				collect(Collectors.toSet());

		return getMargin(bucketResults, riskClass);
	}
}
