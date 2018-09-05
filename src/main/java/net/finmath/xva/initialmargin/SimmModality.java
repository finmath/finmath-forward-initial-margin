package net.finmath.xva.initialmargin;

/**
 * Determines the modality of the initial margin exchange.
 */
public class SimmModality {
	private final SIMMParameter parameterSet;
	private final String calculationCurrency;
	private final double thresholdAmount;

	public SimmModality(SIMMParameter parameterSet, String calculationCurrency, double thresholdAmount) {
		this.parameterSet = parameterSet;
		this.calculationCurrency = calculationCurrency;
		this.thresholdAmount = thresholdAmount;
	}

	public SIMMParameter getParameterSet() {
		return parameterSet;
	}

	public String getCalculationCurrency() {
		return calculationCurrency;
	}

	public double getThresholdAmount() {
		return thresholdAmount;
	}
}
