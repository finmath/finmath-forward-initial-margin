package net.finmath.lch.initialmargin.swapclear.aggregationscheme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGridFactory;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGridFactory.GridType;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.simulation.scenarios.CurveScenarios;
import net.finmath.lch.initialmargin.simulation.scenarios.ScenarioFactory;
import net.finmath.lch.initialmargin.simulation.scenarios.ScenarioFactory.Simulation;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.Currency;
import net.finmath.lch.initialmargin.swapclear.evaluation.MarginValuationAdjustment;
import net.finmath.lch.initialmargin.swapclear.evaluation.PortfolioFactory;
import net.finmath.lch.initialmargin.swapclear.products.LocalPortfolio;
import net.finmath.lch.initialmargin.swapclear.products.components.LchNotionalFromConstant;
import net.finmath.lch.initialmargin.swapclear.products.components.LchSwapLeg;
import net.finmath.lch.initialmargin.swapclear.sensitivities.ForwardAndDiscountSensitivities;
import net.finmath.lch.initialmargin.swapclear.sensitivities.SensitivityComponentsForSwapLeg;
import net.finmath.lch.initialmargin.swapclear.sensitivities.SensitivityMatrix;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.Period;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleFromPeriods;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;
import net.finmath.time.daycount.DayCountConvention_ACT_365;

public class AnalyticInitialMarginIntegrationTest {
    private static final LocalDate REFERENCE = LocalDate.of(2024, 2, 5);
    private static final LocalDateTime REFERENCE_TIME = REFERENCE.atStartOfDay();
    private static final TenorGrid GRID = grid();
    private static final TenorGrid LCH_GRID = TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID);
    private static final double EPS = 1E-10;

    @Test
    public void taylorUsesBothMixedGammaOrientationsAndRetainsZeroRiskBuckets() throws Exception {
        SensitivityMatrix delta = new SensitivityMatrix();
        delta.addValue(1.0, 1.0, paths(2.0, 4.0));
        delta.addValue(3.0, 3.0, paths(-3.0, 1.0));
        SensitivityMatrix gamma = new SensitivityMatrix();
        gamma.addValue(1.0, 1.0, paths(0.1, 0.2));
        gamma.addValue(1.0, 3.0, paths(0.3, -0.4));
        gamma.addValue(3.0, 1.0, paths(0.3, -0.4));
        gamma.addValue(3.0, 3.0, paths(0.5, 0.6));
        InitialMarginContainer data = PairsInitialMargin.getScenarioData(delta, gamma, handScenarios(), REFERENCE_TIME,
                Simulation.EXPECTED_SHORTFALL_4, GRID);
        assertEquals(3, data.getCurveScenarios(Simulation.EXPECTED_SHORTFALL_4).getCurveScenarios().size());
        for (int k = 1; k <= 4; k++) {
            LocalDateTime date = REFERENCE_TIME.minusDays(k);
            RandomVariable pnl = data.getPnlSeries(Simulation.EXPECTED_SHORTFALL_4).getValue(date);
            assertEquals(-8.0 * k + 0.45 * k * k, pnl.get(0), EPS);
            assertEquals(-2.0 * k + 2.1 * k * k, pnl.get(1), EPS);
            assertEquals(-0.0007 * k, data.getCurveScenarios(Simulation.EXPECTED_SHORTFALL_4)
                    .getCurveShifts(date).getRate(2.0, null).get(0), EPS);
        }
        InitialMarginContainer deltaOnly = PairsInitialMargin.getScenarioData(delta, new SensitivityMatrix(),
                handScenarios(), REFERENCE_TIME, Simulation.EXPECTED_SHORTFALL_4, GRID);
        assertEquals(-8.0, deltaOnly.getPnlSeries(Simulation.EXPECTED_SHORTFALL_4)
                .getValue(REFERENCE_TIME.minusDays(1)).get(0), EPS);
    }

    @Test
    public void emptySensitivitiesRetainDatesAndInconsistentScenariosFail() throws Exception {
        InitialMarginContainer data = PairsInitialMargin.getScenarioData(new SensitivityMatrix(), new SensitivityMatrix(),
                handScenarios(), REFERENCE_TIME, Simulation.EXPECTED_SHORTFALL_4, GRID);
        assertEquals(4, data.getPnlSeries(Simulation.EXPECTED_SHORTFALL_4).getSeries().size());
        for (RandomVariable pnl : data.getPnlSeries(Simulation.EXPECTED_SHORTFALL_4).getSeries().values()) {
            assertEquals(0.0, pnl.get(0), 0.0);
        }
        ScenarioFactory missing = new ScenarioFactory(Simulation.EXPECTED_SHORTFALL_4) {
            @Override
            public RandomVariableSeries getTenorPointScenarios(LocalDateTime date, double tenor) throws CalculationException {
                RandomVariableSeries series = handScenarios().getTenorPointScenarios(date, tenor);
                if (tenor == 2.0) series.getSeries().remove(REFERENCE_TIME.minusDays(1));
                return series;
            }
        };
        assertThrows(CalculationException.class, () -> PairsInitialMargin.getScenarioData(new SensitivityMatrix(),
                new SensitivityMatrix(), missing, REFERENCE_TIME, Simulation.EXPECTED_SHORTFALL_4, GRID));
    }

    @Test
    public void mvaTaylorIncludesGammaAtCurrentDateWhileFreezingOnlyScenarioCutoff() throws Exception {
        ZeroRateModel model = model(70, true);
        LocalDateTime evaluation = REFERENCE_TIME.plusDays(7);
        List<LocalDateTime> calls = new ArrayList<>();
        ForwardAndDiscountSensitivities sensitivities = new ForwardAndDiscountSensitivities(
                new SensitivityComponentsForSwapLeg(List.of(), model, model), LCH_GRID) {
            @Override
            public SensitivityMatrix getDeltaSensitivities(LocalDateTime date) {
                calls.add(date);
                SensitivityMatrix delta = new SensitivityMatrix();
                delta.addValue(1.0, 1.0, paths(2.0, 4.0));
                return delta;
            }
            @Override
            public SensitivityMatrix getGammaSensitivities(LocalDateTime date) {
                calls.add(date);
                SensitivityMatrix gamma = new SensitivityMatrix();
                gamma.addValue(1.0, 1.0, paths(0.2, 0.4));
                return gamma;
            }
        };
        RandomVariable frozen = MarginValuationAdjustment.getInitialMarginWithTaylorPnL(evaluation, model,
                sensitivities, handScenarios(), Simulation.EXPECTED_SHORTFALL_4, true, false);
        RandomVariable moving = MarginValuationAdjustment.getInitialMarginWithTaylorPnL(evaluation, model,
                sensitivities, handScenarios(), Simulation.EXPECTED_SHORTFALL_4, true, true);
        assertEquals(4.25, frozen.get(0), EPS);
        assertEquals(8.5, frozen.get(1), EPS);
        assertEquals(7.0, moving.get(0), EPS);
        assertEquals(14.0, moving.get(1), EPS);
        assertEquals(Arrays.asList(evaluation, evaluation, evaluation, evaluation), calls);
    }

    @Test
    public void expectedShortfallAndVarUseSignedLossesAndClampGainOnlyTails() throws Exception {
        List<RandomVariable> pnls = new ArrayList<>();
        for (int k = 1; k <= 20; k++) pnls.add(new Scalar(-k));
        assertEquals(17.5, PairsInitialMargin.getInitialMarginFromLosses(pnls, Simulation.EXPECTED_SHORTFALL_6).get(0), EPS);
        assertEquals(8.0, PairsInitialMargin.getInitialMarginFromLosses(pnls, Simulation.VALUE_AT_RISK).get(0), EPS);
        List<RandomVariable> gains = new ArrayList<>();
        for (int k = 1; k <= 20; k++) gains.add(new Scalar(k));
        assertEquals(0.0, PairsInitialMargin.getInitialMarginFromLosses(gains, Simulation.EXPECTED_SHORTFALL_6).get(0), EPS);
        assertEquals(0.0, PairsInitialMargin.getInitialMarginFromLosses(gains, Simulation.VALUE_AT_RISK).get(0), EPS);
        assertThrows(CalculationException.class, () -> PairsInitialMargin.getInitialMarginFromLosses(
                List.of(new Scalar(-1.0)), Simulation.EXPECTED_SHORTFALL_6));
    }

    @Test
    public void couponPairsMatchesIndependentExhaustiveCashflowLossesOnEveryPath() throws Exception {
        ZeroRateModel model = model(2505, true);
        LocalPortfolio coupon = couponPortfolio();
        LocalDateTime evaluation = REFERENCE_TIME.plusDays(7);
        for (boolean moving : new boolean[] {false, true}) {
            LocalDateTime cutoff = moving ? evaluation : REFERENCE_TIME;
            for (boolean pathwise : new boolean[] {false, true}) {
                RandomVariable base = coupon.getBaseInitialMargin(evaluation, model, pathwise, moving);
                RandomVariable floor = coupon.getFloorInitialMargin(evaluation, model, pathwise, moving);
                RandomVariable pairs = coupon.getPairsInitialMargin(evaluation, model, pathwise, moving);
                RandomVariable expectedBase = exhaustiveCouponMargin(model, evaluation, cutoff, Simulation.EXPECTED_SHORTFALL_6, pathwise);
                RandomVariable expectedFloor = exhaustiveCouponMargin(model, evaluation, cutoff, Simulation.VALUE_AT_RISK, pathwise);
                for (int path = 0; path < 2; path++) {
                    assertTrue(base.get(path) > 0.0);
                    assertEquals(expectedBase.get(path), base.get(path), EPS);
                    assertEquals(expectedFloor.get(path), floor.get(path), EPS);
                    assertEquals(Math.max(base.get(path), floor.get(path)), pairs.get(path), EPS);
                }
            }
        }
    }

    @Test
    public void actualSwapUsesTwentyTaylorCandidatesThenRanksFullRevaluations() throws Exception {
        ZeroRateModel model = model(2505, true);
        LocalPortfolio portfolio = PortfolioFactory.getSwapPortfolio(PortfolioFactory.create10YPayerSwap())
                .getLocalPortfolio(Currency.EUR);
        LocalDateTime evaluation = REFERENCE_TIME.plusDays(7);
        ForwardAndDiscountSensitivities sensitivities = new ForwardAndDiscountSensitivities(
                new SensitivityComponentsForSwapLeg(portfolio.getSwaps(), model, model), LCH_GRID);
        InitialMarginContainer data = PairsInitialMargin.getScenarioData(sensitivities.getDeltaSensitivities(evaluation),
                sensitivities.getGammaSensitivities(evaluation), new ScenarioFactory(model, Simulation.EXPECTED_SHORTFALL_6),
                evaluation, Simulation.EXPECTED_SHORTFALL_6, LCH_GRID);
        for (boolean pathwise : new boolean[] {true, false}) {
            RandomVariable actual = portfolio.getBaseInitialMargin(evaluation, model, pathwise, true);
            for (int path = 0; path < 2; path++) {
                final int selectedPath = path;
                List<LocalDateTime> ordered = new ArrayList<>(data.getPnlSeries(Simulation.EXPECTED_SHORTFALL_6).getSeries().keySet());
                ordered.sort(Comparator.comparingDouble(date -> {
                    RandomVariable pnl = data.getPnlSeries(Simulation.EXPECTED_SHORTFALL_6).getValue(date);
                    return pathwise ? pnl.get(selectedPath) : pnl.getAverage();
                }));
                List<double[]> pnls = new ArrayList<>();
                for (LocalDateTime scenario : ordered.subList(0, 20)) {
                    double[] pnl = new double[2];
                    for (LchSwapLeg leg : portfolio.getSwaps()) {
                        // Value all paths, then select independently of the production scalar path.
                        RandomVariable change = leg.getValue(evaluation, model, data.getCurveScenarios(Simulation.EXPECTED_SHORTFALL_6)
                                .getCurveShifts(scenario), null).sub(leg.getValue(evaluation, model, null, null));
                        for (int p = 0; p < 2; p++) pnl[p] += change.get(p);
                    }
                    pnls.add(pnl);
                }
                pnls.sort(Comparator.comparingDouble(values -> pathwise ? values[selectedPath] : (values[0] + values[1]) / 2.0));
                double expected = Math.max(0.0, -pnls.subList(0, 6).stream()
                        .mapToDouble(values -> values[selectedPath]).average().getAsDouble());
                assertEquals(expected, actual.get(path), 1E-8);
            }
        }
    }

    @Test
    public void exhaustiveMvaHelperUsesFrozenOrMovingScenariosAndExpiredPortfolioHasZeroMargin() throws Exception {
        ZeroRateModel model = model(70, true);
        LocalPortfolio coupon = couponPortfolio();
        LocalDateTime evaluation = REFERENCE_TIME.plusDays(7);
        for (boolean moving : new boolean[] {false, true}) {
            RandomVariable full = MarginValuationAdjustment.getInitialMarginWithFullRevaluation(evaluation, model,
                    coupon, null, Simulation.EXPECTED_SHORTFALL_4, true, moving);
            RandomVariable expected = exhaustiveCouponMargin(model, evaluation, moving ? evaluation : REFERENCE_TIME,
                    Simulation.EXPECTED_SHORTFALL_4, true);
            for (int path = 0; path < 2; path++) assertEquals(expected.get(path), full.get(path), EPS);
        }
        assertEquals(0.0, coupon.getPairsInitialMargin(REFERENCE_TIME.plusDays(730), model, true, true).get(0), 0.0);
        assertEquals(0.0, new LocalPortfolio(Currency.EUR).getPairsInitialMargin(evaluation, model, true, true).get(0), 0.0);
    }

    private static RandomVariable exhaustiveCouponMargin(ZeroRateModel model, LocalDateTime evaluation,
            LocalDateTime cutoff, Simulation simulation, boolean pathwise) throws Exception {
        CurveScenarios scenarios = PairsInitialMargin.getCurveScenarios(new ScenarioFactory(model, simulation), cutoff, LCH_GRID);
        double maturity = FloatingpointDate.getFloatingPointDateFromDate(evaluation, REFERENCE_TIME.plusDays(730));
        List<double[]> pnls = new ArrayList<>();
        for (LocalDateTime date : scenarios.getSeries(LCH_GRID.getFixingSmallest()).getSeries().keySet()) {
            RandomVariable shift = scenarios.getCurveShifts(date).getRate(maturity, null);
            double[] values = new double[2];
            for (int path = 0; path < 2; path++) {
                double base = Math.exp(-maturity * model.getZeroRate(evaluation.toLocalDate(), maturity).get(path));
                values[path] = 10.0 * base * Math.expm1(-maturity * shift.get(path));
            }
            pnls.add(values);
        }
        int count = simulation == Simulation.VALUE_AT_RISK ? 13 : simulation == Simulation.EXPECTED_SHORTFALL_6 ? 6 : 4;
        double[] margins = new double[2];
        for (int path = 0; path < 2; path++) {
            final int p = path;
            pnls.sort(Comparator.comparingDouble(values -> pathwise ? values[p] : (values[0] + values[1]) / 2.0));
            double tail = simulation == Simulation.VALUE_AT_RISK ? pnls.get(count - 1)[path]
                    : pnls.subList(0, count).stream().mapToDouble(values -> values[p]).average().getAsDouble();
            margins[path] = Math.max(0.0, -tail);
        }
        return paths(margins[0], margins[1]);
    }

    private static ScenarioFactory handScenarios() {
        return new ScenarioFactory(Simulation.EXPECTED_SHORTFALL_4) {
            @Override
            public RandomVariableSeries getTenorPointScenarios(LocalDateTime cutoff, double tenor) {
                double factor = cutoff.isAfter(REFERENCE_TIME) ? 2.0 : 1.0;
                double shock = tenor == 1.0 ? -0.0001 : tenor == 2.0 ? -0.0007 : 0.0002;
                HashMap<LocalDateTime, RandomVariable> values = new HashMap<>();
                for (int k = 1; k <= 4; k++) values.put(REFERENCE_TIME.minusDays(k), new Scalar(factor * k * shock));
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
                for (double tenor : GRID.getFixings()) curve.addRate(tenor, new Scalar(0.02 + loading(tenor) * offset));
                history.put(date.atStartOfDay(), curve);
            }
        }
        HistoricalZeroRateSource historical = new HistoricalZeroRateSource(history);
        if (!simulated) return new ZeroRateModel(historical, calendar);
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
            @Override public List<LchSwapLeg> getSwaps() { return List.of(coupon); }
            @Override public double getLastPaymentDate() { return 2.0; }
        };
    }

    private static RandomVariable paths(double first, double second) {
        return new RandomVariableFromDoubleArray(0.0, new double[] {first, second});
    }

    private static TenorGrid grid() {
        LinkedHashMap<Double, String> points = new LinkedHashMap<>();
        points.put(1.0, "1Y"); points.put(2.0, "2Y"); points.put(3.0, "3Y");
        return new TenorGrid(points);
    }
}
