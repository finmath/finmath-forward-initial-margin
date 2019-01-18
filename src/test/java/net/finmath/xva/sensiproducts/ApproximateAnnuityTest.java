package net.finmath.xva.sensiproducts;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.stream.IntStream;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.sensitivities.simm2.MarginType;
import net.finmath.sensitivities.simm2.ProductClass;
import net.finmath.sensitivities.simm2.RiskClass;
import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.sensitivities.simm2.Vertex;
import net.finmath.sensitivities.simm2.products.ApproximateAnnuity;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.xva.tradespecifications.IRCurveSpec;
import net.finmath.xva.tradespecifications.Indices;
import net.finmath.xva.tradespecifications.SIMMTradeSpecification;

@RunWith(Theories.class)
public class ApproximateAnnuityTest {

	@DataPoints("irCurves")
	public static IRCurveSpec[] getIndices() {
		return new IRCurveSpec[]{Indices.getLibor("EUR", "3M"), Indices.getLibor("EUR", "6M")};
	}

	@DataPoints("times")
	public static double[] getTimes() {
		return IntStream.range(0, 21).mapToDouble(i -> 0.5 * i).toArray();
	}

	@DataPoints("vertices")
	public static Vertex[] getVertices() {
		return Vertex.values();
	}

	@DataPoints("notionals")
	public static double[] getNotionals() {
		return IntStream.range(0, 11).mapToDouble(i -> 1000000 * i).toArray();
	}

	private static LIBORModelMonteCarloSimulationModel getDummyModel() {
		return new SimulationStub(new TimeDiscretizationFromArray(0.0));
	}

	@Theory
	public void testGetValue(@FromDataPoints("times") double maturity,
			@FromDataPoints("times") double evalTime,
			@FromDataPoints("irCurves") IRCurveSpec irCurve,
			@FromDataPoints("notionals") double notional) throws CalculationException {

		SIMMTradeSpecification spec = new SIMMTradeSpecification(notional, maturity, irCurve);

		assertThat(new ApproximateAnnuity(spec).getValue(evalTime, (getDummyModel())).getAverage(),
				is(closeTo(0.0001 * notional * irCurve.getPeriodLength() / irCurve.getDayInYears(), 1E-6)));
	}

	@Theory
	public void testGetSimmSensitivitiesAtTimeZeroForExactMaturities(@FromDataPoints("vertices") Vertex maturityAccordingToSimm,
			@FromDataPoints("irCurves") IRCurveSpec irCurve,
			@FromDataPoints("notionals") double notional) {

		SIMMTradeSpecification spec = new SIMMTradeSpecification(notional, maturityAccordingToSimm.getIdealizedYcf(), irCurve);

		assertThat(new ApproximateAnnuity(spec).getGradient(0.0, getDummyModel()),
				hasEntry(equalTo(new SimmCoordinate(maturityAccordingToSimm, irCurve.getName(), irCurve.getCurrency(), RiskClass.INTEREST_RATE, MarginType.DELTA, ProductClass.RATES_FX)),
						average(closeTo(0.0001 * notional * irCurve.getPeriodLength() / irCurve.getDayInYears(), 1E-6)))
				);
	}

	private FeatureMatcher<RandomVariable, Double> average(Matcher<Double> matcher) {
		return new FeatureMatcher<RandomVariable, Double>(matcher, "average of rv", "average") {
			@Override
			protected Double featureValueOf(RandomVariable x) {
				return x.getAverage();
			}
		};
	}
}