package net.finmath.lch.initialmargin.swapclear.libormodels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.CurveName;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.BlendedLocalVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;

public class ModelFactoryScenarioGridTest {

	private static final CurveName CURVE = CurveName.EUR_EURIBOR_3M;
	private static final LocalDate REFERENCE = ModelFactory.REFERENCE_DATE.toLocalDate();

	@Test
	public void defaultWeekdayGridMatchesWorkbookCalendarAndAct365() {
		LocalDate start = LocalDate.of(2024, 3, 28);
		TimeDiscretization times = ModelFactory.createBusinessdayTimeDiscretization(start, LocalDate.of(2024, 4, 3));

		assertEquals(5, times.getNumberOfTimes());
		assertContainsDate(times, start, LocalDate.of(2024, 3, 29)); // Good Friday is in the workbook.
		assertContainsDate(times, start, LocalDate.of(2024, 4, 1)); // Easter Monday is in the workbook.
		assertTrue(times.getTimeIndex(2.0 / 365) < 0);
		assertTrue(times.getTimeIndex(3.0 / 365) < 0);
		assertEquals(6.0 / 365, times.getTime(times.getNumberOfTimeSteps()), 1E-15);
	}

	@Test
	public void explicitTargetCalendarExcludesInteriorHolidays() {
		LocalDate start = LocalDate.of(2024, 3, 28);
		TimeDiscretization times = ModelFactory.createBusinessdayTimeDiscretization(start, LocalDate.of(2024, 4, 3),
				new BusinessdayCalendarExcludingTARGETHolidays());

		assertEquals(3, times.getNumberOfTimes());
		assertTrue(times.getTimeIndex(1.0 / 365) < 0);
		assertTrue(times.getTimeIndex(4.0 / 365) < 0);
		assertContainsDate(times, start, LocalDate.of(2024, 4, 2));
	}

	@Test
	public void calendarBoundariesAndLeapDayAreRepresentedExactly() {
		LocalDate start = LocalDate.of(2024, 2, 28);
		LocalDate end = LocalDate.of(2024, 3, 3); // The terminal boundary may fall on a Sunday.
		TimeDiscretization times = ModelFactory.createBusinessdayTimeDiscretization(start, end);

		assertEquals(4, times.getNumberOfTimes());
		assertContainsDate(times, start, LocalDate.of(2024, 2, 29));
		assertContainsDate(times, start, end);
	}

	@Test
	public void modelIncludesFixingsAndCoversRollingSixtyYearTenors() throws CalculationException {
		LocalDate end = REFERENCE.plusDays(94);
		LIBORModelMonteCarloSimulationModel model = ModelFactory.getModel(CURVE, end, 60.0);
		double observationTime = FloatingpointDate.getFloatingPointDateFromDate(REFERENCE, end);

		assertContainsDate(model.getTimeDiscretization(), REFERENCE, end);
		assertTrue(model.getTimeDiscretization().getTimeIndex(0.25) >= 0);
		assertEquals(60.5, model.getLiborPeriod(model.getNumberOfLibors()), 0.0);
		RandomVariable longForward = model.getForwardRate(observationTime, observationTime, observationTime + 60.0);
		assertTrue(Double.isFinite(longForward.getAverage()));
		assertTrue("Long-tenor forwards must evolve on the simulated paths.", longForward.getVariance() > 0.0);
	}

	@Test
	public void defaultHorizonExtendsVolatilityWithoutSimulatingTheFullModel() throws CalculationException {
		LIBORModelMonteCarloSimulationModel model = ModelFactory.getModel(CURVE);
		double end = FloatingpointDate.getFloatingPointDateFromDate(REFERENCE, REFERENCE.plusYears(20));
		assertEquals(end, model.getTimeDiscretization().getTime(model.getTimeDiscretization().getNumberOfTimeSteps()), 1E-13);
		assertEquals(80.25, model.getLiborPeriod(model.getNumberOfLibors()), 0.0);

		LIBORMarketModelFromCovarianceModel lmm = (LIBORMarketModelFromCovarianceModel) model.getModel();
		BlendedLocalVolatilityModel blended = (BlendedLocalVolatilityModel) lmm.getCovarianceModel();
		LIBORVolatilityModel volatility = ((LIBORCovarianceModelFromVolatilityAndCorrelation)
				blended.getBaseCovarianceModel()).getVolatilityModel();
		int timeIndex = model.getTimeDiscretization().getTimeIndex(1.0);
		double lastCalibrated = volatility.getVolatility(timeIndex, model.getLiborPeriodIndex(31.0)).doubleValue();
		assertEquals(-0.011628112608719269, lastCalibrated, 0.0);
		assertEquals(lastCalibrated, volatility.getVolatility(timeIndex, model.getLiborPeriodIndex(60.0)).doubleValue(), 0.0);
	}

	@Test
	public void analyticAndLmmZeroRatesUseLinearInterpolationAndFlatTails() throws CalculationException {
		AnalyticModel curves = ModelFactory.getAnalyticModel(CURVE,
				new double[] { 0.0, 0.25, 1.0, 2.0 }, new double[] { 0.0, 0.04, 0.03, 0.02 });
		DiscountCurve discount = curves.getDiscountCurve(CURVE.toString());
		LIBORModelMonteCarloSimulationModel model = ModelFactory.createLIBORMarketModel(1, 1, 3.0, 0.25,
				new TimeDiscretizationFromArray(0.0, 0.01), curves, CURVE, new double[] { 0.0 }, 0.5);
		assertEquals(1.0, discount.getDiscountFactor(curves, 0.0), 0.0);
		double[] tenors = { 1.0 / 365, 0.125, 0.25, 0.625, 1.0, 1.25, 2.0, 2.5 };
		double[] expected = { 0.04, 0.04, 0.04, 0.035, 0.03, 0.0275, 0.02, 0.02 };
		for (int i = 0; i < tenors.length; i++) {
			double tenor = tenors[i];
			assertEquals(expected[i], -Math.log(discount.getDiscountFactor(curves, tenor)) / tenor, 1E-13);
			assertEquals(expected[i], Math.log1p(tenor * model.getForwardRate(0.0, 0.0, tenor).doubleValue()) / tenor, 1E-13);
		}
	}

	@Test
	public void customCoarseGridRemainsAvailableAndInvalidHorizonsFailEarly() throws CalculationException {
		AnalyticModel curves = ModelFactory.getAnalyticModel(CURVE, new double[] { 1.0, 2.0 }, new double[] { 0.02, 0.02 });
		LIBORModelMonteCarloSimulationModel model = ModelFactory.createLIBORMarketModel(1, 1, 2.0, 0.25,
				0.5, curves, CURVE, new double[] { 0.0 }, 0.5);
		assertEquals(4, model.getTimeDiscretization().getNumberOfTimeSteps());
		assertEquals(0.5, model.getTimeDiscretization().getTimeStep(0), 0.0);
		assertThrows(IllegalArgumentException.class, () -> ModelFactory.getModel(CURVE, REFERENCE, 1.0));
		assertThrows(IllegalArgumentException.class, () -> ModelFactory.getModel(CURVE, REFERENCE.plusDays(1), Double.NaN));
		assertThrows(IllegalArgumentException.class, () -> ModelFactory.createLIBORMarketModel(1, 1, 1.0, 0.25,
				new TimeDiscretizationFromArray(0.0, 2.0), curves, CURVE, new double[] { 0.0 }, 0.5));
	}

	private static void assertContainsDate(TimeDiscretization times, LocalDate start, LocalDate date) {
		double time = FloatingpointDate.getFloatingPointDateFromDate(start, date);
		int index = times.getTimeIndex(time);
		assertTrue("Missing observation date " + date, index >= 0);
		assertEquals(time, times.getTime(index), 1E-14);
	}
}
