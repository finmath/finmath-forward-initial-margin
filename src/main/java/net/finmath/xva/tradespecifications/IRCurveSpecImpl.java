package net.finmath.xva.tradespecifications;

import net.finmath.sensitivities.simm2.SubCurve;

public class IRCurveSpecImpl implements IRCurveSpec {
	private String currency;
	private SubCurve name;
	private double periodLength;
	private double daysInYear;

	public IRCurveSpecImpl(String currency, SubCurve name, double periodLength, double daysInYear) {
		this.currency = currency;
		this.name = name;
		this.periodLength = periodLength;
		this.daysInYear = daysInYear;
	}

	@Override
	public String getCurrency() {
		return currency;
	}

	@Override
	public SubCurve getName() {
		return name;
	}

	@Override
	public double getPeriodLength() {
		return periodLength;
	}

	@Override
	public double getDayInYears() {
		return daysInYear;
	}
}
