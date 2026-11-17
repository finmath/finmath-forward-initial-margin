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
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGridFactory;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGridFactory.GridType;
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

/** Independent analytical delta/Hessian checks against bumped pathwise swap prices. */
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
        // The complete second derivative includes both projection/discount cross terms.
        RandomVariable expectedGamma = accumulation.mult(2.0 * length - 4.0 * payment)
                .add(coupon.mult(payment * payment))
                .mult(model.getDiscountFactor(date, payment, null, null)).mult(NOTIONAL * (92.0 / 360.0) * 1E-8);
        assertPaths(expectedGamma, sumAll(analytical(List.of(leg), model).getGammaSensitivities(date)), 1E-12);
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
                RandomVariable value = leg.getValue(date, model, null, model, null, null);
                double maturity = (135.0 - day) / 365.0;
                assertPaths(value.mult(-maturity * 1E-4),
                        sum(new ForwardAndDiscountSensitivities(components, GRID).getDeltaSensitivities(date)), 1E-10);
                assertEquals(0.0, sumAll(new ForwardSensitivities(components, GRID).getGammaSensitivities(date))
                        .abs().getMax(), 0.0);
                assertPaths(value.mult(maturity * maturity * 1E-8),
                        sumAll(new ForwardAndDiscountSensitivities(components, GRID).getGammaSensitivities(date)), 1E-12);
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
            ForwardAndDiscountSensitivities actual = new ForwardAndDiscountSensitivities(swap, model, GRID);
            assertMatrices(analytical(legs, model).getDeltaSensitivities(date), actual.getDeltaSensitivities(date), 1E-10);
            assertBumpedSwap(swap, model, date, GRID, actual.getDeltaSensitivities(date), actual.getGammaSensitivities(date));
        }
    }

    @Test
    public void fullLchGridMatchesBucketAndMixedBumpsAtAndBetweenRemainingTenorKnots() throws Exception {
        ZeroRateModel model = model();
        TenorGrid grid = TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID);
        assertEquals(37, grid.getFixings().size());
        LchSwap swap = new LchSwap(Currency.EUR, CurveName.EUR_EURIBOR_3M,
                leg(new LchLIBORIndex(2.0 / 365.0, 90.0 / 365.0), 0.002, false, false),
                leg(null, 0.031, true, false));
        ForwardAndDiscountSensitivities sensitivities = new ForwardAndDiscountSensitivities(swap, model, grid);
        // At day 2 the remaining forward start is exactly the 30-day LCH knot;
        // at day 3 it is between knots. Day 29 exercises the short end before fixing.
        for (int day : new int[] {2, 3, 29}) {
            LocalDateTime date = REFERENCE.plusDays(day).atStartOfDay();
            assertBumpedSwap(swap, model, date, grid,
                    sensitivities.getDeltaSensitivities(date), sensitivities.getGammaSensitivities(date));
        }
    }

    @Test
    public void secondOrderDirectionalTaylorRemainderDecaysCubically() throws Exception {
        ZeroRateModel model = model();
        LchSwap swap = new LchSwap(Currency.EUR, CurveName.EUR_EURIBOR_3M,
                leg(new LchLIBORIndex(2.0 / 365.0, 90.0 / 365.0), 0.004, false, false),
                leg(null, 0.035, true, false));
        LocalDateTime date = REFERENCE.plusDays(10).atStartOfDay();
        ForwardAndDiscountSensitivities sensitivities = new ForwardAndDiscountSensitivities(swap, model, GRID);
        SensitivityMatrix delta = sensitivities.getDeltaSensitivities(date);
        SensitivityMatrix gamma = sensitivities.getGammaSensitivities(date);
        double[] direction = {0.5, -0.8, 1.1, -0.6, 0.9, 0.3};
        RandomVariable value = price(swap, model, date, null);
        RandomVariable first = new Scalar(0.0);
        RandomVariable second = new Scalar(0.0);
        List<Double> tenors = GRID.getFixings();
        for (int i = 0; i < tenors.size(); i++) {
            first = first.add(delta(delta, tenors.get(i)).mult(direction[i]));
            for (int j = 0; j < tenors.size(); j++) {
                second = second.add(entry(gamma, tenors.get(i), tenors.get(j)).mult(0.5 * direction[i] * direction[j]));
            }
        }
        RandomVariable largeRemainder = null;
        for (double scaleBp : new double[] {100.0, 50.0}) {
            StochasticCurve shift = new StochasticCurve();
            for (int i = 0; i < tenors.size(); i++) {
                shift.addRate(tenors.get(i), new Scalar(scaleBp * 1E-4 * direction[i]));
            }
            RandomVariable exactPnl = price(swap, model, date, shift).sub(value);
            RandomVariable firstOrderError = exactPnl.sub(first.mult(scaleBp)).abs();
            RandomVariable remainder = exactPnl.sub(first.mult(scaleBp)).sub(second.mult(scaleBp * scaleBp)).abs();
            for (int path = 0; path < 2; path++) {
                assertTrue("Gamma must improve directional P&L on path " + path,
                        remainder.get(path) < 0.02 * firstOrderError.get(path));
                if (largeRemainder != null) {
                    assertTrue(largeRemainder.get(path) > 1E-6);
                    double ratio = remainder.get(path) / largeRemainder.get(path);
                    assertTrue("Halving a shock should divide a cubic remainder by eight: " + ratio,
                            ratio > 0.11 && ratio < 0.14);
                }
            }
            largeRemainder = remainder;
        }
    }

    @Test
    public void expiredAndEmptyPortfoliosHaveZeroDeltaAndGamma() throws Exception {
        ZeroRateModel model = model();
        ForwardAndDiscountSensitivities reference = analytical(List.of(leg(null, 0.04, false, false)), model);
        for (int day : new int[] {135, 136}) {
            LocalDateTime date = REFERENCE.plusDays(day).atStartOfDay();
            assertEquals(0.0, sum(reference.getDeltaSensitivities(date)).abs().getMax(), 0.0);
            assertEquals(0.0, sumAll(reference.getGammaSensitivities(date)).abs().getMax(), 0.0);
        }
        ForwardAndDiscountSensitivities empty = analytical(List.of(), model);
        assertEquals(0.0, sum(empty.getDeltaSensitivities(REFERENCE.atStartOfDay())).abs().getMax(), 0.0);
        assertEquals(0.0, sumAll(empty.getGammaSensitivities(REFERENCE.atStartOfDay())).abs().getMax(), 0.0);
        assertThrows(IllegalArgumentException.class, () -> reference.getDeltaSensitivities(REFERENCE.atTime(12, 0)));
        assertThrows(IllegalArgumentException.class, () -> reference.getGammaSensitivities(REFERENCE.atTime(12, 0)));
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
        LchSwap swap = new LchSwap(Currency.EUR, CurveName.EUR_EURIBOR_3M, leg, null);
        assertBumpedSwap(swap, model, date, GRID, total,
                new ForwardAndDiscountSensitivities(components, GRID).getGammaSensitivities(date));
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

    private static void assertBumpedSwap(LchSwap swap, ZeroRateModel model, LocalDateTime date,
            TenorGrid grid, SensitivityMatrix delta, SensitivityMatrix gamma) throws Exception {
        double deltaBump = 1E-6;
        double gammaBump = 2E-4;
        RandomVariable base = price(swap, model, date, null);
        List<Double> tenors = grid.getFixings();
        for (int i = 0; i < tenors.size(); i++) {
            double first = tenors.get(i);
            RandomVariable bumpedDelta = price(swap, model, date, shifts(grid, first, deltaBump, first, 0.0))
                    .sub(price(swap, model, date, shifts(grid, first, -deltaBump, first, 0.0)))
                    .mult(1E-4 / (2.0 * deltaBump));
            assertPaths(bumpedDelta, delta(delta, first), 3E-6);
            for (int j = i; j < tenors.size(); j++) {
                double second = tenors.get(j);
                RandomVariable bumpedGamma;
                if (i == j) {
                    bumpedGamma = price(swap, model, date, shifts(grid, first, gammaBump, first, 0.0))
                            .add(price(swap, model, date, shifts(grid, first, -gammaBump, first, 0.0)))
                            .sub(base.mult(2.0)).mult(1E-8 / (gammaBump * gammaBump));
                } else {
                    bumpedGamma = price(swap, model, date, shifts(grid, first, gammaBump, second, gammaBump))
                            .sub(price(swap, model, date, shifts(grid, first, gammaBump, second, -gammaBump)))
                            .sub(price(swap, model, date, shifts(grid, first, -gammaBump, second, gammaBump)))
                            .add(price(swap, model, date, shifts(grid, first, -gammaBump, second, -gammaBump)))
                            .mult(1E-8 / (4.0 * gammaBump * gammaBump));
                }
                assertPaths("gamma " + first + ", " + second + " at " + date,
                        bumpedGamma, entry(gamma, first, second), 5E-8);
                assertPaths(entry(gamma, first, second), entry(gamma, second, first), 1E-12);
            }
        }
    }

    private static RandomVariable price(LchSwap swap, ZeroRateModel model, LocalDateTime date,
            StochasticCurve shift) throws Exception {
        return swap.getValue(date, model, shift, model, shift, null);
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
        return shifts(GRID, selected, value, selected, 0.0);
    }

    private static StochasticCurve shifts(TenorGrid grid, double first, double firstValue,
            double second, double secondValue) {
        StochasticCurve shifts = new StochasticCurve();
        for (double tenor : grid.getFixings()) {
            shifts.addRate(tenor, new Scalar((tenor == first ? firstValue : 0.0) + (tenor == second ? secondValue : 0.0)));
        }
        return shifts;
    }

    private static RandomVariable delta(SensitivityMatrix matrix, double tenor) {
        return entry(matrix, tenor, tenor);
    }

    private static RandomVariable entry(SensitivityMatrix matrix, double first, double second) {
        TreeMap<Double, RandomVariable> row = matrix.getSensitivityMatrix().get(first);
        return row == null ? new Scalar(0.0) : row.getOrDefault(second, new Scalar(0.0));
    }

    private static RandomVariable sumAll(SensitivityMatrix matrix) {
        RandomVariable sum = new Scalar(0.0);
        for (TreeMap<Double, RandomVariable> row : matrix.getSensitivityMatrix().values()) {
            for (RandomVariable value : row.values()) {
                sum = sum.add(value);
            }
        }
        return sum;
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
        assertPaths("", expected, actual, tolerance);
    }

    private static void assertPaths(String description, RandomVariable expected, RandomVariable actual, double tolerance) {
        for (int path = 0; path < Math.max(expected.size(), actual.size()); path++) {
            assertEquals(description + " path " + path, expected.get(path), actual.get(path), tolerance);
        }
    }
}
