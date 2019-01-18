/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 28.02.2015
 */

package net.finmath.initialmargin.regression.products;

import net.finmath.exception.CalculationException;
import net.finmath.initialmargin.regression.products.components.AbstractNotional;
import net.finmath.initialmargin.regression.products.indices.AbstractIndex;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.ScheduleInterface;

/**
 * Create a swap from schedules, notional, indices and spreads (fixed coupons).
 * <p>
 * The getValue method of this class simple returns
 * <code>
 * legReceiver.getValue(evaluationTime, model).sub(legPayer.getValue(evaluationTime, model))
 * </code>
 * where <code>legReceiver</code> and <code>legPayer</code> are {@link net.finmath.initialmargin.regression.products.SwapLeg}s.
 *
 * @author Christian Fries
 */
public class Swap extends AbstractLIBORMonteCarloRegressionProduct {

	private final AbstractLIBORMonteCarloRegressionProduct legReceiver;
	private final AbstractLIBORMonteCarloRegressionProduct legPayer;

	/**
	 * Create a swap which values as <code>legReceiver - legPayer</code>.
	 *
	 * @param legReceiver The receiver leg.
	 * @param legPayer    The payer leg.
	 */
	public Swap(AbstractLIBORMonteCarloRegressionProduct legReceiver, AbstractLIBORMonteCarloRegressionProduct legPayer) {
		super();
		this.legReceiver = legReceiver;
		this.legPayer = legPayer;
	}

	/**
	 * Create a swap from schedules, notional, indices and spreads (fixed coupons).
	 *
	 * @param notional           The notional.
	 * @param scheduleReceiveLeg The period schedule for the receiver leg.
	 * @param indexReceiveLeg    The index of the receiver leg, may be null if no index is received.
	 * @param spreadReceiveLeg   The constant spread or fixed coupon rate of the receiver leg.
	 * @param schedulePayLeg     The period schedule for the payer leg.
	 * @param indexPayLeg        The index of the payer leg, may be null if no index is paid.
	 * @param spreadPayLeg       The constant spread or fixed coupon rate of the payer leg.
	 */
	public Swap(AbstractNotional notional,
			ScheduleInterface scheduleReceiveLeg,
			AbstractIndex indexReceiveLeg, double spreadReceiveLeg,
			ScheduleInterface schedulePayLeg, AbstractIndex indexPayLeg,
			double spreadPayLeg) {
		super();

		legReceiver = new SwapLeg(scheduleReceiveLeg, notional, indexReceiveLeg, spreadReceiveLeg, false);
		legPayer = new SwapLeg(schedulePayLeg, notional, indexPayLeg, spreadPayLeg, false);
	}

	@Override
	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		RandomVariable value = legReceiver.getValue(evaluationTime, model);
		if (legPayer != null) {
			value = value.sub(legPayer.getValue(evaluationTime, model));
		}

		return value;
	}

	@Override
	public RandomVariable getCF(double initialTime, double finalTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		RandomVariable cashFlow = legReceiver.getCF(initialTime, finalTime, model);
		if (legPayer != null) {
			cashFlow = cashFlow.sub(legPayer.getCF(initialTime, finalTime, model));
		}
		return cashFlow;
	}
}
