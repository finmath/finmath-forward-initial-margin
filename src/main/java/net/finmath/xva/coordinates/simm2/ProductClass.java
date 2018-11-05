package net.finmath.xva.coordinates.simm2;

/**
 * Defines the four product classes as in ISDA SIMM v2.0, B6 (p. 1-2).
 */
public enum ProductClass {
	RATES_FX {
		@Override
		public String getCrifName() {
			return "RatesFX";
		}
	},
	CREDIT {
		@Override
		public String getCrifName() {
			return "Credit";
		}
	},
	EQUITY {
		@Override
		public String getCrifName() {
			return "Equity";
		}
	},
	COMMODITY {
		@Override
		public String getCrifName() {
			return "Commodity";
		}
	};

	/**
	 * @return Returns the product class name as used in the CRIF.
	 */
	public abstract String getCrifName();

	public static ProductClass parseCrifProductClass(String classString) {
		switch (classString.toLowerCase()) {
		case "ratesfx":
			return RATES_FX;
		case "credit":
			return CREDIT;
		case "equity":
			return EQUITY;
		case "commodity":
			return COMMODITY;
		default:
			throw new IllegalArgumentException(String.format("Unknown CRIF product class name %1$s", classString));
		}
	}
}
