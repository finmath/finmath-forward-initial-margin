package net.finmath.initialmargin.isdasimm.products;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.initialmargin.isdasimm.changedfinmath.LIBORModelMonteCarloSimulationInterface;
import net.finmath.optimizer.SolverException;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * The interface to be implemented by all products whose initial margin (MVA) we are interested in, i.e. all classes
 * extending <code> AbstractSIMMProduct </code>.
 *
 * @author Mario Viehmann
 */
public interface SIMMProductInterface {

	/**
	 * Calculate the forward initial margin with AAD
	 *
	 * @param evaluationTime The forward initial margin time
	 * @param model The LIBOR market model
	 * @return The forward initial margin
	 * @throws CalculationException
	 */
	public RandomVariableInterface getInitialMargin(double evaluationTime, LIBORModelMonteCarloSimulationInterface model, String calculationCCY) throws CalculationException;

	/**
	 * Returns the sensitivity of the SIMMProduct w.r.t the specified parameters if a sensitivity is available.
	 * This function will be called by the <code> SIMMSchemeIRDelta </code>.
	 *
	 * @param productClass The SIMM product class of this product (RatesFx etc.)
	 * @param riskClass The SIMM risk class of this product (InterestRate etc.)
	 * @param curveIndexNames The name of the relevant curves for this product (OIS, Libor6m etc.)
	 * @param currency The currency of this product
	 * @param bucketKey The SIMM bucket key of this product (null for risk class InterestRate)
	 * @param hasOptionality True if this product is not linear
	 * @param evaluationTime The time of evaluation
	 * @return The SIMM sensitivity of the product
	 * @throws CalculationException
	 * @throws CloneNotSupportedException
	 * @throws SolverException
	 */
	public RandomVariableInterface getSensitivity(String productClass,
			String riskClass,
			String maturityBucket, // only for IR and Credit risk class, null otherwise
			String curveIndexName, // null if riskClass not IR
			String bucketKey,      // currency for IR otherwise bucket nr.
			String riskType, double evaluationTime) throws SolverException, CloneNotSupportedException, CalculationException;

	/** Calculate the delta sensitivities of the product w.r.t. the forward curve, i.e. dV/dL.
	 *
	 * @param evaluationTime The time as of which we calculate the forward sensitivities
	 * @param model The LIBOR market model
	 * @return The sensitivities of the product w.r.t. the LIBORs, dV/dL
	 * @throws CalculationException
	 */
	public RandomVariableInterface[] getLiborModelSensitivities(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException;

	/** Calculates the delta sensitivities w.r.t. the OIS curve for equidistant time points of length equal
	 *  to the LIBOR period length.
	 *
	 * @param riskClass The risk class to be considered
	 * @param evaluationTime The time of evaluation
	 * @param model The LIBOR market model
	 * @return The delta sensitivities w.r.t. the OIS curve
	 * @throws CalculationException
	 */
	public RandomVariableInterface[] getOISModelSensitivities(String riskClass, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException;

	/**
	 * Calculates the delta sensitivities w.r.t. the numeraire at all times the numeraire is used in the
	 * valuation of the product. This method is only used if SensitivityMode.ExactConsideringDependecies is enabled.
	 *
	 * @param riskClass The risk class to be considered
	 * @param evaluationTime The time of evaluation
	 * @param model The LIBOR market model
	 * @return The map of times at which the numeraire is requested in the product valuation and the
	 *         corresponding delta sensitivities w.r.t. the numeraire.
	 * @throws CalculationException
	 */
	public RandomVariableInterface[] getValueNumeraireSensitivities(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException;

	/** Returns an indicator of the paths on which we have exercised at the given time. 1 if exercised, 0 if not exercised.
	 *  This is necessary for the sensitivity calculation of Bermudans and Swaptions after the (first) exercise time.
	 * @param time The time of evaluation
	 * @return The exercise indicator
	 * @throws CalculationException
	 */
	public RandomVariableInterface getExerciseIndicator(double time, LIBORModelMonteCarloSimulationInterface model) throws CalculationException;

	/** Set the conditional expectation operator for this product at a given time. We need the
	 *  conditional expectation operator for the calculation of forward sensitivities.
	 *
	 * @param time The time of evaluation
	 * @throws CalculationException
	 */
	public void setConditionalExpectationOperator(double time, LIBORModelMonteCarloSimulationInterface model) throws CalculationException;

	/** Returns the AAD gradient of the product.
	 *
	 * @param model The Model
	 * @return The gradient of the product
	 * @throws CalculationException
	 */
	Map<Long, RandomVariableInterface> getGradient(LIBORModelMonteCarloSimulationInterface model) throws CalculationException;

	/** Returns the final time of the product (maturity). This is important for sensitivity interpolation
	 *  where we need to known the latest time to be considered as right interpolation input.
	 *
	 * @return The terminal time of the product
	 */
	public double getFinalMaturity();

	/** Returns the time at which the sensitivities are reset to the true sensitivities in case of Melting.
	 *  E.g. we may reset the sensitivities for a swaption with physical delivery at maturity.
	 * @param model 
	 *
	 * @return The time of sensitivity reset for melting.
	 * @throws CalculationException 
	 */
	public double getMeltingResetTime(LIBORModelMonteCarloSimulationInterface model) throws CalculationException;

	/** Get the exact delta sensitivities from cache <code> exactDeltaCache </code>: Return for a given time, index curve, risk class and model (class variable) the sensitivities of the product.
	 *  calculated by AAD or analytically. The sensitivities may be later obtained from the map instead of being re-calculated over and over.
	 *  This chache is useful for the sensitivity interpolation and melting.
	 *
	 * @param time The time at which the sensitivities should be set
	 * @param riskClass The risk class of the product
	 * @param curveIndexName The name of the index curve
	 * @param isMarketRateSensi true if the exact delta should be returned already mapped onto the SIMM Buckets, false
	 *                          false if the exact delta should be the model sensitivity (useful for Melting on LIBOR buckets).
	 * @throws SolverException
	 * @throws CloneNotSupportedException
	 * @throws CalculationException
	 */
	public RandomVariableInterface[] getExactDeltaFromCache(double time, String riskClass, String curveIndexName, boolean isMarketRateSensi) throws SolverException, CloneNotSupportedException, CalculationException;
}