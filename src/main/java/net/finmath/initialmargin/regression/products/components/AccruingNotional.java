/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 21.01.2016
 */

package net.finmath.initialmargin.regression.products.components;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * @author Christian Fries
 */
public class AccruingNotional implements AbstractNotional {

	AbstractNotional previousPeriodNotional;
	AbstractPeriod previousPeriod;

	/**
	 * Creates a notion where the notional of the period start is calculated as
	 * the notional of the previous period's period end and the notional at period end
	 * is calculated as being accrued via getCoupon on the current period.
	 *
	 * @param previousPeriodNotional The notional of the previous period.
	 * @param previousPeriod         The previous period.
	 */
	public AccruingNotional(AbstractNotional previousPeriodNotional, AbstractPeriod previousPeriod) {
		this.previousPeriodNotional = previousPeriodNotional;
		this.previousPeriod = previousPeriod;
	}

	@Override
	public String getCurrency() {
		return previousPeriodNotional.getCurrency();
	}

	@Override
	public RandomVariable getNotionalAtPeriodStart(AbstractPeriod period, LIBORModelMonteCarloSimulationModel model) throws CalculationException {
		return previousPeriodNotional.getNotionalAtPeriodEnd(previousPeriod, model).mult(previousPeriod.getCoupon(model).add(1.0));
	}

	@Override
	public RandomVariable getNotionalAtPeriodEnd(AbstractPeriod period, LIBORModelMonteCarloSimulationModel model) throws CalculationException {
		return getNotionalAtPeriodStart(period, model);
	}
}
