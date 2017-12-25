/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
//import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.MonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.process.component.factordrift.FactorDriftInterface;
import net.finmath.stochastic.RandomVariableInterface;

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
	
	public abstract RandomVariableInterface getCF(double initialTime, double finalTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException;
	
	
	public RandomVariableInterface getCF(double initialTime, double finalTime, MonteCarloSimulationInterface model) throws CalculationException{
		return getCF(initialTime, finalTime, (LIBORModelMonteCarloSimulationInterface)model);

	};


	

}
