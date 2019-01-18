package net.finmath.xva.initialmargin.simm2.calculation;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.finmath.sensitivities.simm2.RiskClass;
import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.xva.initialmargin.simm2.specs.ParameterSet;

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
	public BucketResult getBucketAggregation(String bucketName, Map<SimmCoordinate, RandomVariable> gradient) {
		double threshold = parameter.getConcentrationThreshold(gradient.keySet().stream().
				findFirst().orElseThrow(() -> new IllegalArgumentException("Gradient is empty")));

		RandomVariable concentrationRiskFactor = gradient.values().stream().
				reduce(new Scalar(0.0), RandomVariable::add).abs().div(threshold).sqrt().cap(1.0);

		Set<WeightedSensitivity> weightedSensitivities = gradient.entrySet().stream().
				map(z-> new WeightedSensitivity(z.getKey(), concentrationRiskFactor, z.getValue().mult(concentrationRiskFactor).mult(parameter.getRiskWeight(z.getKey())))).
				collect(Collectors.toSet());

		RandomVariable k = weightedSensitivities.stream().
				flatMap(w -> weightedSensitivities.stream().map(v -> w.getCrossTermWithoutConcentration(v, parameter))).
				reduce(new Scalar(0.0), RandomVariable::add).sqrt();

		return new BucketResult(bucketName, weightedSensitivities, k);
	}

	public RandomVariable getMargin(Map<SimmCoordinate, RandomVariable> gradient) {
		return getMargin(RiskClass.INTEREST_RATE, gradient);
	}
}
