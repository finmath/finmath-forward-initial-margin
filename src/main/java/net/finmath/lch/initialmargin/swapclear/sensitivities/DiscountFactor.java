package net.finmath.lch.initialmargin.swapclear.sensitivities;


import net.finmath.stochastic.RandomVariable;


/**
 * Handles discount factors of interest rate swaps.
 */
public class DiscountFactor {
	
	public enum Discounting	 					{LIBOR, OIS};
	public enum DiscountDate					{PERIOD_START, PERIOD_END, PAYMENT}; // PeriodEnd = Fixing + PeriodLength
	
	private final double	 					timeToMaturity;
	private final RandomVariable 				discountFactor;
	
	
	public DiscountFactor(RandomVariable discountfactor, double timeToMaturity) {
		this.discountFactor = discountfactor;
		this.timeToMaturity = timeToMaturity;
	}
	
    
	public RandomVariable getDiscountFactor() {
		return discountFactor;
	}

    
	public double getTimeToMaturity() {
        return timeToMaturity;
    }
	
	
	/**
	 * @param order The order of the derivative
	 * @return The derivative of the discount factor
	 */
	public RandomVariable getDerivative(int order) {
		// Derivative discount factor: (-1)^n * (T-t)^n * P(T;t)
        double factor = Math.pow(-1.0, order) * Math.pow(timeToMaturity, order);  
		return discountFactor.mult(factor);
	}
	
	
	/**
	 * @param order The order of the derivative
	 * @return The derivative of the inverse discount factor, i.e. 1/P(T;t)
	 */
	public RandomVariable getInverseDerivative(int order) {
		// Since 1/P = exp((T-t)*z), all derivative orders have a positive factor.
        double factor = Math.pow(timeToMaturity, order);
        return discountFactor.invert().mult(factor);
	}
	
}
