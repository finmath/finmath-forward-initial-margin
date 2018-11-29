package net.finmath.xva.initialmargin.simm2.calculation;

import com.google.common.collect.ImmutableMap;
import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.Scalar;
import net.finmath.sensitivities.simm2.MarginType;
import net.finmath.sensitivities.simm2.ProductClass;
import net.finmath.sensitivities.simm2.RiskClass;
import net.finmath.sensitivities.simm2.SubCurve;
import net.finmath.sensitivities.simm2.Vertex;
import net.finmath.xva.initialmargin.simm2.specs.ParameterSet;
import net.finmath.xva.initialmargin.simm2.specs.Simm2_0;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.IsCloseTo.closeTo;

public class SimmIRSchemeTest {

	private static final ParameterSet PARAMETERS = new Simm2_0();

	@Test
	public void testGetMarginDeltaForSingleVertex() {

		final SimmCoordinate coordinate = new SimmCoordinate(Vertex.M1, SubCurve.Libor3m, "EUR", RiskClass.INTEREST_RATE, MarginType.DELTA, ProductClass.RATES_FX);
		final double riskWeight = PARAMETERS.getRiskWeight(coordinate);

		//By choosing a sensitivity above the threshold we don't have to care about it in the assertion
		final double marketSensitivity = PARAMETERS.getConcentrationThreshold(coordinate)*2.0;


		//For a single weighted sensitivity (one curve/tenor combination)
		//the result should be the weighted sensitivity
		assertThat(new SimmIRScheme(PARAMETERS).getMargin(ImmutableMap.of(coordinate, new Scalar(marketSensitivity))).getAverage(),
				is(closeTo(marketSensitivity*riskWeight, 1E-8)));
	}

	@Test
	public void testGetMarginDeltaForTwoDeterministicVerticesOfSameCurrency() {

		final SimmCoordinate coordinate1m = new SimmCoordinate(Vertex.M1, SubCurve.Libor3m, "EUR", RiskClass.INTEREST_RATE, MarginType.DELTA, ProductClass.RATES_FX);
		final SimmCoordinate coordinate3m = new SimmCoordinate(Vertex.M3, SubCurve.Libor3m, "EUR", RiskClass.INTEREST_RATE, MarginType.DELTA, ProductClass.RATES_FX);
		final double riskWeight1m = PARAMETERS.getRiskWeight(coordinate1m);
		final double riskWeight3m = PARAMETERS.getRiskWeight(coordinate3m);

		//By choosing a sensitivity above the threshold we don't have to care about it in the assertion
		final double marketSensitivity = PARAMETERS.getConcentrationThreshold(coordinate1m);

		Map<SimmCoordinate, RandomVariableInterface> gradient = ImmutableMap.of(coordinate1m, new Scalar(marketSensitivity), coordinate3m, new Scalar(marketSensitivity));

		//We have two weighted sensitivities in the same bucket
		//These are the correlations needed from ISDA SIMM v2.0
		//Cross-tenor correlation: phi(1M,3M) = 0.79
		//Cross-curve correlation: rho(Libor3m, Libor3m) = 1.0

		final double ws1m = riskWeight1m * marketSensitivity;
		final double ws3m = riskWeight3m * marketSensitivity;

		assertThat(new SimmIRScheme(PARAMETERS).getMargin(gradient).getAverage(),
				is(closeTo(Math.sqrt(ws1m*ws1m + ws3m*ws3m + 0.79*2.0*ws1m*ws3m), 1E-8)));
	}

	@Test
	public void testGetMarginDeltaForTwoDeterministicVerticesOfDifferentCurrencies() {

		final SimmCoordinate coordinateEur = new SimmCoordinate(Vertex.M1, SubCurve.Libor3m, "EUR", RiskClass.INTEREST_RATE, MarginType.DELTA, ProductClass.RATES_FX);
		final SimmCoordinate coordinateUsd = new SimmCoordinate(Vertex.M3, SubCurve.Libor3m, "USD", RiskClass.INTEREST_RATE, MarginType.DELTA, ProductClass.RATES_FX);
		final double riskWeightEur = PARAMETERS.getRiskWeight(coordinateEur);
		final double riskWeightUsd = PARAMETERS.getRiskWeight(coordinateUsd);

		//By choosing a sensitivity above the threshold we don't have to care about it in the assertion
		//USD and EUR also have the same threshold so we can re-use the value
		final double marketSensitivity = PARAMETERS.getConcentrationThreshold(coordinateEur) * 2.0;

		Map<SimmCoordinate, RandomVariableInterface> gradient = ImmutableMap.of(
				coordinateEur, new Scalar(marketSensitivity), coordinateUsd, new Scalar(marketSensitivity)
		);

		//We have two weighted sensitivities in different buckets
		//The general cross-bucket correlation is 0.23
		//So we have K1 = RW1 * marketSensitivity
		//           K2 = RW2 * marketSensitivity
		//  DeltaMargin = (K1^2 + K2^2 + 2*K1*K2*0.23)^0.5

		final double kEur = riskWeightEur*marketSensitivity;
		final double kUsd = riskWeightUsd * marketSensitivity;

		assertThat(new SimmIRScheme(PARAMETERS).getMargin(gradient).getAverage(),
				is(closeTo(Math.sqrt(kEur*kEur + kUsd*kUsd + 2.0*kEur*kUsd*0.23), 1E-8)));
	}

	@Test
	public void testGetMarginVegaForSingleVertex() {

		SimmCoordinate coordinate = new SimmCoordinate(Vertex.Y10, "EUR", RiskClass.INTEREST_RATE, MarginType.VEGA, ProductClass.RATES_FX);

		//By choosing a value above the threshold we don't have to care about the concentration risk factors
		double volWeightedMarketSensitivity = PARAMETERS.getConcentrationThreshold(coordinate) * 2.0;

		assertThat(new SimmIRScheme(PARAMETERS).getMargin(ImmutableMap.of(coordinate, new Scalar(volWeightedMarketSensitivity))).getAverage(),
				is(closeTo(volWeightedMarketSensitivity * PARAMETERS.getRiskWeight(coordinate), 1E-8)));

	}

	@Test
	public void testGetMarginVegaForTwoDifferentVerticesOfSameCurrency() {

		SimmCoordinate coordinate5Y= new SimmCoordinate(Vertex.Y5, "EUR", RiskClass.INTEREST_RATE, MarginType.VEGA, ProductClass.RATES_FX);
		SimmCoordinate coordinate10Y = new SimmCoordinate(Vertex.Y10, "EUR", RiskClass.INTEREST_RATE, MarginType.VEGA, ProductClass.RATES_FX);

		double v = PARAMETERS.getConcentrationThreshold(coordinate5Y) * 2.0;

		final ImmutableMap<SimmCoordinate, RandomVariableInterface> gradient = ImmutableMap.of(coordinate5Y, new Scalar(v), coordinate10Y, new Scalar(v));

		double vr5Y = PARAMETERS.getRiskWeight(coordinate5Y) * v;
		double vr10Y = PARAMETERS.getRiskWeight(coordinate10Y) * v;
		double correlation = PARAMETERS.getIntraBucketCorrelation(coordinate5Y, coordinate10Y);

		assertThat(new SimmIRScheme(PARAMETERS).getMargin(gradient).getAverage(),
				is(closeTo(Math.sqrt(vr5Y*vr5Y + vr10Y*vr10Y + 2.0*correlation*vr5Y*vr10Y), 1E-8)));
	}

	@Test
	public void testGetMarginVegaForTwoDifferentVerticesOfDifferentCurrency() {

		SimmCoordinate coordinateEur = new SimmCoordinate(Vertex.Y10, "EUR", RiskClass.INTEREST_RATE, MarginType.VEGA, ProductClass.RATES_FX);
		SimmCoordinate coordinateUsd = new SimmCoordinate(Vertex.Y10, "USD", RiskClass.INTEREST_RATE, MarginType.VEGA, ProductClass.RATES_FX);

		double v = PARAMETERS.getConcentrationThreshold(coordinateEur ) * 2.0;

		final ImmutableMap<SimmCoordinate, RandomVariableInterface> gradient = ImmutableMap.of(coordinateEur , new Scalar(v), coordinateUsd, new Scalar(v));

		double vrEur = PARAMETERS.getRiskWeight(coordinateEur ) * v;
		double vrUsd = PARAMETERS.getRiskWeight(coordinateUsd) * v;
		double correlation = PARAMETERS.getCrossBucketCorrelation(RiskClass.INTEREST_RATE, "EUR", "USD");

		assertThat(new SimmIRScheme(PARAMETERS).getMargin(gradient).getAverage(),
				is(closeTo(Math.sqrt(vrEur*vrEur + vrUsd*vrUsd + 2.0*correlation*vrEur*vrUsd), 1E-8)));
	}
}