/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.initialmargin.isdasimm.changedfinmath;

import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelInterface;
import net.finmath.montecarlo.process.AbstractProcess;
import net.finmath.montecarlo.process.AbstractProcessInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implements convenient methods for a LIBOR market model,
 * based on a given <code>LIBORMarketModel</code> model
 * and <code>AbstractLogNormalProcess</code> process.
 *
 * @author Christian Fries
 * @version 0.7
 */
public class LIBORModelMonteCarloSimulation extends net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation implements LIBORModelMonteCarloSimulationInterface {


	public LIBORModelMonteCarloSimulation(LIBORModelInterface model, AbstractProcessInterface process) {
		// @TODO Cast can be removed with 3.2.15 of finmath-lib
		super(model, (AbstractProcess)process);
	}

	@Override
	public Map<Double, RandomVariableInterface> getNumeraireAdjustmentMap(){
		return ((LIBORMarketModelInterface) getModel()).getNumeraireAdjustmentMap();
	}

	@Override
	public Map<Double, RandomVariableInterface> getNumeraireCache() {
		return ((LIBORMarketModelInterface) getModel()).getNumeraireCache();
	}

	@Override
	public RandomVariableInterface getNumeraireOISAdjustmentFactor(double time) throws CalculationException{
		return ((LIBORMarketModelInterface) getModel()).getNumeraireOISAdjustmentFactor(time);
	}

	@Override
	public RandomVariableInterface getForwardBondLibor(double T, double t) throws CalculationException{
		return ((LIBORMarketModelInterface) getModel()).getForwardBondLibor(T, t);
	}

	@Override
	public RandomVariableInterface getForwardBondOIS(double T, double t) throws CalculationException{
		return ((LIBORMarketModelInterface) getModel()).getForwardBondOIS(T, t);
	}


	@Override
	public Object getCloneWithModifiedSeed(int seed) {
		AbstractProcess process = (AbstractProcess) ((AbstractProcess)getProcess()).getCloneWithModifiedSeed(seed);
		return new LIBORModelMonteCarloSimulation(getModel(), process);
	}

	@Override
	public LIBORModelMonteCarloSimulationInterface getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {
		LIBORModelInterface modelClone = (LIBORModelInterface) getModel().getCloneWithModifiedData(dataModified);
		if(dataModified.containsKey("discountCurve") && dataModified.size() == 1) {
			// In this case we may re-use the underlying process
			LIBORModelMonteCarloSimulation lmmSimClone = new LIBORModelMonteCarloSimulation(modelClone, getProcess());
			return lmmSimClone;
		} else {
			return new LIBORModelMonteCarloSimulation(modelClone, (AbstractProcess)getProcess().clone());
		}
	}

	/**
	 * Create a clone of this simulation modifying one of its properties (if any).
	 *
	 * @param entityKey The entity to modify.
	 * @param dataModified The data which should be changed in the new model
	 * @return Returns a clone of this model, where the specified part of the data is modified data (then it is no longer a clone :-)
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public LIBORModelMonteCarloSimulationInterface getCloneWithModifiedData(String entityKey, Object dataModified) throws CalculationException
	{
		Map<String, Object> dataModifiedMap = new HashMap<String, Object>();
		dataModifiedMap.put(entityKey, dataModified);
		return getCloneWithModifiedData(dataModifiedMap);
	}
}
