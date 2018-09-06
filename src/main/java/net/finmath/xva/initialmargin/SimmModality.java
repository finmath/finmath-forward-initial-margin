package net.finmath.xva.initialmargin;

/**
 * Determines the modality of the initial margin posting.
 */
public class SimmModality {
	private final SIMMParameter parameterSet;
	private final String calculationCurrency;
	private final double postingThreshold;

	public SimmModality(SIMMParameter parameterSet, String calculationCurrency, double postingThreshold) {
		this.parameterSet = parameterSet;
		this.calculationCurrency = calculationCurrency;
		this.postingThreshold = postingThreshold;
	}

	public SIMMParameter getParameterSet() {
		return parameterSet;
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
