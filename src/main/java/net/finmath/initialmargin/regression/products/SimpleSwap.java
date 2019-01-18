/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 15.02.2004
 */
package net.finmath.initialmargin.regression.products;

import java.util.Arrays;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariable;

/**
 * Implements the valuation of a swap under a LIBORModelMonteCarloSimulationInterface
 *
 * @author Christian Fries
 * @version 1.2
 */
public class SimpleSwap extends AbstractLIBORMonteCarloRegressionProduct {
	private final double[] fixingDates;    // Vector of fixing dates
	private final double[] paymentDates;    // Vector of payment dates (same length as fixing dates)
	private final double[] swaprates;        // Vector of strikes

	private final boolean isPayFix;
	private final double notional;

	/**
	 * Create a swap.
	 *
	 * @param fixingDates  Vector of fixing dates
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param swaprates    Vector of strikes (must have same length as fixing dates)
	 * @param isPayFix     If true, the swap is receive float - pay fix, otherwise its receive fix - pay float.
	 */
	public SimpleSwap(
			double[] fixingDates,
			double[] paymentDates,
			double[] swaprates,
			boolean isPayFix,
			double notional) {
		super();
		this.fixingDates = fixingDates;
		this.paymentDates = paymentDates;
		this.swaprates = swaprates;
		this.isPayFix = isPayFix;
		this.notional = notional;
	}

	/**
	 * Create a swap.
	 *
	 * @param fixingDates  Vector of fixing dates
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param swaprates    Vector of strikes (must have same length as fixing dates)
	 */
	public SimpleSwap(
			double[] fixingDates,
			double[] paymentDates,
			double[] swaprates,
			double notional) {
		this(fixingDates, paymentDates, swaprates, true, notional);
	}

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model          The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		RandomVariable values = model.getRandomVariableForConstant(0.0);

		for (int period = 0; period < fixingDates.length; period++) {
			double fixingDate = fixingDates[period];
			double paymentDate = paymentDates[period];
			double swaprate = swaprates[period];
			double periodLength = paymentDate - fixingDate;

			if (paymentDate < evaluationTime) {
				continue;
			}

			// Get random variables
			RandomVariable libor = model.getLIBOR(fixingDate, fixingDate, paymentDate);
			RandomVariable payoff = libor.sub(swaprate).mult(periodLength).mult(notional);
			if (!isPayFix) {
				payoff = payoff.mult(-1.0);
			}

			RandomVariable numeraire = model.getNumeraire(paymentDate);
			RandomVariable monteCarloProbabilities = model.getMonteCarloWeights(paymentDate);
			payoff = payoff.div(numeraire).mult(monteCarloProbabilities);

			values = values.add(payoff);
		}

		RandomVariable numeraireAtEvalTime = model.getNumeraire(evaluationTime);
		RandomVariable monteCarloProbabilitiesAtEvalTime = model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtEvalTime).div(monteCarloProbabilitiesAtEvalTime);

		return values;
	}

	@Override
	public String toString() {
		return super.toString()
				+ "\n" + "fixingDates: " + Arrays.toString(fixingDates)
				+ "\n" + "paymentDates: " + Arrays.toString(paymentDates)
				+ "\n" + "swaprates: " + Arrays.toString(swaprates);
	}

	@Override
	public RandomVariable getCF(double initialTime, double finalTime,
			LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		// find paymentDate in interval [initialTime, finalTime]
		RandomVariable CF = new RandomVariableFromDoubleArray(0.0);
		for (int period = 0; period < paymentDates.length; period++) {
			if (paymentDates[period] >= initialTime && paymentDates[period] < finalTime) {
				double fixingDate = fixingDates[period];
				double paymentDate = paymentDates[period];
				double swaprate = swaprates[period];
				double periodLength = paymentDate - fixingDate;

				// Get random variables
				RandomVariable libor = model.getLIBOR(fixingDate, fixingDate, paymentDate);
				CF = libor.sub(swaprate).mult(periodLength).mult(notional);
				RandomVariable numeraire = model.getNumeraire(paymentDate);
				RandomVariable numeraireAtEval = model.getNumeraire(initialTime);
				CF = CF.div(numeraire).mult(numeraireAtEval);
				if (!isPayFix) {
					CF = CF.mult(-1.0);
				}
			}
		}
		return CF;
	}
}
