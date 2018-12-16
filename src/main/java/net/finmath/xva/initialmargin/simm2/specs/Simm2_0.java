package net.finmath.xva.initialmargin.simm2.specs;

import com.google.common.collect.ImmutableSet;
import net.finmath.sensitivities.simm2.MarginType;
import net.finmath.sensitivities.simm2.RiskClass;
import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.sensitivities.simm2.Vertex;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

import static net.finmath.functions.NormalDistribution.inverseCumulativeDistribution;

public final class Simm2_0 implements ParameterSet {
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
	private static final double[] EQUITY_VEGA_THRESHOLDS = {800E6, 800E6, 800E6, 800E6, 7300E6, 7300E6, 7300E6, 7300E6, 70E6, 300E6, 21000E6, 21000E6, 70E6};

	private static final double[] COMMODITY_INTRA_BUCKET_CORRELATIONS = {0.3, 0.97, 0.93, 0.98, 0.99, 0.92, 1.0, 0.58, 1.0, 0.1, 0.55, 0.64, 0.71, 0.22, 0.29, 0.0, 0.21};
	private static final double[][] COMMODITY_CROSS_BUCKET_CORRELATIONS = {
			{+0.00, +0.18, +0.15, +0.20, +0.25, +0.08, +0.19, +0.01, +0.27, +0.00, +0.15, +0.02, +0.06, +0.07, -0.04, +0.00, +0.06},
			{+0.18, +0.00, +0.89, +0.94, +0.93, +0.32, +0.22, +0.27, +0.24, +0.09, +0.45, +0.21, +0.32, +0.28, +0.17, +0.00, +0.37},
			{+0.15, +0.89, +0.00, +0.87, +0.88, +0.25, +0.16, +0.19, +0.12, +0.10, +0.26, -0.01, +0.19, +0.17, +0.10, +0.00, +0.27},
			{+0.20, +0.94, +0.87, +0.00, +0.92, +0.29, +0.22, +0.26, +0.19, +0.00, +0.32, +0.05, +0.20, +0.22, +0.13, +0.00, +0.28},
			{+0.25, +0.93, +0.88, +0.92, +0.00, +0.30, +0.26, +0.22, +0.28, +0.12, +0.42, +0.23, +0.28, +0.29, +0.17, +0.00, +0.34},
			{+0.08, +0.32, +0.25, +0.29, +0.30, +0.00, +0.13, +0.57, +0.05, +0.14, +0.15, -0.02, +0.13, +0.17, +0.01, +0.00, +0.26},
			{+0.19, +0.22, +0.16, +0.22, +0.26, +0.13, +0.00, +0.07, +0.80, +0.19, +0.16, +0.05, +0.17, +0.18, +0.00, +0.00, +0.18},
			{+0.01, +0.27, +0.19, +0.26, +0.22, +0.57, +0.07, +0.00, +0.13, +0.06, +0.16, +0.03, +0.10, +0.12, +0.06, +0.00, +0.23},
			{+0.27, +0.24, +0.12, +0.19, +0.28, +0.05, +0.80, +0.13, +0.00, +0.15, +0.17, +0.05, +0.15, +0.13, -0.03, +0.00, +0.13},
			{+0.00, +0.09, +0.10, +0.00, +0.12, +0.14, +0.19, +0.06, +0.15, +0.00, +0.07, +0.07, +0.17, +0.10, +0.02, +0.00, +0.11},
			{+0.15, +0.45, +0.26, +0.32, +0.42, +0.15, +0.16, +0.16, +0.17, +0.07, +0.00, +0.34, +0.20, +0.21, +0.16, +0.00, +0.27},
			{+0.02, +0.21, -0.01, +0.05, +0.23, -0.02, +0.05, +0.03, +0.05, +0.07, +0.34, +0.00, +0.17, +0.26, +0.11, +0.00, +0.14},
			{+0.06, +0.32, +0.19, +0.20, +0.28, +0.13, +0.17, +0.10, +0.15, +0.17, +0.20, +0.17, +0.00, +0.35, +0.09, +0.00, +0.22},
			{+0.07, +0.28, +0.17, +0.22, +0.29, +0.17, +0.18, +0.12, +0.13, +0.10, +0.21, +0.26, +0.35, +0.00, +0.06, +0.00, +0.20},
			{-0.04, +0.17, +0.10, +0.13, +0.17, +0.01, +0.00, +0.06, -0.03, +0.02, +0.16, +0.11, +0.09, +0.06, +0.00, +0.00, +0.16},
			{+0.00, +0.00, +0.00, +0.00, +0.00, +0.00, +0.00, +0.00, +0.00, +0.00, +0.00, +0.00, +0.00, +0.00, +0.00, +0.00, +0.00},
			{+0.06, +0.37, +0.27, +0.28, +0.34, +0.26, +0.18, +0.23, +0.13, +0.11, +0.27, +0.14, +0.22, +0.20, +0.16, +0.00, +0.00}
	};
	private static final double[] COMMODITY_DELTA_RISK_WEIGHTS = {19.0, 20.0, 17.0, 18.0, 24.0, 20.0, 24.0, 41.0, 25.0, 91.0, 20.0, 19.0, 16.0, 15.0, 10.0, 91.0, 17.0};
	private static final double[] COMMODITY_DELTA_THRESHOLDS = {1.4E9, 2.0E10, 3.5E9, 3.5E9, 3.5E9, 6.4E9, 6.4E9, 2.5E9, 2.5E9, 3.0E8, 2.9E9, 7.6E9, 3.9E9, 3.9E9, 3.9E9, 3.0E8, 1.2E10};
	private static final double[] COMMODITY_VEGA_THRESHOLDS = {250E6, 2000E6, 510E6, 510E6, 510E6, 1900E6, 1900E6, 870E6, 870E6, 220E6, 450E6, 740E6, 370E6, 370E6, 370E6, 220E6, 430E6};

	private static final double[] CREDIT_Q_DELTA_RISK_WEIGHTS = {85.0, 85.0, 73.0, 49.0, 48.0, 43.0, 161.0, 238.0, 151.0, 210.0, 141.0, 102.0, 238.0};
	private static final double[] CREDIT_Q_DELTA_THRESHOLDS = {950000.0, 290000.0, 290000.0, 290000.0, 290000.0, 290000.0, 950000.0, 290000.0, 290000.0, 290000.0, 290000.0, 290000.0, 290000.0};
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
	private static final double[] CREDIT_NON_Q_DELTA_THRESHOLDS = {9500000.0, 500000.0, 500000.0};

	private static final Set<String> FX_CATEGORY_1 = ImmutableSet.of("USD", "EUR", "JPY", "GBP", "AUD", "CHF", "CAD");
	private static final Set<String> FX_CATEGORY_2 = ImmutableSet.of("BRL", "CNY", "HKD", "INR", "KRW", "MXN", "NOK", "NZD", "RUB", "SEK", "SGD", "TRY", "ZAR");
	private static final double[] FX_VEGA_THRESHOLDS = {4000E6, 1900E6, 320E6, 1900E6, 120E6, 110E6, 320E6, 110E6, 110E6};

	private static final double[] IR_DELTA_RISK_WEIGHTS_REG = {113, 113, 98, 69, 56, 52, 51, 51, 51, 53, 56, 64};
	private static final double[] IR_DELTA_RISK_WEIGHTS_LO = {21, 21, 10, 11, 15, 20, 22, 21, 19, 20, 23, 27};
	private static final double[] IR_DELTA_RISK_WEIGHTS_HI = {93, 93, 90, 94, 97, 103, 101, 103, 102, 101, 102, 101};
	private static final Set<String> IR_REGULAR_WELL_TRADED_CURRENCIES = ImmutableSet.of("USD", "EUR", "GBP");
	private static final Set<String> IR_REGULAR_CURRENCIES = ImmutableSet.of("USD", "EUR", "GBP", "CHF", "AUD", "NZD", "CAD", "SEK", "NOK", "DKK", "HKD", "KRW", "SGD", "TWD");
	private static final Set<String> IR_LOW_VOLATILITY_CURRENCIES = ImmutableSet.of("JPY");
	private static final double[][] IR_CROSS_VERTEX_CORRELATIONS = {
			{1.00, 1.00, 0.79, 0.67, 0.53, 0.42, 0.37, 0.30, 0.22, 0.18, 0.16, 0.12},
			{1.00, 1.00, 0.79, 0.67, 0.53, 0.42, 0.37, 0.30, 0.22, 0.18, 0.16, 0.12},
			{0.79, 0.79, 1.00, 0.85, 0.69, 0.57, 0.50, 0.42, 0.32, 0.25, 0.23, 0.20},
			{0.67, 0.67, 0.85, 1.00, 0.86, 0.76, 0.69, 0.59, 0.47, 0.40, 0.37, 0.32},
			{0.53, 0.53, 0.69, 0.86, 1.00, 0.93, 0.87, 0.77, 0.63, 0.57, 0.54, 0.50},
			{0.42, 0.42, 0.57, 0.76, 0.93, 1.00, 0.98, 0.90, 0.77, 0.70, 0.67, 0.63},
			{0.37, 0.37, 0.50, 0.69, 0.87, 0.98, 1.00, 0.96, 0.84, 0.78, 0.75, 0.71},
			{0.30, 0.30, 0.42, 0.59, 0.77, 0.90, 0.96, 1.00, 0.93, 0.89, 0.86, 0.82},
			{0.22, 0.22, 0.32, 0.47, 0.63, 0.77, 0.84, 0.93, 1.00, 0.98, 0.96, 0.94},
			{0.18, 0.18, 0.25, 0.40, 0.57, 0.70, 0.78, 0.89, 0.98, 1.00, 0.99, 0.98},
			{0.16, 0.16, 0.23, 0.37, 0.54, 0.67, 0.75, 0.86, 0.96, 0.99, 1.00, 0.99},
			{0.12, 0.12, 0.20, 0.32, 0.50, 0.63, 0.71, 0.82, 0.94, 0.98, 0.99, 1.00}
	};

	private static final double VRW_SCALE = Math.sqrt(365.0 / 14.0) / inverseCumulativeDistribution(0.99);

	private static final double[][] RISK_CLASS_CORRELATIONS = {
			{1.00, 0.28, 0.18, 0.18, 0.30, 0.22},
			{0.28, 1.00, 0.30, 0.66, 0.46, 0.27},
			{0.18, 0.30, 1.00, 0.23, 0.25, 0.18},
			{0.18, 0.66, 0.23, 1.00, 0.39, 0.24},
			{0.30, 0.46, 0.25, 0.39, 1.00, 0.32},
			{0.22, 0.27, 0.18, 0.24, 0.32, 1.00}
	};

	private int getCurrencyCategory(String currencyCode) {
		if (FX_CATEGORY_1.contains(currencyCode)) {
			return 1;
		}

		if (FX_CATEGORY_2.contains(currencyCode)) {
			return 2;
		}

		return 3;
	}

	@Override
	public double getCrossBucketCorrelation(RiskClass rc, String left, String right) {

		if (rc == RiskClass.INTEREST_RATE) {
			return left.equalsIgnoreCase(right) ? 1.0 : 0.23;
		}

		int i = Integer.parseInt(left);
		int j = Integer.parseInt(right);

		switch (rc) {
			case FX:
				return 1.0; //There is only one FX bucket
			case EQUITY:
				return EQUITY_CROSS_BUCKET_CORRELATIONS[i][j];
			case COMMODITY:
				return COMMODITY_CROSS_BUCKET_CORRELATIONS[i][j];
			case CREDIT_Q:
				return CREDIT_Q_CROSS_BUCKET_CORRELATIONS[i][j];
			case CREDIT_NON_Q:
				return i == j ? 1.0 : 0.21;
			default:
				throw new IllegalArgumentException("Unknown risk class " + rc);
		}
	}

	@Override
	public double getConcentrationThreshold(SimmCoordinate coordinate) {
		switch (coordinate.getMarginType()) {
			case DELTA:
				return getDeltaConcentrationThreshold(coordinate);
			case VEGA:
				return getVegaConcentrationThreshold(coordinate);
			default:
				throw new UnsupportedOperationException("Non-delta CR cannot be calculated yet");
		}
	}

	private double getVegaConcentrationThreshold(SimmCoordinate coordinate) {
		switch (coordinate.getRiskClass()) {
			case COMMODITY:
				return getBucketwiseValue(coordinate, COMMODITY_VEGA_THRESHOLDS);
			case CREDIT_NON_Q:
				return 65E6;
			case CREDIT_Q:
				return 290E6;
			case EQUITY:
				return getBucketwiseValue(coordinate, EQUITY_VEGA_THRESHOLDS);
			case INTEREST_RATE:
				final String currency = coordinate.getQualifier().getCurrency();

				if (IR_REGULAR_CURRENCIES.contains(currency)) {
					return IR_REGULAR_WELL_TRADED_CURRENCIES.contains(currency) ? 2700E6 : 150E6;
				}

				if (IR_LOW_VOLATILITY_CURRENCIES.contains(currency)) {
					return 960E6;
				}

				return 110E6;
			case FX:
				final Pair<String, String> currencyPair = coordinate.getQualifier().getCurrencyPair();

				return FX_VEGA_THRESHOLDS[3 * (getCurrencyCategory(currencyPair.getLeft()) - 1) + (getCurrencyCategory(currencyPair.getRight()) - 1)];
			default:
				throw new IllegalArgumentException("Unknown risk class " + coordinate.getRiskClass());

		}
	}

	private double getDeltaConcentrationThreshold(SimmCoordinate coordinate) {
		final String currency = coordinate.getQualifier().getCurrency();
		switch (coordinate.getRiskClass()) {
			case FX:
				if (FX_CATEGORY_1.contains(currency)) {
					return 8400E6;
				}
				if (FX_CATEGORY_2.contains(currency)) {
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
			case INTEREST_RATE:
				if (IR_REGULAR_CURRENCIES.contains(currency)) {
					return IR_REGULAR_WELL_TRADED_CURRENCIES.contains(currency) ? 230.0E6 : 28.0E6;
				}

				if (IR_LOW_VOLATILITY_CURRENCIES.contains(currency)) {
					return 82.0E6;
				}

				return 8.0E6;
			default:
				throw new IllegalArgumentException("Unknown risk class");
		}
	}

	@Override
	public double getIntraBucketCorrelation(SimmCoordinate left, SimmCoordinate right) {

		if (left.equals(right)) {
			return 1.0;
		}

		//B.11 (c) states the correlations for the curvature margin are squared
		if (left.getMarginType() == MarginType.CURVATURE) {
			return Math.pow(getIntraBucketCorrelation(left.withMarginType(MarginType.VEGA), right.withMarginType(MarginType.VEGA)), 2.0);
		}

		switch (left.getRiskClass()) {
			case FX:
				return 0.5;
			case EQUITY:
				return getBucketwiseValue(left, EQUITY_INTRA_BUCKET_CORRELATIONS);
			case COMMODITY:
				return getBucketwiseValue(left, COMMODITY_INTRA_BUCKET_CORRELATIONS);
			case CREDIT_Q:
			case CREDIT_NON_Q:
				if (left.getSimmBucket().equalsIgnoreCase(RESIDUAL_BUCKET)) {
					return 0.5;
				}
				throw new UnsupportedOperationException("Cannot retrieve credit intra-bucket correlation yet.");
			case INTEREST_RATE:
				return getCrossVertexCorrelation(left.getVertex(), right.getVertex()) * getCrossCurveCorrelation(left, right);
			default:
				throw new IllegalArgumentException("Unknown risk class " + left.getRiskClass());
		}
	}

	private double getCrossCurveCorrelation(SimmCoordinate left, SimmCoordinate right) {

		if (left.getSubCurve() == null) { //There is no sub-curve for non-delta margin
			return 1.0;
		}

		if (left.getSubCurve().equals(right.getSubCurve())) {
			return 1.0;
		}

		return 0.98;
	}

	private double getCrossVertexCorrelation(Vertex left, Vertex right) {
		return IR_CROSS_VERTEX_CORRELATIONS[left.ordinal()][right.ordinal()];
	}

	@Override
	public double getRiskWeight(SimmCoordinate coordinate) {
		switch (coordinate.getMarginType()) {
			case DELTA:
				return getDeltaRiskWeight(coordinate);
			case VEGA:
				return getVegaRiskWeight(coordinate);
			case CURVATURE:
				return 0.5 * Math.min(1.0, 14.0 / coordinate.getVertex().getIdealizedDaycount());
			default:
				throw new UnsupportedOperationException("Risk weight for BaseCorr not implemented yet.");
		}
	}

	private double getDeltaRiskWeight(SimmCoordinate coordinate) {
		switch (coordinate.getRiskClass()) {
			case FX:
				return 8.2;
			case EQUITY:
				return getBucketwiseValue(coordinate, EQUITY_DELTA_RISK_WEIGHTS);
			case COMMODITY:
				return getBucketwiseValue(coordinate, COMMODITY_DELTA_RISK_WEIGHTS);
			case CREDIT_Q:
				return getBucketwiseValue(coordinate, CREDIT_Q_DELTA_RISK_WEIGHTS);
			case CREDIT_NON_Q:
				return getBucketwiseValue(coordinate, CREDIT_NON_Q_DELTA_RISK_WEIGHTS);
			case INTEREST_RATE:
				if (IR_REGULAR_CURRENCIES.contains(coordinate.getQualifier().getCurrency())) {
					return getVertexwiseValue(coordinate, IR_DELTA_RISK_WEIGHTS_REG);
				}
				if (IR_LOW_VOLATILITY_CURRENCIES.contains(coordinate.getQualifier().getCurrency())) {
					return getVertexwiseValue(coordinate, IR_DELTA_RISK_WEIGHTS_LO);
				}

				return getVertexwiseValue(coordinate, IR_DELTA_RISK_WEIGHTS_HI);
			default:
				throw new IllegalArgumentException("Unknown risk class " + coordinate.getRiskClass());
		}
	}

	private double getVegaRiskWeight(SimmCoordinate coordinate) {
		switch (coordinate.getRiskClass()) {
			case INTEREST_RATE:
				return 0.21; //D.1.34
			case CREDIT_Q:
			case CREDIT_NON_Q:
				return 0.27; //E.1.39
			case EQUITY:
				return coordinate.getSimmBucket().equals("12") ? 0.64 : 0.28; //G.1.57
			case COMMODITY:
				return 0.38; //H.1.62
			case FX:
				return 0.33; //I.1.67
			default:
				throw new IllegalArgumentException("Unknown risk class " + coordinate.getRiskClass());
		}
	}

	@Override
	public double getAdditionalWeight(SimmCoordinate coordinate) {
		if (coordinate.getMarginType() != MarginType.VEGA) {
			return 1.0;
		}

		switch (coordinate.getRiskClass()) {
			case INTEREST_RATE:
			case CREDIT_Q:
			case CREDIT_NON_Q:
				return 1.0; //Vegas for IR, CQ and CNQ are supposed to be already volatility-weighted, see Risk Data Standards v1.36, table in §2.8, p. 10
			default:
				return getHistoricalVolatilityRatio(coordinate) * VRW_SCALE * getDeltaRiskWeight(coordinate.withMarginType(MarginType.DELTA));
		}
	}

	private double getHistoricalVolatilityRatio(SimmCoordinate coordinate) {
		switch (coordinate.getRiskClass()) {
			case FX:
				return 0.6; //I.1.66
			case EQUITY:
				return 0.65; //G.1.56
			case CREDIT_Q:
			case CREDIT_NON_Q:
			case INTEREST_RATE:
				return 1.0; //B.10 (c) -- p.5, first line
			case COMMODITY:
				return 0.8; //H.1.61
			default:
				throw new IllegalArgumentException("Unknown risk class.");
		}
	}

	@Override
	public double getRiskClassCorrelation(RiskClass left, RiskClass right) {
		return RISK_CLASS_CORRELATIONS[left.ordinal()][right.ordinal()];
	}

	/**
	 * @param coordinate The coordinate determining the bucket.
	 * @param array      The array that contains values for each bucket where the last value will be used for the "Residual" bucket.
	 * @return The value in the array at the given bucket.
	 */
	private double getBucketwiseValue(SimmCoordinate coordinate, double[] array) {
		if (coordinate.getSimmBucket().equalsIgnoreCase(RESIDUAL_BUCKET)) {
			return array[array.length - 1];
		}

		return array[Integer.parseInt(coordinate.getSimmBucket())];
	}

	private double getVertexwiseValue(SimmCoordinate coordinate, double[] array) {
		return array[coordinate.getVertex().ordinal()];
	}
}
