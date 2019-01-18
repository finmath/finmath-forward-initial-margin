package net.finmath.initialmargin.isdasimm.products;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import net.finmath.exception.CalculationException;
import net.finmath.initialmargin.isdasimm.changedfinmath.LIBORModelMonteCarloSimulationInterface;
import net.finmath.initialmargin.isdasimm.sensitivity.AbstractSIMMSensitivityCalculation;
import net.finmath.initialmargin.isdasimm.sensitivity.AbstractSIMMSensitivityCalculation.SensitivityMode;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.montecarlo.interestrate.products.BermudanSwaption;
import net.finmath.montecarlo.interestrate.products.SimpleSwap;
import net.finmath.montecarlo.interestrate.products.TermStructureMonteCarloProduct;
import net.finmath.optimizer.SolverException;
import net.finmath.stochastic.RandomVariable;

/**
 * This class describes a Bermudan Swaption for SIMM initial margin (MVA) calculation.
 * Both exercise types "Callable" and "Cancelable" are supported.
 *
 * @author Mario Viehmann
 */
public class SIMMBermudanSwaption extends AbstractSIMMProduct {

	// SIMM classification
	static final String productClass = "RATES_FX";
	static final String[] riskClass = new String[]{"INTEREST_RATE"};

	// Swap after exercise (delivery product)
	private SimpleSwap swap = null;
	private SIMMSimpleSwap SIMMSwap = null; // Only used if we take the numeraire as the model sensitivity, such that we can easily calculate the sensitivities of the swap w.r.t. the numeraire.

	private BermudanSwaption bermudan;

	public enum ExerciseType {Callable, Cancelable}

	private ExerciseType exerciseType;
	private Map<String, RandomVariable[]> swapSensitivityMap = new HashMap<>(); // Cache for sensitivities of underlying swap

	/**
	 * Construct a bermudan swaption as a product for the SIMM. Initial margin and MVA can be calculated for this product.
	 *
	 * @param bermudan
	 * @param curveIndexNames
	 * @param currency
	 */
	public SIMMBermudanSwaption(BermudanSwaption bermudan, String[] curveIndexNames, String currency) {

		super(productClass, riskClass, curveIndexNames, currency, null /*bucketKey*/, true /*hasOptionality*/);

		this.bermudan = bermudan;
		this.exerciseType = bermudan.getIsCallable() ? ExerciseType.Callable : ExerciseType.Cancelable;
		if (exerciseType == ExerciseType.Callable) {
			this.swap = bermudan.getSwap();
			this.SIMMSwap = new SIMMSimpleSwap(swap, curveIndexNames, currency);
		}
	}

	/**
	 * Construct a bermudan swaption as a product for the SIMM. Initial margin and MVA can be calculated for this product.
	 *
	 * @param fixingDates
	 * @param periodLengths
	 * @param paymentDates
	 * @param periodNotionals
	 * @param swapRates
	 * @param isPeriodStartDateExerciseDate
	 * @param exerciseType
	 * @param curveIndexNames
	 * @param currency
	 */
	public SIMMBermudanSwaption(double[] fixingDates, double[] periodLengths, double[] paymentDates, double[] periodNotionals,
			double[] swapRates, boolean[] isPeriodStartDateExerciseDate, ExerciseType exerciseType, String[] curveIndexNames, String currency) {
		super(productClass, riskClass, curveIndexNames, currency, null /*bucketKey*/, false /*hasOptionality*/);
		boolean isCallable = exerciseType == ExerciseType.Callable;
		this.bermudan = new BermudanSwaption(isPeriodStartDateExerciseDate, fixingDates, periodLengths, paymentDates, periodNotionals, swapRates, isCallable);
		this.exerciseType = exerciseType;
		if (exerciseType == ExerciseType.Callable) {
			this.swap = bermudan.getSwap();
			this.SIMMSwap = new SIMMSimpleSwap(swap, curveIndexNames, currency);
		}
	}

	@Override
	public TermStructureMonteCarloProduct getLIBORMonteCarloProduct(double time) {
		return this.bermudan;
	}

	@Override
	public RandomVariable[] getLiborModelSensitivities(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

		// Get Bermudan sensitivities
		RandomVariable[] bermudanSensis = super.getLiborModelSensitivities(evaluationTime, model);

		// Set exercise indicator
		RandomVariable[] indicatorOrig = new RandomVariable[]{getExerciseIndicator(evaluationTime, model)};
		RandomVariable[] indicator = indicatorOrig.clone();
		indicator[0] = indicator[0].sub(0.5); // i.e. -0.5: not exercised, 0.5: exercised

		// @TODO: Perfomance improvement possible, since this triggerst an unneccesary valuation.
		RandomVariable exerciseTime = (RandomVariable) bermudan.getValues(evaluationTime, model).get("exerciseTime");

		// Set sensitivities on exercised paths
		if (evaluationTime >= exerciseTime.getMin()) {

			switch (exerciseType) {

			case Callable:

				// Calculate sensis analytically
				RandomVariable[] swapSensis = SIMMSimpleSwap.getAnalyticSensitivities(evaluationTime, swap.getFixingDates(), swap.getSwapRates(), model.getLiborPeriodDiscretization().getTimeStep(0), swap.getNotional(), model, "Libor");

				if (evaluationTime >= bermudan.getExerciseTimes()[bermudan.getExerciseTimes().length - 1]) {

					// Set sensis of not exercised paths to zero
					for (int i = 0; i < bermudanSensis.length; i++) {
						bermudanSensis[i] = indicator[0].choose(swapSensis[i], new RandomVariableFromDoubleArray(0.0));
					}
				} else {

					// Set sensitivities on paths: Bermudan sensis if not exercised, swap sensis if exercised.
					for (int i = 0; i < bermudanSensis.length; i++) {
						bermudanSensis[i] = indicator[0].choose(swapSensis[i], bermudanSensis[i]);
					}
				}

				break;

			case Cancelable:

				// Set the sensitivities on exercised paths to zero
				for (int i = 0; i < bermudanSensis.length; i++) {
					bermudanSensis[i] = indicator[0].choose(new RandomVariableFromDoubleArray(0.0), bermudanSensis[i]);
				}

				break;

			default:
				break;
			}
		}

		return bermudanSensis;
	}

	@Override
	public RandomVariable[] getOISModelSensitivities(String riskClass,
			double evaluationTime,
			LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

		// Get Bermudan sensitivities
		double[] futureDiscountTimes = null; // the vector of the times after evaluation time at which the numeraire has been used for this product
		RandomVariable[] dVdP = null;
		RandomVariable[] bermudanSensis = super.getOISModelSensitivities(evaluationTime, futureDiscountTimes, dVdP /* null => use AAD*/, riskClass, model);

		// Set exercise indicator
		RandomVariable[] indicatorOrig = new RandomVariable[]{getExerciseIndicator(evaluationTime, model)};
		RandomVariable[] indicator = indicatorOrig.clone();
		indicator[0] = indicator[0].sub(0.5); // i.e. -0.5: not exercised, 0.5: exercised

		// @TODO: Perfomance improvement possible, since this triggerst an unneccesary valuation.
		RandomVariable exerciseTime = (RandomVariable) bermudan.getValues(evaluationTime, model).get("exerciseTime");

		// Set sensitivities on exercised paths
		if (evaluationTime >= exerciseTime.getMin()) {

			switch (exerciseType) {

			case Callable:

				// Return zero if evaluationTime is later than the last time where an adjustment is available (i.e. the last time where a cash flow occurred)
				if (!Arrays.stream(swap.getPaymentDates()).filter(time -> time > evaluationTime).findAny().isPresent()) {
					return AbstractSIMMSensitivityCalculation.zeroBucketsIR;
				}

				// Get Swap Sensitivities analytically
				dVdP = SIMMSimpleSwap.getAnalyticSensitivities(evaluationTime, swap.getFixingDates(), swap.getSwapRates(), model.getLiborPeriodDiscretization().getTimeStep(0), swap.getNotional(), model, "OIS");
				futureDiscountTimes = Arrays.stream(swap.getPaymentDates()).filter(n -> n > evaluationTime).toArray();
				RandomVariable[] swapSensis = getOISModelSensitivities(evaluationTime, futureDiscountTimes, dVdP, riskClass, model);

				if (evaluationTime >= bermudan.getExerciseTimes()[bermudan.getExerciseTimes().length - 1]) {

					// Set sensis of not exercised paths to zero
					for (int i = 0; i < bermudanSensis.length; i++) {
						bermudanSensis[i] = indicator[0].choose(swapSensis[i], new RandomVariableFromDoubleArray(0.0));
					}
				} else {

					// Set sensitivities on paths: Bermudan sensis if not exercised, swap sensis if exercised.
					for (int i = 0; i < bermudanSensis.length; i++) {
						bermudanSensis[i] = indicator[0].choose(swapSensis[i], bermudanSensis[i]);
					}
				}

				break;

			case Cancelable:

				// Set the sensitivities on exercised paths to zero
				for (int i = 0; i < bermudanSensis.length; i++) {
					bermudanSensis[i] = indicator[0].choose(new RandomVariableFromDoubleArray(0.0), bermudanSensis[i]);
				}

				break;

			default:
				break;
			}
		}

		return bermudanSensis;
	}

	/**
	 * This function sets the sensitivities on the exercised paths to either swap sensitivities (for a Callable)
	 * or to zero (for a Cancelable). This is necessary as the melting function in class <code> SIMMSensitivityCalculation <code>
	 * does not know on which paths we have already exercised.
	 *
	 * @param evaluationTime       The time of evaluation
	 * @param model
	 * @param curveIndexName       The name of the curve
	 * @param meltedBermudanSensis The bermudan sensitivities obtained by melting at evaluation time.
	 * @return The true melted bermudan sensitivities considering the paths on which we have exercised.
	 * @throws CalculationException
	 * @throws SolverException
	 * @throws CloneNotSupportedException
	 */
	public RandomVariable[] changeMeltedSensitivitiesOnExercisedPaths(double evaluationTime, LIBORModelMonteCarloSimulationInterface model, String curveIndexName, RandomVariable[] meltedBermudanSensis) throws CalculationException, SolverException, CloneNotSupportedException {

		// @TODO: Perfomance improvement possible, since this triggerst an unneccesary valuation.
		RandomVariable exerciseTime = (RandomVariable) bermudan.getValues(evaluationTime, model).get("exerciseTime");

		if (evaluationTime >= exerciseTime.getMin()) {

			RandomVariable indicator = getExerciseIndicator(evaluationTime, model).sub(0.5);

			switch (exerciseType) {

			case Callable:
				// Get swap sensis
				RandomVariable[] swapSensis = getSwapSensitivitiesFromCache(curveIndexName);

				// Melt swap sensis
				double initialMeltingTime = swap.getStartTime();
				RandomVariable[] meltedSwapSensis = sensitivityCalculationScheme.getMeltedSensitivities(this, swapSensis, initialMeltingTime, evaluationTime, curveIndexName, "INTEREST_RATE");

				// Put swap sensis on exercised paths
				if (evaluationTime >= bermudan.getExerciseTimes()[bermudan.getExerciseTimes().length - 1]) {

					// Set sensis of not exercised paths to zero
					for (int i = 0; i < meltedBermudanSensis.length; i++) {
						meltedBermudanSensis[i] = indicator.choose(meltedSwapSensis[i], new RandomVariableFromDoubleArray(0.0));
					}
				} else {

					// Set sensitivities on paths: Bermudan sensis if not exercised, swap sensis if exercised.
					for (int i = 0; i < meltedBermudanSensis.length; i++) {
						meltedBermudanSensis[i] = indicator.choose(meltedSwapSensis[i], meltedBermudanSensis[i]);
					}
				}

				break;

			case Cancelable:
				// Set sensis on exercised paths to zero
				for (int i = 0; i < meltedBermudanSensis.length; i++) {
					meltedBermudanSensis[i] = indicator.choose(new RandomVariableFromDoubleArray(0.0), meltedBermudanSensis[i]);
				}

				break;

			default:
				break;
			}
		}

		return meltedBermudanSensis;
	}

	/**
	 * Get the sensitivities of the underlying swap. This function is called when melting sensitivities at each time step.
	 *
	 * @param curveIndexName The name of the curve
	 * @return The sensitivities of the underlying swap from the cache "swapSensitivityMap".
	 * @throws CalculationException
	 */
	private RandomVariable[] getSwapSensitivitiesFromCache(String curveIndexName) throws CalculationException {

		SensitivityMode meltingMode = sensitivityCalculationScheme.getSensitivityMode();
		if (!swapSensitivityMap.containsKey(curveIndexName)) {
			double evaluationTime = swap.getStartTime();
			// Get OIS Sensis
			RandomVariable[] dVdP = SIMMSimpleSwap.getAnalyticSensitivities(evaluationTime, swap.getFixingDates(), swap.getSwapRates(), modelCache.getLiborPeriodDiscretization().getTimeStep(0), swap.getNotional(), modelCache, "OIS");
			double[] futureDiscountTimes = Arrays.stream(swap.getPaymentDates()).filter(n -> n > evaluationTime).toArray();
			RandomVariable[] swapSensisOIS = getOISModelSensitivities(evaluationTime, futureDiscountTimes, dVdP, "INTEREST_RATE", modelCache);
			if (meltingMode == SensitivityMode.MELTINGSIMMBUCKETS) { // Melting on SIMM Buckets
				swapSensisOIS = sensitivityCalculationScheme.mapOISBondToMarketRateSensitivities(evaluationTime, swapSensisOIS, modelCache);
				swapSensisOIS = AbstractSIMMSensitivityCalculation.mapSensitivitiesOnBuckets(swapSensisOIS, SIMMSimpleSwap.riskClass[0], null, modelCache);
			}
			swapSensitivityMap.put("OIS", swapSensisOIS);

			// Get forward curve sensis
			RandomVariable[] swapSensisLibor = SIMMSimpleSwap.getAnalyticSensitivities(evaluationTime, swap.getFixingDates(), swap.getSwapRates(), modelCache.getLiborPeriodDiscretization().getTimeStep(0), swap.getNotional(), modelCache, "Libor");

			if (meltingMode == SensitivityMode.MELTINGSIMMBUCKETS) {
				// Calculate dV/dS = dV/dL * dL/dS
				swapSensisLibor = sensitivityCalculationScheme.mapLiborToMarketRateSensitivities(evaluationTime, swapSensisLibor, modelCache);
				// Map Sensitivities on SIMM Buckets
				swapSensisLibor = AbstractSIMMSensitivityCalculation.mapSensitivitiesOnBuckets(swapSensisLibor, "INTEREST_RATE" /*riskClass*/, null, modelCache);
			}

			swapSensitivityMap.put("Libor6m", swapSensisLibor);
		}

		return swapSensitivityMap.get(curveIndexName);
	}

	@Override
	public RandomVariable getExerciseIndicator(double time, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		// @TODO: Perfomance improvement possible, since this triggerst an unneccesary valuation.
		RandomVariable exerciseTime = (RandomVariable) bermudan.getValues(time, model).get("exerciseTime");
		RandomVariable indicator = exerciseTime.sub(time + 0.00001).choose(new RandomVariableFromDoubleArray(0.0), new RandomVariableFromDoubleArray(1.0));
		return indicator;
	}

	@Override
	public double getFinalMaturity() {
		return bermudan.getPaymentDates()[bermudan.getPaymentDates().length - 1];
	}

	@Override
	public double getMeltingResetTime(LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		// @TODO: Perfomance improvement possible, since this triggerst an unneccesary valuation.
		RandomVariable exerciseTime = (RandomVariable) bermudan.getValues(0, model).get("exerciseTime");
		return exerciseTime.getMin();
	}

	@Override
	public void setConditionalExpectationOperator(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

		// Bermudan Swaption: Set paths on which we have already exercised to zero.
		RandomVariable indicator = getExerciseIndicator(evaluationTime, model).sub(0.5).choose(new RandomVariableFromDoubleArray(0.0), new RandomVariableFromDoubleArray(1.0));

		// Create a conditional expectation estimator with some basis functions (predictor variables) for conditional expectation estimation.
		RandomVariable[] regressor = new RandomVariable[2];
		regressor[0] = model.getLIBOR(evaluationTime, evaluationTime, evaluationTime + model.getLiborPeriodDiscretization().getTimeStep(0));
		regressor[1] = model.getLIBOR(evaluationTime, evaluationTime, model.getLiborPeriodDiscretization().getTime(model.getNumberOfLibors() - 1));
		ArrayList<RandomVariable> basisFunctions = getRegressionBasisFunctions(regressor, 2, indicator);
		this.conditionalExpectationOperator = new MonteCarloConditionalExpectationRegression(basisFunctions.toArray(new RandomVariable[0]));
	}

	private static ArrayList<RandomVariable> getRegressionBasisFunctions(RandomVariable[] libors, int order, RandomVariable indicator) {
		ArrayList<RandomVariable> basisFunctions = new ArrayList<RandomVariable>();
		// Create basis functions - here: 1, S, S^2, S^3, S^4

		for (int liborIndex = 0; liborIndex < libors.length; liborIndex++) {
			for (int powerOfRegressionMonomial = 0; powerOfRegressionMonomial <= order; powerOfRegressionMonomial++) {
				basisFunctions.add(libors[liborIndex].pow(powerOfRegressionMonomial).mult(indicator));
			}
		}
		return basisFunctions;
	}

	public ExerciseType getExerciseType() {
		return this.exerciseType;
	}

	public void clearSwapSensitivityMap() {
		swapSensitivityMap.clear();
	}

	//----------------------------------------------------------------------------------------------------------------------------------
	// Additional method for the case SensitivityMode.ExactConsideringDependencies, i.e. correct OIS-Libor dependence
	// NOT USED IN THE THESIS! PRELIMINARY TRIAL
	//----------------------------------------------------------------------------------------------------------------------------------

	@Override
	public RandomVariable[] getValueNumeraireSensitivities(double evaluationTime,
			LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		// Get Bermudan sensitivities
		RandomVariable[] bermudanSensis = getValueNumeraireSensitivitiesAAD(evaluationTime, model);

		// Set exercise indicator
		RandomVariable[] indicator = new RandomVariable[]{getExerciseIndicator(evaluationTime, model)}.clone();
		indicator[0] = indicator[0].sub(0.5); // i.e. -0.5: not exercised, 0.5: exercised

		// @TODO: Perfomance improvement possible, since this triggerst an unneccesary valuation.
		RandomVariable exerciseTime = (RandomVariable) bermudan.getValues(0, model).get("exerciseTime");

		// Set sensitivities on exercised paths
		if (evaluationTime >= exerciseTime.getMin()) {

			switch (exerciseType) {

			case Callable:

				RandomVariable[] swapSensis = SIMMSwap.getValueNumeraireSensitivities(evaluationTime, model);

				if (evaluationTime >= bermudan.getExerciseTimes()[bermudan.getExerciseTimes().length - 1]) {

					// Set sensis of not exercised paths to zero
					IntStream.range(0, bermudanSensis.length).forEach(i -> {
						bermudanSensis[i] =
								indicator[0].choose(swapSensis[i], new RandomVariableFromDoubleArray(0.0));
					});
				} else {

					// Set sensitivities on paths: Bermudan sensis if not exercised, swap sensis if exercised.
					IntStream.range(0, bermudanSensis.length).forEach(i -> {
						bermudanSensis[i] =
								indicator[0].choose(swapSensis[i], bermudanSensis[i]);
					});
				}

				break;

			case Cancelable:

				// Set the sensitivities on exercised paths to zero
				IntStream.range(0, bermudanSensis.length).forEach(i -> {
					bermudanSensis[i] =
							indicator[0].choose(new RandomVariableFromDoubleArray(0.0), bermudanSensis[i]);
				});

				break;

			default:
				break;
			}
		}

		return bermudanSensis;
	}
}
