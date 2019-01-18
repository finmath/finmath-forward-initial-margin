/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.initialmargin.isdasimm.changedfinmath;

import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.LIBORModel;
import net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * Implements convenient methods for a LIBOR market model,
 * based on a given <code>LIBORMarketModelFromCovarianceModel</code> model
 * and <code>AbstractLogNormalProcess</code> process.
 *
 * @author Christian Fries
 * @version 0.7
 */
public class LIBORModelMonteCarloSimulation extends net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel implements LIBORModelMonteCarloSimulationInterface {

	public LIBORModelMonteCarloSimulation(LIBORModel model, MonteCarloProcess process) {
		super(model, process);
	}

	@Override
	public Map<Double, RandomVariable> getNumeraireAdjustmentMap() {
		return ((LIBORMarketModelFromCovarianceModel) getModel()).getNumeraireAdjustments();
	}

	@Override
	public RandomVariable getNumeraireOISAdjustmentFactor(double time) throws CalculationException {
		/*
		if (((LIBORMarketModelFromCovarianceModel) getModel()).getNumeraireAdjustments().containsKey(time)) {
			return ((LIBORMarketModelFromCovarianceModel) getModel()).getNumeraireAdjustments().get(time);
		}
		 */

		if(time == 0) return new Scalar(1.0);
		return getForwardBondLibor(time, 0).mult(time).add(1.0).mult(getModel().getDiscountCurve().getDiscountFactor(time));

		/*
		// Get unadjusted Numeraire
		RandomVariable numeraireUnadjusted = ((LIBORMarketModelFromCovarianceModel) getModel()).getNumerairetUnAdjusted(time);
		RandomVariable adjustment = getRandomVariableForConstant(numeraireUnadjusted.invert().getAverage()).div(getModel().getDiscountCurve().getDiscountFactor(time));

		return adjustment;
		 */
	}

	@Override
	public RandomVariable getForwardBondLibor(double T, double t) throws CalculationException {
		if(t > T) return new Scalar(0);

		return this.getLIBOR(t, t, T).mult(T - t).add(1.0).invert();
		//		return (new LIBORBond(T)).getValue(t, this);
		//		return ((LIBORMarketModel) getModel()).getForwardBondLibor(T, t);
	}

	@Override
	public RandomVariable getForwardBondOIS(double T, double t) throws CalculationException {
		return new Scalar(getModel().getDiscountCurve().getDiscountFactor(T) / getModel().getDiscountCurve().getDiscountFactor(t));
		/*
		// Get bondOIS = P^OIS(T;t) = P^L(T;t)*a_t/a_T
		RandomVariable adjustment_t = getNumeraireOISAdjustmentFactor(t);
		RandomVariable adjustment_T = getNumeraireOISAdjustmentFactor(T);

		return getForwardBondLibor(T, t).mult(adjustment_t).div(adjustment_T);
		 */
	}

	@Override
	public Object getCloneWithModifiedSeed(int seed) {
		MonteCarloProcessFromProcessModel process = (MonteCarloProcessFromProcessModel) ((MonteCarloProcessFromProcessModel) getProcess()).getCloneWithModifiedSeed(seed);
		return new LIBORModelMonteCarloSimulation(getModel(), process);
	}

	@Override
	public LIBORModelMonteCarloSimulationInterface getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {
		LIBORModel modelClone = getModel().getCloneWithModifiedData(dataModified);
		if (dataModified.containsKey("discountCurve") && dataModified.size() == 1) {
			// In this case we may re-use the underlying process
			LIBORModelMonteCarloSimulation lmmSimClone = new LIBORModelMonteCarloSimulation(modelClone, getProcess());
			return lmmSimClone;
		} else {
			return new LIBORModelMonteCarloSimulation(modelClone, getProcess().clone());
		}
	}

	/**
	 * Create a clone of this simulation modifying one of its properties (if any).
	 *
	 * @param entityKey    The entity to modify.
	 * @param dataModified The data which should be changed in the new model
	 * @return Returns a clone of this model, where the specified part of the data is modified data (then it is no longer a clone :-)
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public LIBORModelMonteCarloSimulationInterface getCloneWithModifiedData(String entityKey, Object dataModified) throws CalculationException {
		Map<String, Object> dataModifiedMap = new HashMap<String, Object>();
		dataModifiedMap.put(entityKey, dataModified);
		return getCloneWithModifiedData(dataModifiedMap);
	}
}
