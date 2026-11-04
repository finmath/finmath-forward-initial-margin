package net.finmath.lch.initialmargin.swapclear.aggregationscheme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.lch.initialmargin.simulation.modeldata.HistoricalZeroRateSource;
import net.finmath.lch.initialmargin.simulation.modeldata.RandomVariableSeries;
import net.finmath.lch.initialmargin.simulation.modeldata.StochasticCurve;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGrid;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.simulation.scenarios.ScenarioFactory;
import net.finmath.lch.initialmargin.simulation.scenarios.ScenarioFactory.Simulation;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.Currency;
import net.finmath.lch.initialmargin.swapclear.evaluation.MarginValuationAdjustment;
import net.finmath.lch.initialmargin.swapclear.evaluation.PortfolioFactory;
import net.finmath.lch.initialmargin.swapclear.products.LchSwap;
import net.finmath.lch.initialmargin.swapclear.products.LocalPortfolio;
import net.finmath.lch.initialmargin.swapclear.products.components.LchNotionalFromConstant;
import net.finmath.lch.initialmargin.swapclear.products.components.LchSwapLeg;
import net.finmath.lch.initialmargin.swapclear.sensitivities.ForwardAndDiscountSensitivities;
import net.finmath.lch.initialmargin.swapclear.sensitivities.AadZeroRateSensitivities;
import net.finmath.lch.initialmargin.swapclear.sensitivities.Sensitivities;
import net.finmath.lch.initialmargin.swapclear.sensitivities.SensitivityComponentsForSwapLeg;
import net.finmath.lch.initialmargin.swapclear.sensitivities.SensitivityMatrix;
import net.finmath.lch.initialmargin.valuation.CurveValuation;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.Period;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleFromPeriods;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;
import net.finmath.time.daycount.DayCountConvention_ACT_365;

public class DeltaInitialMarginIntegrationTest {

    private static final LocalDate REFERENCE = LocalDate.of(2024, 2, 5);
    private static final LocalDateTime REFERENCE_TIME = REFERENCE.atStartOfDay();
    private static final TenorGrid GRID = grid();
    private static final double EPS = 1E-11;

    @Test
    public void deltaPnlUsesItsOwnRowsAndRetainsEveryRevaluationBucket() throws CalculationException {
        SensitivityMatrix delta = new SensitivityMatrix();
        delta.addValue(1.0, 1.0, new RandomVariableFromDoubleArray(0.0, new double[] {2.0, 4.0}));
        delta.addValue(3.0, 3.0, new RandomVariableFromDoubleArray(0.0, new double[] {-3.0, 1.0}));
        InitialMarginContainer data = PairsInitialMargin.getDeltaScenarioData(delta, handScenarios(), REFERENCE_TIME,
                Simulation.EXPECTED_SHORTFALL_4, GRID);

        assertEquals(3, data.getCurveScenarios(Simulation.EXPECTED_SHORTFALL_4).getCurveScenarios().size());
        assertEquals(4, data.getPnlSeries(Simulation.EXPECTED_SHORTFALL_4).getSeries().size());
        for (int k = 1; k <= 4; k++) {
            LocalDateTime date = REFERENCE_TIME.minusDays(k);
            RandomVariable pnl = data.getPnlSeries(Simulation.EXPECTED_SHORTFALL_4).getValue(date);
            assertEquals(-8.0 * k, pnl.get(0), EPS);
            assertEquals(-2.0 * k, pnl.get(1), EPS);
            // The middle shock must not be replaced by interpolation from the two nonzero-delta buckets.
            assertEquals(-0.0007 * k, data.getCurveScenarios(Simulation.EXPECTED_SHORTFALL_4)
                    .getCurveShifts(date).getRate(2.0, null).get(0), EPS);
        }
    }

    @Test
    public void zeroDeltaKeepsScenarioDatesAndInconsistentTenorDatesAreRejected() throws CalculationException {
        InitialMarginContainer data = PairsInitialMargin.getDeltaScenarioData(new SensitivityMatrix(), handScenarios(),
                REFERENCE_TIME, Simulation.EXPECTED_SHORTFALL_4, GRID);
        assertEquals(4, data.getPnlSeries(Simulation.EXPECTED_SHORTFALL_4).getSeries().size());
        for (RandomVariable pnl : data.getPnlSeries(Simulation.EXPECTED_SHORTFALL_4).getSeries().values()) {
            assertEquals(0.0, pnl.get(0), 0.0);
        }
        ScenarioFactory missingDate = new ScenarioFactory(Simulation.EXPECTED_SHORTFALL_4) {
            @Override
            public RandomVariableSeries getTenorPointScenarios(LocalDateTime date, double tenor) throws CalculationException {
                RandomVariableSeries series = handScenarios().getTenorPointScenarios(date, tenor);
                if (tenor == 2.0) {
                    series.getSeries().remove(REFERENCE_TIME.minusDays(1));
                }
                return series;
            }
        };
        assertThrows(CalculationException.class, () -> PairsInitialMargin.getDeltaScenarioData(
                new SensitivityMatrix(), missingDate, REFERENCE_TIME, Simulation.EXPECTED_SHORTFALL_4, GRID));
    }

    @Test
    public void mvaDeltaPnlNeverRequestsGammaAndSeparatesValuationFromScenarioCutoff() throws CalculationException {
        ZeroRateModel model = model(70, true);
        LocalDateTime evaluation = REFERENCE.plusDays(7).atStartOfDay();
        List<LocalDateTime> sensitivityDates = new ArrayList<>();
        Sensitivities deltaOnly = new Sensitivities() {
            @Override
            public SensitivityMatrix getDeltaSensitivities(LocalDateTime date) {
                sensitivityDates.add(date);
                SensitivityMatrix delta = new SensitivityMatrix();
                delta.addValue(1.0, 1.0, new RandomVariableFromDoubleArray(0.0, new double[] {2.0, 4.0}));
                return delta;
            }
            @Override
            public SensitivityMatrix getGammaSensitivities(LocalDateTime date) {
                throw new AssertionError("A delta-only calculation must not request gamma.");
            }
        };
        RandomVariable frozen = MarginValuationAdjustment.getInitialMarginWithTaylorPnL(evaluation, model, deltaOnly,
                handScenarios(), Simulation.EXPECTED_SHORTFALL_4, true, false, GRID);
        RandomVariable moving = MarginValuationAdjustment.getInitialMarginWithTaylorPnL(evaluation, model, deltaOnly,
                handScenarios(), Simulation.EXPECTED_SHORTFALL_4, true, true, GRID);
        assertEquals(5.0, frozen.get(0), EPS);
        assertEquals(10.0, frozen.get(1), EPS);
        assertEquals(10.0, moving.get(0), EPS);
        assertEquals(20.0, moving.get(1), EPS);
        assertEquals(Arrays.asList(evaluation, evaluation), sensitivityDates);
    }

    @Test
    public void fullRevaluationUsesCompleteGridAndFrozenOrMovingShocks() throws CalculationException {
        ZeroRateModel model = model(70, true);
        LocalPortfolio portfolio = couponPortfolio();
        CurveValuation directCoupon = (date, curves, shifts, path) -> {
            double remainingTime = FloatingpointDate.getFloatingPointDateFromDate(date,
                    REFERENCE.plusDays(730).atStartOfDay());
            return curves.getDiscountFactor(date, remainingTime, shifts, path).mult(10.0);
        };
        LocalDateTime evaluation = REFERENCE.plusDays(7).atStartOfDay();
        for (boolean moving : new boolean[] {false, true}) {
            RandomVariable actual = MarginValuationAdjustment.getInitialMarginWithFullRevaluation(
                    evaluation, model, portfolio, Simulation.EXPECTED_SHORTFALL_4, true, moving, GRID);
            RandomVariable generic = MarginValuationAdjustment.getInitialMarginWithFullRevaluation(
                    evaluation, model, directCoupon, Simulation.EXPECTED_SHORTFALL_4, true, moving, GRID);
            LocalDateTime cutoff = moving ? evaluation : REFERENCE_TIME;
            ScenarioFactory scenarios = new ScenarioFactory(model, Simulation.EXPECTED_SHORTFALL_4);
            RandomVariableSeries first = scenarios.getTenorPointScenarios(cutoff, 1.0);
            RandomVariableSeries second = scenarios.getTenorPointScenarios(cutoff, 2.0);
            double maturity = FloatingpointDate.getFloatingPointDateFromDate(evaluation,
                    REFERENCE.plusDays(730).atStartOfDay());
            for (int path = 0; path < 2; path++) {
                double zeroRate = model.getZeroRate(evaluation.toLocalDate(), maturity).get(path);
                List<Double> losses = new ArrayList<>();
                for (LocalDateTime date : first.getSeries().keySet()) {
                    double shift = (2.0 - maturity) * first.getValue(date).get(path)
                            + (maturity - 1.0) * second.getValue(date).get(path);
                    // One fixed coupon: notional100 * rate0.05 * accrual2 =10.
                    losses.add(10.0 * (Math.exp(-(zeroRate + shift) * maturity) - Math.exp(-zeroRate * maturity)));
                }
                losses.sort(Double::compare);
                double expected = Math.abs(losses.subList(0, 4).stream().mapToDouble(Double::doubleValue).average().getAsDouble());
                assertEquals(expected, actual.get(path), EPS);
                assertEquals(expected, generic.get(path), EPS);
            }
        }
    }

    @Test
    public void defaultPairsAadDeltaAgreesWithInjectedAnalyticalDeltaForFixedCoupon() throws CalculationException {
        // The public ES6 contract needs 2500 returns; these are small scalar curves, without a simulation.
        ZeroRateModel model = model(2505, false);
        LocalPortfolio portfolio = couponPortfolio();
        Sensitivities analytical = new ForwardAndDiscountSensitivities(
                new SensitivityComponentsForSwapLeg(portfolio.getSwaps(), model, model), GRID);
        RandomVariable expected = PairsInitialMargin.getBaseInitialMargin(REFERENCE_TIME, model, portfolio,
                false, false, analytical, GRID);
        RandomVariable actual = PairsInitialMargin.getBaseInitialMargin(REFERENCE_TIME, model, portfolio, false, false);
        assertTrue(actual.get(0) > 0.0);
        assertEquals(expected.get(0), actual.get(0), EPS);
    }

    @Test
    public void genericSwapPairsUsesEitherProviderAndPreservesLegacyPortfolioEntry() throws CalculationException {
        ZeroRateModel model = model(2505, true);
        LchSwap swap = PortfolioFactory.create10YPayerSwap();
        CurveValuation valuation = selectedArrayValuation(swap);
        Sensitivities aad = new AadZeroRateSensitivities(valuation, model, GRID);
        Sensitivities analytical = new ForwardAndDiscountSensitivities(swap, model, GRID);
        LocalPortfolio portfolio = PortfolioFactory.getSwapPortfolio(swap).getLocalPortfolio(Currency.EUR);
        for (LocalDateTime evaluation : List.of(REFERENCE_TIME, REFERENCE.plusDays(7).atStartOfDay())) {
            RandomVariable expected = PairsInitialMargin.getBaseInitialMargin(evaluation, model, swap,
                    true, false, analytical, GRID);
            RandomVariable actual = PairsInitialMargin.getBaseInitialMargin(evaluation, model, valuation,
                    true, false, aad, GRID);
            RandomVariable legacy = PairsInitialMargin.getBaseInitialMargin(evaluation, model, portfolio,
                    true, false, aad, GRID);
            for (int path = 0; path < 2; path++) {
                assertTrue(actual.get(path) > 0.0);
                assertEquals(expected.get(path), actual.get(path), 1E-8);
                assertEquals(actual.get(path), legacy.get(path), 1E-8);
            }
        }
    }

    @Test
    public void genericValuationWorksWithAadAndPrecomputedFullRevaluationScenarios() throws CalculationException {
        ZeroRateModel model = model(70, true);
        LocalDateTime evaluation = REFERENCE.plusDays(7).atStartOfDay();
        CurveValuation shortDiscountBond = selectedArrayValuation((date, curves, shifts, path) ->
                curves.getDiscountFactor(date, 2.0, shifts, path).mult(-10.0));
        Sensitivities aad = new AadZeroRateSensitivities(shortDiscountBond, model, GRID);
        RandomVariable full = PairsInitialMargin.getInitialMarginWithFullRevaluation(evaluation, model,
                shortDiscountBond, PairsInitialMargin.getCurveScenarios(handScenarios(), REFERENCE_TIME, GRID),
                Simulation.EXPECTED_SHORTFALL_4, true);
        RandomVariable delta = MarginValuationAdjustment.getInitialMarginWithTaylorPnL(evaluation, model, aad,
                handScenarios(), Simulation.EXPECTED_SHORTFALL_4, true, false, GRID);
        for (int path = 0; path < 2; path++) {
            double baseDiscountFactor = Math.exp(-2.0 * model.getZeroRate(evaluation.toLocalDate(), 2.0).get(path));
            double expectedFull = 0.0;
            for (int k = 1; k <= 4; k++) {
                expectedFull += 10.0 * baseDiscountFactor * (Math.exp(2.0 * 0.0007 * k) - 1.0) / 4.0;
            }
            assertEquals(expectedFull, full.get(path), EPS);
            assertEquals(20.0 * baseDiscountFactor * 0.0007 * 2.5, delta.get(path), EPS);
        }
        assertThrows(CalculationException.class, () -> PairsInitialMargin.getInitialMarginFromLosses(
                List.of(new Scalar(-1.0)), Simulation.EXPECTED_SHORTFALL_4));
    }

    private static CurveValuation selectedArrayValuation(CurveValuation valuation) {
        return (date, model, shifts, path) -> {
            RandomVariable value = valuation.getValue(date, model, shifts, path);
            // A selected realization need not use Scalar as its implementation.
            return path == null ? value : new RandomVariableFromDoubleArray(value.getFiltrationTime(),
                    new double[] {value.get(0)});
        };
    }

    private static ScenarioFactory handScenarios() {
        return new ScenarioFactory(Simulation.EXPECTED_SHORTFALL_4) {
            @Override
            public RandomVariableSeries getTenorPointScenarios(LocalDateTime cutoff, double tenor) {
                double factor = cutoff.isAfter(REFERENCE_TIME) ? 2.0 : 1.0;
                double shock = tenor == 1.0 ? -0.0001 : tenor == 2.0 ? -0.0007 : 0.0002;
                HashMap<LocalDateTime, RandomVariable> values = new HashMap<>();
                for (int k = 1; k <= 4; k++) {
                    values.put(REFERENCE_TIME.minusDays(k), new Scalar(factor * k * shock));
                }
                return new RandomVariableSeries(values);
            }
        };
    }

    private static ZeroRateModel model(int observations, boolean simulated) throws CalculationException {
        NavigableMap<LocalDateTime, StochasticCurve> history = new TreeMap<>();
        BusinessdayCalendarExcludingWeekends calendar = new BusinessdayCalendarExcludingWeekends();
        for (LocalDate date = REFERENCE; history.size() < observations; date = date.minusDays(1)) {
            if (calendar.isBusinessday(date)) {
                double offset = 0.001 * Math.sin(0.13 * ChronoUnit.DAYS.between(REFERENCE, date));
                StochasticCurve curve = new StochasticCurve();
                for (double tenor : GRID.getFixings()) {
                    curve.addRate(tenor, new Scalar(0.02 + loading(tenor) * offset));
                }
                history.put(date.atStartOfDay(), curve);
            }
        }
        HistoricalZeroRateSource historical = new HistoricalZeroRateSource(history);
        if (!simulated) {
            return new ZeroRateModel(historical, calendar);
        }
        return new ZeroRateModel(historical, (date, tenor) -> {
            long days = ChronoUnit.DAYS.between(REFERENCE, date);
            double shift = loading(tenor) * days * 0.0002;
            return new RandomVariableFromDoubleArray(days / 365.0, new double[] {0.02 + shift, 0.02 - shift});
        }, REFERENCE, calendar, 2, EPS);
    }

    private static double loading(double tenor) {
        if (tenor <= 1.0) return 1.0;
        if (tenor <= 2.0) return 1.0 + 3.0 * (tenor - 1.0);
        if (tenor <= 3.0) return 4.0 - 6.0 * (tenor - 2.0);
        return -2.0;
    }

    private static LocalPortfolio couponPortfolio() {
        LocalDate payment = REFERENCE.plusDays(730);
        Schedule schedule = new ScheduleFromPeriods(REFERENCE, new DayCountConvention_ACT_365(),
                new Period(REFERENCE, payment, REFERENCE, payment));
        LchSwapLeg coupon = new LchSwapLeg(schedule, new LchNotionalFromConstant(100.0), null, 0.05, false, false);
        return new LocalPortfolio(Currency.EUR) {
            @Override
            public List<LchSwapLeg> getSwaps() { return List.of(coupon); }
            @Override
            public double getLastPaymentDate() { return 2.0; }
        };
    }

    private static TenorGrid grid() {
        LinkedHashMap<Double, String> points = new LinkedHashMap<>();
        points.put(1.0, "1Y");
        points.put(2.0, "2Y");
        points.put(3.0, "3Y");
        return new TenorGrid(points);
    }
}
