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
	 * @return The risk weight as floating point number.
	 */
	double getRiskWeight(SimmCoordinate sensitivity);

	/**
	 * Returns the weight for a sensitivity not included in the risk weight. This will be included in the concentration risk factor.
	 * @param coordinate The coordinate of the net sensitivity.
	 * @return The additional weight as a floating point number.
	 */
	double getAdditionalWeight(SimmCoordinate coordinate);

	/**
	 * Returns the correlations between risk classes for the aggregation of initial margins in a single product class.
	 * @param left The risk class of one initial margin.
	 * @param right The risk class of another initial margin.
	 * @return The correlation between the two risk classes.
	 */
	double getRiskClassCorrelation(RiskClass left, RiskClass right);
}
