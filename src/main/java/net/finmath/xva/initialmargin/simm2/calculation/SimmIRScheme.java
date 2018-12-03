package net.finmath.xva.initialmargin.simm2.calculation;

import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.Scalar;
import net.finmath.sensitivities.simm2.RiskClass;
import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.xva.initialmargin.simm2.specs.ParameterSet;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calculates the initial margin to be posted at a fixed time according to SIMM. This calculation scheme will consider the IR Delta contribution to the total margin.
 */
public class SimmIRScheme extends SimmBaseScheme {

	public SimmIRScheme(ParameterSet parameter) {
		super(parameter);
	}

	/**
	 * Calculates the result of a bucket aggregation, i. e. the figures K including the constituents' weighted sensitivities.
	 *
	 * @return A {@link BucketResult} containing the whole thing.
	 */
	@Override
	public BucketResult getBucketAggregation(String bucketName, Map<SimmCoordinate, RandomVariableInterface> gradient) {
		double threshold = parameter.getConcentrationThreshold(gradient.keySet().stream().
				findFirst().orElseThrow(() -> new IllegalArgumentException("Gradient is empty")));

		RandomVariableInterface concentrationRiskFactor = gradient.values().stream().
				reduce(new Scalar(0.0), RandomVariableInterface::add).abs().div(threshold).sqrt().cap(1.0);

		Set<WeightedSensitivity> weightedSensitivities = gradient.entrySet().stream().
				map(z-> new WeightedSensitivity(z.getKey(), concentrationRiskFactor, z.getValue().mult(concentrationRiskFactor).mult(parameter.getRiskWeight(z.getKey())))).
				collect(Collectors.toSet());

		RandomVariableInterface k = weightedSensitivities.stream().
				flatMap(w -> weightedSensitivities.stream().map(v -> w.getCrossTermIR(v, parameter))).
				reduce(new Scalar(0.0), RandomVariableInterface::add).sqrt();

		return new BucketResult(bucketName, weightedSensitivities, k);
	}

	public RandomVariableInterface getMargin(Map<SimmCoordinate, RandomVariableInterface> gradient) {
		return getMargin(RiskClass.INTEREST_RATE, gradient);
	}
}
