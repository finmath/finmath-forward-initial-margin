package net.finmath.lch.initialmargin.swapclear.aggregationscheme;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

import net.finmath.exception.CalculationException;
import net.finmath.lch.initialmargin.simulation.modeldata.RandomVariableSeries;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGrid;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGridFactory;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGridFactory.GridType;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.simulation.scenarios.CurveScenarios;
import net.finmath.lch.initialmargin.simulation.scenarios.ScenarioFactory;
import net.finmath.lch.initialmargin.simulation.scenarios.ScenarioFactory.Simulation;
import net.finmath.lch.initialmargin.swapclear.products.LocalPortfolio;
import net.finmath.lch.initialmargin.swapclear.products.components.LchSwapLeg;
import net.finmath.lch.initialmargin.swapclear.sensitivities.ForwardAndDiscountSensitivities;
import net.finmath.lch.initialmargin.swapclear.sensitivities.SensitivityComponentsForSwapLeg;
import net.finmath.lch.initialmargin.swapclear.sensitivities.SensitivityMatrix;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.FloatingpointDate;

/**
 * Swap PAIRS core: analytical delta-gamma ranking, full revaluation of the twenty
 * lowest Taylor PnLs, then ES6 for base IM and VaR13 for floor IM. PAIRS takes the
 * pathwise maximum. The supplied zero curve is shared by projection and discounting.
 */
public class PairsInitialMargin {
    private static final double DECIMAL_TO_BASIS_POINT = 10000.0;
    private static final int NUMBER_OF_FULL_REVALUATIONS = 20;

    public static RandomVariable getBaseInitialMargin(LocalDateTime evaluationDate, ZeroRateModel model,
            LocalPortfolio portfolio, boolean pathwise, boolean movingScenarioWindow) throws CalculationException {
        return getInitialMargin(evaluationDate, model, portfolio, pathwise, movingScenarioWindow,
                Simulation.EXPECTED_SHORTFALL_6);
    }

    public static RandomVariable getFloorInitialMargin(LocalDateTime evaluationDate, ZeroRateModel model,
            LocalPortfolio portfolio, boolean pathwise, boolean movingScenarioWindow) throws CalculationException {
        return getInitialMargin(evaluationDate, model, portfolio, pathwise, movingScenarioWindow, Simulation.VALUE_AT_RISK);
    }

    public static RandomVariable getPairsInitialMargin(LocalDateTime evaluationDate, ZeroRateModel model,
            LocalPortfolio portfolio, boolean pathwise, boolean movingScenarioWindow) throws CalculationException {
        TenorGrid grid = TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID);
        ForwardAndDiscountSensitivities sensitivities = sensitivities(portfolio, model, grid);
        SensitivityMatrix delta = sensitivities.getDeltaSensitivities(evaluationDate);
        SensitivityMatrix gamma = sensitivities.getGammaSensitivities(evaluationDate);
        RandomVariable base = getRankedInitialMargin(evaluationDate, model, portfolio, delta, gamma,
                Simulation.EXPECTED_SHORTFALL_6, pathwise, movingScenarioWindow, grid);
        RandomVariable floor = getRankedInitialMargin(evaluationDate, model, portfolio, delta, gamma,
                Simulation.VALUE_AT_RISK, pathwise, movingScenarioWindow, grid);
        return base.floor(floor);
    }

    private static RandomVariable getInitialMargin(LocalDateTime date, ZeroRateModel model,
            LocalPortfolio portfolio, boolean pathwise, boolean moving, Simulation simulation) throws CalculationException {
        TenorGrid grid = TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID);
        ForwardAndDiscountSensitivities sensitivities = sensitivities(portfolio, model, grid);
        return getRankedInitialMargin(date, model, portfolio, sensitivities.getDeltaSensitivities(date),
                sensitivities.getGammaSensitivities(date), simulation, pathwise, moving, grid);
    }

    private static ForwardAndDiscountSensitivities sensitivities(LocalPortfolio portfolio, ZeroRateModel model,
            TenorGrid grid) {
        return new ForwardAndDiscountSensitivities(new SensitivityComponentsForSwapLeg(portfolio.getSwaps(), model, model), grid);
    }

    private static RandomVariable getRankedInitialMargin(LocalDateTime date, ZeroRateModel model,
            LocalPortfolio portfolio, SensitivityMatrix delta, SensitivityMatrix gamma, Simulation simulation,
            boolean pathwise, boolean moving, TenorGrid grid) throws CalculationException {
        // Empty or fully expired swap portfolios have no remaining cashflows.
        if (delta.getSensitivityMatrix().isEmpty() && gamma.getSensitivityMatrix().isEmpty()) {
            return new Scalar(0.0);
        }
        LocalDateTime scenarioDate = moving ? date : model.getReferenceDate();
        InitialMarginContainer data = getScenarioData(delta, gamma, new ScenarioFactory(model, simulation),
                scenarioDate, simulation, grid);
        if (!pathwise) {
            return getInitialMarginFromLosses(getRevaluationLosses(date, portfolio, model,
                    data.getCurveScenarios(simulation), data.getPnlSeries(simulation)
                            .getNLowestValues(NUMBER_OF_FULL_REVALUATIONS, null), null), simulation);
        }
        double[] margins = new double[model.getNumberOfPaths()];
        for (int path = 0; path < margins.length; path++) {
            List<LocalDateTime> dates = data.getPnlSeries(simulation).getNLowestValues(NUMBER_OF_FULL_REVALUATIONS, path);
            margins[path] = getInitialMarginFromLosses(getRevaluationLosses(date, portfolio, model,
                    data.getCurveScenarios(simulation), dates, path), simulation).get(0);
        }
        return new RandomVariableFromDoubleArray(FloatingpointDate.getFloatingPointDateFromDate(model.getReferenceDate(), date), margins);
    }

    /**
     * Builds complete risk-grid scenarios and delta.h + 1/2 h^T.gamma.h.
     * Delta is currency/bp, gamma is the full symmetric Hessian in currency/bp^2;
     * scenario shocks are decimal zero-rate changes. Both matrix orientations of
     * mixed gamma are traversed, with exactly one Taylor factor 1/2.
     */
    public static InitialMarginContainer getScenarioData(SensitivityMatrix delta, SensitivityMatrix gamma,
            ScenarioFactory scenarios, LocalDateTime scenarioDate, Simulation simulation, TenorGrid grid)
            throws CalculationException {
        Objects.requireNonNull(delta, "delta");
        Objects.requireNonNull(gamma, "gamma");
        CurveScenarios curves = getCurveScenarios(scenarios, scenarioDate, grid);
        HashMap<LocalDateTime, RandomVariable> values = new HashMap<>();
        for (LocalDateTime date : curves.getSeries(grid.getFixingSmallest()).getSeries().keySet()) {
            values.put(date, new Scalar(0.0));
        }
        for (Map.Entry<Double, TreeMap<Double, RandomVariable>> row : delta.getSensitivityMatrix().entrySet()) {
            if (row.getValue().size() != 1 || !row.getValue().containsKey(row.getKey())) {
                throw new IllegalArgumentException("Delta must be diagonal at tenor " + row.getKey());
            }
            RandomVariableSeries shifts = requireSeries(curves, row.getKey());
            RandomVariable sensitivity = row.getValue().get(row.getKey()).mult(DECIMAL_TO_BASIS_POINT);
            for (LocalDateTime date : values.keySet()) {
                values.put(date, values.get(date).addProduct(shifts.getValue(date), sensitivity));
            }
        }
        for (Map.Entry<Double, TreeMap<Double, RandomVariable>> row : gamma.getSensitivityMatrix().entrySet()) {
            RandomVariableSeries first = requireSeries(curves, row.getKey());
            for (Map.Entry<Double, RandomVariable> column : row.getValue().entrySet()) {
                RandomVariableSeries second = requireSeries(curves, column.getKey());
                RandomVariable coefficient = column.getValue().mult(0.5 * DECIMAL_TO_BASIS_POINT * DECIMAL_TO_BASIS_POINT);
                for (LocalDateTime date : values.keySet()) {
                    values.put(date, values.get(date).addProduct(first.getValue(date).mult(second.getValue(date)), coefficient));
                }
            }
        }
        InitialMarginContainer data = new InitialMarginContainer();
        data.setDeltaSensitivities(delta);
        data.setGammaSensitivities(gamma);
        data.setCurveScenarios(simulation, curves);
        data.setPnLSeries(simulation, new RandomVariableSeries(values));
        return data;
    }

    private static RandomVariableSeries requireSeries(CurveScenarios curves, double tenor) {
        if (!curves.containsSeries(tenor)) {
            throw new IllegalArgumentException("Sensitivity tenor is outside the scenario risk grid: " + tenor);
        }
        return curves.getSeries(tenor);
    }

    /** Includes zero-risk buckets, which are still needed for shocked-curve interpolation. */
    public static CurveScenarios getCurveScenarios(ScenarioFactory scenarios, LocalDateTime date, TenorGrid grid)
            throws CalculationException {
        Objects.requireNonNull(scenarios, "scenarios");
        Objects.requireNonNull(date, "scenarioDate");
        Objects.requireNonNull(grid, "grid");
        if (grid.getFixings().isEmpty()) {
            throw new IllegalArgumentException("Scenario risk grid must not be empty.");
        }
        CurveScenarios curves = new CurveScenarios();
        Set<LocalDateTime> dates = null;
        for (double tenor : grid.getFixings()) {
            RandomVariableSeries series = scenarios.getTenorPointScenarios(date, tenor);
            if (series.getSeries().isEmpty()) {
                throw new CalculationException("No scenarios at tenor " + tenor);
            }
            if (dates != null && !dates.equals(series.getSeries().keySet())) {
                throw new CalculationException("Scenario dates differ across risk-grid tenors.");
            }
            dates = series.getSeries().keySet();
            curves.addSeries(tenor, series);
        }
        return curves;
    }

    /** Exhaustive scenario revaluation for comparison with the twenty-candidate PAIRS method. */
    public static RandomVariable getInitialMarginWithFullRevaluation(LocalDateTime date, ZeroRateModel model,
            LocalPortfolio portfolio, CurveScenarios scenarios, Simulation simulation, boolean pathwise)
            throws CalculationException {
        if (scenarios.getCurveScenarios().isEmpty()) {
            throw new CalculationException("Full revaluation requires scenario curves.");
        }
        List<LocalDateTime> dates = new ArrayList<>(scenarios.getCurveScenarios().values().iterator().next().getSeries().keySet());
        if (!pathwise) {
            return getInitialMarginFromLosses(getRevaluationLosses(date, portfolio, model, scenarios, dates, null), simulation);
        }
        double[] margins = new double[model.getNumberOfPaths()];
        for (int path = 0; path < margins.length; path++) {
            margins[path] = getInitialMarginFromLosses(getRevaluationLosses(date, portfolio, model, scenarios, dates, path), simulation).get(0);
        }
        return new RandomVariableFromDoubleArray(FloatingpointDate.getFloatingPointDateFromDate(model.getReferenceDate(), date), margins);
    }

    /** Signed PnL (shocked value minus base value), with all swap legs netted before ranking. */
    public static List<RandomVariable> getRevaluationLosses(LocalDateTime date, LocalPortfolio portfolio,
            ZeroRateModel model, CurveScenarios scenarios, List<LocalDateTime> dates, Integer path)
            throws CalculationException {
        List<RandomVariable> values = new ArrayList<>();
        for (int index = 0; index < dates.size(); index++) {
            values.add(new Scalar(0.0));
        }
        for (LchSwapLeg leg : portfolio.getSwaps()) {
            RandomVariable base = leg.getValue(date, model, null, path);
            for (int index = 0; index < dates.size(); index++) {
                values.set(index, values.get(index).add(leg.getValue(date, model,
                        scenarios.getCurveShifts(dates.get(index)), path).sub(base)));
            }
        }
        return values;
    }

    /** Selects the lowest signed PnLs and returns max(0,-ES) or max(0,-VaR). */
    public static RandomVariable getInitialMarginFromLosses(List<RandomVariable> pnls, Simulation simulation)
            throws CalculationException {
        int count;
        switch (simulation) {
        case EXPECTED_SHORTFALL_4: count = 4; break;
        case EXPECTED_SHORTFALL_6: count = 6; break;
        case VALUE_AT_RISK: count = 13; break;
        default: throw new IllegalArgumentException("Unsupported simulation: " + simulation);
        }
        if (pnls.size() < count) {
            throw new CalculationException("Initial margin requires at least " + count + " scenario PnLs.");
        }
        // Scalars give pathwise ranking; otherwise the average ranks a common scenario set.
        PriorityQueue<RandomVariable> worst = new PriorityQueue<>(Comparator.comparingDouble(pnl -> -pnl.getAverage()));
        for (RandomVariable pnl : pnls) {
            for (int path = 0; path < pnl.size(); path++) {
                if (!Double.isFinite(pnl.get(path))) {
                    throw new CalculationException("Scenario PnL must be finite on every path.");
                }
            }
            worst.add(pnl);
            if (worst.size() > count) {
                worst.poll();
            }
        }
        RandomVariable tail = new Scalar(0.0);
        if (simulation == Simulation.VALUE_AT_RISK) {
            tail = worst.peek();
        } else {
            for (RandomVariable pnl : worst) {
                tail = tail.add(pnl);
            }
            tail = tail.div(count);
        }
        return tail.mult(-1.0).floor(0.0);
    }
}
