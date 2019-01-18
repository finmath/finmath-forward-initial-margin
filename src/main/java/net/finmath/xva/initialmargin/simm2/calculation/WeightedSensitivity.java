package net.finmath.xva.initialmargin.simm2.calculation;

import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.stochastic.RandomVariable;
import net.finmath.xva.initialmargin.simm2.specs.ParameterSet;

/**
 * Represents a weighted sensitivity (cf. ISDA SIMM v2.0, B.7(b), B.8(b)), remembering factors that are needed later on.
 */
public class WeightedSensitivity {
	private SimmCoordinate coordinate;
	private RandomVariable concentrationRiskFactor;
	private RandomVariable weightedSensitivity;

	public WeightedSensitivity(SimmCoordinate coordinate, RandomVariable concentrationRiskFactor, RandomVariable weightedSensitivity) {
		this.coordinate = coordinate;
		this.concentrationRiskFactor = concentrationRiskFactor;
		this.weightedSensitivity = weightedSensitivity;
	}

	/**
	 * Returns the cross-term for non-IR cross-aggregation of weighted sensitivities in the same bucket found in ISDA SIMM v2.0 p.3, B.8 (c).
	 * @param v The other weighted sensitivity.
	 * @param parameter The {@link ParameterSet} instance providing correlation parameters.
	 * @return The term of <i>ρ<sub>kl</sub> × f<sub>kl</sub> × WS<sub>k</sub> × WS<sub>l</sub></i> in the formula for <i>K</i>.
	 */
	public RandomVariable getCrossTermNonIR(WeightedSensitivity v, ParameterSet parameter) {
		return getCrossTermWithoutConcentration(v, parameter).
				mult(getConcentrationRiskFactor().cap(v.getConcentrationRiskFactor())). //numerator f
				div(getConcentrationRiskFactor().floor(v.getConcentrationRiskFactor())); //denominator f
	}

	/**
	 * Returns the cross-term for IR cross-aggregation of weighted sensitivities in the same bucket found in ISDA SIMM v2.0 p.2, B.7 (c).
	 * @param v The other weighted sensitivity.
	 * @param parameter The {@link ParameterSet} instance providing correlation parameters.
	 * @return The term of <i>φ<sub>i,j</sub> × ρ<sub>k,l</sub> × WS<sub>k,i</sub> × WS<sub>l,j</sub></i> in the formula for <i>K</i>.
	 */
	public RandomVariable getCrossTermWithoutConcentration(WeightedSensitivity v, ParameterSet parameter) {
		return getWeightedSensitivity().
				mult(v.getWeightedSensitivity()).
				mult(parameter.getIntraBucketCorrelation(getCoordinate(), v.getCoordinate())); //rho (times phi for IR)
	}

	/**
	 * @return Retrieves the concentration risk factor used for weighting the sensitivity.
	 */
	public RandomVariable getConcentrationRiskFactor() {
		return concentrationRiskFactor;
	}

	/**
	 * @return Returns the net sensitivity multiplied with the risk weight and concentration risk factor.
	 */
	public RandomVariable getWeightedSensitivity() {
		return weightedSensitivity;
	}

	public SimmCoordinate getCoordinate() {
		return coordinate;
	}
}
