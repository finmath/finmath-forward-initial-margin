package net.finmath.initialmargin.regression;

import java.util.ArrayList;

import net.finmath.exception.CalculationException;
import net.finmath.functions.NormalDistribution;
import net.finmath.initialmargin.regression.products.Portfolio;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.TimeDiscretization;

/**
 * This class implements the Dynamic Initial Margin by Regression as described in
 * https://papers.ssrn.com/sol3/papers.cfm?abstract_id=2911167
 *
 * @author Mario Viehmann
 */
public class InitialMarginForwardRegression {
	private final double confidenceLevel = 0.99;
	private final double MPR = 10.0 / 250.0;       // The Marginal Period of Risk: 10 Days

	private enum Method {SIMPLE, LSQREGRESSION} // The Method to calculate Initial Margin

	private Method method;
	private int polynomialOrder;                 // The order of the regression polynomial

	private LIBORModelMonteCarloSimulationModel model;
	private Portfolio portfolio;

	public InitialMarginForwardRegression(Portfolio portfolio,
			LIBORModelMonteCarloSimulationModel model,
			int polynomialOrder,
			String method) {
		this.model = model;
		this.portfolio = portfolio;
		this.polynomialOrder = polynomialOrder;
		this.method = Method.valueOf(method.toUpperCase());
	}

	/**
	 * Calculate initial margin at for a given time.
	 *
	 * @param evaluationTime The time at which the initial margin is calculaed.
	 * @return The initial margin of the portfolio.
	 * @throws CalculationException
	 */
	public double getInitialMargin(double evaluationTime) throws CalculationException {

		RandomVariable variance;
		double initialMargin = 0;

		switch (method) {

		case LSQREGRESSION: // Least Square Regression

			variance = getVarianceForecast(evaluationTime, model);

			double normalQuantile = NormalDistribution.inverseCumulativeDistribution(confidenceLevel);

			RandomVariable initialMarginPathwise = variance.sqrt().mult(normalQuantile);

			initialMargin = initialMarginPathwise.getAverage();

			break;

		case SIMPLE: // Simple Dynamic Initial Margin

			initialMargin = -getCleanPortfolioValueChange(evaluationTime).getQuantile(confidenceLevel);

			break;

		default:
			break;
		}

		return initialMargin;
	}

	public double[] getInitialMargin(TimeDiscretizationFromArray initialMarginTimes) throws CalculationException {
		double[] initialMargin = new double[initialMarginTimes.getNumberOfTimes()];
		for (int timeIndex = 0; timeIndex < initialMarginTimes.getNumberOfTimes(); timeIndex++) {
			initialMargin[timeIndex] = getInitialMargin(initialMarginTimes.getTime(timeIndex));
		}
		return initialMargin;
	}

	/**
	 * Calculates the forecast of the variance of the clean portfolio value change over the marginal period of risk for a given time point.
	 *
	 * @param forwardVaRTime The time for which the variance of the clean portfolio value change over the marginal period of risk is calculated
	 * @param model          Interface implementing the Libor Market Model
	 * @return The variance of the clean portfolio value change over the MPR
	 * @throws CalculationException
	 */
	public RandomVariable getVarianceForecast(double forwardVaRTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {
		RandomVariable cleanValueChange = getCleanPortfolioValueChange(forwardVaRTime);

		ConditionalExpectationEstimator condExpEstimator = getConditionalExpectationEstimator(forwardVaRTime, model);

		RandomVariable variance = cleanValueChange.squared().getConditionalExpectation(condExpEstimator).floor(0.0);

		return variance;
	}

	/**
	 * Calculates the clean portfolio value change, i.e. V(t+MPR)-V(t)+CF({t,t+MPR})
	 *
	 * @param time the time t at which the marginal period of risk (MPR) starts
	 * @return The clean portfolio value change over the MPR
	 * @throws CalculationException
	 */
	public RandomVariable getCleanPortfolioValueChange(double time) throws CalculationException {

		double lastFixingTime = model.getLiborPeriodDiscretization().getTime(model.getLiborPeriodDiscretization().getTimeIndex(portfolio.getInitialLifeTime()) - 1);

		RandomVariable cashFlows = portfolio.getCF(time, time + MPR, model);

		RandomVariable initialValue = portfolio.getValue(time, model);
		initialValue = initialValue.sub(cashFlows);

		if (time > 0 && time < lastFixingTime) {

			ConditionalExpectationEstimator condExpOperatorInitial = getConditionalExpectationEstimatorLibor(time, model);

			initialValue = initialValue.getConditionalExpectation(condExpOperatorInitial);
		}

		RandomVariable finalValue = portfolio.getValue(time + MPR, model);

		if (time + MPR < lastFixingTime) {

			ConditionalExpectationEstimator condExpOperatorFinal = getConditionalExpectationEstimatorLibor(time + MPR, model);

			finalValue = finalValue.getConditionalExpectation(condExpOperatorFinal);
		}

		return finalValue.sub(initialValue);
	}

	/**
	 * Calculates the forecast of the variance of the clean portfolio value change over the marginal period of risk for all time points on a time discretization.
	 *
	 * @param forwardVaRTimes The times for which the variance of the clean portfolio value change over the marginal period of risk is calculated
	 * @param model           Interface implementing the Libor Market Model
	 * @return The variance of the clean portfolio value change over the MPR
	 * @throws CalculationException
	 */
	public RandomVariable[] getVarianceForecast(TimeDiscretization forwardVaRTimes, LIBORModelMonteCarloSimulationModel model) throws CalculationException {

		RandomVariable[] VaRForecast = new RandomVariable[forwardVaRTimes.getNumberOfTimes()];
		for (int timeIndex = 0; timeIndex < forwardVaRTimes.getNumberOfTimes(); timeIndex++) {
			VaRForecast[timeIndex] = getVarianceForecast(forwardVaRTimes.getTime(timeIndex), model);
		}

		return VaRForecast;
	}

	/**
	 * Return the conditional expectation estimator suitable for this product.
	 *
	 * @param forwardVaRTime The condition time.
	 * @param model          The model
	 * @return The conditional expectation estimator suitable for this product
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	private ConditionalExpectationEstimator getConditionalExpectationEstimator(double forwardVaRTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {
		MonteCarloConditionalExpectationRegression condExpEstimator = new MonteCarloConditionalExpectationRegression(
				getRegressionBasisFunctions(forwardVaRTime, model)
				);
		return condExpEstimator;
	}

	/**
	 * Provides basis funtions for the calculation of the forward variance which is the
	 * conditional expectation of the squared portfolio value change over the marginal period of risk
	 * conditional on the NPV
	 *
	 * @param forwardVaRTime the time when the value at risk regression is performed
	 * @param model          The model
	 * @return The basis functions based on the NPV of the portfolio under consideration in this class
	 * @throws CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	private RandomVariable[] getRegressionBasisFunctions(double forwardVaRTime,
			LIBORModelMonteCarloSimulationModel model
			) throws CalculationException {
		// If Libor for last CF is not yet fixed
		RandomVariable NPV = portfolio.getValue(forwardVaRTime, model);
		double lastFixingTime = model.getLiborPeriodDiscretization().getTime(model.getLiborPeriodDiscretization().getTimeIndex(portfolio.getInitialLifeTime()) - 1);
		if (forwardVaRTime < lastFixingTime) {
			// to get NPV at time t
			ConditionalExpectationEstimator condExpEstimatorLibor = getConditionalExpectationEstimatorLibor(forwardVaRTime, model);
			// State Variables: NPV of portfolio
			NPV = NPV.getConditionalExpectation(condExpEstimatorLibor);
		}

		//RandomVariable NPV = portfolio.getValue(forwardVaRTime, model);
		ArrayList<RandomVariable> basisFunctions = new ArrayList<RandomVariable>();

		// Basis Functions
		for (int orderIndex = 0; orderIndex <= polynomialOrder; orderIndex++) {
			basisFunctions.add(NPV.pow(orderIndex));
		}

		RandomVariable[] finalBasisFunctions = basisFunctions.toArray(new RandomVariable[basisFunctions.size()]);
		return finalBasisFunctions;
	}

	/**
	 * Provides a conditional expectation estimator for the calculation of the future portfolio value V(t)
	 *
	 * @param forwardVaRTime the time at which the value at risk regression is performed
	 * @param model
	 * @return
	 * @throws CalculationException
	 */
	private ConditionalExpectationEstimator getConditionalExpectationEstimatorLibor(double forwardVaRTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {
		MonteCarloConditionalExpectationRegression condExpEstimator = new MonteCarloConditionalExpectationRegression(
				getRegressionBasisFunctionsLibor(forwardVaRTime, model)
				);
		return condExpEstimator;
	}

	/**
	 * Provides basis functions based on Libor rates for the calculation of the future portfolio value V(t)
	 *
	 * @param forwardVaRTime the time at which the value at risk regression is performed
	 * @param model
	 * @return
	 * @throws CalculationException
	 */
	private RandomVariable[] getRegressionBasisFunctionsLibor(double forwardVaRTime,
			LIBORModelMonteCarloSimulationModel model
			) throws CalculationException {

		ArrayList<RandomVariable> basisFunctions = new ArrayList<RandomVariable>();
		ArrayList<RandomVariable> libors = new ArrayList<RandomVariable>();
		// State Variables: Libors
		int timeIndex = model.getTimeDiscretization().getTimeIndexNearestLessOrEqual(forwardVaRTime);
		int firstLiborIndex = model.getLiborPeriodDiscretization().getTimeIndexNearestGreaterOrEqual(forwardVaRTime);
		int lastLiborIndex = model.getLiborPeriodDiscretization().getTimeIndexNearestGreaterOrEqual(portfolio.getInitialLifeTime());
		for (int liborIndex = firstLiborIndex; liborIndex < lastLiborIndex; liborIndex++) {
			libors.add(model.getLIBOR(timeIndex, liborIndex));
		}
		RandomVariable[] finalLibors = libors.toArray(new RandomVariable[libors.size()]);
		// Basis Functions
		for (int liborIndex = 0; liborIndex < finalLibors.length; liborIndex++) {
			for (int orderIndex = 0; orderIndex <= 2; orderIndex++) {
				basisFunctions.add(finalLibors[liborIndex].pow(orderIndex));
			}
		}
		RandomVariable numeraire = model.getNumeraire(forwardVaRTime);

		basisFunctions.add(numeraire);
		basisFunctions.add(numeraire.pow(2));
		RandomVariable[] finalBasisFunctions = basisFunctions.toArray(new RandomVariable[basisFunctions.size()]);
		return finalBasisFunctions;
	}
}
