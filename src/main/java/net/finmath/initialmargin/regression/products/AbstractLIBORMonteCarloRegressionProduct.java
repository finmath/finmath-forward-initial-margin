/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.initialmargin.regression.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.MonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;

/**
 * Base calls for product that need an AbstractLIBORMarketModel as base class
 *
 * @author Christian Fries
 */
public abstract class AbstractLIBORMonteCarloRegressionProduct extends AbstractLIBORMonteCarloProduct {

	/**
	 * @param currency The currency of this product (may be null for "any currency").
	 */
	public AbstractLIBORMonteCarloRegressionProduct(String currency) {
		super(currency);
	}

	/**
	 *
	 */
	public AbstractLIBORMonteCarloRegressionProduct() {
		super(null);
	}

	public abstract RandomVariable getCF(double initialTime, double finalTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException;

	public RandomVariable getCF(double initialTime, double finalTime, MonteCarloSimulationModel model) throws CalculationException {
		return getCF(initialTime, finalTime, (LIBORModelMonteCarloSimulationModel) model);
	}
}
