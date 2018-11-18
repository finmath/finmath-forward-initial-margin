package net.finmath.xva.initialmargin.simm2.specs;

import net.finmath.sensitivities.simm2.RiskClass;
import net.finmath.sensitivities.simm2.SimmCoordinate;

public interface ParameterSet {
	double getCrossBucketCorrelation(RiskClass rc, String left, String right);

	double getConcentrationThreshold(SimmCoordinate sensitivity);

	/**
	 * Calculates the correlation for two sensitivities in the same bucket (of the same risk class).
	 * @param left The coordinate for one sensitivity.
	 * @param right The coordinate for the other sensitivity.
	 * @return A correlation for the bucket aggregation.
	 */
	double getIntraBucketCorrelation(SimmCoordinate left, SimmCoordinate right);

	/**
	 * Returns the risk weight for a given net sensitivity.
	 * @param sensitivity The coordinate of the net sensitivity.
	 * @return
	 */
	double getRiskWeightWithScaling(SimmCoordinate sensitivity);

	/**
	 * Returns the historical volatility ratio (HVR) for a given risk class.
	 * @param c The risk class whose HVR is requested.
	 * @return The HVR as a floating point number.
	 */
	double getHistoricalVolatilityRatio(SimmCoordinate c);
}
