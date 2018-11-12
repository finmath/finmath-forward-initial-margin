package net.finmath.xva.initialmargin.simm2;

import net.finmath.xva.initialmargin.simm2.specs.ParameterSet;
import net.finmath.xva.initialmargin.simm2.specs.Simm2_0;

/**
 * Determines the modality of the initial margin posting.
 */
public class SimmModality {
	private final String calculationCurrency;
	private final double postingThreshold;

	public SimmModality(String calculationCurrency, double postingThreshold) {
		this.calculationCurrency = calculationCurrency;
		this.postingThreshold = postingThreshold;
	}

	public ParameterSet getParams() {
		return new Simm2_0();
	}

	public String getCalculationCurrency() {
		return calculationCurrency;
	}

	/**
	 * @return Returns the threshold below which no initial margin will be posted.
	 */
	public double getPostingThreshold() {
		return postingThreshold;
	}
}
