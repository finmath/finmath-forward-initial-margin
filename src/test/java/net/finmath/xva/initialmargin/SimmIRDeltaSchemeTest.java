package net.finmath.xva.initialmargin;

import com.google.common.collect.ImmutableMap;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.Scalar;
import net.finmath.xva.coordinates.simm2.MarginType;
import net.finmath.xva.coordinates.simm2.ProductClass;
import net.finmath.xva.coordinates.simm2.RiskClass;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;
import net.finmath.xva.coordinates.simm2.SubCurve;
import net.finmath.xva.coordinates.simm2.Vertex;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.IsCloseTo.closeTo;

public class SimmIRDeltaSchemeTest {

	@Test
	public void testGetValueForSingleDeterministicVertex() {

		final Simm2Parameter parameters = new Simm2ParameterImpl();
		final SimmIRDeltaScheme scheme = new SimmIRDeltaScheme(parameters);
		final Simm2Coordinate coordinate = new Simm2Coordinate(Vertex.M1, SubCurve.Libor3m, "EUR", RiskClass.INTEREST_RATE, MarginType.DELTA, ProductClass.RATES_FX);
		final double riskWeight = parameters.getRiskWeight(coordinate);

		//By choosing a sensitivity above the threshold we don't have to care about it in the assertion
		final double marketSensitivity = parameters.getConcentrationThreshold(coordinate)*2.0;

		Map<Simm2Coordinate, RandomVariableInterface> gradient = ImmutableMap.of(
				coordinate,
				new Scalar(marketSensitivity)
		);

		final RandomVariableInterface result = scheme.getMargin(gradient);

		//For a single weighted sensitivity (one curve/tenor combination)
		//the result should be the weighted sensitivity

		assertThat(result.getAverage(), is(closeTo(marketSensitivity*riskWeight, 1E-8)));
	}

	@Test
	public void testGetValueForTwoDeterministicVerticesOfSameCurrency() {

		final Simm2Parameter parameters = new Simm2ParameterImpl();
		final SimmIRDeltaScheme scheme = new SimmIRDeltaScheme(parameters);
		final Simm2Coordinate coordinate1m = new Simm2Coordinate(Vertex.M1, SubCurve.Libor3m, "EUR", RiskClass.INTEREST_RATE, MarginType.DELTA, ProductClass.RATES_FX);
		final Simm2Coordinate coordinate3m = new Simm2Coordinate(Vertex.M3, SubCurve.Libor3m, "EUR", RiskClass.INTEREST_RATE, MarginType.DELTA, ProductClass.RATES_FX);
		final double riskWeight1m = parameters.getRiskWeight(coordinate1m);
		final double riskWeight3m = parameters.getRiskWeight(coordinate3m);

		//By choosing a sensitivity above the threshold we don't have to care about it in the assertion
		final double marketSensitivity = parameters.getConcentrationThreshold(coordinate1m);

		Map<Simm2Coordinate, RandomVariableInterface> gradient = ImmutableMap.of(coordinate1m, new Scalar(marketSensitivity), coordinate3m, new Scalar(marketSensitivity));

		final RandomVariableInterface result = scheme.getMargin(gradient);

		//We have two weighted sensitivities in the same bucket
		//These are the correlations needed from ISDA SIMM v2.0
		//Cross-tenor correlation: phi(1M,3M) = 0.79
		//Cross-curve correlation: rho(Libor3m, Libor3m) = 1.0

		final double ws1m = riskWeight1m * marketSensitivity;
		final double ws3m = riskWeight3m * marketSensitivity;

		assertThat(result.getAverage(), is(closeTo(Math.sqrt(ws1m*ws1m + ws3m*ws3m + 0.79*2.0*ws1m*ws3m), 1E-8)));
	}

	@Test
	public void testGetValueForTwoDeterministicVerticesOfDifferentCurrencies() {

		final Simm2Parameter parameters = new Simm2ParameterImpl();
		final SimmIRDeltaScheme scheme = new SimmIRDeltaScheme(parameters);
		final Simm2Coordinate coordinateEur = new Simm2Coordinate(Vertex.M1, SubCurve.Libor3m, "EUR", RiskClass.INTEREST_RATE, MarginType.DELTA, ProductClass.RATES_FX);
		final Simm2Coordinate coordinateUsd = new Simm2Coordinate(Vertex.M3, SubCurve.Libor3m, "USD", RiskClass.INTEREST_RATE, MarginType.DELTA, ProductClass.RATES_FX);
		final double riskWeightEur = parameters.getRiskWeight(coordinateEur);
		final double riskWeightUsd = parameters.getRiskWeight(coordinateUsd);

		//By choosing a sensitivity above the threshold we don't have to care about it in the assertion
		//USD and EUR also have the same threshold so we can re-use the value
		final double marketSensitivity = parameters.getConcentrationThreshold(coordinateEur) * 2.0;

		Map<Simm2Coordinate, RandomVariableInterface> gradient = ImmutableMap.of(
				coordinateEur, new Scalar(marketSensitivity), coordinateUsd, new Scalar(marketSensitivity)
		);

		final RandomVariableInterface result = scheme.getMargin(gradient);

		//We have two weighted sensitivities in different buckets
		//The general cross-bucket correlation is 0.23
		//So we have K1 = RW1 * marketSensitivity
		//           K2 = RW2 * marketSensitivity
		//  DeltaMargin = (K1^2 + K2^2 + 2*K1*K2*0.23)^0.5

		final double kEur = riskWeightEur*marketSensitivity;
		final double kUsd = riskWeightUsd * marketSensitivity;

		assertThat(result.getAverage(), is(closeTo(Math.sqrt(kEur*kEur + kUsd*kUsd + 2.0*kEur*kUsd*0.23), 1E-8)));
	}
}