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

public class SimmIRDeltaSchemeTest {

	@Test
	public void testGetValueForSingleDeterministicVertex() {


		final SimmModality modality = new SimmModality("EUR", 0.0);
		final SimmIRDeltaScheme scheme = new SimmIRDeltaScheme(modality);
		final Simm2Coordinate coordinate = new Simm2Coordinate(Vertex.M1, SubCurve.Libor3m, "EUR", RiskClass.INTEREST_RATE, MarginType.DELTA, ProductClass.RATES_FX);
		final double riskWeight = modality.getParams().getRiskWeight(coordinate);

		//By choosing a sensitivity above the threshold we don't have to care about it in the assertion
		final double marketSensitivity = modality.getParams().getConcentrationThreshold(coordinate)*2.0;

		Map<Simm2Coordinate, RandomVariableInterface> gradient = ImmutableMap.of(
				coordinate,
				new Scalar(marketSensitivity)
		);

		final RandomVariableInterface result = scheme.getValue(gradient);

		//For a single weighted sensitivity (one curve/tenor combination)
		//the result should be the weighted sensitivity

		assertThat(result.getAverage(),
				is(equalTo(marketSensitivity*riskWeight)));

	}

	@Test
	public void testGetValueForTwoDeterministicVertices() {

		final SimmModality modality = new SimmModality("EUR", 0.0);
		final SimmIRDeltaScheme scheme = new SimmIRDeltaScheme(modality);
		final Simm2Coordinate coordinate1 = new Simm2Coordinate(Vertex.M1, SubCurve.Libor3m, "EUR", RiskClass.INTEREST_RATE, MarginType.DELTA, ProductClass.RATES_FX);
		final Simm2Coordinate coordinate2 = new Simm2Coordinate(Vertex.M3, SubCurve.Libor3m, "EUR", RiskClass.INTEREST_RATE, MarginType.DELTA, ProductClass.RATES_FX);
		final double riskWeight = modality.getParams().getRiskWeight(coordinate1);//1 bucket -> 1 weight

		//By choosing a sensitivity above the threshold we don't have to care about it in the assertion
		final double marketSensitivity = modality.getParams().getConcentrationThreshold(coordinate1);

		Map<Simm2Coordinate, RandomVariableInterface> gradient = ImmutableMap.of(
				coordinate1, new Scalar(marketSensitivity),
				coordinate2, new Scalar(marketSensitivity)
		);

		final RandomVariableInterface result = scheme.getValue(gradient);

		//We have two weighted sensitivities in the same bucket
		//These are the correlations needed from ISDA SIMM v2.0
		//Cross-tenor correlation: phi(1M,3M) = 0.79
		//Cross-curve correlation: rho(Libor3m, Libor3m) = 1.0

		final double weightedSensitivity = riskWeight*marketSensitivity;

		assertThat(result.getAverage(),
				is(equalTo(Math.sqrt(
						2*weightedSensitivity*weightedSensitivity +
								0.79*1.0*weightedSensitivity*weightedSensitivity*2))));

	}
}