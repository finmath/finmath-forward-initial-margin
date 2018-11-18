package net.finmath.xva.initialmargin.simm2.calculation;

import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.Scalar;
import net.finmath.sensitivities.simm2.RiskClass;
import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.xva.initialmargin.simm2.specs.ParameterSet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calculates the initial margin to be posted at a fixed time according to SIMM. This calculation scheme will consider the IR Delta contribution to the total margin.
 */
public class SimmIRDeltaScheme {
	private final ParameterSet parameter;

	public SimmIRDeltaScheme(ParameterSet parameter) {
		this.parameter = parameter;
	}

	/**
	 * Calculates the result of a bucket aggregation, i. e. the figures K including the constituents' weighted sensitivities.
	 *
	 * @return A {@link BucketResult} containing the whole thing.
	 */
	public BucketResult getBucketAggregation(String bucketName, Map<SimmCoordinate, RandomVariableInterface> gradient) {
		double threshold = parameter.getConcentrationThreshold(gradient.keySet().stream().
				findFirst().orElseThrow(() -> new IllegalArgumentException("Gradient is empty")));

		RandomVariableInterface concentrationRiskFactor = gradient.values().stream().
				reduce(new Scalar(0.0), RandomVariableInterface::add).abs().div(threshold).sqrt().cap(1.0);

		Set<WeightedSensitivity> weightedSensitivities = gradient.entrySet().stream().
				map(z-> new WeightedSensitivity(z.getKey(), concentrationRiskFactor, z.getValue().mult(concentrationRiskFactor).mult(parameter.getRiskWeightWithScaling(z.getKey())))).
				collect(Collectors.toSet());

		RandomVariableInterface k = weightedSensitivities.stream().
				flatMap(w -> weightedSensitivities.stream().map(v -> w.getCrossTermIR(v, parameter))).
				reduce(new Scalar(0.0), RandomVariableInterface::add).sqrt();

		return new BucketResult(bucketName, weightedSensitivities, k);
	}

	/**
	 * Calculates the resulting delta margin according to ISDA SIMM v2.0 B.8 (d)
	 *
	 * @param results A collection of per-bucket results.
	 * @return The delta margin.
	 */
	public RandomVariableInterface getMargin(Collection<BucketResult> results) {
		return results.stream().
				flatMap(bK1 -> results.stream().
						map(bK2 -> {

							if (bK1.getBucketName().equalsIgnoreCase(bK2.getBucketName())) {
								return bK1.getK().squared();
							}
							return bK1.getS().mult(bK2.getS()).
									mult(bK1.getG(bK2)).
									mult(parameter.getCrossBucketCorrelation(RiskClass.INTEREST_RATE, bK1.getBucketName(), bK2.getBucketName()));
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
	public RandomVariableInterface getMargin(Map<SimmCoordinate, RandomVariableInterface> gradient) {

		Set<BucketResult> bucketResults = gradient.entrySet().stream().
				collect(Collectors.groupingBy(e -> e.getKey().getSimmBucket(), Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))).
				entrySet().stream().
				map(bucketWS -> getBucketAggregation(bucketWS.getKey(), bucketWS.getValue())).
				collect(Collectors.toSet());

		return getMargin(bucketResults);
	}
}
