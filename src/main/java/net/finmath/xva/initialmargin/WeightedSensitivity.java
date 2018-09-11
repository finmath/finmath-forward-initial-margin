package net.finmath.xva.initialmargin;

import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

/**
 * Represents a weighted sensitivity (cf. ISDA SIMM v2.0, B.7(b), B.8(b)), remembering factors that are needed later on.
 */
public class WeightedSensitivity {
	private Simm2Coordinate coordinate;
	private RandomVariableInterface concentrationRiskFactor;
	private RandomVariableInterface weightedSensitivity;

	public WeightedSensitivity(Simm2Coordinate coordinate, RandomVariableInterface concentrationRiskFactor, RandomVariableInterface weightedSensitivity) {
		this.coordinate = coordinate;
		this.concentrationRiskFactor = concentrationRiskFactor;
		this.weightedSensitivity = weightedSensitivity;
	}

	public RandomVariableInterface getCrossTerm(WeightedSensitivity v, SimmModality modality) {
		return getWeightedSensitivity().
			mult(v.getWeightedSensitivity()).
			mult(getConcentrationRiskFactor().cap(v.getConcentrationRiskFactor())). //numerator f
			div(getConcentrationRiskFactor().floor(v.getConcentrationRiskFactor())). //denominator f
			mult(modality.getParams().getIntraBucketCorrelation(getCoordinate(), v.getCoordinate())); //rho
	}

	public RandomVariableInterface getConcentrationRiskFactor() {
		return concentrationRiskFactor;
	}

	public RandomVariableInterface getWeightedSensitivity() {
		return weightedSensitivity;
	}

	public Simm2Coordinate getCoordinate() {
		return coordinate;
	}
}
