/*
 * Created on 06.12.2009
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.initialmargin.regression.products.indices;

import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariable;

/**
 * A fixed coupon index paying constant coupon..
 *
 * @author Christian Fries
 */
public class FixedCoupon extends AbstractIndex {

	private static final long serialVersionUID = 5375406324063846793L;

	private final RandomVariable coupon;

	/**
	 * Creates a fixed coupon index paying constant coupon.
	 *
	 * @param coupon The coupon.
	 */
	public FixedCoupon(double coupon) {
		super();
		this.coupon = new RandomVariableFromDoubleArray(coupon);
	}

	@Override
	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		return coupon;
	}

	/**
	 * Returns the coupon.
	 *
	 * @return the coupon
	 */
	public RandomVariable getCoupon() {
		return coupon;
	}

	@Override
	public Set<String> queryUnderlyings() {
		return null;
	}

	@Override
	public String toString() {
		return "FixedCoupon [coupon=" + coupon + ", toString()="
				+ super.toString() + "]";
	}

	// INSERTED
	@Override
	public RandomVariable getValue(double evaluationTime, double fixingDate,
			LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		return coupon;
	}

	@Override
	public RandomVariable getCF(double initialTime, double finalTime,
			LIBORModelMonteCarloSimulationInterface model) {
		// TODO Auto-generated method stub
		return null;
	}
}
