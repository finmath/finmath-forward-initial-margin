/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate.initialmargin;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.interestrate.TermStructureModelMonteCarloSimulation;
import net.finmath.montecarlo.process.AbstractProcess;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implements convenient methods for a LIBOR market model,
 * based on a given <code>LIBORMarketModel</code> model
 * and <code>AbstractLogNormalProcess</code> process.
 * 
 * @author Christian Fries
 * @version 0.7
 */
public class TermStructureModelMonteCarloSimulationSIMM extends TermStructureModelMonteCarloSimulation implements LIBORModelMonteCarloSimulationSIMMInterface {

	private final TermStructureModelSIMMInterface model;

	/**
	 * Create a LIBOR Monte-Carlo Simulation from a given LIBORMarketModel and an AbstractProcess.
	 * 
	 * @param model The LIBORMarketModel.
	 * @param process The process.
	 */
	public TermStructureModelMonteCarloSimulationSIMM(TermStructureModelSIMMInterface model, AbstractProcess process) {
		super(model,process);
		this.model		= model;

		this.model.setProcess(process);
		process.setModel(model);
	}

	/**
	 * Create a LIBOR Monte-Carlo Simulation from a given LIBORMarketModel.
	 * 
	 * @param model The LIBORMarketModel.
	 */
	public TermStructureModelMonteCarloSimulationSIMM(TermStructureModelSIMMInterface model) {
		super(model);
		this.model		= model;
	}

	
	
	@Override
	public AbstractRandomVariableFactory getRandomVariableFactory(){
		return model.getRandomVariableFactory();
	}

	
	@Override 
	public Map<Double, RandomVariableInterface> getNumeraireAdjustmentMap(){
		return model.getNumeraireAdjustmentMap();
	}
	
	@Override
	public RandomVariableInterface getNumeraireOISAdjustmentFactor(double time) throws CalculationException{
		return model.getNumeraireOISAdjustmentFactor(time);
	}
	
	@Override
	public RandomVariableInterface getForwardBondLibor(double T, double t) throws CalculationException{
		return model.getForwardBondLibor(T, t);
	}

	@Override
	public RandomVariableInterface getForwardBondOIS(double T, double t) throws CalculationException{
		return model.getForwardBondOIS(T, t);
	}
}
