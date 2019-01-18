/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 15.02.2004
 */
package net.finmath.initialmargin.regression.products;

import java.util.Arrays;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.TimeDiscretization;

/**
 * Implements the valuation of a swaption under a LIBORModelMonteCarloSimulationModel
 * <p>
 * Important: If the LIBOR Market Model is a multi-curve model in the sense that the
 * numeraire is not calculated from the forward curve, then this valuation does
 * assume that the basis deterministic. For the valuation of a fully generalize swaption,
 * you have to use the <code>Option</code> component on a <code>Swap</code>.
 *
 * @author Christian Fries
 * @version 1.3
 */
public class Swaption extends AbstractLIBORMonteCarloRegressionProduct {
	private double exerciseDate;    // Exercise date
	private double[] fixingDates;        // Vector of fixing dates (must be sorted)
	private double[] paymentDates;    // Vector of payment dates (same length as fixing dates)
	private double[] periodLengths;    // Vector of payment dates (same length as fixing dates)
	private double[] swaprates;        // Vector of strikes

	private RandomVariable barrierIndicator = null;
	private AbstractLIBORMonteCarloRegressionProduct swap;
	private final double notional;

	/**
	 * Create a swaption.
	 *
	 * @param exerciseDate  Vector of exercise dates.
	 * @param fixingDates   Vector of fixing dates.
	 * @param paymentDates  Vector of payment dates (must have same length as fixing dates).
	 * @param periodLengths Vector of period lengths.
	 * @param swaprates     Vector of strikes (must have same length as fixing dates).
	 */
	public Swaption(double exerciseDate, double[] fixingDates, double[] paymentDates, double[] periodLengths, double[] swaprates, double notional) {
		super();
		this.exerciseDate = exerciseDate;
		this.fixingDates = fixingDates;
		this.paymentDates = paymentDates;
		this.periodLengths = periodLengths;
		this.swaprates = swaprates;
		this.notional = notional;
	}

	/**
	 * Create a swaption.
	 *
	 * @param exerciseDate Vector of exercise dates.
	 * @param fixingDates  Vector of fixing dates.
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates).
	 * @param swaprates    Vector of strikes (must have same length as fixing dates).
	 */
	public Swaption(
			double exerciseDate,
			double[] fixingDates,
			double[] paymentDates,
			double[] swaprates,
			double notional) {
		super();
		this.exerciseDate = exerciseDate;
		this.fixingDates = fixingDates;
		this.paymentDates = paymentDates;
		this.periodLengths = null;
		this.swaprates = swaprates;
		this.notional = notional;
	}

	/**
	 * Creates a swaption using a TimeDiscretizationFromArray
	 *
	 * @param exerciseDate Exercise date.
	 * @param swapTenor    Object specifying period start and end dates.
	 * @param swaprate     Strike.
	 */
	public Swaption(
			double exerciseDate,
			TimeDiscretization swapTenor,
			double swaprate,
			double notional) {
		super();
		this.exerciseDate = exerciseDate;
		this.notional = notional;

		this.fixingDates = new double[swapTenor.getNumberOfTimeSteps()];
		this.paymentDates = new double[swapTenor.getNumberOfTimeSteps()];
		for (int periodIndex = 0; periodIndex < fixingDates.length; periodIndex++) {
			fixingDates[periodIndex] = swapTenor.getTime(periodIndex);
			paymentDates[periodIndex] = swapTenor.getTime(periodIndex + 1);
		}

		this.periodLengths = null;

		this.swaprates = new double[swapTenor.getNumberOfTimeSteps()];
		java.util.Arrays.fill(swaprates, swaprate);
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
	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {
		RandomVariable values;
		// CHANGED
		if (evaluationTime > exerciseDate) {
			if (barrierIndicator == null) {
				barrierIndicator = getValue(exerciseDate, model).mult(-1.0);
			}
			if (swap == null) {
				swap = new SimpleSwap(fixingDates, paymentDates, swaprates, true, notional);
			}
			RandomVariable swapValue = swap.getValue(evaluationTime, model);
			values = barrierIndicator.choose(new RandomVariableFromDoubleArray(0.0), swapValue);
		} else {

			/*
			 * Calculate value of the swap at exercise date on each path (beware of perfect foresight - all rates are simulationTime=exerciseDate)
			 */
			RandomVariable valueOfSwapAtExerciseDate = model.getRandomVariableForConstant(/*fixingDates[fixingDates.length-1],*/0.0);

			// Calculate the value of the swap by working backward through all periods
			for (int period = fixingDates.length - 1; period >= 0; period--) {
				double fixingDate = fixingDates[period];
				double paymentDate = paymentDates[period];
				double swaprate = swaprates[period];

				if (paymentDate <= evaluationTime) {
					break;
				}

				double periodLength = periodLengths != null ? periodLengths[period] : paymentDate - fixingDate;

				// Get random variables - note that this is the rate at simulation time = exerciseDate
				RandomVariable libor = model.getLIBOR(exerciseDate, fixingDate, paymentDate);

				// Calculate payoff
				RandomVariable payoff = libor.sub(swaprate).mult(periodLength).mult(notional);

				// Calculated the adjustment for the discounting curve, assuming a deterministic basis
				// @TODO: Need to check if the model fulfills the assumptions (all models implementing the interface currently do so).
				double discountingDate = Math.max(fixingDate, exerciseDate);
				double discountingAdjustment = 1.0;
				if (model.getModel().getDiscountCurve() != null) {
					AnalyticModelInterface analyticModel = model.getModel().getAnalyticModel();
					DiscountCurveInterface discountCurve = model.getModel().getDiscountCurve();
					ForwardCurveInterface forwardCurve = model.getModel().getForwardRateCurve();
					DiscountCurveInterface discountCurveFromForwardCurve = new DiscountCurveFromForwardCurve(forwardCurve);

					double forwardBondOnForwardCurve = discountCurveFromForwardCurve.getDiscountFactor(analyticModel, discountingDate) / discountCurveFromForwardCurve.getDiscountFactor(analyticModel, paymentDate);
					double forwardBondOnDiscountCurve = discountCurve.getDiscountFactor(analyticModel, discountingDate) / discountCurve.getDiscountFactor(analyticModel, paymentDate);
					discountingAdjustment = forwardBondOnForwardCurve / forwardBondOnDiscountCurve;
				}

				// Add payment received at end of period
				valueOfSwapAtExerciseDate = valueOfSwapAtExerciseDate.add(payoff);

				// Discount back to beginning of period
				valueOfSwapAtExerciseDate = valueOfSwapAtExerciseDate.discount(libor, paymentDate - discountingDate).mult(discountingAdjustment);
			}
			/*
			 * Calculate swaption value
			 */
			values = valueOfSwapAtExerciseDate.floor(0.0);
			RandomVariable numeraire = model.getNumeraire(exerciseDate);
			RandomVariable monteCarloProbabilities = model.getMonteCarloWeights(exerciseDate);
			values = values.div(numeraire).mult(monteCarloProbabilities);

			RandomVariable numeraireAtZero = model.getNumeraire(evaluationTime);
			RandomVariable monteCarloProbabilitiesAtZero = model.getMonteCarloWeights(evaluationTime);
			values = values.mult(numeraireAtZero).div(monteCarloProbabilitiesAtZero);
		}
		return values;
	}

	/**
	 * This method returns the value of the product using a Black-Scholes model for the swap rate
	 * The model is determined by a discount factor curve and a swap rate volatility.
	 *
	 * @param forwardCurve       The forward curve on which to value the swap.
	 * @param swaprateVolatility The Black volatility.
	 * @return Value of this product
	 */
	public double getValue(ForwardCurveInterface forwardCurve, double swaprateVolatility) {
		double swaprate = swaprates[0];
		for (double swaprate1 : swaprates) {
			if (swaprate1 != swaprate) {
				throw new RuntimeException("Uneven swaprates not allows for analytical pricing.");
			}
		}

		double[] swapTenor = new double[fixingDates.length + 1];
		System.arraycopy(fixingDates, 0, swapTenor, 0, fixingDates.length);
		swapTenor[swapTenor.length - 1] = paymentDates[paymentDates.length - 1];

		double forwardSwapRate = Swap.getForwardSwapRate(new TimeDiscretizationFromArray(swapTenor), new TimeDiscretizationFromArray(swapTenor), forwardCurve);
		double swapAnnuity = SwapAnnuity.getSwapAnnuity(new TimeDiscretizationFromArray(swapTenor), forwardCurve);

		return AnalyticFormulas.blackModelSwaptionValue(forwardSwapRate, swaprateVolatility, exerciseDate, swaprate, swapAnnuity);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return super.toString()
				+ "\n" + "exerciseDate: " + exerciseDate
				+ "\n" + "fixingDates: " + Arrays.toString(fixingDates)
				+ "\n" + "paymentDates: " + Arrays.toString(paymentDates)
				+ "\n" + "periodLengths: " + Arrays.toString(periodLengths)
				+ "\n" + "swaprates: " + Arrays.toString(swaprates);
	}

	@Override
	public RandomVariable getCF(double initialTime, double finalTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {
		if (finalTime <= exerciseDate) {
			return new RandomVariableFromDoubleArray(0.0);
		}
		if (swap == null) {
			swap = new SimpleSwap(fixingDates, paymentDates, swaprates, true, notional);
		}
		if (barrierIndicator == null) {
			barrierIndicator = getValue(exerciseDate, model).mult(-1.0);
		}
		RandomVariable swapCF = swap.getCF(initialTime, finalTime, model);
		RandomVariable swapCFOnPathWhereExercised = barrierIndicator.choose(new RandomVariableFromDoubleArray(0.0), swapCF);
		return swapCFOnPathWhereExercised;
	}
}
