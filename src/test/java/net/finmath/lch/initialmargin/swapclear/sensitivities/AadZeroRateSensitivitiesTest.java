package net.finmath.lch.initialmargin.swapclear.sensitivities;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

import org.junit.Test;

import net.finmath.lch.initialmargin.simulation.modeldata.HistoricalZeroRateSource;
import net.finmath.lch.initialmargin.simulation.modeldata.SimulatedZeroRateSource;
import net.finmath.lch.initialmargin.simulation.modeldata.StochasticCurve;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGrid;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.swapclear.products.components.LchNotionalFromConstant;
import net.finmath.lch.initialmargin.swapclear.products.components.LchSwapLeg;
import net.finmath.lch.initialmargin.swapclear.products.indices.LchAbstractIndex;
import net.finmath.lch.initialmargin.swapclear.products.indices.LchLIBORIndex;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.CurveName;
import net.finmath.lch.initialmargin.swapclear.libormodels.ModelFactory;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.Period;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleFromPeriods;
import net.finmath.time.daycount.DayCountConvention_ACT_365;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;

public class AadZeroRateSensitivitiesTest {
    private static final LocalDate REFERENCE = LocalDate.of(2024, 2, 5);
    private static final double NOTIONAL = 1_000_000.0;
    private static final TenorGrid GRID = grid(1.0 / 365.0, 30.0 / 365.0, 0.25, 0.5, 1.0, 2.0);

    @Test
    public void fixedCouponMatchesAnalyticDeltaAndInterpolationOnEveryPath() throws Exception {
        ZeroRateModel model = model(false);
        LchSwapLeg leg = leg(0, 0, 180, 300, null, 0.04, false);
        LocalDateTime date = REFERENCE.plusDays(10).atStartOfDay();
        SensitivityMatrix aad = new AadZeroRateSensitivities(List.of(leg), model, GRID).getDeltaSensitivities(date);
        SensitivityMatrix analytic = new ForwardAndDiscountSensitivities(
                new SensitivityComponentsForSwapLeg(List.of(leg), model, model), GRID).getDeltaSensitivities(date);
        double tau = 290.0 / 365.0;
        RandomVariable value = leg.getValue(date, model, null, null);
        double upperWeight = (tau - 0.5) / 0.5;
        for (int path = 0; path < 2; path++) {
            assertEquals(-tau * value.get(path) * 1E-4 * (1 - upperWeight), delta(aad, 0.5).get(path), 1E-10);
            assertEquals(-tau * value.get(path) * 1E-4 * upperWeight, delta(aad, 1.0).get(path), 1E-10);
        }
        assertMatrices(analytic, aad, 1E-10);
        assertEquals(0.0, delta(aad, 0.25).getAverage(), 0.0);
    }

    @Test
    public void forwardAndDiscountRiskMatchesRevaluationWithPaymentLagAndDifferentAccrual() throws Exception {
        ZeroRateModel model = model(false);
        LchSwapLeg leg = leg(30, 32, 124, 129, new LchLIBORIndex(2.0 / 365.0, 90.0 / 365.0), 0.003, false);
        assertBumps(model, List.of(leg), REFERENCE.plusDays(10).atStartOfDay());
    }

    @Test
    public void fixingBoundaryKeepsCouponFixedButRetainsDiscountRisk() throws Exception {
        ZeroRateModel model = model(true);
        LchSwapLeg leg = leg(30, 30, 120, 125, new LchLIBORIndex(0.0, 90.0 / 365.0), 0.0, false);
        for (int day : new int[] {29, 30, 31}) {
            LocalDateTime date = REFERENCE.plusDays(day).atStartOfDay();
            assertBumps(model, List.of(leg), date);
            if (day >= 30) {
                SensitivityMatrix delta = new AadZeroRateSensitivities(List.of(leg), model, GRID).getDeltaSensitivities(date);
                RandomVariable sum = new Scalar(0.0);
                for (double tenor : GRID.getFixings()) {
                    sum = sum.add(delta(delta, tenor));
                }
                RandomVariable expected = leg.getValue(date, model, null, null).mult(-(125.0 - day) / 365.0 * 1E-4);
                assertPaths(expected, sum, 1E-10);
            }
        }
    }

    @Test
    public void historicalStartAndModelAadBaseProduceTheSameLocalDeltas() throws Exception {
        StochasticCurve curve = new StochasticCurve();
        curve.addRate(0.25, new Scalar(0.03));
        curve.addRate(2.0, new Scalar(0.03));
        TreeMap<LocalDateTime, StochasticCurve> history = new TreeMap<>();
        history.put(REFERENCE.atStartOfDay(), curve);
        ZeroRateModel historical = new ZeroRateModel(new HistoricalZeroRateSource(history), new BusinessdayCalendarExcludingWeekends());
        LchSwapLeg leg = leg(0, 0, 30, 30, null, 0.04, false);
        assertBumps(historical, List.of(leg), REFERENCE.atStartOfDay());
        SensitivityMatrix shortDelta = new AadZeroRateSensitivities(List.of(leg), historical, GRID)
                .getDeltaSensitivities(REFERENCE.atStartOfDay());
        assertTrue(delta(shortDelta, 30.0 / 365.0).getAverage() < 0.0);
        assertEquals(0.0, delta(shortDelta, 0.25).getAverage(), 0.0);

        LocalDateTime date = REFERENCE.plusDays(10).atStartOfDay();
        SensitivityMatrix ordinary = new AadZeroRateSensitivities(List.of(leg), model(false), GRID).getDeltaSensitivities(date);
        SensitivityMatrix aadBase = new AadZeroRateSensitivities(List.of(leg), model(true), GRID).getDeltaSensitivities(date);
        assertMatrices(ordinary, aadBase, 1E-12);
        for (double tenor : GRID.getFixings()) {
            assertFalse(delta(aadBase, tenor) instanceof RandomVariableDifferentiable);
        }
    }

    @Test
    public void actualDailyLmmWorksWithoutAnAadSimulationGraph() throws Exception {
        LocalDate end = REFERENCE.plusDays(10);
        net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel simulation =
                ModelFactory.getModel(CurveName.EUR_EURIBOR_3M, end, 2.0);
        ZeroRateModel model = new ZeroRateModel(new SimulatedZeroRateSource(simulation, REFERENCE,
                simulation.getLiborPeriodDiscretization().getLastTime()), new BusinessdayCalendarExcludingWeekends());
        LchSwapLeg leg = leg(30, 32, 124, 129, new LchLIBORIndex(2.0 / 365.0, 90.0 / 365.0), 0.003, false);
        assertBumps(model, List.of(leg), REFERENCE.atStartOfDay());
        assertBumps(model, List.of(leg), end.atStartOfDay());
    }

    @Test
    public void portfolioNettingExpiryAndEmptyPortfoliosRetainCompleteZeroGrid() throws Exception {
        ZeroRateModel model = model(false);
        LchSwapLeg receiver = leg(0, 0, 90, 120, null, 0.04, false);
        LchSwapLeg payer = leg(0, 0, 90, 120, null, 0.04, true);
        AadZeroRateSensitivities provider = new AadZeroRateSensitivities(List.of(receiver, payer), model, GRID);
        assertZero(provider.getDeltaSensitivities(REFERENCE.atStartOfDay()));
        assertZero(new AadZeroRateSensitivities(List.of(receiver), model, GRID)
                .getDeltaSensitivities(REFERENCE.plusDays(120).atStartOfDay()));
        assertZero(new AadZeroRateSensitivities(List.of(), model, GRID).getDeltaSensitivities(REFERENCE.atStartOfDay()));
        assertThrows(UnsupportedOperationException.class, () -> provider.getGammaSensitivities(REFERENCE.atStartOfDay()));
        assertThrows(IllegalArgumentException.class, () -> provider.getDeltaSensitivities(REFERENCE.atTime(12, 0)));
    }

    @Test
    public void valuationViewsLeaveScenarioHistoryAndOriginalPricingUntouched() throws Exception {
        ZeroRateModel original = model(true);
        ZeroRateModel view = original.withPricingSource((date, tenor) -> original.getZeroRate(date, tenor).getValues().add(0.01));
        LocalDateTime end = REFERENCE.plusDays(3).atStartOfDay();
        assertPaths(original.getZeroRate(end.toLocalDate(), 0.5).add(0.01), view.getZeroRate(end.toLocalDate(), 0.5), 0.0);
        assertFalse(view.getZeroRate(end.toLocalDate(), 0.5) instanceof RandomVariableDifferentiable);
        assertPaths(original.getRateSeries(end, 0.5).lastEntry().getValue(), view.getRateSeries(end, 0.5).lastEntry().getValue(), 0.0);
        assertThrows(net.finmath.exception.CalculationException.class,
                () -> original.withPricingSource((date, tenor) -> new Scalar(Double.NaN)).getZeroRate(REFERENCE, 0.5));
    }

    private static void assertBumps(ZeroRateModel model, List<LchSwapLeg> legs, LocalDateTime date) throws Exception {
        SensitivityMatrix aad = new AadZeroRateSensitivities(legs, model, GRID).getDeltaSensitivities(date);
        double epsilon = 1E-6;
        for (double bucket : GRID.getFixings()) {
            StochasticCurve up = shifts(bucket, epsilon);
            StochasticCurve down = shifts(bucket, -epsilon);
            RandomVariable finiteDifference = new Scalar(0.0);
            for (LchSwapLeg leg : legs) {
                finiteDifference = finiteDifference.add(leg.getValue(date, model, up, null)
                        .sub(leg.getValue(date, model, down, null)).mult(1E-4 / (2 * epsilon)));
            }
            assertPaths(finiteDifference, delta(aad, bucket), 2E-6);
        }
    }

    private static StochasticCurve shifts(double bucket, double shift) {
        StochasticCurve curve = new StochasticCurve();
        for (double tenor : GRID.getFixings()) {
            curve.addRate(tenor, new Scalar(tenor == bucket ? shift : 0.0));
        }
        return curve;
    }

    private static ZeroRateModel model(boolean aad) throws Exception {
        RandomVariableDifferentiableAADFactory factory = new RandomVariableDifferentiableAADFactory();
        return new ZeroRateModel(null, (date, tenor) -> {
            double time = ChronoUnit.DAYS.between(REFERENCE, date) / 365.0;
            double[] rates = {0.02 + 0.002 * tenor + time * 0.001, 0.04 + 0.003 * tenor - time * 0.001};
            return aad ? factory.createRandomVariable(time, rates) : new RandomVariableFromDoubleArray(time, rates);
        }, REFERENCE, new BusinessdayCalendarExcludingWeekends(), 2, 0.0);
    }

    private static LchSwapLeg leg(int fixing, int start, int end, int payment, LchAbstractIndex index, double spread, boolean payer) {
        Schedule schedule = new ScheduleFromPeriods(REFERENCE, new DayCountConvention_ACT_365(),
                new Period(REFERENCE.plusDays(fixing), REFERENCE.plusDays(payment), REFERENCE.plusDays(start), REFERENCE.plusDays(end)));
        return new LchSwapLeg(schedule, new LchNotionalFromConstant(NOTIONAL), index, spread, payer, false);
    }

    private static TenorGrid grid(double... tenors) {
        LinkedHashMap<Double, String> points = new LinkedHashMap<>();
        for (double tenor : tenors) {
            points.put(tenor, Double.toString(tenor));
        }
        return new TenorGrid(points);
    }

    private static RandomVariable delta(SensitivityMatrix matrix, double tenor) {
        TreeMap<Double, RandomVariable> row = matrix.getSensitivityMatrix().get(tenor);
        return row == null ? new Scalar(0.0) : row.getOrDefault(tenor, new Scalar(0.0));
    }

    private static void assertMatrices(SensitivityMatrix expected, SensitivityMatrix actual, double tolerance) {
        for (double tenor : GRID.getFixings()) {
            assertPaths(delta(expected, tenor), delta(actual, tenor), tolerance);
        }
    }

    private static void assertPaths(RandomVariable expected, RandomVariable actual, double tolerance) {
        for (int path = 0; path < Math.max(expected.size(), actual.size()); path++) {
            assertEquals("path " + path, expected.get(path), actual.get(path), tolerance);
        }
    }

    private static void assertZero(SensitivityMatrix delta) {
        assertEquals(GRID.getFixings().size(), delta.getSensitivityMatrix().size());
        for (double tenor : GRID.getFixings()) {
            assertEquals(0.0, delta(delta, tenor).abs().getMax(), 0.0);
        }
    }
}
