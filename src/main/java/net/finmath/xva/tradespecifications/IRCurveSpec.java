package net.finmath.xva.tradespecifications;

import net.finmath.sensitivities.simm2.SubCurve;

/**
 * Holds the details of an interest rate curve.
 */
public interface IRCurveSpec {
	String getCurrency();

	SubCurve getName();

	double getPeriodLength();

	double getDayInYears();
}
