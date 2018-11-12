package net.finmath.sensitivities.simm2;

/**
 * Defines the six risk classes as in ISDA SIMM v2.0, B5 (p. 2).
 */
public enum RiskClass {
	INTEREST_RATE,
	CREDIT_Q,
	CREDIT_NON_Q,
	EQUITY,
	COMMODITY,
	FX
}
