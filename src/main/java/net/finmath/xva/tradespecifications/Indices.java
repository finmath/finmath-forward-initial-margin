package net.finmath.xva.tradespecifications;

public final class Indices {
	private Indices() {
	}

	public static IRCurveSpec getLibor(String currency, String offsetCode) {
		switch (offsetCode.toUpperCase()) {
			case "3M":
				return new IRCurveSpecImpl(currency, "Libor3M", 30.0, currency.equalsIgnoreCase("GBP") ? 365.0 : 360.0);
			case "6M":
				return new IRCurveSpecImpl(currency, "Libor6M", 60.0, currency.equalsIgnoreCase("GBP") ? 365.0 : 360.0);
			default:
				throw new IllegalArgumentException(String.format("Unknown LIBOR period %1$s", offsetCode));
		}
	}
}
