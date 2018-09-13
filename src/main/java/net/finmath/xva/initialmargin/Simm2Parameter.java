package net.finmath.xva.initialmargin;

import net.finmath.xva.coordinates.simm2.RiskClass;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

public interface Simm2Parameter {
	double getCrossBucketCorrelation(RiskClass rc, String left, String right);

	double getConcentrationThreshold(Simm2Coordinate sensitivity);

	/**
	 * Calculates the correlation for two sensitivities in the same bucket (of the same risk class).
	 * @param left The coordinate for one sensitivity.
	 * @param right The coordinate for the other sensitivity.
	 * @return A correlation for the bucket aggregation.
	 */
	double getIntraBucketCorrelation(Simm2Coordinate left, Simm2Coordinate right);

	/**
	 * Returns the risk weight for a given net sensitivity.
	 * @param sensitivity The coordinate of the net sensitivity.
	 * @return
	 */
	double getRiskWeight(Simm2Coordinate sensitivity);

	/**
	 * Returns the historical volatility ratio (HVR) for a given risk class.
	 * @param c The risk class whose HVR is requested.
	 * @return The HVR as a floating point number.
	 */
	double getHistoricalVolatilityRatio(Simm2Coordinate c);
}
