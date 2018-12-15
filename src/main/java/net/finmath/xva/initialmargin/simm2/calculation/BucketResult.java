package net.finmath.xva.initialmargin.simm2.calculation;

import java.util.Set;

import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.Scalar;

/**
 * Holds the result of a per-bucket aggregation together with the constituents' weighted sensitivities.
 */
public class BucketResult {
	private String bucketName;
	private Set<WeightedSensitivity> singleSensitivities;
	private RandomVariableInterface aggregatedResult;

	/**
	 * @param bucketName
	 * @param singleSensitivities
	 * @param aggregatedResult The figure K_b.
	 */
	public BucketResult(String bucketName, Set<WeightedSensitivity> singleSensitivities, RandomVariableInterface aggregatedResult) {
		this.bucketName = bucketName;
		this.singleSensitivities = singleSensitivities;
		this.aggregatedResult = aggregatedResult;
	}

	public Set<WeightedSensitivity> getSingleSensitivities() {
		return singleSensitivities;
	}

	public RandomVariableInterface getK() {
		return aggregatedResult;
	}

	/**
	 * Returns the single end result per bucket used in the final margin formula.
	 * @return The figure <i>S<sub>b</sub></i> of ISDA SIMM v2.0, B.8 (d)'s formula.
	 */
	public RandomVariableInterface getS() {
		return singleSensitivities.stream().
				map(WeightedSensitivity::getWeightedSensitivity).
				reduce(new Scalar(0.0), RandomVariableInterface::add).
				cap(aggregatedResult).floor(aggregatedResult.mult(-1.0));
	}

	private RandomVariableInterface getConcentrationRiskFactor() {
		return singleSensitivities.stream().
				findFirst().map(WeightedSensitivity::getConcentrationRiskFactor).orElse(new Scalar(1.0));
	}

	/**
	 * Returns the cross-bucket scaling for the concentration risk factor as defined in ISDA SIMM v2.0, p. 3, B.8 (d).
	 * @param c The other bucket.
	 * @return The figure <i>g<sub>bc</sub></i>.
	 */
	public RandomVariableInterface getG(BucketResult c) {
		final RandomVariableInterface myConcentrationRiskFactor = getConcentrationRiskFactor();
		final RandomVariableInterface otherConcentrationRiskFactor = c.getConcentrationRiskFactor();

		return myConcentrationRiskFactor.cap(otherConcentrationRiskFactor).div(myConcentrationRiskFactor.floor(otherConcentrationRiskFactor));
	}

	public String getBucketName() {
		return bucketName;
	}
}
