/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 04.02.2016
 */

package net.finmath.montecarlo.interestrate.initialmargin;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.interestrate.LIBORModelInterface;
import net.finmath.stochastic.RandomVariableInterface;

import java.util.Map;

/**
 * @author Christian Fries
 */
public interface LIBORModelSIMMInterface extends LIBORModelInterface {

	Map<Double, RandomVariableInterface> getNumeraireAdjustmentMap();

	RandomVariableInterface getNumeraireOISAdjustmentFactor(double time) throws CalculationException;

	RandomVariableInterface getForwardBondLibor(double T, double t) throws CalculationException;

	RandomVariableInterface getForwardBondOIS(double T, double t) throws CalculationException;

	AbstractRandomVariableFactory getRandomVariableFactory();
}