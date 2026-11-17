package net.finmath.lch.initialmargin.swapclear.products;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.PairsInitialMargin;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.Currency;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.CurveName;
import net.finmath.lch.initialmargin.swapclear.products.components.LchSwapLeg;
import net.finmath.exception.CalculationException;
import net.finmath.stochastic.RandomVariable;


/**
 * The LocalPortfolio stores swaps with different underlyings (curves) but the same currency. Collection of IndexPortfolio.
 */
public class LocalPortfolio {
	
	private Map<CurveName, IndexPortfolio>			localPortfolio;
	private final Currency 							currency;
	
	
	public LocalPortfolio(Currency currency) {
		this.localPortfolio = new HashMap<>();
		this.currency = currency;
	}
	
	
	public void addSwap(LchSwap swap) {
    	Currency swapCurrency = swap.getLchCurrency(); // both legs have same currency
    	if (swapCurrency.equals(currency)) {
        	CurveName swapCurve = swap.getCurveName();
        	localPortfolio.computeIfAbsent(swapCurve, k -> new IndexPortfolio(swapCurrency, swapCurve)).addSwap(swap);;
    	}
	}
	
	/**
	 * Base IM is calculated on a 10y window of historical scenarios.
	 * A currency-specific standardized zero curve, e.g. EUR_EURIBOR, is used for swap valuing and risk sampling/mapping.
	 * Initial Margin is calculated as an Expected-Shortfall on the 6 worst losses
	 * 
	 * @param evaluationDate Time for which IM will be calculated
	 * @param zeroRateModel Model under which IM is valued (risk, swap values, scenarios)
	 * @param pathWiseEvaluation If true scenarios and IM will be calculated on each path separately, otherwise as the average of all paths
	 * @param movingScenarioWindow If true the scenarios will move with the model evaluation date -> LMM of ZeroRateModel will generate additional scenarios
	 * @return The Base Initial Margin of the LCH methodology
	 * @throws CalculationException
	 */
	public RandomVariable getBaseInitialMargin(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel, boolean pathWise, boolean movingScenarioWindow) throws CalculationException {
		return PairsInitialMargin.getBaseInitialMargin(evaluationDate, zeroRateModel, this, pathWise, movingScenarioWindow);
	}
	
	
	/**
	 * Floor IM is calculated on a 10y window of historical scenarios plus fixed stress period January 2008 - June 2010.
	 * A currency-specific standardized zero curve, e.g. EUR_EURIBOR, is used for swap valuing and risk sampling/mapping.
	 * Initial Margin is calculated as a Value-at-Risk at 13th worst loss.
	 * 
	 * @param evaluationDate Time for which IM will be calculated
	 * @param zeroRateModel Model under which IM is valued (risk, swap values, scenarios)
	 * @param pathWiseEvaluation If true scenarios and IM will be calculated on each path separately, otherwise as the average of all paths
	 * @param movingScenarioWindow If true the scenarios will move with the model evaluation date -> LMM of ZeroRateModel will generate additional scenarios
	 * @return The Base Initial Margin of the LCH methodology
	 * @throws CalculationException
	 */
	public RandomVariable getFloorInitialMargin(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel, boolean pathWise, boolean movingScenarioWindow) throws CalculationException {
		return PairsInitialMargin.getFloorInitialMargin(evaluationDate, zeroRateModel, this, pathWise, movingScenarioWindow);
	}
	

	/**
	 * Pairs IM is the maximum of the Base IM and Floor IM.
	 * 
	 * @param evaluationDate Time for which IM will be calculated
	 * @param zeroRateModel Model under which IM is valued (risk, swap values, scenarios)
	 * @param pathWiseEvaluation If true scenarios and IM will be calculated on each path separately, otherwise as the average of all paths
	 * @param movingScenarioWindow If true the scenarios will move with the model evaluation date -> LMM of ZeroRateModel will generate additional scenarios
	 * @return The Base Initial Margin of the LCH methodology
	 * @throws CalculationException
	 */
	public RandomVariable getPairsInitialMargin(LocalDateTime evaluationDate, ZeroRateModel zeroRateModel, boolean pathWise, boolean movingScenarioWindow) throws CalculationException {
		return PairsInitialMargin.getPairsInitialMargin(evaluationDate, zeroRateModel, this, pathWise, movingScenarioWindow);
	}
	
	
	// Base Initial Margin does value all swaps of the same currency under the same standardized curve
	public List<LchSwapLeg> getSwaps() {
		List<LchSwapLeg> swapLegs = new ArrayList<>();
		for (IndexPortfolio indexPortfolio : localPortfolio.values()) {
			swapLegs.addAll(indexPortfolio.getSwaps());
		}
		return swapLegs;
	}
	
	
	/**
	 * @return The latest payment date of the portfolio
	 */
	public double getLastPaymentDate() {
		double lastPaymentDate = 0.0;
		for (IndexPortfolio indexPortfolio: localPortfolio.values()) {
			lastPaymentDate = Math.max(lastPaymentDate, indexPortfolio.getLastPaymentDate());
		}
		return lastPaymentDate;
	}
	
	
	public Currency getCurrency() {
		return currency;
	}
		
	
}
