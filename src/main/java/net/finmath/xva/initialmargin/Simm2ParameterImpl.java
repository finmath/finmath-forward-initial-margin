package net.finmath.xva.initialmargin;

import net.finmath.xva.coordinates.simm2.RiskClass;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Simm2ParameterImpl implements Simm2Parameter {
	private static final String RESIDUAL_BUCKET = "Residual";
	private static final double[] EQUITY_INTRA_BUCKET_CORRELATIONS = {0.14, 0.2, 0.19, 0.21, 0.24, 0.35, 0.34, 0.34, 0.2, 0.24, 0.62, 0.62, 0.0};
	private static final double[][] EQUITY_CROSS_BUCKET_CORRELATIONS = {
			{1.00, 0.15, 0.14, 0.16, 0.10, 0.12, 0.10, 0.11, 0.13, 0.09, 0.17, 0.17},
			{0.15, 1.00, 0.16, 0.17, 0.10, 0.11, 0.10, 0.11, 0.14, 0.09, 0.17, 0.17},
			{0.14, 0.16, 1.00, 0.19, 0.14, 0.17, 0.18, 0.17, 0.16, 0.14, 0.25, 0.25},
			{0.16, 0.17, 0.19, 1.00, 0.15, 0.18, 0.18, 0.18, 0.18, 0.14, 0.28, 0.28},
			{0.10, 0.10, 0.14, 0.15, 1.00, 0.28, 0.23, 0.27, 0.13, 0.21, 0.35, 0.35},
			{0.12, 0.11, 0.17, 0.18, 0.28, 1.00, 0.30, 0.34, 0.16, 0.26, 0.45, 0.45},
			{0.10, 0.10, 0.18, 0.18, 0.23, 0.30, 1.00, 0.29, 0.15, 0.24, 0.41, 0.41},
			{0.11, 0.11, 0.17, 0.18, 0.27, 0.34, 0.29, 1.00, 0.16, 0.26, 0.44, 0.44},
			{0.13, 0.14, 0.16, 0.18, 0.13, 0.16, 0.15, 0.16, 1.00, 0.13, 0.24, 0.24},
			{0.09, 0.09, 0.14, 0.14, 0.21, 0.26, 0.24, 0.26, 0.13, 1.00, 0.33, 0.33},
			{0.17, 0.17, 0.25, 0.28, 0.35, 0.45, 0.41, 0.44, 0.24, 0.33, 1.00, 0.62},
			{0.17, 0.17, 0.25, 0.28, 0.35, 0.45, 0.41, 0.44, 0.24, 0.33, 0.62, 1.00}
	};
	private static final double[] EQUITY_DELTA_RISK_WEIGHTS = {25.0, 32.0, 29.0, 27.0, 18.0, 21.0, 25.0, 22.0, 27.0, 29.0, 16.0, 16.0, 32.0};
	private static final double[] EQUITY_DELTA_THRESHOLDS = {3300000.0, 3300000.0, 3300000.0, 3300000.0, 3.0E7, 3.0E7, 3.0E7, 3.0E7, 600000.0, 2300000.0, 9.0E8, 9.0E8, 600000.0};
	private static final double[] EQUITY_VEGA_THRESHOLDS = {800E6, 800E6, 800E6, 800E6, 7300E6, 7300E6, 7300E6, 7300E6, 70E6, 300E6, 21000E6, 21000E6, 70};
	private static final double[] COMMODITY_INTRA_BUCKET_CORRELATIONS = {0.3, 0.97, 0.93, 0.98, 0.99, 0.92, 1.0, 0.58, 1.0, 0.1, 0.55, 0.64, 0.71, 0.22, 0.29, 0.0, 0.21};
	private static final double[][] COMMODITY_CROSS_BUCKET_CORRELATIONS = {
			{0.00, 0.18, 0.15, 0.20, 0.25, 0.08, 0.19, 0.01, 0.27, 0.00, 0.15, 0.02, 0.06, 0.07, -0.04, 0.00, 0.06},
			{0.18, 0.00, 0.89, 0.94, 0.93, 0.32, 0.22, 0.27, 0.24, 0.09, 0.45, 0.21, 0.32, 0.28, 0.17, 0.00, 0.37},
			{0.15, 0.89, 0.00, 0.87, 0.88, 0.25, 0.16, 0.19, 0.12, 0.10, 0.26, -0.01, 0.19, 0.17, 0.10, 0.00, 0.27},
			{0.20, 0.94, 0.87, 0.00, 0.92, 0.29, 0.22, 0.26, 0.19, 0.00, 0.32, 0.05, 0.20, 0.22, 0.13, 0.00, 0.28},
			{0.25, 0.93, 0.88, 0.92, 0.00, 0.30, 0.26, 0.22, 0.28, 0.12, 0.42, 0.23, 0.28, 0.29, 0.17, 0.00, 0.34},
			{0.08, 0.32, 0.25, 0.29, 0.30, 0.00, 0.13, 0.57, 0.05, 0.14, 0.15, -0.02, 0.13, 0.17, 0.01, 0.00, 0.26},
			{0.19, 0.22, 0.16, 0.22, 0.26, 0.13, 0.00, 0.07, 0.80, 0.19, 0.16, 0.05, 0.17, 0.18, 0.00, 0.00, 0.18},
			{0.01, 0.27, 0.19, 0.26, 0.22, 0.57, 0.07, 0.00, 0.13, 0.06, 0.16, 0.03, 0.10, 0.12, 0.06, 0.00, 0.23},
			{0.27, 0.24, 0.12, 0.19, 0.28, 0.05, 0.80, 0.13, 0.00, 0.15, 0.17, 0.05, 0.15, 0.13, -0.03, 0.00, 0.13},
			{0.00, 0.09, 0.10, 0.00, 0.12, 0.14, 0.19, 0.06, 0.15, 0.00, 0.07, 0.07, 0.17, 0.10, 0.02, 0.00, 0.11},
			{0.15, 0.45, 0.26, 0.32, 0.42, 0.15, 0.16, 0.16, 0.17, 0.07, 0.00, 0.34, 0.20, 0.21, 0.16, 0.00, 0.27},
			{0.02, 0.21, -0.01, 0.05, 0.23, -0.02, 0.05, 0.03, 0.05, 0.07, 0.34, 0.00, 0.17, 0.26, 0.11, 0.00, 0.14},
			{0.06, 0.32, 0.19, 0.20, 0.28, 0.13, 0.17, 0.10, 0.15, 0.17, 0.20, 0.17, 0.00, 0.35, 0.09, 0.00, 0.22},
			{0.07, 0.28, 0.17, 0.22, 0.29, 0.17, 0.18, 0.12, 0.13, 0.10, 0.21, 0.26, 0.35, 0.00, 0.06, 0.00, 0.20},
			{-0.04, 0.17, 0.10, 0.13, 0.17, 0.01, 0.00, 0.06, -0.03, 0.02, 0.16, 0.11, 0.09, 0.06, 0.00, 0.00, 0.16},
			{0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00},
			{0.06, 0.37, 0.27, 0.28, 0.34, 0.26, 0.18, 0.23, 0.13, 0.11, 0.27, 0.14, 0.22, 0.20, 0.16, 0.00, 0.00}
	};
	private static final double[] COMMODITY_DELTA_RISK_WEIGHTS = {19.0, 20.0, 17.0, 18.0, 24.0, 20.0, 24.0, 41.0, 25.0, 91.0, 20.0, 19.0, 16.0, 15.0, 10.0, 91.0, 17.0};
	private static final double[] COMMODITY_DELTA_THRESHOLDS = {1.4E9, 2.0E10, 3.5E9, 3.5E9, 3.5E9, 6.4E9, 6.4E9, 2.5E9, 2.5E9, 3.0E8, 2.9E9, 7.6E9, 3.9E9, 3.9E9, 3.9E9, 3.0E8, 1.2E10};
	private static final double[] COMMODITY_VEGA_THRESHOLDS = {250E6, 2000E6, 510E6, 510E6, 510E6, 1900E6, 1900E6, 870E6, 870E6, 220E6, 450E6, 740E6, 370E6, 370E6, 370E6, 220E6, 430E6};
	private static final double[] CREDIT_Q_DELTA_RISK_WEIGHTS = {85.0, 85.0, 73.0, 49.0, 48.0, 43.0, 161.0, 238.0, 151.0, 210.0, 141.0, 102.0, 238.0};
	private static final double[] CREDIT_Q_DELTA_THRESHOLDS = {950000.0,290000.0,290000.0,290000.0,290000.0,290000.0,950000.0,290000.0,290000.0,290000.0,290000.0,290000.0,290000.0};
	private static final double[][] CREDIT_Q_CROSS_BUCKET_CORRELATIONS = {
			{0.00, 0.42, 0.39, 0.39, 0.40, 0.38, 0.39, 0.34, 0.37, 0.39, 0.37, 0.31},
			{0.42, 0.00, 0.44, 0.45, 0.47, 0.45, 0.33, 0.40, 0.41, 0.44, 0.43, 0.37},
			{0.39, 0.44, 0.00, 0.43, 0.45, 0.43, 0.32, 0.35, 0.41, 0.42, 0.40, 0.36},
			{0.39, 0.45, 0.43, 0.00, 0.47, 0.44, 0.30, 0.34, 0.39, 0.43, 0.39, 0.36},
			{0.40, 0.47, 0.45, 0.47, 0.00, 0.47, 0.31, 0.35, 0.40, 0.44, 0.42, 0.37},
			{0.38, 0.45, 0.43, 0.44, 0.47, 0.00, 0.30, 0.34, 0.38, 0.40, 0.39, 0.38},
			{0.39, 0.33, 0.32, 0.30, 0.31, 0.30, 0.00, 0.28, 0.31, 0.31, 0.30, 0.26},
			{0.34, 0.40, 0.35, 0.34, 0.35, 0.34, 0.28, 0.00, 0.34, 0.35, 0.33, 0.30},
			{0.37, 0.41, 0.41, 0.39, 0.40, 0.38, 0.31, 0.34, 0.00, 0.40, 0.37, 0.32},
			{0.39, 0.44, 0.42, 0.43, 0.44, 0.40, 0.31, 0.35, 0.40, 0.00, 0.40, 0.35},
			{0.37, 0.43, 0.40, 0.39, 0.42, 0.39, 0.30, 0.33, 0.37, 0.40, 0.00, 0.34},
			{0.31, 0.37, 0.36, 0.36, 0.37, 0.38, 0.26, 0.30, 0.32, 0.35, 0.34, 0.00}
	};
	private static final double[] CREDIT_NON_Q_DELTA_RISK_WEIGHTS = {140.0, 2000.0, 2000.0};
	private static final double[] CREDIT_NON_Q_DELTA_THRESHOLDS = {9500000.0,500000.0,500000.0};
	private static final Set<String> FX_CATEGORY_1 = new HashSet<>(Arrays.asList("USD", "EUR", "JPY", "GBP", "AUD", "CHF", "CAD"));
	private static final Set<String> FX_CATEGORY_2 = new HashSet<>(Arrays.asList("BRL", "CNY", "HKD", "INR", "KRW", "MXN", "NOK", "NZD", "RUB", "SEK", "SGD", "TRY", "ZAR"));

	@Override
	public double getCrossBucketCorrelation(RiskClass rc, String left, String right) {
		int i = Integer.parseInt(left);
		int j = Integer.parseInt(right);

		switch (rc) {
			case FX:
				return 1.0;
			case EQUITY:
				return EQUITY_CROSS_BUCKET_CORRELATIONS[i][j];
			case COMMODITY:
				return COMMODITY_CROSS_BUCKET_CORRELATIONS[i][j];
			case CREDIT_Q:
				return CREDIT_Q_CROSS_BUCKET_CORRELATIONS[i][j];
			case CREDIT_NON_Q:
				return 0.21;
			case INTEREST_RATE:
				return 1;
			default:
				throw new UnsupportedOperationException("Cannot calculate cross-bucket IR correlation yet.");
		}
	}

	@Override
	public double getConcentrationThreshold(Simm2Coordinate sensitivity) {
		switch (sensitivity.getRiskType()) {
			case DELTA:
				return getDeltaConcentrationThreshold(sensitivity);
			case VEGA:
				return getVegaConcentrationThreshold(sensitivity);
			default:
				throw new UnsupportedOperationException("Non-delta CR cannot be calculated yet");
		}
	}

	private double getVegaConcentrationThreshold(Simm2Coordinate coordinate) {
		switch (coordinate.getRiskClass()) {
			case COMMODITY:
				return getBucketwiseValue(coordinate, COMMODITY_VEGA_THRESHOLDS);
			case CREDIT_NON_Q:
				return 65E6;
			case CREDIT_Q:
				return 290E6;
			case EQUITY:
				return getBucketwiseValue(coordinate, EQUITY_VEGA_THRESHOLDS);
			default:
				throw new UnsupportedOperationException("Vega concentration threshold for IR/FX not available yet");

		}
	}

	private double getDeltaConcentrationThreshold(Simm2Coordinate coordinate) {
		switch (coordinate.getRiskClass()) {
			case FX:
				if (FX_CATEGORY_1.contains(coordinate.getQualifier().getCurrency())) {
					return 8400E6;
				}
				if (FX_CATEGORY_2.contains(coordinate.getQualifier().getCurrency())) {
					return 1900E6;
				}
				return 560E6;
			case EQUITY:
				return getBucketwiseValue(coordinate, EQUITY_DELTA_THRESHOLDS);
			case COMMODITY:
				return getBucketwiseValue(coordinate, COMMODITY_DELTA_THRESHOLDS);
			case CREDIT_Q:
				return getBucketwiseValue(coordinate, CREDIT_Q_DELTA_THRESHOLDS);
			case CREDIT_NON_Q:
				return getBucketwiseValue(coordinate, CREDIT_NON_Q_DELTA_THRESHOLDS);
			default:
				throw new UnsupportedOperationException("Delta concentration threshold for IR not available yet.");
		}
	}

	@Override
	public double getIntraBucketCorrelation(Simm2Coordinate left, Simm2Coordinate right) {
		switch (left.getRiskClass()) {
			case FX:
				return 0.5;
			case EQUITY:
				return getBucketwiseValue(left, EQUITY_INTRA_BUCKET_CORRELATIONS);
			case COMMODITY:
				return getBucketwiseValue(left, COMMODITY_INTRA_BUCKET_CORRELATIONS);
			case CREDIT_Q:
			case CREDIT_NON_Q:
				if (left.getBucketKey().equalsIgnoreCase(RESIDUAL_BUCKET)) {
					return 0.5;
				}
				throw new UnsupportedOperationException("Cannot retrieve credit intra-bucket correlation yet.");
			default:
				throw new UnsupportedOperationException("Cannot retrieve IR intra-bucket correlation yet.");
		}
	}

	@Override
	public double getRiskWeight(Simm2Coordinate sensitivity) {
		switch (sensitivity.getRiskType()) {
			case DELTA:
				return getDeltaRiskWeight(sensitivity);
			default:
				throw new UnsupportedOperationException("Risk weight for non-delta not implemented yet.");
		}
	}

	@Override
	public double getHistoricalVolatilityRatio(Simm2Coordinate c) {
		switch (c.getRiskClass()) {
			case FX:
				return 0.6;
			case EQUITY:
				return 0.65;
			case CREDIT_Q:
			case CREDIT_NON_Q:
			case INTEREST_RATE:
				return 1.0; //B.10 (c) -- p.5, first line
			case COMMODITY:
				return 0.8;
			default:
				throw new IllegalArgumentException("Unknown risk class.");
		}
	}

	private double getDeltaRiskWeight(Simm2Coordinate sensitivity) {
		switch (sensitivity.getRiskClass()) {
			case FX:
				return 8.2;
			case EQUITY:
				return getBucketwiseValue(sensitivity, EQUITY_DELTA_RISK_WEIGHTS);
			case COMMODITY:
				return getBucketwiseValue(sensitivity, COMMODITY_DELTA_RISK_WEIGHTS);
			case CREDIT_Q:
				return getBucketwiseValue(sensitivity, CREDIT_Q_DELTA_RISK_WEIGHTS);
			case CREDIT_NON_Q:
				return getBucketwiseValue(sensitivity, CREDIT_NON_Q_DELTA_RISK_WEIGHTS);
			default:
				throw new UnsupportedOperationException("IR delta risk weight not supported yet.");
		}
	}

	/**
	 * @param coordinate The coordinate determining the bucket.
	 * @param array      The array that contains values for each bucket where the last value will be used for the "Residual" bucket.
	 * @return The value in the array at the given bucket.
	 */
	private double getBucketwiseValue(Simm2Coordinate coordinate, double[] array) {
		if (coordinate.getBucketKey().equalsIgnoreCase(RESIDUAL_BUCKET)) {
			return array[array.length - 1];
		}

		return array[Integer.parseInt(coordinate.getBucketKey())];
	}
}
