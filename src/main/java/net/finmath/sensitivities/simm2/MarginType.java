package net.finmath.sensitivities.simm2;

/**
 * Defines the four types of margin of ISDA SIMM v2.0, B5 (p. 1).
 */
public enum MarginType {
	DELTA,
	VEGA,
	CURVATURE,
	/**
	 * Only applies to Credit (Qualifying).
	 */
	BASE_CORR
}
