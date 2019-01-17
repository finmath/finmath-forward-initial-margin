package net.finmath.xva.tradespecifications;

import net.finmath.sensitivities.simm2.SubCurve;

public final class Indices {
	private Indices() {
	}

	public static IRCurveSpec getLibor(String currency, String offsetCode) {
		switch (offsetCode.toUpperCase()) {
		case "3M":
			return new IRCurveSpecImpl(currency, SubCurve.Libor3m, 30.0, currency.equalsIgnoreCase("GBP") ? 365.0 : 360.0);
		case "6M":
			return new IRCurveSpecImpl(currency, SubCurve.Libor6m, 60.0, currency.equalsIgnoreCase("GBP") ? 365.0 : 360.0);
		default:
			throw new IllegalArgumentException(String.format("Unknown LIBOR period %1$s", offsetCode));
		}
	}
}
