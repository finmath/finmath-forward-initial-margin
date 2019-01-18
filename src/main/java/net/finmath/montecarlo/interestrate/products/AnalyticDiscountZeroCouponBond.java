package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

public class AnalyticDiscountZeroCouponBond extends AnalyticZeroCouponBond {
	/**
	 * Creates an {@link AnalyticDiscountZeroCouponBond} using the discount curve's adjustments on the forward rate of the Libor Market Model.
	 *
	 * @param maturity The maturity of the zero coupon bond as a floating point time.
	 */
	public AnalyticDiscountZeroCouponBond(double maturity) {
		super(maturity);
	}

	@Override
	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationModel simulation) throws CalculationException {
		RandomVariable liborPriceAtEvaluation = super.getValue(evaluationTime, simulation);
		double liborBondPriceAtZero = super.getValue(0.0, simulation).getAverage();
		double discountBondPriceAtZero = simulation.getModel().getDiscountCurve().getDiscountFactor(simulation.getModel().getAnalyticModel(), getMaturity());

		double adjustment = liborBondPriceAtZero / discountBondPriceAtZero;

		return liborPriceAtEvaluation.div(adjustment);
	}
}
