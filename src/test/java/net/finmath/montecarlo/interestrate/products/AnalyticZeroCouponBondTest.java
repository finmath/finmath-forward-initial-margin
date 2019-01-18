package net.finmath.montecarlo.interestrate.products;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;

import java.util.stream.IntStream;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.BrownianMotionLazyInit;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModel;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelExponentialForm5Param;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.TimeDiscretizationFromArray.ShortPeriodLocation;
import net.finmath.time.TimeDiscretization;

@RunWith(Theories.class)
public class AnalyticZeroCouponBondTest {

	@DataPoints("timeSpans")
	public static double[] timeSpans = new double[] { 0.01, 0.1, 0.25, 0.5, 1.0 };

	@DataPoints("timePoints")
	public static double[] evaluationTimes = new double[] { 0.0, 0.5, 1.0 };

	@DataPoints("forwardRates")
	public static double[] forwardRates = IntStream.range(1, 26).mapToDouble(x -> x*0.01).toArray();

	@Theory
	public void testGetValueAfterMaturity(@FromDataPoints("timePoints") double maturity, @FromDataPoints("timeSpans") double timeAfter) throws CalculationException {

		//This also asserts that after maturity essentially no simulated model is needed by passing null

		assertThat(
				new AnalyticZeroCouponBond(maturity).getValue(maturity + timeAfter, null).getAverage(),
				is(equalTo(0.0)));
	}

	@Theory
	public void testGetValueSinglePeriod(@FromDataPoints("forwardRates") double forwardRate, @FromDataPoints("timeSpans") double periodLength)
			throws CalculationException {

		TimeDiscretization periodTenor = new TimeDiscretizationFromArray(0.0, periodLength);
		TimeDiscretization processTenor = periodTenor.union(
				new TimeDiscretizationFromArray(0.0, periodLength, 0.1, ShortPeriodLocation.SHORT_PERIOD_AT_END));

		ForwardCurveInterface forwardCurve = ForwardCurve.createForwardCurveFromForwards("",
				new double[] {0.0}, new double[] {forwardRate}, periodLength);
		AbstractLIBORCovarianceModel covariance = new LIBORCovarianceModelExponentialForm5Param(processTenor, periodTenor, 1, new double[] { 0.1, 0.1, 0.1, 0.1, 0.1});

		LIBORModelMonteCarloSimulation simulation = new LIBORModelMonteCarloSimulation(
				new LIBORMarketModel(periodTenor, forwardCurve, covariance),
				new ProcessEulerScheme(new BrownianMotionLazyInit(processTenor, 1, 100, 42)));

		double priceFromForward = 1.0 / (forwardCurve.getValue(0.0)*periodLength + 1.0);

		assertThat(new AnalyticZeroCouponBond(periodLength).getValue(0.0, simulation).getAverage(),
				is(closeTo(priceFromForward, 1E-3)));
	}

	@Theory
	public void testGetValueTwoPeriods(@FromDataPoints("forwardRates") double forward1, @FromDataPoints("forwardRates") double forward2, @FromDataPoints("timeSpans") double periodLength)
			throws CalculationException {

		TimeDiscretization periodTenor = new TimeDiscretizationFromArray(0.0, periodLength, periodLength*2);
		TimeDiscretization processTenor = periodTenor.union(
				new TimeDiscretizationFromArray(0.0, periodLength*2, 0.1, ShortPeriodLocation.SHORT_PERIOD_AT_END));

		ForwardCurveInterface forwardCurve = ForwardCurve.createForwardCurveFromForwards("",
				new double[] {0.0, periodLength}, new double[] {forward1, forward2}, periodLength);
		AbstractLIBORCovarianceModel covariance = new LIBORCovarianceModelExponentialForm5Param(processTenor, periodTenor, 1, new double[] { 0.1, 0.1, 0.1, 0.1, 0.1});

		LIBORModelMonteCarloSimulation simulation = new LIBORModelMonteCarloSimulation(
				new LIBORMarketModel(periodTenor, forwardCurve, covariance),
				new ProcessEulerScheme(new BrownianMotionLazyInit(processTenor, 1, 100, 42)));

		double priceFromForward1 = 1.0 / (forwardCurve.getValue(0.0)*periodLength + 1.0);
		double priceFromForward2 = 1.0 / (forwardCurve.getValue(periodLength)*periodLength + 1.0);

		assertThat(new AnalyticZeroCouponBond(periodLength*2).getValue(0.0, simulation).getAverage(),
				is(closeTo(priceFromForward1*priceFromForward2, 1E-3)));
	}
}