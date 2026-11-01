package net.finmath.lch.initialmargin.simulation.scenarios;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

import net.finmath.exception.CalculationException;
import net.finmath.lch.initialmargin.simulation.modeldata.RandomVariableSeries;
import net.finmath.lch.initialmargin.simulation.ratetransformations.RateTransformation;
import net.finmath.lch.initialmargin.simulation.ratetransformations.TransformationContext;
import net.finmath.stochastic.RandomVariable;

/**
 * Transforms an observation history and then selects scenario windows.
 * The caller supplies consecutive business-day observations at a constant tenor.
 * Historical scalar values and subsequent simulated path vectors may coexist;
 * stochastic observations must use the same paths in the same order.
 *
 * Only observations on or before the evaluation date enter the transformations.
 * The complete available prefix is transformed before selecting a window, so
 * EWMA history is retained and all shocks are rescaled at the evaluation date.
 */
public class ScenarioBuilder {

    private final List<RateTransformation> rateTransformations;

    public ScenarioBuilder(List<RateTransformation> rateTransformations) {
        this.rateTransformations = List.copyOf(rateTransformations);
    }

    /** Selects the last N scenarios plus an inclusive fixed interval, without duplicates. */
    public RandomVariableSeries getTenorPointScenarios(LocalDateTime evaluationDate,
            NavigableMap<LocalDateTime, RandomVariable> rates, int numberOfMovingScenarios,
            LocalDateTime startDateFixedScenarios, LocalDateTime endDateFixedScenarios) throws CalculationException {
        Objects.requireNonNull(startDateFixedScenarios, "startDateFixedScenarios");
        Objects.requireNonNull(endDateFixedScenarios, "endDateFixedScenarios");
        if (startDateFixedScenarios.isAfter(endDateFixedScenarios)) {
            throw new IllegalArgumentException("The fixed scenario interval must be chronological.");
        }
        NavigableMap<LocalDateTime, RandomVariable> scenarios = getTransformedRateSeries(evaluationDate, rates);
        HashMap<LocalDateTime, RandomVariable> selected = getLastScenarios(scenarios, numberOfMovingScenarios);
        selected.putAll(scenarios.subMap(startDateFixedScenarios, true, endDateFixedScenarios, true));
        return new RandomVariableSeries(selected);
    }

    /** Selects the last N scenarios from the transformed history. */
    public RandomVariableSeries getTenorPointScenarios(LocalDateTime evaluationDate,
            NavigableMap<LocalDateTime, RandomVariable> rates, int numberOfScenarios) throws CalculationException {
        return new RandomVariableSeries(getLastScenarios(
                getTransformedRateSeries(evaluationDate, rates), numberOfScenarios));
    }

    /** Returns all scenarios in the available history up to the evaluation date. */
    public RandomVariableSeries getTenorPointScenarios(LocalDateTime evaluationDate,
            NavigableMap<LocalDateTime, RandomVariable> rates) throws CalculationException {
        return new RandomVariableSeries(new HashMap<>(getTransformedRateSeries(evaluationDate, rates)));
    }

    private NavigableMap<LocalDateTime, RandomVariable> getTransformedRateSeries(LocalDateTime evaluationDate,
            NavigableMap<LocalDateTime, RandomVariable> rates) throws CalculationException {
        Objects.requireNonNull(evaluationDate, "evaluationDate");
        Objects.requireNonNull(rates, "rates");
        // Do not inherit a caller's descending comparator.
        NavigableMap<LocalDateTime, RandomVariable> chronologicalRates = new TreeMap<>();
        chronologicalRates.putAll(rates);
        TransformationContext context = new TransformationContext(chronologicalRates.headMap(evaluationDate, true));
        context.addTransformationResult("InitialDataInput", context.getPrimaryData());
        for (RateTransformation transformation : rateTransformations) {
            transformation.applyTransformation(context);
        }
        return context.getPrimaryData();
    }

    private HashMap<LocalDateTime, RandomVariable> getLastScenarios(
            NavigableMap<LocalDateTime, RandomVariable> scenarios, int numberOfScenarios) {
        if (numberOfScenarios <= 0) {
            throw new IllegalArgumentException("The number of scenarios must be strictly positive.");
        }
        if (numberOfScenarios > scenarios.size()) {
            throw new IllegalArgumentException("Requested " + numberOfScenarios
                    + " scenarios, but only " + scenarios.size() + " are available.");
        }
        HashMap<LocalDateTime, RandomVariable> selected = new HashMap<>();
        for (Map.Entry<LocalDateTime, RandomVariable> entry : scenarios.descendingMap().entrySet()) {
            selected.put(entry.getKey(), entry.getValue());
            if (selected.size() == numberOfScenarios) {
                break;
            }
        }
        return selected;
    }
}
