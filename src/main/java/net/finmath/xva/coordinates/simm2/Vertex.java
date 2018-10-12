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

		@Override
		public double getIdealizedYcf() {
			return 0.5 / 12.0;
		}
	},
	M1 {
		@Override
		public String getCrifTenor() {
			return "1m";
		}

		@Override
		public double getIdealizedYcf() {
			return 1.0 / 12.0;
		}
	},
	M3 {
		@Override
		public String getCrifTenor() {
			return "3m";
		}

		@Override
		public double getIdealizedYcf() {
			return 0.25;
		}
	},
	M6 {
		@Override
		public String getCrifTenor() {
			return "6m";
		}

		@Override
		public double getIdealizedYcf() {
			return 0.5;
		}
	},
	Y1 {
		@Override
		public String getCrifTenor() {
			return "1y";
		}

		@Override
		public double getIdealizedYcf() {
			return 1.0;
		}
	},
	Y2 {
		@Override
		public String getCrifTenor() {
			return "2y";
		}

		@Override
		public double getIdealizedYcf() {
			return 2.0;
		}
	},
	Y3 {
		@Override
		public String getCrifTenor() {
			return "3y";
		}

		@Override
		public double getIdealizedYcf() {
			return 3.0;
		}
	},
	Y5 {
		@Override
		public String getCrifTenor() {
			return "5y";
		}

		@Override
		public double getIdealizedYcf() {
			return 5.0;
		}
	},
	Y10 {
		@Override
		public String getCrifTenor() {
			return "10y";
		}

		@Override
		public double getIdealizedYcf() {
			return 10.0;
		}
	},
	Y15 {
		@Override
		public String getCrifTenor() {
			return "15y";
		}

		@Override
		public double getIdealizedYcf() {
			return 15.0;
		}
	},
	Y20 {
		@Override
		public String getCrifTenor() {
			return "20y";
		}

		@Override
		public double getIdealizedYcf() {
			return 20.0;
		}
	},
	Y30 {
		@Override
		public String getCrifTenor() {
			return "30y";
		}

		@Override
		public double getIdealizedYcf() {
			return 30.0;
		}
	};

	/**
	 * @return Returns the name of the tenor in the CRIF.
	 */
	public abstract String getCrifTenor();

	public abstract double getIdealizedYcf();

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
