/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.initialmargin.regression.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.MonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Base class for products requiring an MonteCarloSimulationInterface for valuation.
 * 
 * @author Christian Fries
 */
public abstract class AbstractMonteCarloRegressionProduct extends AbstractMonteCarloProduct {

	public AbstractMonteCarloRegressionProduct(String currency) {
		super(currency);
	}

	public AbstractMonteCarloRegressionProduct() {
		this(null);
	}
	
	public abstract RandomVariableInterface getCF(double initialTime, double finalTime, MonteCarloSimulationInterface model) throws CalculationException;

}
