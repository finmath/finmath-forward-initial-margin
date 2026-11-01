package net.finmath.lch.initialmargin.simulation.scenarios;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.NavigableMap;
import java.util.Objects;

import net.finmath.exception.CalculationException;
import net.finmath.lch.initialmargin.simulation.modeldata.RandomVariableSeries;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.simulation.ratetransformations.RateTransformationEWMA;
import net.finmath.lch.initialmargin.simulation.ratetransformations.RateTransformationMidVolatilityScaling;
import net.finmath.lch.initialmargin.simulation.ratetransformations.RateTransformationReturns;
import net.finmath.lch.initialmargin.simulation.ratetransformations.RateTransformationReturns.ReturnType;
import net.finmath.stochastic.RandomVariable;

/**
 * Provides LCH scenario transformations independently of the source of daily rates.
 * Scenario dates denote return end dates, including membership of the stress window.
 */
public class ScenarioFactory {

    public enum Simulation {EXPECTED_SHORTFALL_4, EXPECTED_SHORTFALL_6, VALUE_AT_RISK}

    private static final int SCENARIO_WINDOW = 2500;
    private static final LocalDateTime STRESS_START = LocalDateTime.of(2008, 1, 1, 0, 0);
    private static final LocalDateTime STRESS_END = LocalDateTime.of(2010, 6, 30, 0, 0);

    private final ZeroRateModel zeroRateModel;
    private final ScenarioBuilder scenarioBuilder;
    private final Simulation simulation;

    /** Creates a factory for caller-supplied observation histories. */
    public ScenarioFactory(Simulation simulation) {
        this(null, simulation);
    }

    /** Retains the existing model-based entry point as a data-source adapter. */
    public ScenarioFactory(ZeroRateModel zeroRateModel, Simulation simulation) {
        this.zeroRateModel = zeroRateModel;
        this.simulation = Objects.requireNonNull(simulation, "simulation");
        switch (simulation) {
        case EXPECTED_SHORTFALL_4:
        case EXPECTED_SHORTFALL_6:
            scenarioBuilder = new ScenarioBuilder(Arrays.asList(
                    new RateTransformationReturns(5, ReturnType.ABSOLUTE),
                    new RateTransformationEWMA(60, 0.992),
                    new RateTransformationMidVolatilityScaling("Returns")));
            break;
        case VALUE_AT_RISK:
            scenarioBuilder = new ScenarioBuilder(Arrays.asList(
                    new RateTransformationReturns(5, ReturnType.ABSOLUTE)));
            break;
        default:
            throw new IllegalArgumentException("Unsupported simulation type: " + simulation);
        }
    }

    /**
     * Builds pathwise scenarios from a complete constant-tenor business-day history.
     * Historical values may be deterministic; simulated observations may contain
     * paths. All stochastic observations must share path count and ordering.
     * Future observations are ignored. No interpolation, averaging, or model-data
     * extraction takes place here.
     *
     * To freeze risk factors at a reference date, query that date. To evolve them,
     * query a later date with an extended history; even retained ES shocks are
     * then rescaled using the new pathwise current volatility.
     */
    public RandomVariableSeries getTenorPointScenarios(LocalDateTime evaluationDate,
            NavigableMap<LocalDateTime, RandomVariable> rates) throws CalculationException {
        switch (simulation) {
        case EXPECTED_SHORTFALL_4:
            // Expanding window over the supplied history.
            return scenarioBuilder.getTenorPointScenarios(evaluationDate, rates);
        case EXPECTED_SHORTFALL_6:
            return scenarioBuilder.getTenorPointScenarios(evaluationDate, rates, SCENARIO_WINDOW);
        case VALUE_AT_RISK:
            return scenarioBuilder.getTenorPointScenarios(evaluationDate, rates, SCENARIO_WINDOW, STRESS_START, STRESS_END);
        default:
            throw new IllegalArgumentException("Unsupported simulation type: " + simulation);
        }
    }

    /**
     * Existing model adapter. The model remains responsible for providing complete,
     * correctly aligned observations and the historical-to-simulated transition.
     */
    public RandomVariableSeries getTenorPointScenarios(LocalDateTime evaluationDate, double tenorFixing) throws CalculationException {
        if (zeroRateModel == null) {
            throw new IllegalStateException("No zero-rate model configured; supply an observation history instead.");
        }
        return getTenorPointScenarios(evaluationDate, zeroRateModel.getRateSeries(evaluationDate, tenorFixing));
    }
}
