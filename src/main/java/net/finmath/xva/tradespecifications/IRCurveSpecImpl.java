package net.finmath.xva.tradespecifications;

public class IRCurveSpecImpl implements IRCurveSpec {
	private String currency;
	private String name;
	private double periodLength;
	private double daysInYear;

	public IRCurveSpecImpl(String currency, String name, double periodLength, double daysInYear) {
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
	public String getName() {
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
