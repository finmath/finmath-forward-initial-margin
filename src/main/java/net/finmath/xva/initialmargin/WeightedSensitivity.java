package net.finmath.xva.initialmargin;

import net.finmath.stochastic.RandomVariableInterface;

/**
 * Represents a weighted sensitivity (cf. ISDA SIMM v2.0, B.7(b), B.8(b)), remembering factors that are needed later on.
 */
public class WeightedSensitivity {
	private RandomVariableInterface concentrationRiskFactor;
	private RandomVariableInterface weightedSensitivity;

	public WeightedSensitivity(RandomVariableInterface concentrationRiskFactor, RandomVariableInterface weightedSensitivity) {
		this.concentrationRiskFactor = concentrationRiskFactor;
		this.weightedSensitivity = weightedSensitivity;
	}

	public RandomVariableInterface getConcentrationRiskFactor() {
		return concentrationRiskFactor;
	}

	public RandomVariableInterface getWeightedSensitivity() {
		return weightedSensitivity;
	}
}
