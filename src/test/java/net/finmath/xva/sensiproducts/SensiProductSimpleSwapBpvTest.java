package net.finmath.xva.sensiproducts;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.stream.IntStream;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.MonteCarloSimulationInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.xva.tradespecifications.IRCurveSpec;
import net.finmath.xva.tradespecifications.Indices;
import net.finmath.xva.tradespecifications.SIMMTradeSpecification;

@RunWith(Theories.class)
public class SensiProductSimpleSwapBpvTest {

	@DataPoints("irCurves")
	public static IRCurveSpec[] getIndices() {
		return new IRCurveSpec[] {Indices.getLibor("EUR", "3M"), Indices.getLibor("EUR", "6M")};
	}

	@DataPoints("times")
	public static double[] getTimes() {
		return IntStream.range(0, 21).mapToDouble(i -> 0.5*i).toArray();
	}

	@DataPoints("notionals")
	public static double[] getNotionals() {
		return IntStream.range(0, 11).mapToDouble(i -> 1000000*i).toArray();
	}

	private static MonteCarloSimulationInterface getDummyModel() {
		return new SimulationStub(new TimeDiscretization(0.0));
	}

	@Theory
	public void getValue(@FromDataPoints("times") double maturity,
			@FromDataPoints("times") double evalTime,
			@FromDataPoints("irCurves") IRCurveSpec irCurve,
			@FromDataPoints("notionals") double notional) throws CalculationException {

		SIMMTradeSpecification spec = new SIMMTradeSpecification(notional, maturity, irCurve.getName());

		assertThat(new SensiProductSimpleSwapBpv(spec).getValue(evalTime, (getDummyModel())).getAverage(),
				is(closeTo(0.0001 * notional * irCurve.getPeriodLength() / irCurve.getDayInYears(), 1E-6)));
	}
}