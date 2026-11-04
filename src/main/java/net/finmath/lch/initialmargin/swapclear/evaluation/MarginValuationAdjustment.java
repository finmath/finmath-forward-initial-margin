package net.finmath.lch.initialmargin.swapclear.evaluation;

import static net.finmath.lch.initialmargin.swapclear.aggregationscheme.PairsInitialMargin.getInitialMarginFromLosses;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import net.finmath.lch.initialmargin.simulation.modeldata.RandomVariableSeries;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGrid;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGridFactory;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGridFactory.GridType;
import net.finmath.lch.initialmargin.simulation.scenarios.CurveScenarios;
import net.finmath.lch.initialmargin.simulation.scenarios.ScenarioFactory;
import net.finmath.lch.initialmargin.simulation.scenarios.ScenarioFactory.Simulation;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.PairsInitialMargin;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.Currency;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.CurveName;
import net.finmath.lch.initialmargin.swapclear.libormodels.ModelFactory;
import net.finmath.lch.initialmargin.swapclear.products.LchSwap;
import net.finmath.lch.initialmargin.swapclear.products.LocalPortfolio;
import net.finmath.lch.initialmargin.swapclear.sensitivities.AadZeroRateSensitivities;
import net.finmath.lch.initialmargin.swapclear.sensitivities.Sensitivities;
import net.finmath.lch.initialmargin.valuation.CurveValuation;
import net.finmath.exception.CalculationException;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.FloatingpointDate;


public class MarginValuationAdjustment {
	
	public enum InitialMargin  {BASE, FLOOR, PAIRS};
	
	/*
	 * Last date of available zero rate data
	 * LMM's of ModelFactory are calibrated to the same date
	 */
	private static final LocalDateTime REFERENCE_DATE = ModelFactory.REFERENCE_DATE; 

	private static final double NANOSECONDS_TO_SECONDS = 1E09;
	
	public static void main(String args[]) throws CalculationException {
		
		// Get the model under which swaps and MVA is valued
		final LIBORModelMonteCarloSimulationModel liborModel = ModelFactory.getModel(CurveName.EUR_EURIBOR_3M);
		
		// Create ZeroRateModel based on zero rate history and model data
		final ZeroRateModel zeroRateModel = new ZeroRateModel(Currency.EUR, CurveName.EUR_EURIBOR_3M, liborModel,"spreadsheets/EUR_EURIBOR_3M_Input_Data.xlsx");
		
		// Create a portfolio of swaps belonging to the same currency, but not necessarily same index 
		LchSwap swap1 = PortfolioFactory.create5YPayerSwap();
		LchSwap swap2 = PortfolioFactory.create15YReceiverSwap();
		LocalPortfolio swapPortfolio = PortfolioFactory.getSwapPortfolio(swap1, swap2).getLocalPortfolio(Currency.EUR); 

		// Calculate MVA of the portfolio 
		int daysPerStep = 1;
		double fundingSpread = 0.005;
		
//		getMVA(swapPortfolio, zeroRateModel, liborModel, REFERENCE_DATE, daysPerStep, fundingSpread, InitialMargin.FLOOR, false, false);
//		getMVA(swapPortfolio, zeroRateModel, liborModel, REFERENCE_DATE, daysPerStep, fundingSpread, InitialMargin.FLOOR, false, true);
//		getMVA(swapPortfolio, zeroRateModel, liborModel, REFERENCE_DATE, daysPerStep, fundingSpread, InitialMargin.FLOOR, true, false);
//		getMVA(swapPortfolio, zeroRateModel, liborModel, REFERENCE_DATE, daysPerStep, fundingSpread, InitialMargin.FLOOR, true, true);
//		
//		getMVA(swapPortfolio, zeroRateModel, liborModel, REFERENCE_DATE, daysPerStep, fundingSpread, InitialMargin.BASE, false, false);
//		getMVA(swapPortfolio, zeroRateModel, liborModel, REFERENCE_DATE, daysPerStep, fundingSpread, InitialMargin.BASE, false, true);
//		getMVA(swapPortfolio, zeroRateModel, liborModel, REFERENCE_DATE, daysPerStep, fundingSpread, InitialMargin.BASE, true, false);
//		getMVA(swapPortfolio, zeroRateModel, liborModel, REFERENCE_DATE, daysPerStep, fundingSpread, InitialMargin.BASE, true, true);
//		
//		getMVA(swapPortfolio, zeroRateModel, liborModel, REFERENCE_DATE, daysPerStep, fundingSpread, InitialMargin.PAIRS, false, false);
//		getMVA(swapPortfolio, zeroRateModel, liborModel, REFERENCE_DATE, daysPerStep, fundingSpread, InitialMargin.PAIRS, false, true);
//		getMVA(swapPortfolio, zeroRateModel, liborModel, REFERENCE_DATE, daysPerStep, fundingSpread, InitialMargin.PAIRS, true, false);
//		getMVA(swapPortfolio, zeroRateModel, liborModel, REFERENCE_DATE, daysPerStep, fundingSpread, InitialMargin.PAIRS, true, true);
				

	}
	
	
	
	public static void getMVA(LocalPortfolio localPortfolio, ZeroRateModel zeroRateModel, LIBORModelMonteCarloSimulationModel liborModel, LocalDateTime startDate, int daysPerStep, double fundingSpread, InitialMargin type, boolean pathWiseEvaluation, boolean movingScenarioWindow) throws CalculationException {
		System.out.println("Calculating Margin Valuation Adjustment Exact:");
		String analysis = "Average";
		if (pathWiseEvaluation) {
			analysis = "Pathwise";
		}
		String window = "Fixed";
		if (movingScenarioWindow) {
			window = "Moving";
		}
		System.out.println("IM type: " + type.toString() + " | Evaluation: " + analysis + " | Scenarios: " + window);

		RandomVariable mva = new RandomVariableFromDoubleArray(0.0);
		LocalDateTime lastPaymentDate = FloatingpointDate.getDateFromFloatingPointDate(startDate, localPortfolio.getLastPaymentDate());
		double time = 0.0;
		double nextTime = 1/365.0; 
		long startTime = System.nanoTime();
		for (LocalDateTime date = startDate; date.isBefore(lastPaymentDate); date = date.plusDays(daysPerStep)) {
			if (SchemeStorage.getLocalCalendar(Currency.EUR).isBusinessday(date.toLocalDate())) {
				RandomVariable initialMargin;
				switch (type) {
				case BASE:
					initialMargin = localPortfolio.getBaseInitialMargin(date, zeroRateModel, pathWiseEvaluation, movingScenarioWindow);
					break;
				case FLOOR:
					initialMargin = localPortfolio.getFloorInitialMargin(date, zeroRateModel, pathWiseEvaluation, movingScenarioWindow);
					break;
				case PAIRS:
					initialMargin = localPortfolio.getPairsInitialMargin(date, zeroRateModel, pathWiseEvaluation, movingScenarioWindow);
					break;
				default:
					throw new IllegalArgumentException("Initial Margin type not defined.");
				}
				System.out.println("Initial Margin as of " + date +": " + initialMargin.getAverage());
				// d(1/N^fd(t)) = 1/N^fd(t_i+1) - 1/N^fd(t_i)
				RandomVariable fundingNumeraire = liborModel.getNumeraire(nextTime).mult(Math.exp((nextTime) * fundingSpread)).invert();
				fundingNumeraire = fundingNumeraire.sub(liborModel.getNumeraire(time).mult(Math.exp(time * fundingSpread)).invert());
				mva = mva.add(fundingNumeraire.mult(initialMargin));
				time += daysPerStep/365.0;
			}
			nextTime += daysPerStep/365.0;
		}
		long endTime = System.nanoTime();


		System.out.println("Elapsed time: " + ((endTime - startTime)/NANOSECONDS_TO_SECONDS) + "seconds");
		System.out.println("MVA: " + (-mva.getAverage()));
		System.out.println();
	}
	
	
	public static void getMVAWithTaylorPnL(LocalPortfolio localPortfolio, ZeroRateModel zeroRateModel, LIBORModelMonteCarloSimulationModel liborModel, LocalDateTime startDate, int daysPerStep, double fundingSpread, Simulation simulation, boolean pathWiseEvaluation, boolean movingScenarioWindow) throws CalculationException {
		System.out.println("Calculating Margin Valuation Adjustment Delta PnL:");
		String analysis = "Average";
		if (pathWiseEvaluation) {
			analysis = "Pathwise";
		}
		String window = "Fixed";
		if (movingScenarioWindow) {
			window = "Moving";
		}
		System.out.println("Simulation: " + simulation.toString() + " | Evaluation: " + analysis + " | Scenarios: " + window);

		TenorGrid tenorGrid = TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID);
		Sensitivities sensitivities = new AadZeroRateSensitivities(localPortfolio, zeroRateModel, tenorGrid);
		ScenarioFactory scenarios = new ScenarioFactory(zeroRateModel, simulation);	

		RandomVariable mva = new RandomVariableFromDoubleArray(0.0);
		LocalDateTime lastPaymentDate = FloatingpointDate.getDateFromFloatingPointDate(startDate, localPortfolio.getLastPaymentDate());
		double time = 0.0;
		double nextTime = 1/365.0; 
		long startTime = System.nanoTime();
		for (LocalDateTime date = startDate; date.isBefore(lastPaymentDate); date = date.plusDays(daysPerStep)) {
			if (SchemeStorage.getLocalCalendar(Currency.EUR).isBusinessday(date.toLocalDate())) {
				RandomVariable initialMargin = getInitialMarginWithTaylorPnL(date, zeroRateModel, sensitivities, scenarios, simulation, pathWiseEvaluation, movingScenarioWindow, tenorGrid);
//				System.out.println("Initial Margin as of " + date +": " + initialMargin.getAverage());
				// d(1/N^fd(t)) = 1/N^fd(t_i+1) - 1/N^fd(t_i)
				RandomVariable fundingNumeraire = liborModel.getNumeraire(nextTime).mult(Math.exp((nextTime) * fundingSpread)).invert();
				fundingNumeraire = fundingNumeraire.sub(liborModel.getNumeraire(time).mult(Math.exp(time * fundingSpread)).invert());
				mva = mva.add(fundingNumeraire.mult(initialMargin));
				time += daysPerStep/365.0;
			}
			nextTime += daysPerStep/365.0;
		}
		long endTime = System.nanoTime();

		System.out.println("Elapsed time: " + ((endTime - startTime)/NANOSECONDS_TO_SECONDS) + "seconds");
		System.out.println("MVA: " + (-mva.getAverage()));
		System.out.println();
	}
	
	
    /** First-order PnL only. TODO: gamma requires a separately validated provider. */
    public static RandomVariable getInitialMarginWithTaylorPnL(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel,
            Sensitivities sensitivities, ScenarioFactory scenarios, Simulation simulation,
            boolean pathWiseEvaluation, boolean movingScenarioWindow) throws CalculationException {
        return getInitialMarginWithTaylorPnL(evaluationDate, zeroRateModel, sensitivities, scenarios, simulation,
                pathWiseEvaluation, movingScenarioWindow, TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID));
    }

    public static RandomVariable getInitialMarginWithTaylorPnL(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel,
            Sensitivities sensitivities, ScenarioFactory scenarios, Simulation simulation,
            boolean pathWiseEvaluation, boolean movingScenarioWindow, TenorGrid tenorGrid) throws CalculationException {
        LocalDateTime scenarioDate = movingScenarioWindow ? evaluationDate : zeroRateModel.getReferenceDate();
        RandomVariableSeries pnLSeries = PairsInitialMargin.getDeltaScenarioData(
                sensitivities.getDeltaSensitivities(evaluationDate), scenarios, scenarioDate, simulation, tenorGrid)
                .getPnlSeries(simulation);
		RandomVariable initialMargin;
		if (pathWiseEvaluation) {
			double[] initialMarginRealizations = new double[zeroRateModel.getNumberOfPaths()];
			for (int pathNumber = 0; pathNumber < zeroRateModel.getNumberOfPaths(); pathNumber++) {
				List<RandomVariable> pnLs = getLosses(pnLSeries, pathNumber);
				initialMarginRealizations[pathNumber] = getInitialMarginFromLosses(pnLs, simulation).get(pathNumber);
			}
			double filtrationTime = FloatingpointDate.getFloatingPointDateFromDate(zeroRateModel.getReferenceDate(), evaluationDate);
			initialMargin = new RandomVariableFromDoubleArray(filtrationTime, initialMarginRealizations);	
		} else {
			List<RandomVariable> pnLs = getLosses(pnLSeries, null);
    		initialMargin = getInitialMarginFromLosses(pnLs, simulation);
		}
		return initialMargin;
	}
	
	
	public static void getMVAWithFullRevaluation(LocalPortfolio localPortfolio, ZeroRateModel zeroRateModel, LIBORModelMonteCarloSimulationModel liborModel, LocalDateTime startDate, int daysPerStep, double fundingSpread, Simulation simulation, boolean pathWiseEvaluation, boolean movingScenarioWindow) throws CalculationException {
		System.out.println("Calculating Margin Valuation Adjustment Full Revaluation:");
		String analysis = "Average";
		if (pathWiseEvaluation) {
			analysis = "Pathwise";
		}
		String window = "Fixed";
		if (movingScenarioWindow) {
			window = "Moving";
		}
		System.out.println("Simulation: " + simulation.toString() + " | Evaluation: " + analysis + " | Scenarios: " + window);

		TenorGrid tenorGrid = TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID);
	
		RandomVariable mva = new RandomVariableFromDoubleArray(0.0);
		LocalDateTime lastPaymentDate = FloatingpointDate.getDateFromFloatingPointDate(startDate, localPortfolio.getLastPaymentDate());
		double time = 0.0;
		double nextTime = 1/365.0; 
		long startTime = System.nanoTime();
		for (LocalDateTime date = startDate; date.isBefore(lastPaymentDate); date = date.plusDays(daysPerStep)) {
			if (SchemeStorage.getLocalCalendar(Currency.EUR).isBusinessday(date.toLocalDate())) {
				RandomVariable initialMargin = getInitialMarginWithFullRevaluation(date, zeroRateModel, localPortfolio, simulation, pathWiseEvaluation, movingScenarioWindow, tenorGrid);
//				System.out.println("Initial Margin as of " + date +": " + initialMargin.getAverage());
				// d(1/N^fd(t)) = 1/N^fd(t_i+1) - 1/N^fd(t_i)
				RandomVariable fundingNumeraire = liborModel.getNumeraire(nextTime).mult(Math.exp((nextTime) * fundingSpread)).invert();
				fundingNumeraire = fundingNumeraire.sub(liborModel.getNumeraire(time).mult(Math.exp(time * fundingSpread)).invert());
				mva = mva.add(fundingNumeraire.mult(initialMargin));
				time += daysPerStep/365.0;
			}
			nextTime += daysPerStep/365.0;
		}
		long endTime = System.nanoTime();
		System.out.println("Elapsed time: " + ((endTime - startTime)/NANOSECONDS_TO_SECONDS) + "seconds");
		System.out.println("MVA: " + (-mva.getAverage()));
		System.out.println();
	}
	
	
    /** Compatibility entry point: full revaluation does not require sensitivities. */
    public static RandomVariable getInitialMarginWithFullRevaluation(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel,
            LocalPortfolio localPortfolio, Sensitivities sensitivities, Simulation simulation,
            boolean pathWiseEvaluation, boolean movingScenarioWindow) throws CalculationException {
        return getInitialMarginWithFullRevaluation(evaluationDate, zeroRateModel, localPortfolio, simulation,
                pathWiseEvaluation, movingScenarioWindow, TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID));
    }

    public static RandomVariable getInitialMarginWithFullRevaluation(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel,
            LocalPortfolio localPortfolio, Simulation simulation, boolean pathWiseEvaluation,
            boolean movingScenarioWindow, TenorGrid tenorGrid) throws CalculationException {
        return getInitialMarginWithFullRevaluation(evaluationDate, zeroRateModel, (CurveValuation) localPortfolio,
                simulation, pathWiseEvaluation, movingScenarioWindow, tenorGrid);
    }

    public static RandomVariable getInitialMarginWithFullRevaluation(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel,
            CurveValuation valuation, Simulation simulation, boolean pathWiseEvaluation,
            boolean movingScenarioWindow) throws CalculationException {
        return getInitialMarginWithFullRevaluation(evaluationDate, zeroRateModel, valuation, simulation,
                pathWiseEvaluation, movingScenarioWindow, TenorGridFactory.getTenorGrid(GridType.INITIAL_MARGIN_RISK_GRID));
    }

    /** Local curve revaluation shared with PAIRS, with product-specific details inside the valuation. */
    public static RandomVariable getInitialMarginWithFullRevaluation(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel,
            CurveValuation valuation, Simulation simulation, boolean pathWiseEvaluation,
            boolean movingScenarioWindow, TenorGrid tenorGrid) throws CalculationException {
        LocalDateTime scenarioDate = movingScenarioWindow ? evaluationDate : zeroRateModel.getReferenceDate();
        CurveScenarios curveScenarios = PairsInitialMargin.getCurveScenarios(
                new ScenarioFactory(zeroRateModel, simulation), scenarioDate, tenorGrid);
		return PairsInitialMargin.getInitialMarginWithFullRevaluation(evaluationDate, zeroRateModel, valuation,
                curveScenarios, simulation, pathWiseEvaluation);
	}
	

	// Piecewise linear
	public static void getMVAWithInterpolation(LocalPortfolio localPortfolio, ZeroRateModel zeroRateModel, LIBORModelMonteCarloSimulationModel liborModel, LocalDateTime startDate, int pointsInBetween, int daysPerStep, double fundingSpread, InitialMargin type, boolean pathWiseEvaluation, boolean movingScenarioWindow) throws CalculationException {		
		System.out.println("Calculating Margin Valuation Adjustment by interpolation of " + (pointsInBetween) + " equidistant points:");
		String analysis = "Average";
		if (pathWiseEvaluation) {
			analysis = "Pathwise";
		}
		String window = "Fixed";
		if (movingScenarioWindow) {
			window = "Moving";
		}
		System.out.println("IM type: " + type.toString() + " | Evaluation: " + analysis + " | Scenarios: " + window);

		LocalDateTime lastPaymentDate = FloatingpointDate.getDateFromFloatingPointDate(startDate, localPortfolio.getLastPaymentDate());
		long startTime = System.nanoTime();
		double timeUntilLastPaymentDate = FloatingpointDate.getFloatingPointDateFromDate(startDate, lastPaymentDate);
		double timeBetweenIMEvaluations =  timeUntilLastPaymentDate / (pointsInBetween + 1);
		NavigableMap<LocalDateTime, RandomVariable> initialMargins = new TreeMap<>();
		for (int i = 0; i < pointsInBetween + 2; i++) {
			LocalDateTime date = startDate.plusDays(i * (int) (timeBetweenIMEvaluations * 365));
			while (!SchemeStorage.getLocalCalendar(Currency.EUR).isBusinessday(date.toLocalDate())) {
				date = date.minusDays(1);
			}
			
			switch (type) {
			case BASE:
				initialMargins.put(date, localPortfolio.getBaseInitialMargin(date, zeroRateModel, pathWiseEvaluation, movingScenarioWindow));
				break;
			case FLOOR:
				initialMargins.put(date, localPortfolio.getFloorInitialMargin(date, zeroRateModel, pathWiseEvaluation, movingScenarioWindow));
				break;
			case PAIRS:
				initialMargins.put(date, localPortfolio.getPairsInitialMargin(date, zeroRateModel, pathWiseEvaluation, movingScenarioWindow));
				break;
			default:
				throw new IllegalArgumentException("Initial Margin type not defined.");
			}
		}		
		RandomVariable mva = new RandomVariableFromDoubleArray(0.0);
		double time = 0.0;
		double nextTime = 1/365.0; 		
		for (LocalDateTime date = startDate; date.isBefore(lastPaymentDate); date = date.plusDays(daysPerStep)) {
			if (SchemeStorage.getLocalCalendar(Currency.EUR).isBusinessday(date.toLocalDate())) {
				LocalDateTime lowerKey = initialMargins.lowerKey(date);
				if (lowerKey == null) {
					lowerKey = initialMargins.firstKey();
				}
				LocalDateTime upperKey = initialMargins.higherKey(date);
				if (upperKey == null) {
					upperKey = initialMargins.lastKey();
				}
				RandomVariable initialMargin;
				// Case if evaluation date is after last date in initilMargins map
				if (date.isAfter(initialMargins.lastKey())) {
					initialMargin = new Scalar(0.0);
				} else {
					initialMargin = getInitialMarginWithInterpolation(date, lowerKey, upperKey, initialMargins.get(lowerKey), initialMargins.get(upperKey));
				}
//				System.out.println("Initial Margin as of " + date +": " + initialMargin.getAverage());
				// d(1/N^fd(t)) = 1/N^fd(t_i+1) - 1/N^fd(t_i)
				RandomVariable fundingNumeraire = liborModel.getNumeraire(nextTime).mult(Math.exp((nextTime) * fundingSpread)).invert();
				fundingNumeraire = fundingNumeraire.sub(liborModel.getNumeraire(time).mult(Math.exp(time * fundingSpread)).invert());
				mva = mva.add(fundingNumeraire.mult(initialMargin));
				time += daysPerStep/365.0;
			}
			nextTime += daysPerStep/365.0;
		}
		long endTime = System.nanoTime();
		System.out.println("Elapsed time: " + ((endTime - startTime)/NANOSECONDS_TO_SECONDS) + "seconds");
		System.out.println("MVA: " + (-mva.getAverage()));
		System.out.println();
	}
	
	
	// Assuming leftBoundary <= evaluationDate <= rightBoundary
	public static RandomVariable getInitialMarginWithInterpolation(LocalDateTime evaluationDate, LocalDateTime leftBoundary, LocalDateTime rightBoundary, RandomVariable leftValue, RandomVariable rightValue) {
		double distanceToLeftBoundary = FloatingpointDate.getFloatingPointDateFromDate(leftBoundary, evaluationDate);
		double totalDistance = FloatingpointDate.getFloatingPointDateFromDate(leftBoundary, rightBoundary);
		double weight = distanceToLeftBoundary / totalDistance;
		return leftValue.mult(1 - weight).add(rightValue.mult(weight));
	}
	
	
	private static List<RandomVariable> getLosses(RandomVariableSeries pnLSeries, Integer pathOrState) {
		if (pathOrState == null) {
			return new ArrayList<>(pnLSeries.getSeries().values());
		}
		List<RandomVariable> losses = new ArrayList<>(pnLSeries.getSeries().size());
		for (RandomVariable value : pnLSeries.getSeries().values()) {
			losses.add(new Scalar(value.get(pathOrState)));
		}
		return losses;
	}
		
	
}
