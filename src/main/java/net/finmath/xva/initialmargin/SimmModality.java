package net.finmath.xva.initialmargin;

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

	public Simm2Parameter getParams() {
		return new Simm2ParameterImpl();
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
