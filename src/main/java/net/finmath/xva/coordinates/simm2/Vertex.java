package net.finmath.xva.coordinates.simm2;

/**
 * Defines the vertices from ISDA SIMM v2.0, C.1.14 (interest rate) and C.1.15/16 (credit).
 */
public enum Vertex {
	W2 {
		@Override
		public String getCrifTenor() {
			return "2w";
		}
	},
	M1 {
		@Override
		public String getCrifTenor() {
			return "1m";
		}
	},
	M3 {
		@Override
		public String getCrifTenor() {
			return "3m";
		}
	},
	M6 {
		@Override
		public String getCrifTenor() {
			return "6m";
		}
	},
	Y1 {
		@Override
		public String getCrifTenor() {
			return "1y";
		}
	},
	Y2 {
		@Override
		public String getCrifTenor() {
			return "2y";
		}
	},
	Y3 {
		@Override
		public String getCrifTenor() {
			return "3y";
		}
	},
	Y5 {
		@Override
		public String getCrifTenor() {
			return "5y";
		}
	},
	Y10 {
		@Override
		public String getCrifTenor() {
			return "10y";
		}
	},
	Y15 {
		@Override
		public String getCrifTenor() {
			return "15y";
		}
	},
	Y20 {
		@Override
		public String getCrifTenor() {
			return "20y";
		}
	},
	Y30 {
		@Override
		public String getCrifTenor() {
			return "30y";
		}
	};

	/**
	 * @return Returns the name of the tenor in the CRIF.
	 */
	public abstract String getCrifTenor();

	public static Vertex parseCrifTenor(String tenor) {
		switch (tenor.toLowerCase().trim()) {
			case "2w":
				return W2;
			case "1m":
				return M1;
			case "3m":
				return M3;
			case "6m":
				return M6;
			case "1y":
				return Y1;
			case "2y":
				return Y2;
			case "3y":
				return Y3;
			case "5y":
				return Y5;
			case "10y":
				return Y10;
			case "15y":
				return Y15;
			case "20y":
				return Y20;
			case "30y":
				return Y30;
			default:
				throw new IllegalArgumentException(String.format("Unknown CRIF tenor %1$s", tenor));
		}
	}
}
