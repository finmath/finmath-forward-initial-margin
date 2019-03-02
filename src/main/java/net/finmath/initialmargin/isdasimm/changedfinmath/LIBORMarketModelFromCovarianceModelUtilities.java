/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.initialmargin.isdasimm.changedfinmath;

import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * Implements convenient methods for a LIBOR market model.
 *
 * @author Christian Fries
 * @version 0.7
 */
public class LIBORMarketModelFromCovarianceModelUtilities {

	public static RandomVariable getNumeraireOISAdjustmentFactor(LIBORModelMonteCarloSimulationModel model, double time) throws CalculationException {
		/*
		if (((LIBORMarketModelFromCovarianceModel) getModel()).getNumeraireAdjustments().containsKey(time)) {
			return ((LIBORMarketModelFromCovarianceModel) getModel()).getNumeraireAdjustments().get(time);
		}
		 */

		if(time == 0) return new Scalar(1.0);
		return getForwardBondLibor(model, time, 0).mult(time).add(1.0).mult(model.getModel().getDiscountCurve().getDiscountFactor(time));

		/*
		// Get unadjusted Numeraire
		RandomVariable numeraireUnadjusted = ((LIBORMarketModelFromCovarianceModel) getModel()).getNumerairetUnAdjusted(time);
		RandomVariable adjustment = getRandomVariableForConstant(numeraireUnadjusted.invert().getAverage()).div(getModel().getDiscountCurve().getDiscountFactor(time));

		return adjustment;
		 */
	}

	public static RandomVariable getForwardBondLibor(LIBORModelMonteCarloSimulationModel model, double T, double t) throws CalculationException {
		if(t > T) return new Scalar(0);

		return model.getLIBOR(t, t, T).mult(T - t).add(1.0).invert();
		//		return (new LIBORBond(T)).getValue(t, this);
		//		return ((LIBORMarketModel) getModel()).getForwardBondLibor(T, t);
	}

	public static RandomVariable getForwardBondOIS(LIBORModelMonteCarloSimulationModel model, double T, double t) throws CalculationException {
		return new Scalar(model.getModel().getDiscountCurve().getDiscountFactor(T) / model.getModel().getDiscountCurve().getDiscountFactor(t));
		/*
		// Get bondOIS = P^OIS(T;t) = P^L(T;t)*a_t/a_T
		RandomVariable adjustment_t = getNumeraireOISAdjustmentFactor(t);
		RandomVariable adjustment_T = getNumeraireOISAdjustmentFactor(T);

		return getForwardBondLibor(T, t).mult(adjustment_t).div(adjustment_T);
		 */
	}
}
