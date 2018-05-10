/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 04.02.2016
 */

package net.finmath.initialmargin.isdasimm.changedfinmath;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Christian Fries
 *
 */
public interface LIBORModelInterface extends net.finmath.montecarlo.interestrate.LIBORModelInterface {

	// Added by Mario Viehmann
	
	Map<Double, RandomVariableInterface> getNumeraireAdjustmentMap();
	
	Map<Double, RandomVariableInterface> getNumeraireCache();
	
	RandomVariableInterface getNumeraireOISAdjustmentFactor(double time) throws CalculationException;
	
	RandomVariableInterface getForwardBondLibor(double T, double t) throws CalculationException;

	RandomVariableInterface getForwardBondOIS(double T, double t) throws CalculationException;
}