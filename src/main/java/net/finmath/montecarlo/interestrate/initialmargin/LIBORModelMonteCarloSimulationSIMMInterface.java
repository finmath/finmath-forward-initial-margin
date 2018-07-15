/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 01.03.2008
 */
package net.finmath.montecarlo.interestrate.initialmargin;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
//import net.finmath.montecarlo.interestrate.TermStructureModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Basic interface which has to be implemented by Monte Carlo models for LIBOR processes.
 * 
 * @author Christian Fries
 * @version 1.0
 */

public interface LIBORModelMonteCarloSimulationSIMMInterface extends LIBORModelMonteCarloSimulationInterface {

	
	/**
	 * Returns the map of <code> Double <code> (time) and <code> RandomVariableInterface <code> (numeraire Adjustment)
	 * @return The numeraire adjustment map
	 */
	public Map<Double, RandomVariableInterface> getNumeraireAdjustmentMap();
	
	/**
	 * Returns the numeraire adjustment
	 * @param time The time
	 * @return
	 * @throws CalculationException 
	 */
	RandomVariableInterface getNumeraireOISAdjustmentFactor(double time) throws CalculationException;
	
	/** 
	 * Returns the forward bond P(T;t) on the forward curve. Calculated directly from Libors without using conditional expectation
	 * @param T final time
	 * @param t initial time 
	 * @return P(T;t)
	 * @throws CalculationException 
	 */
	RandomVariableInterface getForwardBondLibor(double T, double t) throws CalculationException;

	/**
	 * Returns the forward bond P(T;t) from on the OIS curve for a given Indices market model
	 * @param T The maturity of the forward bond 
	 * @param t The inception of the forward bond
	 * @return The forward bond P(T;t) on the OIS curve
	 * @throws CalculationException
	 */
	RandomVariableInterface getForwardBondOIS(double T, double t) throws CalculationException;

	/**
	 * 
	 * @return The random variable factory of this model
	 */
	public AbstractRandomVariableFactory getRandomVariableFactory();
	

}