package net.finmath.lch.initialmargin.simulation.modeldata;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.lch.initialmargin.simulation.scenarios.ScenarioFactory;
import net.finmath.lch.initialmargin.simulation.scenarios.ScenarioFactory.Simulation;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.Currency;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.CurveName;
import net.finmath.lch.initialmargin.swapclear.libormodels.ModelFactory;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;

public class ZeroRateModelTest {

    private static final LocalDate REFERENCE = LocalDate.of(2024, 2, 5);
    private static final BusinessdayCalendar WEEKDAYS = new BusinessdayCalendarExcludingWeekends();
    private static final double EPS = 1E-12;

    @Test
    public void bundledHistoryConnectsToActualDailyLmmAndPairsScenarios() throws CalculationException {
        LocalDate end = REFERENCE.plusDays(10);
        LIBORModelMonteCarloSimulationModel simulation = ModelFactory.getModel(CurveName.EUR_EURIBOR_3M, end, 60.0);
        ZeroRateModel model = new ZeroRateModel(Currency.EUR, CurveName.EUR_EURIBOR_3M, simulation,
                "spreadsheets/EUR_EURIBOR_3M_Input_Data.xlsx");
        // This interpolated tenor previously differed between the two initial curves.
        assertEquals(4200, model.getRateSeries(REFERENCE.atStartOfDay(), 0.25).size());
        NavigableMap<LocalDateTime, RandomVariable> extended = model.getRateSeries(end.atStartOfDay(), 0.25);
        assertEquals(4208, extended.size());
        assertTrue(extended.get(REFERENCE.atStartOfDay()).isDeterministic());
        assertEquals(simulation.getNumberOfPaths(), extended.lastEntry().getValue().size());
        assertTrue(extended.lastEntry().getValue().getVariance() > 0.0);
        // Short and long PAIRS buckets remain genuine tenors, including the flat historical short end.
        assertEquals(4208, model.getRateSeries(end.atStartOfDay(), 1.0 / 365.0).size());
        assertEquals(4208, model.getRateSeries(end.atStartOfDay(), 60.0).size());
        RandomVariableSeries scenarios = new ScenarioFactory(model, Simulation.EXPECTED_SHORTFALL_6)
                .getTenorPointScenarios(end.atStartOfDay(), 0.25);
        assertEquals(2500, scenarios.getSeries().size());
        for (RandomVariable scenario : scenarios.getSeries().values()) {
            assertEquals(simulation.getNumberOfPaths(), scenario.size());
            for (int path = 0; path < scenario.size(); path++) {
                assertTrue(Double.isFinite(scenario.get(path)));
            }
        }
    }

    @Test
    public void completeHistoryDoesNotDependOnCallsAndDiscardsObservedFuture() throws CalculationException {
        NavigableMap<LocalDateTime, StochasticCurve> history = history(70);
        history.put(REFERENCE.plusDays(1).atStartOfDay(), curve(99.0));
        HistoricalZeroRateSource historical = new HistoricalZeroRateSource(history);
        List<LocalDate> queries = new ArrayList<>();
        ZeroRateSource simulated = (date, tenor) -> {
            queries.add(date);
            return simulatedRate(date, tenor);
        };
        ZeroRateModel direct = combined(historical, simulated);
        LocalDate cutoff = REFERENCE.plusDays(10);
        NavigableMap<LocalDateTime, RandomVariable> actual = direct.getRateSeries(cutoff.atStartOfDay(), 1.0);
        assertEquals(78, actual.size());
        assertEquals(initialRate(1.0), actual.get(REFERENCE.atStartOfDay()).get(0), EPS);
        assertEquals(initialRate(1.0) + 0.001, actual.get(REFERENCE.plusDays(1).atStartOfDay()).get(0), EPS);
        assertFalse(queries.stream().anyMatch(date -> date.isAfter(cutoff)));

        ZeroRateModel stepped = combined(historical, ZeroRateModelTest::simulatedRate);
        for (LocalDate date = REFERENCE; !date.isAfter(cutoff); date = date.plusDays(1)) {
            stepped.getRateSeries(date.atStartOfDay(), 1.0);
            if (WEEKDAYS.isBusinessday(date)) {
                // Pricing a separate, arbitrary tenor must not alter scenario support.
                stepped.getZeroRate(date, 0.7);
            }
        }
        assertSeriesEquals(actual, stepped.getRateSeries(cutoff.atStartOfDay(), 1.0), 2);
        assertThrows(UnsupportedOperationException.class, actual::clear);
        direct.getRateSeries(cutoff.plusDays(7).atStartOfDay(), 1.0);
        assertEquals(78, actual.size());
        assertSeriesEquals(actual, direct.getRateSeries(cutoff.atStartOfDay(), 1.0), 2);
    }

    @Test
    public void transitionChecksNativeAndInterpolatedTenorsWithoutRebasing() throws CalculationException {
        HistoricalZeroRateSource historical = new HistoricalZeroRateSource(history(70));
        assertThrows(CalculationException.class,
                () -> combined(historical, (date, tenor) -> new Scalar(initialRate(tenor) + 0.001)));
        ZeroRateModel model = combined(historical, (date, tenor) -> new Scalar(
                initialRate(tenor) + (tenor == 0.5 ? 0.001 : 0.0)));
        assertThrows(CalculationException.class, () -> model.getZeroRate(REFERENCE, 0.5));
        assertThrows(CalculationException.class, () -> model.getZeroRate(REFERENCE.plusDays(1), 0.5));
        // No silent tenor clamping: a short-end convention mismatch is also reported.
        assertThrows(CalculationException.class, () -> model.getRateSeries(REFERENCE.atStartOfDay(), 1.0 / 365.0));
    }

    @Test
    public void missingHistoricalBusinessDayIsRejected() {
        NavigableMap<LocalDateTime, StochasticCurve> history = history(70);
        history.remove(REFERENCE.minusDays(4).atStartOfDay());
        HistoricalZeroRateSource missingDay = new HistoricalZeroRateSource(history);
        assertThrows(CalculationException.class,
                () -> combined(missingDay, ZeroRateModelTest::simulatedRate));
        history = history(70);
        history.remove(REFERENCE.atStartOfDay());
        HistoricalZeroRateSource missingClosingCurve = new HistoricalZeroRateSource(history);
        assertThrows(CalculationException.class,
                () -> combined(missingClosingCurve, ZeroRateModelTest::simulatedRate));
    }

    @Test
    public void calendarControlsObservationLagAndWeekendAsOf() throws CalculationException {
        HistoricalZeroRateSource historical = new HistoricalZeroRateSource(history(70));
        ZeroRateModel weekdays = combined(historical, ZeroRateModelTest::simulatedRate);
        LocalDate saturday = LocalDate.of(2024, 3, 30);
        assertEquals(saturday.minusDays(1).atStartOfDay(), weekdays.getRateSeries(saturday.atStartOfDay(), 1.0).lastKey());
        ZeroRateModel target = new ZeroRateModel(historical, ZeroRateModelTest::simulatedRate,
                REFERENCE, new BusinessdayCalendarExcludingTARGETHolidays(), 2, EPS);
        // Good Friday is a row in the bundled weekday convention but not in TARGET.
        assertEquals(LocalDate.of(2024, 3, 28).atStartOfDay(), target.getRateSeries(saturday.atStartOfDay(), 1.0).lastKey());
        assertThrows(IllegalArgumentException.class, () -> weekdays.getRateSeries(saturday.atTime(12, 0), 1.0));
    }

    @Test
    public void pureHistoricalAndPureSimulatedSourcesHaveExplicitCoverage() throws CalculationException {
        HistoricalZeroRateSource historical = new HistoricalZeroRateSource(history(70));
        ZeroRateModel historicalOnly = new ZeroRateModel(historical, WEEKDAYS);
        assertEquals(70, historicalOnly.getRateSeries(REFERENCE.atStartOfDay(), 1.0).size());
        assertThrows(CalculationException.class,
                () -> historicalOnly.getRateSeries(REFERENCE.plusDays(1).atStartOfDay(), 1.0));
        ZeroRateModel simulatedOnly = new ZeroRateModel(null, ZeroRateModelTest::simulatedRate,
                REFERENCE, WEEKDAYS, 2, EPS);
        assertEquals(1, simulatedOnly.getRateSeries(REFERENCE.atStartOfDay(), 1.0).size());
        assertThrows(CalculationException.class, () -> simulatedOnly.getRateSeries(REFERENCE.minusDays(1).atStartOfDay(), 1.0));
        assertThrows(CalculationException.class, () -> new ScenarioFactory(simulatedOnly, Simulation.EXPECTED_SHORTFALL_6)
                .getTenorPointScenarios(REFERENCE.atStartOfDay(), 1.0));
    }

    @Test
    public void evolvingScenarioPipelineKeepsPathsAndFrozenScenarios() throws CalculationException {
        ZeroRateModel model = combined(new HistoricalZeroRateSource(history(70)), ZeroRateModelTest::simulatedRate);
        ScenarioFactory factory = new ScenarioFactory(model, Simulation.EXPECTED_SHORTFALL_4);
        RandomVariableSeries frozen = factory.getTenorPointScenarios(REFERENCE.atStartOfDay(), 1.0);
        LocalDate future = REFERENCE.plusDays(10);
        RandomVariableSeries moving = factory.getTenorPointScenarios(future.atStartOfDay(), 1.0);
        assertEquals(65, frozen.getSeries().size());
        assertEquals(73, moving.getSeries().size());
        for (int path = 0; path < 2; path++) {
            NavigableMap<LocalDateTime, RandomVariable> singlePath = new TreeMap<>();
            for (Map.Entry<LocalDateTime, RandomVariable> entry : model.getRateSeries(future.atStartOfDay(), 1.0).entrySet()) {
                singlePath.put(entry.getKey(), new Scalar(entry.getValue().get(path)));
            }
            RandomVariableSeries expected = new ScenarioFactory(Simulation.EXPECTED_SHORTFALL_4)
                    .getTenorPointScenarios(future.atStartOfDay(), singlePath);
            for (LocalDateTime date : moving.getSeries().keySet()) {
                assertEquals(expected.getValue(date).get(0), moving.getValue(date).get(path), EPS);
            }
        }
        RandomVariableSeries frozenAgain = factory.getTenorPointScenarios(REFERENCE.atStartOfDay(), 1.0);
        assertEquals(frozen.getSeries().keySet(), frozenAgain.getSeries().keySet());
        for (LocalDateTime date : frozen.getSeries().keySet()) {
            assertEquals(frozen.getValue(date).get(0), frozenAgain.getValue(date).get(0), 0.0);
        }
        LocalDateTime retained = new TreeMap<>(frozen.getSeries()).firstKey();
        assertNotEquals(frozen.getValue(retained).get(0), moving.getValue(retained).get(0), EPS);
    }

    @Test
    public void projectionAndDiscountingUseTheSameCurveAndShift() throws CalculationException {
        ZeroRateModel model = combined(new HistoricalZeroRateSource(history(70)), ZeroRateModelTest::simulatedRate);
        LocalDate date = REFERENCE.plusDays(1);
        StochasticCurve shift = new StochasticCurve();
        shift.addRate(0.25, new Scalar(0.001));
        shift.addRate(2.0, new Scalar(0.001));
        double start = 0.5, end = 1.0;
        RandomVariable pStart = model.getDiscountFactor(date.atStartOfDay(), start, shift, null);
        RandomVariable pEnd = model.getDiscountFactor(date.atStartOfDay(), end, shift, null);
        RandomVariable forward = model.getForwardRate(date.atStartOfDay(), start, end, shift, null);
        for (int path = 0; path < 2; path++) {
            assertEquals(Math.exp(-(simulatedRate(date, end).get(path) + 0.001) * end), pEnd.get(path), EPS);
            assertEquals(pStart.get(path) - pEnd.get(path), forward.mult(end - start).mult(pEnd).get(path), EPS);
            assertEquals(pEnd.get(path), model.getDiscountFactor(date.atStartOfDay(), end, shift, path).get(0), EPS);
        }
    }

    @Test
    public void customSourcesCannotChangePathCountOrUseFutureInformation() throws CalculationException {
        HistoricalZeroRateSource historical = new HistoricalZeroRateSource(history(70));
        ZeroRateModel wrongPaths = combined(historical, (date, tenor) -> date.equals(REFERENCE)
                ? new Scalar(initialRate(tenor)) : new RandomVariableFromDoubleArray(0.0, new double[] {0.01, 0.02, 0.03}));
        assertThrows(CalculationException.class, () -> wrongPaths.getZeroRate(REFERENCE.plusDays(1), 1.0));
        ZeroRateModel futureInfo = combined(historical, (date, tenor) -> date.equals(REFERENCE)
                ? new Scalar(initialRate(tenor)) : new RandomVariableFromDoubleArray(1.0, new double[] {0.01, 0.02}));
        assertThrows(CalculationException.class, () -> futureInfo.getZeroRate(REFERENCE.plusDays(1), 1.0));
    }

    private static ZeroRateModel combined(HistoricalZeroRateSource historical, ZeroRateSource simulated) throws CalculationException {
        return new ZeroRateModel(historical, simulated, REFERENCE, WEEKDAYS, 2, EPS);
    }

    private static NavigableMap<LocalDateTime, StochasticCurve> history(int observations) {
        NavigableMap<LocalDateTime, StochasticCurve> history = new TreeMap<>();
        for (LocalDate date = REFERENCE; history.size() < observations; date = date.minusDays(1)) {
            if (WEEKDAYS.isBusinessday(date)) {
                history.put(date.atStartOfDay(), curve(0.001 * Math.sin(ChronoUnit.DAYS.between(REFERENCE, date) * 0.3)));
            }
        }
        return history;
    }

    private static StochasticCurve curve(double offset) {
        StochasticCurve curve = new StochasticCurve();
        for (double tenor : new double[] {0.25, 1.0, 2.0}) {
            curve.addRate(tenor, new Scalar(initialRate(tenor) + offset));
        }
        return curve;
    }

    private static double initialRate(double tenor) {
        return 0.01 + 0.002 * tenor;
    }

    private static RandomVariable simulatedRate(LocalDate date, double tenor) {
        long days = ChronoUnit.DAYS.between(REFERENCE, date);
        if (days == 0) {
            return new Scalar(initialRate(tenor));
        }
        return new RandomVariableFromDoubleArray(days / 365.0,
                new double[] {initialRate(tenor) + 0.001 * days, initialRate(tenor) - 0.0004 * days});
    }

    private static void assertSeriesEquals(NavigableMap<LocalDateTime, RandomVariable> expected,
            NavigableMap<LocalDateTime, RandomVariable> actual, int paths) {
        assertEquals(expected.keySet(), actual.keySet());
        for (LocalDateTime date : expected.keySet()) {
            for (int path = 0; path < paths; path++) {
                assertEquals(expected.get(date).get(path), actual.get(date).get(path), EPS);
            }
        }
    }
}
