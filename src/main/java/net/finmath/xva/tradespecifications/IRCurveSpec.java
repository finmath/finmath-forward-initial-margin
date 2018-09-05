package net.finmath.xva.tradespecifications;

/**
 * Holds the details of an interest rate curve.
 */
public interface IRCurveSpec {
	String getCurrency();

	String getName();

	double getPeriodLength();

	double getDayInYears();
}
