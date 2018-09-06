package net.finmath.montecarlo.interestrate.initialmargin;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.interestrate.LIBORMarketModelInterface;
import net.finmath.stochastic.RandomVariableInterface;

import java.util.Map;

public interface LIBORMarketModelSIMMInterface extends LIBORMarketModelInterface {//, LIBORModelSIMMInterface{

	Map<Double, RandomVariableInterface> getNumeraireAdjustmentMap();

	AbstractRandomVariableFactory getRandomVariableFactory();

	RandomVariableInterface getNumeraireOISAdjustmentFactor(double time) throws CalculationException;

	RandomVariableInterface getForwardBondLibor(double T, double t) throws CalculationException;

	RandomVariableInterface getForwardBondOIS(double T, double t) throws CalculationException;
}