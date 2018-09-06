package net.finmath.initialmargin.isdasimm.changedfinmath;

import net.finmath.exception.CalculationException;
import net.finmath.stochastic.RandomVariableInterface;

import java.util.Map;

public interface LIBORMarketModelInterface extends net.finmath.montecarlo.interestrate.LIBORMarketModelInterface {

	// Added by Mario Viehmann

	Map<Double, RandomVariableInterface> getNumeraireAdjustmentMap();

	Map<Double, RandomVariableInterface> getNumeraireCache();

	RandomVariableInterface getNumeraireOISAdjustmentFactor(double time) throws CalculationException;

	RandomVariableInterface getForwardBondLibor(double T, double t) throws CalculationException;

	RandomVariableInterface getForwardBondOIS(double T, double t) throws CalculationException;
}