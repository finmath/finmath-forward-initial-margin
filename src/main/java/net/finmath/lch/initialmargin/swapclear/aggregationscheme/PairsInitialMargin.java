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

import net.finmath.lch.initialmargin.simulation.modeldata.RandomVariableSeries;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGrid;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGridFactory;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGridFactory.GridType;
import net.finmath.lch.initialmargin.simulation.scenarios.CurveScenarios;
import net.finmath.lch.initialmargin.simulation.scenarios.ScenarioFactory;
import net.finmath.lch.initialmargin.simulation.scenarios.ScenarioFactory.Simulation;
import net.finmath.lch.initialmargin.swapclear.products.LocalPortfolio;
import net.finmath.lch.initialmargin.swapclear.sensitivities.AadZeroRateSensitivities;
import net.finmath.lch.initialmargin.swapclear.sensitivities.Sensitivities;
import net.finmath.lch.initialmargin.swapclear.sensitivities.SensitivityMatrix;
import net.finmath.lch.initialmargin.valuation.CurveValuation;
import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.FloatingpointDate;


/**
 * Calculation scheme of the LCH PAIRS initial margin methodology.
 * Candidate scenarios are ranked using local zero-rate AAD delta, followed by
 * full revaluation. TODO: add gamma ranking once a validated provider is available.
 * 
 */
public class PairsInitialMargin {
	
	private static final double DECIMAL_TO_BASIS_POINT_CONVERSION_FACTOR = 10000.0;
	private static final int NUMBER_OF_FULL_REVALUATIONS = 20;
	
	
	/**
	 * Base IM is calculated on a 10y window of historical scenarios.
	 * A currency-specific standardized zero curve, e.g. EUR_EURIBOR, is used for swap valuing and risk sampling/mapping.
	 * Initial Margin is calculated as an Expected-Shortfall on the 6 worst losses
	 * 
	 * @param evaluationDate Time for which IM will be calculated
	 * @param zeroRateModel Model under which IM is valued (risk, swap values, scenarios)
	 * @param localPortfolio Portfolio consisting of interest swaps
	 * @param pathWiseEvaluation If true scenarios and IM will be calculated on each path separately, otherwise as the average of all paths
	 * @param movingScenarioWindow If true the scenarios will move with the model evaluation date -> LMM of ZeroRateModel will generate additional scenarios
	 * @return The Base Initial Margin of the LCH methodology
	 * @throws CalculationException
	 */
	public static RandomVariable getBaseInitialMargin(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel, LocalPortfolio localPortfolio, boolean pathWiseEvaluation, boolean movingScenarioWindow) throws CalculationException {
		return getBaseInitialMargin(evaluationDate, zeroRateModel, (CurveValuation) localPortfolio, pathWiseEvaluation, movingScenarioWindow);
	}

	/** Local common-curve valuation of any supported product or portfolio. */
	public static RandomVariable getBaseInitialMargin(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel, CurveValuation valuation, boolean pathWiseEvaluation, boolean movingScenarioWindow) throws CalculationException {
		TenorGrid grid = TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID);
		return getBaseInitialMargin(evaluationDate, zeroRateModel, valuation, pathWiseEvaluation, movingScenarioWindow,
				new AadZeroRateSensitivities(valuation, zeroRateModel, grid), grid);
	}

	/** Delta-ranked PAIRS with an explicitly supplied sensitivity provider and risk grid. */
	public static RandomVariable getBaseInitialMargin(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel,
			LocalPortfolio localPortfolio, boolean pathWiseEvaluation, boolean movingScenarioWindow,
			Sensitivities sensitivities, TenorGrid tenorGrid) throws CalculationException {
		return getBaseInitialMargin(evaluationDate, zeroRateModel, (CurveValuation) localPortfolio, pathWiseEvaluation,
				movingScenarioWindow, sensitivities, tenorGrid);
	}

	public static RandomVariable getBaseInitialMargin(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel,
			CurveValuation valuation, boolean pathWiseEvaluation, boolean movingScenarioWindow,
			Sensitivities sensitivities, TenorGrid tenorGrid) throws CalculationException {
		return getRankedInitialMargin(evaluationDate, zeroRateModel, valuation,
				sensitivities.getDeltaSensitivities(evaluationDate), Simulation.EXPECTED_SHORTFALL_6,
				pathWiseEvaluation, movingScenarioWindow, tenorGrid);
	}
	
	

	/**
	 * Floor IM is calculated on a 10y window of historical scenarios plus fixed stress period January 2008 - June 2010.
	 * A currency-specific standardized zero curve, e.g. EUR_EURIBOR, is used for swap valuing and risk sampling/mapping.
	 * Initial Margin is calculated as a Value-at-Risk at 13th worst loss.
	 * 
	 * @param evaluationDate Time for which IM will be calculated
	 * @param zeroRateModel Model under which IM is valued (risk, swap values, scenarios)
	 * @param localPortfolio Portfolio consisting of interest swaps
	 * @param pathWiseEvaluation If true scenarios and IM will be calculated on each path separately, otherwise as the average of all paths
	 * @param movingScenarioWindow If true the scenarios will move with the model evaluation date -> LMM of ZeroRateModel will generate additional scenarios
	 * @return The Base Initial Margin of the LCH methodology
	 * @throws CalculationException
	 */
	public static RandomVariable getFloorInitialMargin(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel, LocalPortfolio localPortfolio, boolean pathWiseEvaluation, boolean movingScenarioWindow) throws CalculationException {
		return getFloorInitialMargin(evaluationDate, zeroRateModel, (CurveValuation) localPortfolio, pathWiseEvaluation, movingScenarioWindow);
	}

	public static RandomVariable getFloorInitialMargin(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel, CurveValuation valuation, boolean pathWiseEvaluation, boolean movingScenarioWindow) throws CalculationException {
		TenorGrid grid = TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID);
		return getFloorInitialMargin(evaluationDate, zeroRateModel, valuation, pathWiseEvaluation, movingScenarioWindow,
				new AadZeroRateSensitivities(valuation, zeroRateModel, grid), grid);
	}

	/** Delta-ranked floor IM; unavailable gamma is never treated as a zero matrix. */
	public static RandomVariable getFloorInitialMargin(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel,
			LocalPortfolio localPortfolio, boolean pathWiseEvaluation, boolean movingScenarioWindow,
			Sensitivities sensitivities, TenorGrid tenorGrid) throws CalculationException {
		return getFloorInitialMargin(evaluationDate, zeroRateModel, (CurveValuation) localPortfolio, pathWiseEvaluation,
				movingScenarioWindow, sensitivities, tenorGrid);
	}

	public static RandomVariable getFloorInitialMargin(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel,
			CurveValuation valuation, boolean pathWiseEvaluation, boolean movingScenarioWindow,
			Sensitivities sensitivities, TenorGrid tenorGrid) throws CalculationException {
		return getRankedInitialMargin(evaluationDate, zeroRateModel, valuation,
				sensitivities.getDeltaSensitivities(evaluationDate), Simulation.VALUE_AT_RISK,
				pathWiseEvaluation, movingScenarioWindow, tenorGrid);
	}
	
	
	/**
	 * Pairs IM is the maximum of the Base IM and Floor IM.
	 * 
	 * @param evaluationDate Time for which IM will be calculated
	 * @param zeroRateModel Model under which IM is valued (risk, swap values, scenarios)
	 * @param localPortfolio Portfolio consisting of interest swaps
	 * @param pathWiseEvaluation If true scenarios and IM will be calculated on each path separately, otherwise as the average of all paths
	 * @param movingScenarioWindow If true the scenarios will move with the model evaluation date -> LMM of ZeroRateModel will generate additional scenarios
	 * @return The Base Initial Margin of the LCH methodology
	 * @throws CalculationException
	 */
	public static RandomVariable getPairsInitialMargin(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel, LocalPortfolio localPortfolio, boolean pathWiseEvaluation, boolean movingScenarioWindow) throws CalculationException {
		return getPairsInitialMargin(evaluationDate, zeroRateModel, (CurveValuation) localPortfolio, pathWiseEvaluation, movingScenarioWindow);
	}

	public static RandomVariable getPairsInitialMargin(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel, CurveValuation valuation, boolean pathWiseEvaluation, boolean movingScenarioWindow) throws CalculationException {
		TenorGrid grid = TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID);
		return getPairsInitialMargin(evaluationDate, zeroRateModel, valuation, pathWiseEvaluation, movingScenarioWindow,
				new AadZeroRateSensitivities(valuation, zeroRateModel, grid), grid);
	}

	/** Delta-ranked base/floor comparison using a common provider and complete risk grid. */
	public static RandomVariable getPairsInitialMargin(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel,
			LocalPortfolio localPortfolio, boolean pathWiseEvaluation, boolean movingScenarioWindow,
			Sensitivities sensitivities, TenorGrid tenorGrid) throws CalculationException {
		return getPairsInitialMargin(evaluationDate, zeroRateModel, (CurveValuation) localPortfolio, pathWiseEvaluation,
				movingScenarioWindow, sensitivities, tenorGrid);
	}

	public static RandomVariable getPairsInitialMargin(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel,
			CurveValuation valuation, boolean pathWiseEvaluation, boolean movingScenarioWindow,
			Sensitivities sensitivities, TenorGrid tenorGrid) throws CalculationException {
		SensitivityMatrix delta = sensitivities.getDeltaSensitivities(evaluationDate);
		RandomVariable baseInitialMargin = getRankedInitialMargin(evaluationDate, zeroRateModel, valuation, delta,
				Simulation.EXPECTED_SHORTFALL_6, pathWiseEvaluation, movingScenarioWindow, tenorGrid);
		RandomVariable floorInitialMargin = getRankedInitialMargin(evaluationDate, zeroRateModel, valuation, delta,
				Simulation.VALUE_AT_RISK, pathWiseEvaluation, movingScenarioWindow, tenorGrid);
		if (pathWiseEvaluation) {
			return baseInitialMargin.floor(floorInitialMargin);
		}
		return baseInitialMargin.getAverage() > floorInitialMargin.getAverage() ? baseInitialMargin : floorInitialMargin;
	}
	
	
	private static RandomVariable getRankedInitialMargin(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel,
            CurveValuation valuation, SensitivityMatrix delta, Simulation simulation, boolean pathWiseEvaluation,
            boolean movingScenarioWindow, TenorGrid tenorGrid)
            throws CalculationException {
        LocalDateTime scenarioDate = movingScenarioWindow ? evaluationDate : zeroRateModel.getReferenceDate();
        InitialMarginContainer data = getDeltaScenarioData(delta,
                new ScenarioFactory(zeroRateModel, simulation), scenarioDate, simulation, tenorGrid);
        if (!pathWiseEvaluation) {
            return getInitialMarginFromLosses(getNWorstLosses(evaluationDate, valuation, zeroRateModel,
                    data, simulation, NUMBER_OF_FULL_REVALUATIONS, null), simulation);
        }
        double[] margins = new double[zeroRateModel.getNumberOfPaths()];
        for (int path = 0; path < margins.length; path++) {
            margins[path] = getInitialMarginFromLosses(getNWorstLosses(evaluationDate, valuation, zeroRateModel,
                    data, simulation, NUMBER_OF_FULL_REVALUATIONS, path), simulation).get(0);
        }
        double time = FloatingpointDate.getFloatingPointDateFromDate(zeroRateModel.getReferenceDate(), evaluationDate);
        return new RandomVariableFromDoubleArray(time, margins);
    }

    /**
     * Complete scenario curves and first-order PnL for BP-scaled zero-rate deltas.
     * Gamma is not calculated or represented by an empty matrix. The fixed risk
     * grid, rather than the nonzero delta entries, determines the revaluation curve.
     */
    public static InitialMarginContainer getDeltaScenarioData(SensitivityMatrix delta, ScenarioFactory scenarios,
            LocalDateTime scenarioDate, Simulation simulation, TenorGrid tenorGrid) throws CalculationException {
        Objects.requireNonNull(delta, "delta");
        CurveScenarios curves = getCurveScenarios(scenarios, scenarioDate, tenorGrid);
        HashMap<LocalDateTime, RandomVariable> zeroPnls = new HashMap<>();
        for (LocalDateTime date : curves.getSeries(tenorGrid.getFixingSmallest()).getSeries().keySet()) {
            zeroPnls.put(date, new Scalar(0.0));
        }
        RandomVariableSeries pnl = new RandomVariableSeries(zeroPnls);
        for (Map.Entry<Double, TreeMap<Double, RandomVariable>> row : delta.getSensitivityMatrix().entrySet()) {
            RandomVariable sensitivity = row.getValue().get(row.getKey());
            if (sensitivity == null || row.getValue().size() != 1) {
                throw new IllegalArgumentException("Delta sensitivities must be diagonal at tenor " + row.getKey());
            }
            if (!curves.containsSeries(row.getKey())) {
                throw new IllegalArgumentException("Delta tenor is outside the scenario risk grid: " + row.getKey());
            }
            pnl.sum(getFirstOrderTaylorTerm(curves.getSeries(row.getKey()), sensitivity));
        }
        InitialMarginContainer result = new InitialMarginContainer();
        result.setDeltaSensitivities(delta);
        result.setCurveScenarios(simulation, curves);
        result.setPnLSeries(simulation, pnl);
        return result;
    }

    /** Builds all risk-grid shocks, including buckets with zero or absent delta. */
    public static CurveScenarios getCurveScenarios(ScenarioFactory scenarios, LocalDateTime scenarioDate,
            TenorGrid tenorGrid) throws CalculationException {
        Objects.requireNonNull(scenarios, "scenarios");
        Objects.requireNonNull(scenarioDate, "scenarioDate");
        Objects.requireNonNull(tenorGrid, "tenorGrid");
        if (tenorGrid.getFixings().isEmpty()) {
            throw new IllegalArgumentException("Scenario risk grid must not be empty.");
        }
        CurveScenarios curves = new CurveScenarios();
        Set<LocalDateTime> dates = null;
        for (double tenor : tenorGrid.getFixings()) {
            RandomVariableSeries series = scenarios.getTenorPointScenarios(scenarioDate, tenor);
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


	/*
	 * The Taylor expansion for the PnL calculation is a vector operation.
	 * The entries of the vector are the pillar points on which the scenarios and sensitivities are sampled.
	 * This method equals the element-wise multiplication of the i-th entries
	 */
	private static RandomVariableSeries getFirstOrderTaylorTerm(RandomVariableSeries tenorPointScenario, RandomVariable sensitivity) {
		HashMap<LocalDateTime, RandomVariable> pnlOnTenorPoint = new HashMap<>();
		for (Map.Entry<LocalDateTime, RandomVariable> shift : tenorPointScenario.getSeries().entrySet()) {
			pnlOnTenorPoint.put(shift.getKey(), shift.getValue().mult(DECIMAL_TO_BASIS_POINT_CONVERSION_FACTOR).mult(sensitivity));
		}
		return new RandomVariableSeries(pnlOnTenorPoint);
	}

	
	// Revalue the same product used by AAD, under its delta-ranked candidate scenarios.
	private static List<RandomVariable> getNWorstLosses(LocalDateTime evaluationDate, CurveValuation valuation,
            ZeroRateModel zeroRateModel, InitialMarginContainer data, Simulation simulation,
            int numberOfLosses, Integer pathOrState) throws CalculationException {
        List<LocalDateTime> dates = data.getPnlSeries(simulation).getNLowestValues(numberOfLosses, pathOrState);
        return getRevaluationLosses(evaluationDate, valuation, zeroRateModel,
                data.getCurveScenarios(simulation), dates, pathOrState);
    }

    /**
     * Full revaluation of every supplied scenario, using the same valuation and
     * ES/VaR aggregation as delta-ranked PAIRS. No sensitivity provider is required.
     */
    public static RandomVariable getInitialMarginWithFullRevaluation(LocalDateTime evaluationDate,
            ZeroRateModel zeroRateModel, CurveValuation valuation, CurveScenarios scenarios,
            Simulation simulation, boolean pathWiseEvaluation) throws CalculationException {
        Objects.requireNonNull(valuation, "valuation");
        if (scenarios.getCurveScenarios().isEmpty()) {
            throw new CalculationException("Full revaluation requires scenario curves.");
        }
        List<LocalDateTime> dates = new ArrayList<>(scenarios.getCurveScenarios().values().iterator().next()
                .getSeries().keySet());
        if (!pathWiseEvaluation) {
            return getInitialMarginFromLosses(getRevaluationLosses(evaluationDate, valuation, zeroRateModel,
                    scenarios, dates, null), simulation);
        }
        double[] margins = new double[zeroRateModel.getNumberOfPaths()];
        for (int path = 0; path < margins.length; path++) {
            margins[path] = getInitialMarginFromLosses(getRevaluationLosses(evaluationDate, valuation,
                    zeroRateModel, scenarios, dates, path), simulation).get(0);
        }
        double time = FloatingpointDate.getFloatingPointDateFromDate(zeroRateModel.getReferenceDate(), evaluationDate);
        return new RandomVariableFromDoubleArray(time, margins);
    }

    private static List<RandomVariable> getRevaluationLosses(LocalDateTime evaluationDate,
            CurveValuation valuation, ZeroRateModel zeroRateModel, CurveScenarios scenarios,
            List<LocalDateTime> dates, Integer pathOrState) throws CalculationException {
        RandomVariable baseValue = valuation.getValue(evaluationDate, zeroRateModel, null, pathOrState);
        List<RandomVariable> losses = new ArrayList<>(dates.size());
        for (LocalDateTime date : dates) {
            losses.add(valuation.getValue(evaluationDate, zeroRateModel, scenarios.getCurveShifts(date), pathOrState)
                    .sub(baseValue));
        }
        return losses;
    }

	
	/** ES/VaR aggregation shared by delta PnL and full revaluation. */
	public static RandomVariable getInitialMarginFromLosses(List<RandomVariable> nWorstLosses, Simulation simulation) throws CalculationException {
	    int numberOfLosses;
	    switch (simulation) {
        	case EXPECTED_SHORTFALL_4:
        		numberOfLosses = 4; 
        		break;
	        case EXPECTED_SHORTFALL_6:
	        	numberOfLosses = 6; 
	            break;
	        case VALUE_AT_RISK:
	        	numberOfLosses = 13;
	            break;
	        default:
	            throw new IllegalArgumentException("Unsupported simulation type: " + simulation);
	    }
	    if (nWorstLosses.size() < numberOfLosses) {
	        throw new CalculationException("Initial margin requires at least " + numberOfLosses + " scenario losses.");
	    }
	    // In case of path-wise evaluation we pass a List of Scalars to the method, therefore the comparator works for both cases
	    PriorityQueue<RandomVariable> worstLosses = new PriorityQueue<>(Comparator.comparingDouble( losses -> {
	    	return losses.mult(-1.0).getAverage();
	    }));
	    for (RandomVariable loss : nWorstLosses) {
	        worstLosses.add(loss);
	        if (worstLosses.size() > numberOfLosses) {
	            worstLosses.poll(); 
	        }
	    }	    
	    RandomVariable initialMargin = new Scalar(0.0);
	    if (simulation == Simulation.EXPECTED_SHORTFALL_4 || simulation == Simulation.EXPECTED_SHORTFALL_6) {
	    	// ES: average of 4/6 worst losses
	        for (RandomVariable loss : worstLosses) {
	            initialMargin = initialMargin.add(loss);
	        }
	        initialMargin = initialMargin.div(numberOfLosses).abs(); 
	    } else if (simulation == Simulation.VALUE_AT_RISK) {
	    	// VaR: 13th worst loss
	        initialMargin = worstLosses.poll().abs();
	    }
		return initialMargin;
	}

	
}
