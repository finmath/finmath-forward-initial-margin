package net.finmath.xva.initialmargin;

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

	public RandomVariableInterface getK() {
		return aggregatedResult;
	}

	/**
	 * Returns the single end result per bucket used in the final margin formula.
	 * @return The figure S_b of ISDA SIMM v2.0, B.8 (d)'s formula.
	 */
	public RandomVariableInterface getS() {
		return singleSensitivities.stream().
				map(WeightedSensitivity::getWeightedSensitivity).
				reduce(new Scalar(0.0), RandomVariableInterface::add).
				cap(aggregatedResult).floor(aggregatedResult.mult(-1.0));
	}

	public String getBucketName() {
		return bucketName;
	}
}
