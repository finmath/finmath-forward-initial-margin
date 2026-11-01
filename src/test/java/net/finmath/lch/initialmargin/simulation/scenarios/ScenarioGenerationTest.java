package net.finmath.lch.initialmargin.simulation.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.lch.initialmargin.simulation.modeldata.RandomVariableSeries;
import net.finmath.lch.initialmargin.simulation.ratetransformations.RateTransformationReturns;
import net.finmath.lch.initialmargin.simulation.ratetransformations.RateTransformationReturns.ReturnType;
import net.finmath.lch.initialmargin.simulation.ratetransformations.TransformationContext;
import net.finmath.lch.initialmargin.simulation.scenarios.ScenarioFactory.Simulation;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

public class ScenarioGenerationTest {

    private static final LocalDateTime START = LocalDateTime.of(2012, 1, 2, 0, 0);
    private static final double TOLERANCE = 1E-12;

    @Test
    public void exactly2500ScenariosAreAvailableFrom2505Observations() throws CalculationException {
        NavigableMap<LocalDateTime, RandomVariable> rates = history(START, 2505);
        Set<LocalDateTime> expectedDates = new HashSet<>(new ArrayList<>(rates.keySet()).subList(5, 2505));
        for (Simulation simulation : new Simulation[] {Simulation.EXPECTED_SHORTFALL_6, Simulation.VALUE_AT_RISK}) {
            RandomVariableSeries scenarios = new ScenarioFactory(simulation).getTenorPointScenarios(rates.lastKey(), rates);
            assertEquals(expectedDates, scenarios.getSeries().keySet());
        }
    }

    @Test
    public void evaluationCutoffPrecedesTransformationsAndFutureValidation() throws CalculationException {
        NavigableMap<LocalDateTime, RandomVariable> rates = history(START, 70);
        LocalDateTime evaluationDate = rates.lastKey();
        ScenarioFactory factory = new ScenarioFactory(Simulation.EXPECTED_SHORTFALL_4);
        RandomVariableSeries expected = factory.getTenorPointScenarios(evaluationDate, rates);
        rates.put(nextBusinessDay(evaluationDate), new Scalar(Double.NaN));
        // A descending input and an invalid future value must not affect the prefix.
        RandomVariableSeries actual = factory.getTenorPointScenarios(evaluationDate, rates.descendingMap());
        assertSeriesEquals(expected, actual, 1);
        assertEquals(71, rates.size());
        assertTrue(Double.isNaN(rates.lastEntry().getValue().get(0)));
        assertThrows(IllegalArgumentException.class, () -> factory.getTenorPointScenarios(rates.lastKey(), rates));
    }

    @Test
    public void historicalToSimulatedTransitionMatchesIndependentPathsAndKeepsFrozenResults() throws CalculationException {
        NavigableMap<LocalDateTime, RandomVariable> rates = history(START, 70);
        LocalDateTime referenceDate = rates.lastKey();
        ScenarioFactory factory = new ScenarioFactory(Simulation.EXPECTED_SHORTFALL_4);
        RandomVariableSeries frozen = factory.getTenorPointScenarios(referenceDate, rates);
        double lastRate = rates.lastEntry().getValue().get(0);
        LocalDateTime date = referenceDate;
        for (int step = 1; step <= 8; step++) {
            date = nextBusinessDay(date);
            rates.put(date, paths(lastRate + 0.002 * step, lastRate - 0.0002 * step));
        }
        RandomVariableSeries moving = factory.getTenorPointScenarios(date, rates);
        assertEquals(73, moving.getSeries().size());
        for (int path = 0; path < 2; path++) {
            NavigableMap<LocalDateTime, RandomVariable> singlePath = new TreeMap<>();
            for (Map.Entry<LocalDateTime, RandomVariable> entry : rates.entrySet()) {
                singlePath.put(entry.getKey(), new Scalar(entry.getValue().get(path)));
            }
            RandomVariableSeries reference = factory.getTenorPointScenarios(date, singlePath);
            for (LocalDateTime scenarioDate : reference.getSeries().keySet()) {
                assertEquals(reference.getValue(scenarioDate).get(0), moving.getValue(scenarioDate).get(path), TOLERANCE);
            }
        }
        LocalDateTime retainedDate = new TreeMap<>(frozen.getSeries()).firstKey();
        assertNotEquals(frozen.getValue(retainedDate).get(0), moving.getValue(retainedDate).get(0), TOLERANCE);
        assertNotEquals(moving.getValue(retainedDate).get(0), moving.getValue(retainedDate).get(1), TOLERANCE);
        assertSeriesEquals(frozen, factory.getTenorPointScenarios(referenceDate, rates), 1);
        // The last return spans five simulated observations, on the original path identities.
        assertEquals(0.010, moving.getValue(date).get(0), TOLERANCE);
        assertEquals(-0.001, moving.getValue(date).get(1), TOLERANCE);
    }

    @Test
    public void rollingWindowRetainsFullEwmaHistoryAndDropsOnlyOldestScenario() throws CalculationException {
        NavigableMap<LocalDateTime, RandomVariable> rates = history(START, 2510);
        LocalDateTime date = rates.lastKey();
        RandomVariableSeries all = new ScenarioFactory(Simulation.EXPECTED_SHORTFALL_4).getTenorPointScenarios(date, rates);
        ScenarioFactory rollingFactory = new ScenarioFactory(Simulation.EXPECTED_SHORTFALL_6);
        RandomVariableSeries rolling = rollingFactory.getTenorPointScenarios(date, rates);
        List<LocalDateTime> allDates = new ArrayList<>(new TreeMap<>(all.getSeries()).keySet());
        assertEquals(new HashSet<>(allDates.subList(5, 2505)), rolling.getSeries().keySet());
        for (LocalDateTime scenarioDate : rolling.getSeries().keySet()) {
            assertEquals(all.getValue(scenarioDate).get(0), rolling.getValue(scenarioDate).get(0), TOLERANCE);
        }
        LocalDateTime nextDate = nextBusinessDay(date);
        rates.put(nextDate, new Scalar(0.05));
        RandomVariableSeries next = rollingFactory.getTenorPointScenarios(nextDate, rates);
        assertEquals(2500, next.getSeries().size());
        assertFalse(next.getSeries().containsKey(allDates.get(5)));
        assertTrue(next.getSeries().containsKey(nextDate));
        assertNotEquals(rolling.getValue(allDates.get(6)).get(0), next.getValue(allDates.get(6)).get(0), TOLERANCE);
    }

    @Test
    public void floorStressWindowUsesInclusiveReturnEndDatesWithoutDuplicates() throws CalculationException {
        LocalDateTime stressStart = LocalDateTime.of(2008, 1, 1, 0, 0);
        LocalDateTime stressEnd = LocalDateTime.of(2010, 6, 30, 0, 0);
        NavigableMap<LocalDateTime, RandomVariable> rates = history(LocalDateTime.of(2007, 12, 20, 0, 0), 4000);
        ScenarioFactory factory = new ScenarioFactory(Simulation.VALUE_AT_RISK);
        List<LocalDateTime> dates = new ArrayList<>(rates.keySet());
        RandomVariableSeries scenarios = factory.getTenorPointScenarios(rates.lastKey(), rates);
        Set<LocalDateTime> expected = new HashSet<>(dates.subList(1500, 4000));
        expected.addAll(rates.subMap(stressStart, true, stressEnd, true).keySet());
        assertEquals(expected, scenarios.getSeries().keySet());
        assertTrue(scenarios.getSeries().containsKey(stressStart));
        assertTrue(scenarios.getSeries().containsKey(stressEnd));
        assertFalse(scenarios.getSeries().containsKey(stressStart.minusDays(1)));
        assertFalse(scenarios.getSeries().containsKey(nextBusinessDay(stressEnd)));
        int endIndex = dates.indexOf(stressEnd);
        assertEquals(rates.get(stressEnd).sub(rates.get(dates.get(endIndex - 5))).get(0),
                scenarios.getValue(stressEnd).get(0), TOLERANCE);
        // At this earlier date the entire stress interval overlaps the moving window.
        assertEquals(2500, factory.getTenorPointScenarios(dates.get(2504), rates).getSeries().size());
    }

    @Test
    public void flatHistoryProducesFiniteZeroScenarios() throws CalculationException {
        NavigableMap<LocalDateTime, RandomVariable> rates = history(START, 2505);
        rates.replaceAll((date, rate) -> new Scalar(-0.01));
        RandomVariableSeries scenarios = new ScenarioFactory(Simulation.EXPECTED_SHORTFALL_6)
                .getTenorPointScenarios(rates.lastKey(), rates);
        for (RandomVariable scenario : scenarios.getSeries().values()) {
            assertEquals(0.0, scenario.get(0), 0.0);
        }
    }

    @Test
    public void invalidPathCountsNonfiniteInputsAndOverflowAreRejected() {
        NavigableMap<LocalDateTime, RandomVariable> rates = history(START, 70);
        rates.put(START, paths(0.01, 0.02));
        rates.put(nextBusinessDay(START), paths(0.01, 0.02, 0.03));
        ScenarioFactory factory = new ScenarioFactory(Simulation.EXPECTED_SHORTFALL_4);
        assertThrows(IllegalArgumentException.class, () -> factory.getTenorPointScenarios(rates.lastKey(), rates));
        rates.put(nextBusinessDay(START), paths(0.01, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> factory.getTenorPointScenarios(rates.lastKey(), rates));
        NavigableMap<LocalDateTime, RandomVariable> overflowing = new TreeMap<>();
        overflowing.put(START, new Scalar(-Double.MAX_VALUE));
        overflowing.put(nextBusinessDay(START), new Scalar(Double.MAX_VALUE));
        ScenarioBuilder returnsOnly = new ScenarioBuilder(Collections.singletonList(new RateTransformationReturns(1, ReturnType.ABSOLUTE)));
        assertThrows(IllegalArgumentException.class, () -> returnsOnly.getTenorPointScenarios(overflowing.lastKey(), overflowing));
    }

    @Test
    public void contextSnapshotsAreIndependentOfCallerMapMutations() {
        NavigableMap<LocalDateTime, RandomVariable> rates = history(START, 3);
        TransformationContext context = new TransformationContext(rates.descendingMap());
        context.addTransformationResult("Input", context.getPrimaryData());
        rates.clear();
        assertEquals(3, context.getPrimaryData().size());
        assertEquals(START, context.getPrimaryData().firstKey());
        assertThrows(UnsupportedOperationException.class, () -> context.getTransformationResult("Input").clear());
    }

    @Test
    public void unavailableWindowsAndMissingModelHaveExplicitErrors() throws CalculationException {
        NavigableMap<LocalDateTime, RandomVariable> rates = history(START, 7);
        ScenarioBuilder builder = new ScenarioBuilder(Collections.singletonList(new RateTransformationReturns(5, ReturnType.ABSOLUTE)));
        assertEquals(2, builder.getTenorPointScenarios(rates.lastKey(), rates, 2).getSeries().size());
        assertThrows(IllegalArgumentException.class, () -> builder.getTenorPointScenarios(rates.lastKey(), rates, 0));
        assertThrows(IllegalArgumentException.class, () -> builder.getTenorPointScenarios(rates.lastKey(), rates, 3));
        assertThrows(CalculationException.class, () -> builder.getTenorPointScenarios(START, rates));
        assertThrows(IllegalStateException.class, () -> new ScenarioFactory(Simulation.VALUE_AT_RISK).getTenorPointScenarios(START, 5.0));
    }

    private static NavigableMap<LocalDateTime, RandomVariable> history(LocalDateTime start, int observations) {
        NavigableMap<LocalDateTime, RandomVariable> rates = new TreeMap<>();
        LocalDateTime date = start;
        for (int i = 0; i < observations; i++) {
            rates.put(date, new Scalar(0.015 + 0.002 * Math.sin(i * 0.31) + 0.001 * Math.cos(i * 0.07)));
            date = nextBusinessDay(date);
        }
        return rates;
    }

    private static LocalDateTime nextBusinessDay(LocalDateTime date) {
        do {
            date = date.plusDays(1);
        } while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY);
        return date;
    }

    private static RandomVariable paths(double... values) {
        return new RandomVariableFromDoubleArray(0.0, values);
    }

    private static void assertSeriesEquals(RandomVariableSeries expected, RandomVariableSeries actual, int paths) {
        assertEquals(expected.getSeries().keySet(), actual.getSeries().keySet());
        for (LocalDateTime date : expected.getSeries().keySet()) {
            for (int path = 0; path < paths; path++) {
                assertEquals(expected.getValue(date).get(path), actual.getValue(date).get(path), TOLERANCE);
            }
        }
    }
}
