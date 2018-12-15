package net.finmath.montecarlo.interestrate.products;

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
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModel;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelExponentialForm5Param;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

@RunWith(Theories.class)
public class AnalyticDiscountZeroCouponBondTest {

	@DataPoints("timeSpans")
	public static double[] timeSpans = new double[] { 0.01, 0.1, 0.25, 0.5, 1.0 };

	@DataPoints("discountFactors")
	public static double[] discountFactors = IntStream.range(1, 26).mapToDouble(x -> 1.0 - x*0.01).toArray();

	@DataPoints("forwardRates")
	public static double[] forwardRates = IntStream.range(1, 26).mapToDouble(x -> x*0.01).toArray();

	@Theory
	public void testGetValueSinglePeriodInSingleCurve(@FromDataPoints("forwardRates") double forwardRate, @FromDataPoints("timeSpans") double periodLength)
			throws CalculationException {

		TimeDiscretizationInterface periodTenor = new TimeDiscretization(0.0, periodLength);
		TimeDiscretizationInterface processTenor = periodTenor.union(
				new TimeDiscretization(0.0, periodLength, 0.1, TimeDiscretization.ShortPeriodLocation.SHORT_PERIOD_AT_END));

		ForwardCurveInterface forwardCurve = ForwardCurve.createForwardCurveFromForwards("",
				new double[] {0.0}, new double[] {forwardRate}, periodLength);
		AbstractLIBORCovarianceModel covariance = new LIBORCovarianceModelExponentialForm5Param(processTenor, periodTenor, 1, new double[] { 0.1, 0.1, 0.1, 0.1, 0.1});

		LIBORModelMonteCarloSimulation simulation = new LIBORModelMonteCarloSimulation(
				new LIBORMarketModel(periodTenor, forwardCurve, covariance),
				new ProcessEulerScheme(new BrownianMotion(processTenor, 1, 100, 42)));

		double bondPriceIbor = new AnalyticZeroCouponBond(periodLength).getValue(0.0, simulation).getAverage();
		double bondPriceDiscount = new AnalyticDiscountZeroCouponBond(periodLength).getValue(0.0, simulation).getAverage();

		assertThat(bondPriceIbor, is(closeTo(bondPriceDiscount, 1E-3)));
	}

	@Theory
	public void testGetValueSinglePeriodInMultiCurve(@FromDataPoints("forwardRates") double forwardRate,
			@FromDataPoints("discountFactors") double discountFactor,
			@FromDataPoints("timeSpans") double periodLength)
					throws CalculationException {

		TimeDiscretizationInterface periodTenor = new TimeDiscretization(0.0, periodLength);
		TimeDiscretizationInterface processTenor = periodTenor.union(
				new TimeDiscretization(0.0, periodLength, 0.1, TimeDiscretization.ShortPeriodLocation.SHORT_PERIOD_AT_END));

		ForwardCurveInterface forwardCurve = ForwardCurve.createForwardCurveFromForwards("",
				new double[] {0.0}, new double[] {forwardRate}, periodLength);
		DiscountCurveInterface discountCurve = DiscountCurve.createDiscountCurveFromDiscountFactors("", new double[] { periodLength}, new double[] { discountFactor });
		AbstractLIBORCovarianceModel covariance = new LIBORCovarianceModelExponentialForm5Param(processTenor, periodTenor, 1, new double[] { 0.1, 0.1, 0.1, 0.1, 0.1});

		LIBORModelMonteCarloSimulation simulation = new LIBORModelMonteCarloSimulation(
				new LIBORMarketModel(periodTenor, forwardCurve, discountCurve, covariance),
				new ProcessEulerScheme(new BrownianMotion(processTenor, 1, 100, 42)));

		double bondPriceDiscount = new AnalyticDiscountZeroCouponBond(periodLength).getValue(0.0, simulation).getAverage();

		assertThat(bondPriceDiscount, is(closeTo(discountFactor, 1E-3)));
	}
}