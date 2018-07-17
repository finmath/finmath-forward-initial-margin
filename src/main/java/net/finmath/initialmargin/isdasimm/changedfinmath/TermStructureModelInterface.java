package net.finmath.initialmargin.isdasimm.changedfinmath;
/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 04.02.2016
 */

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Christian Fries
 *
 */
public interface TermStructureModelInterface extends net.finmath.montecarlo.interestrate.TermStructureModelInterface {

	// Added by Mario Viehmann

	Map<Double, RandomVariableInterface> getNumeraireAdjustmentMap();

	RandomVariableInterface getNumeraireOISAdjustmentFactor(double time) throws CalculationException;

	RandomVariableInterface getForwardBondLibor(double T, double t) throws CalculationException;

	RandomVariableInterface getForwardBondOIS(double T, double t) throws CalculationException;
}
