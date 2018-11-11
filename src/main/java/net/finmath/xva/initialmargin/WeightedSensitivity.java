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

	/**
	 * Returns the cross-term for non-IR cross-aggregation of weighted sensitivities in the same bucket found in ISDA SIMM v2.0 p.3, B.8 (c).
	 * @param v The other weighted sensitivity.
	 * @param modality The {@link SimmModality} instance providing correlation parameters.
	 * @return The term of <i>ρ<sub>kl</sub> × f<sub>kl</sub> × WS<sub>k</sub> × WS<sub>l</sub></i> in the formula for <i>K</i>.
	 */
	public RandomVariableInterface getCrossTermNonIR(WeightedSensitivity v, SimmModality modality) {
		return getCrossTermIR(v, modality).
			mult(getConcentrationRiskFactor().cap(v.getConcentrationRiskFactor())). //numerator f
			div(getConcentrationRiskFactor().floor(v.getConcentrationRiskFactor())); //denominator f
	}

	/**
	 * Returns the cross-term for IR cross-aggregation of weighted sensitivities in the same bucket found in ISDA SIMM v2.0 p.2, B.7 (c).
	 * @param v The other weighted sensitivity.
	 * @param modality The {@link SimmModality} instance providing correlation parameters.
	 * @return The term of <i>φ<sub>i,j</sub> × ρ<sub>k,l</sub> × WS<sub>k,i</sub> × WS<sub>l,j</sub></i> in the formula for <i>K</i>.
	 */
	public RandomVariableInterface getCrossTermIR(WeightedSensitivity v, SimmModality modality) {
		return getWeightedSensitivity().
				mult(v.getWeightedSensitivity()).
				mult(modality.getParams().getIntraBucketCorrelation(getCoordinate(), v.getCoordinate())); //rho (times phi for IR)
	}

	/**
	 * @return Retrieves the concentration risk factor used for weighting the sensitivity.
	 */
	public RandomVariableInterface getConcentrationRiskFactor() {
		return concentrationRiskFactor;
	}

	/**
	 * @return Returns the net sensitivity multiplied with the risk weight and concentration risk factor.
	 */
	public RandomVariableInterface getWeightedSensitivity() {
		return weightedSensitivity;
	}

	public Simm2Coordinate getCoordinate() {
		return coordinate;
	}
}
