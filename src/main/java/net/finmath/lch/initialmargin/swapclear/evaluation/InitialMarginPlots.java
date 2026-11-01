package net.finmath.lch.initialmargin.swapclear.evaluation;

import java.time.LocalDateTime;

import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.Currency;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.CurveName;
import net.finmath.lch.initialmargin.swapclear.evaluation.MarginValuationAdjustment.InitialMargin;
import net.finmath.lch.initialmargin.swapclear.libormodels.ModelFactory;
import net.finmath.lch.initialmargin.swapclear.products.LchSwap;
import net.finmath.lch.initialmargin.swapclear.products.LocalPortfolio;
import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.plots.DoubleToRandomVariableFunction;
import net.finmath.plots.PlotProcess2D;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.TimeDiscretizationFromArray;

public class InitialMarginPlots {
	
	/*
	 * Last date of available zero rate data
	 * LMM's of ModelFactory are calibrated to the same date
	 */
	private static final LocalDateTime REFERENCE_DATE = ModelFactory.REFERENCE_DATE; 

	
	public static void main(String[] args) throws CalculationException {
	
		LchSwap swap1 = PortfolioFactory.create20YReceiverSwap();
		LchSwap swap2 = PortfolioFactory.create10YPayerSwap();
		LchSwap swap3 = PortfolioFactory.create15YReceiverSwap();
		LocalPortfolio swapPortfolio = PortfolioFactory.getSwapPortfolio(swap1).getLocalPortfolio(Currency.EUR); //, swap10Y, swap10YPlusSpread);		
	    double stepSize = 0.125;
		LocalDateTime lastPlotDate = getObservationDate(swapPortfolio.getLastPaymentDate() + 5 * stepSize);
		final LIBORModelMonteCarloSimulationModel liborModel = ModelFactory.getModel(CurveName.EUR_EURIBOR_3M, lastPlotDate.toLocalDate(), 60.0);
		final ZeroRateModel zeroRateModel = new ZeroRateModel(Currency.EUR, CurveName.EUR_EURIBOR_3M, liborModel, "spreadsheets/EUR_EURIBOR_3M_Input_Data.xlsx");
		boolean movingWindow = false;
		boolean pathWiseScenarios = false;
		plotIMPaths(InitialMargin.FLOOR, pathWiseScenarios, movingWindow, zeroRateModel, swapPortfolio, stepSize);

	}
	
		
	protected static void plotIMPaths(InitialMargin type, boolean pathWiseEvaluation, boolean movingScenarioWindow, ZeroRateModel zeroRateModel, LocalPortfolio localPortfolio, double stepSize) throws CalculationException {
		    
		int numberOfPoints = (int) (localPortfolio.getLastPaymentDate() / stepSize + 5);
		
		DoubleToRandomVariableFunction paths = time -> getInitialMargin(time, zeroRateModel, localPortfolio, type, pathWiseEvaluation, movingScenarioWindow);
		PlotProcess2D plot = new PlotProcess2D(new TimeDiscretizationFromArray(0.0, numberOfPoints, stepSize), paths, 100);
		
		String analysis = "average";
		if (pathWiseEvaluation) {
			analysis = "pathwise";
		}
		String imType = "Base IM";
		if (type.equals(InitialMargin.FLOOR)) {
			imType = "Floor IM";
		}
		if (type.equals(InitialMargin.PAIRS)) {
			imType = "Pairs IM";
		}
		
		String window = "fixed";
		if (movingScenarioWindow) {
			window = "moving";
		}
		plot.setTitle(imType + " with " + analysis + " evaluation and " + window + " scenarios");
		plot.show();
	}
	
	
	protected static RandomVariable getInitialMargin(double time, ZeroRateModel zeroRateModel, LocalPortfolio localPortfolio, InitialMargin type, boolean pathWiseEvaluation, boolean movingScenarioWindow) throws CalculationException {
		LocalDateTime date = getObservationDate(time);
		
		switch (type) {
		case FLOOR:
			return localPortfolio.getFloorInitialMargin(date, zeroRateModel, pathWiseEvaluation, movingScenarioWindow);
		case BASE:
			return localPortfolio.getBaseInitialMargin(date, zeroRateModel, pathWiseEvaluation, movingScenarioWindow);
		case PAIRS:
			return localPortfolio.getPairsInitialMargin(date, zeroRateModel, pathWiseEvaluation, movingScenarioWindow);
		default:
			throw new IllegalArgumentException("Initial Margin type not defined.");
		}
	}

	/** Latest daily observation on or before the requested plot time. */
	static LocalDateTime getObservationDate(double time) {
		LocalDateTime date = FloatingpointDate.getDateFromFloatingPointDate(REFERENCE_DATE, time).toLocalDate().atStartOfDay();
		while (!SchemeStorage.getLocalCalendar(Currency.EUR).isBusinessday(date.toLocalDate())) {
			date = date.minusDays(1);
		}
		return date;
	}
	
	

}
