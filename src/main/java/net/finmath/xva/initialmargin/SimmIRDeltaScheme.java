package net.finmath.xva.initialmargin;

import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.Scalar;
import net.finmath.xva.coordinates.simm2.RiskClass;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calculates the initial margin to be posted at a fixed time according to SIMM. This calculation scheme will consider the IR Delta contribution to the total margin.
 */
public class SimmIRDeltaScheme {
	private final SimmModality modality;

	public SimmIRDeltaScheme(SimmModality modality) {
		this.modality = modality;
	}

	/**
	 * Calculates the result of a bucket aggregation, i. e. the figures K including the constituents' weighted sensitivities.
	 *
	 * @return A {@link BucketResult} containing the whole thing.
	 */
	public BucketResult getBucketAggregation(String bucketName, Map<Simm2Coordinate, RandomVariableInterface> gradient) {
		double threshold = modality.getParams().getConcentrationThreshold(gradient.keySet().stream().
				findFirst().orElseThrow(() -> new IllegalArgumentException("Gradient is empty")));

		RandomVariableInterface concentrationRiskFactor = gradient.values().stream().
				reduce(new Scalar(0.0), RandomVariableInterface::add).abs().div(threshold).sqrt().cap(1.0);

		Set<WeightedSensitivity> weightedSensitivities = gradient.entrySet().stream().
				map(z-> new WeightedSensitivity(z.getKey(), concentrationRiskFactor, z.getValue().mult(concentrationRiskFactor).mult(modality.getParams().getRiskWeight(z.getKey())))).
				collect(Collectors.toSet());

		RandomVariableInterface k = weightedSensitivities.stream().
				flatMap(w -> weightedSensitivities.stream().map(v -> w.getCrossTermIR(v, modality))).
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
									mult(modality.getParams().getCrossBucketCorrelation(RiskClass.INTEREST_RATE, bK1.getBucketName(), bK2.getBucketName()));
						})).
				reduce(new Scalar(0.0), RandomVariableInterface::add).
				sqrt();
	}

	public RandomVariableInterface getValue(Map<Simm2Coordinate, RandomVariableInterface> gradient) {

		Set<BucketResult> bucketResults = gradient.entrySet().stream().
				collect(Collectors.groupingBy(e -> e.getKey().getBucketKey(), Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))).
				entrySet().stream().
				map(bucketWS -> getBucketAggregation(bucketWS.getKey(), bucketWS.getValue())).
				collect(Collectors.toSet());

		return getMargin(bucketResults);
	}
}
