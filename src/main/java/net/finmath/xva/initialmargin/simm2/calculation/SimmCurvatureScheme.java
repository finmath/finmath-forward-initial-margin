package net.finmath.xva.initialmargin.simm2.calculation;

import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.initialmargin.simm2.specs.ParameterSet;

import java.util.Map;

public class SimmCurvatureScheme extends SimmBaseScheme {

	public SimmCurvatureScheme(ParameterSet parameter) {
		super(parameter);
	}

	/**
	 * Calculates the result of a bucket aggregation, i. e. the figures K including the constituents' weighted sensitivities.
	 *
	 * @param bucketName
	 * @param gradient
	 * @return A {@link BucketResult} containing the whole thing.
	 */
	@Override
	protected BucketResult getBucketAggregation(String bucketName, Map<SimmCoordinate, RandomVariableInterface> gradient) {
		return null;
	}
}
