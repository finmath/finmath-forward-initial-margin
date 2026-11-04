package net.finmath.lch.initialmargin.swapclear.sensitivities;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

import org.junit.Test;

import net.finmath.lch.initialmargin.simulation.modeldata.StochasticCurve;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGrid;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.Currency;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.CurveName;
import net.finmath.lch.initialmargin.swapclear.products.LchSwap;
import net.finmath.lch.initialmargin.swapclear.products.components.LchNotionalFromConstant;
import net.finmath.lch.initialmargin.swapclear.products.components.LchSwapLeg;
import net.finmath.lch.initialmargin.swapclear.products.indices.LchAbstractIndex;
import net.finmath.lch.initialmargin.swapclear.products.indices.LchFixedCoupon;
import net.finmath.lch.initialmargin.swapclear.products.indices.LchLIBORIndex;
import net.finmath.lch.initialmargin.swapclear.products.indices.LchLinearCombinationIndex;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.Period;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleFromPeriods;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar.DateRollConvention;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;
import net.finmath.time.daycount.DayCountConvention_ACT_360;

/** Independent reference checks against shifted local prices and the AAD engine. */
public class AnalyticZeroRateSensitivitiesTest {
    private static final LocalDate REFERENCE = LocalDate.of(2024, 2, 5);
    private static final double NOTIONAL = 1_000_000.0;
    private static final TenorGrid GRID = grid(1.0 / 365.0, 0.05, 0.15, 0.3, 0.5, 1.0);

    @Test
    public void scaledSpreadAndDifferentForwardPaymentAccrualDatesMatchEachRiskContribution() throws Exception {
        ZeroRateModel model = model();
        LchAbstractIndex index = new LchLinearCombinationIndex(2.0, new LchLIBORIndex(2.0 / 365.0, 90.0 / 365.0),
                -3.0, new LchFixedCoupon(0.01));
        LchSwapLeg leg = leg(index, 0.004, false, false);
        LocalDateTime date = REFERENCE.plusDays(10).atStartOfDay();
        assertContributions(leg, model, date);
        assertContributions(leg(index, 0.004, true, false), model, date);

        // Parallel-zero-shift derivative derived independently from N alpha P(p) [2 F - .03 + .004].
        double start = 22.0 / 365.0;
        double length = 90.0 / 365.0;
        double payment = 125.0 / 365.0;
        RandomVariable accumulation = model.getDiscountFactor(date, start, null, null)
                .div(model.getDiscountFactor(date, start + length, null, null));
        RandomVariable coupon = accumulation.sub(1.0).div(length).mult(2.0).sub(0.026);
        RandomVariable expected = accumulation.mult(2.0).sub(coupon.mult(payment))
                .mult(model.getDiscountFactor(date, payment, null, null)).mult(NOTIONAL * (92.0 / 360.0) * 1E-4);
        assertPaths(expected, sum(analytical(List.of(leg), model).getDeltaSensitivities(date)), 1E-10);
    }

    @Test
    public void nestedCombinationRetainsSeparateForwardMaturitiesAndCalendarPeriods() throws Exception {
        ZeroRateModel model = model();
        LchLIBORIndex calendar = new LchLIBORIndex("3M", "EUR", "3M",
                new BusinessdayCalendarExcludingWeekends(), DateRollConvention.MODIFIED_FOLLOWING);
        LchAbstractIndex index = new LchLinearCombinationIndex(1.7,
                new LchLinearCombinationIndex(0.4, calendar, -0.8, new LchLIBORIndex(4.0 / 365.0, 61.0 / 365.0)),
                0.6, new LchFixedCoupon(0.025));
        assertTrue(Double.isNaN(calendar.getPeriodLength()));
        assertContributions(leg(index, 0.002, false, false), model, REFERENCE.plusDays(10).atStartOfDay());
    }

    @Test
    public void fixingBoundaryFreezesAllForwardTermsButKeepsPaymentDiscountRisk() throws Exception {
        ZeroRateModel model = model();
        LchAbstractIndex index = new LchLinearCombinationIndex(1.8, new LchLIBORIndex(2.0 / 365.0, 90.0 / 365.0),
                -0.5, new LchFixedCoupon(0.02));
        LchSwapLeg leg = leg(index, 0.003, false, false);
        for (int day : new int[] {29, 30, 31}) {
            LocalDateTime date = REFERENCE.plusDays(day).atStartOfDay();
            assertContributions(leg, model, date);
            if (day >= 30) {
                SensitivityComponentsForSwapLeg components = new SensitivityComponentsForSwapLeg(List.of(leg), model, model);
                assertEquals(0.0, sum(new ForwardSensitivities(components, GRID).getDeltaSensitivities(date)).abs().getMax(), 0.0);
                assertPaths(leg.getValue(date, model, null, null).mult(-(135.0 - day) / 365.0 * 1E-4),
                        sum(new ForwardAndDiscountSensitivities(components, GRID).getDeltaSensitivities(date)), 1E-10);
            }
        }
    }

    @Test
    public void sameSwapConstructorIncludesBothLegsAndSupportsSingleLeg() throws Exception {
        ZeroRateModel model = model();
        LchSwapLeg receiver = leg(new LchLIBORIndex(2.0 / 365.0, 90.0 / 365.0), 0.003, false, false);
        LchSwapLeg payer = leg(null, 0.035, true, false);
        LocalDateTime date = REFERENCE.plusDays(12).atStartOfDay();
        for (LchSwapLeg payLeg : new LchSwapLeg[] {payer, null}) {
            LchSwap swap = new LchSwap(Currency.EUR, CurveName.EUR_EURIBOR_3M, receiver, payLeg);
            List<LchSwapLeg> legs = payLeg == null ? List.of(receiver) : List.of(receiver, payLeg);
            SensitivityMatrix actual = new ForwardAndDiscountSensitivities(swap, model, GRID).getDeltaSensitivities(date);
            SensitivityMatrix aad = new AadZeroRateSensitivities(legs, model, GRID).getDeltaSensitivities(date);
            assertMatrices(aad, actual, 1E-10);
        }
    }

    @Test
    public void expiredAndEmptyPortfoliosAreZeroAndGammaIsExplicitlyUnsupported() throws Exception {
        ZeroRateModel model = model();
        ForwardAndDiscountSensitivities reference = analytical(List.of(leg(null, 0.04, false, false)), model);
        assertEquals(0.0, sum(reference.getDeltaSensitivities(REFERENCE.plusDays(135).atStartOfDay())).getAverage(), 0.0);
        assertEquals(0.0, sum(analytical(List.of(), model).getDeltaSensitivities(REFERENCE.atStartOfDay())).getAverage(), 0.0);
        assertThrows(UnsupportedOperationException.class, () -> reference.getGammaSensitivities(REFERENCE.atStartOfDay()));
        assertThrows(IllegalArgumentException.class, () -> reference.getDeltaSensitivities(REFERENCE.atTime(12, 0)));
    }

    @Test
    public void unsupportedIndexAndNotionalExchangeAreRejectedInsteadOfReturningPartialRisk() throws Exception {
        ZeroRateModel model = model();
        LchAbstractIndex unknown = new LchFixedCoupon(0.03) {
            private static final long serialVersionUID = 1L;
        };
        assertThrows(IllegalArgumentException.class,
                () -> analytical(List.of(leg(unknown, 0.0, false, false)), model));
        assertThrows(IllegalArgumentException.class,
                () -> analytical(List.of(leg(null, 0.03, false, true)), model));
    }

    private static void assertContributions(LchSwapLeg leg, ZeroRateModel model, LocalDateTime date) throws Exception {
        SensitivityComponentsForSwapLeg components = new SensitivityComponentsForSwapLeg(List.of(leg), model, model);
        SensitivityMatrix forward = new ForwardSensitivities(components, GRID).getDeltaSensitivities(date);
        SensitivityMatrix discount = new DiscountSensitivities(components, GRID).getDeltaSensitivities(date);
        SensitivityMatrix total = new ForwardAndDiscountSensitivities(components, GRID).getDeltaSensitivities(date);
        assertMatrices(new AadZeroRateSensitivities(List.of(leg), model, GRID).getDeltaSensitivities(date), total, 1E-10);
        double epsilon = 1E-6;
        for (double tenor : GRID.getFixings()) {
            StochasticCurve up = shifts(tenor, epsilon);
            StochasticCurve down = shifts(tenor, -epsilon);
            RandomVariable projectionBump = leg.getValue(date, model, up, model, null, null)
                    .sub(leg.getValue(date, model, down, model, null, null)).mult(1E-4 / (2 * epsilon));
            RandomVariable discountBump = leg.getValue(date, model, null, model, up, null)
                    .sub(leg.getValue(date, model, null, model, down, null)).mult(1E-4 / (2 * epsilon));
            assertPaths(projectionBump, delta(forward, tenor), 3E-6);
            assertPaths(discountBump, delta(discount, tenor), 3E-6);
            assertPaths(delta(forward, tenor).add(delta(discount, tenor)), delta(total, tenor), 1E-10);
        }
    }

    private static ForwardAndDiscountSensitivities analytical(List<LchSwapLeg> legs, ZeroRateModel model) {
        return new ForwardAndDiscountSensitivities(new SensitivityComponentsForSwapLeg(legs, model, model), GRID);
    }

    private static LchSwapLeg leg(LchAbstractIndex index, double spread, boolean payer, boolean exchange) {
        Schedule schedule = new ScheduleFromPeriods(REFERENCE, new DayCountConvention_ACT_360(),
                new Period(REFERENCE.plusDays(30), REFERENCE.plusDays(135),
                        REFERENCE.plusDays(32), REFERENCE.plusDays(124)));
        return new LchSwapLeg(schedule, new LchNotionalFromConstant(NOTIONAL), index, spread, payer, exchange);
    }

    private static ZeroRateModel model() throws Exception {
        return new ZeroRateModel(null, (date, tenor) -> {
            double time = ChronoUnit.DAYS.between(REFERENCE, date) / 365.0;
            return new RandomVariableFromDoubleArray(time,
                    new double[] {0.02 + 0.002 * tenor + 0.001 * time, 0.04 + 0.004 * tenor - 0.002 * time});
        }, REFERENCE, new BusinessdayCalendarExcludingWeekends(), 2, 0.0);
    }

    private static TenorGrid grid(double... tenors) {
        LinkedHashMap<Double, String> points = new LinkedHashMap<>();
        for (double tenor : tenors) {
            points.put(tenor, Double.toString(tenor));
        }
        return new TenorGrid(points);
    }

    private static StochasticCurve shifts(double selected, double value) {
        StochasticCurve shifts = new StochasticCurve();
        for (double tenor : GRID.getFixings()) {
            shifts.addRate(tenor, new Scalar(tenor == selected ? value : 0.0));
        }
        return shifts;
    }

    private static RandomVariable delta(SensitivityMatrix matrix, double tenor) {
        TreeMap<Double, RandomVariable> row = matrix.getSensitivityMatrix().get(tenor);
        return row == null ? new Scalar(0.0) : row.getOrDefault(tenor, new Scalar(0.0));
    }

    private static RandomVariable sum(SensitivityMatrix matrix) {
        RandomVariable sum = new Scalar(0.0);
        for (double tenor : GRID.getFixings()) {
            sum = sum.add(delta(matrix, tenor));
        }
        return sum;
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
}
