package net.finmath.xva.initialmargin.simm2.calculation;

import net.finmath.sensitivities.simm2.RiskClass;
import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.Scalar;
import net.finmath.xva.initialmargin.simm2.specs.ParameterSet;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *  This calculation scheme will consider the IR Delta or Vega contribution to the total margin.
 */
public class SimmIRScheme extends SimmBaseScheme {

	public SimmIRScheme(ParameterSet parameter) {
		super(parameter);
	}

	/**
	 * @param bucketName A string containing the bucket name (i. e. currency).
	 * @param gradient The gradient of all sensitivities.
	 * @return Returns the {@link BucketResult} for this bucket (i. e. currency).
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
				flatMap(w -> weightedSensitivities.stream().map(v -> w.getCrossTermWithoutConcentration(v, parameter))).
				reduce(new Scalar(0.0), RandomVariableInterface::add).sqrt();

		return new BucketResult(bucketName, weightedSensitivities, k);
	}

	public RandomVariableInterface getMargin(Map<SimmCoordinate, RandomVariableInterface> gradient) {
		return getMargin(RiskClass.INTEREST_RATE, gradient);
	}
}
