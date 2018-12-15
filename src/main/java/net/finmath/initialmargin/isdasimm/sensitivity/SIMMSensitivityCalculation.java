package net.finmath.initialmargin.isdasimm.sensitivity;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;

import net.finmath.exception.CalculationException;
import net.finmath.initialmargin.isdasimm.changedfinmath.LIBORModelMonteCarloSimulationInterface;
import net.finmath.initialmargin.isdasimm.products.AbstractSIMMProduct;
import net.finmath.initialmargin.isdasimm.products.SIMMBermudanSwaption;
import net.finmath.optimizer.SolverException;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * This class performs linear sensitivity melting of sensitivities.
 * The melting is performed different sensitivity vectors
 * <ul>
 * <li>on market rate sensitivities given on SIMM buckets</li>
 * <li>on market rate sensitivities given on model buckets</li>
 * <li>on forward rate sensitivities given on model buckets</li>
 * </ul>
 * (possibly with a reset of the sensitivities to the true sensitivity values obtained by AAD).
 * <p>
 * Moreover, linear interpolation of sensitivities
 * on the SIMM buckets may be done with this class.
 *
 * @author Mario Viehmann
 * @author Christian Fries
 */
public class SIMMSensitivityCalculation extends AbstractSIMMSensitivityCalculation {

	private double interpolationStep;
	private LIBORModelMonteCarloSimulationInterface model;

	/**
	 * Construct a SIMM sensitivity calculation scheme
	 *
	 * @param sensitivityMode                The approximation method for sensitivities (Exact, Melting, Interpolation)
	 * @param liborWeightMode                The model-to-market-rate sensitivity transformation mode: Constant or Time Dependent weights
	 * @param interpolationStep              The time step of equidistant intervals between points at which exact AAD sensitivities are calculated
	 * @param model                          The LIBOR market model
	 * @param isUseAnalyticSwapSensitivities
	 * @param isConsiderOISSensitivities
	 */
	public SIMMSensitivityCalculation(SensitivityMode sensitivityMode, WeightMode liborWeightMode,
			double interpolationStep, LIBORModelMonteCarloSimulationInterface model, boolean isUseAnalyticSwapSensitivities, boolean isConsiderOISSensitivities) {

		super(sensitivityMode, liborWeightMode, isUseAnalyticSwapSensitivities, isConsiderOISSensitivities);
		this.interpolationStep = interpolationStep;
		this.model = model;
	}

	/**
	 * Construct a SIMM sensitivity calculation scheme which takes into consideration OIS sensitivities
	 *
	 * @param sensitivityMode                The approximation method for sensitivities (Exact, Melting, Interpolation)
	 * @param liborWeightMode                The model-to-market-rate sensitivity transformation mode: Constant or Time Dependent weights
	 * @param interpolationStep              The time step of equidistant intervals between points at which exact AAD sensitivities are calculated
	 * @param model                          The LIBOR market model
	 * @param isUseAnalyticSwapSensitivities
	 */
	public SIMMSensitivityCalculation(SensitivityMode sensitivityMode, WeightMode liborWeightMode, double interpolationStep, LIBORModelMonteCarloSimulationInterface model, boolean isUseAnalyticSwapSensitivities) {

		this(sensitivityMode, liborWeightMode, interpolationStep, model, isUseAnalyticSwapSensitivities, true /*isConsiderOISSensitivities*/);
	}

	@Override
	public RandomVariableInterface[] getDeltaSensitivities(AbstractSIMMProduct product,
			String riskClass,
			String curveIndexName,
			double evaluationTime,
			LIBORModelMonteCarloSimulationInterface model) throws SolverException, CloneNotSupportedException, CalculationException {

		RandomVariableInterface[] maturityBucketSensis = null;

		switch (sensitivityMode) {

		case EXACT:

			maturityBucketSensis = getSensitivitiesIRMarketRates(product, curveIndexName, evaluationTime, model);

			boolean isPrintDebugSensiVector = false;
			if (isPrintDebugSensiVector) {
				System.out.print("E " + evaluationTime + "\t");
				for (int i = 0; i < maturityBucketSensis.length; i++) {
					System.out.print(maturityBucketSensis[i].getAverage() + "\t");
				}
				System.out.print("\n");
			}

			// Map sensitivities on SIMM buckets
			maturityBucketSensis = mapSensitivitiesOnBuckets(maturityBucketSensis, "INTEREST_RATE" /*riskClass*/, null, model);

			break;

		case EXACTCONSIDERINGDEPENDENCIES:
			// Model-to-market-rate sensitivity transformation using numeraire derivatives. Not considered in the thesis.
			maturityBucketSensis = doCalculateDeltaSensitivitiesOISLiborDependence(product, curveIndexName, evaluationTime, model);

			break;

		case INTERPOLATION:
			maturityBucketSensis = getInterpolatedSensitivities(product, riskClass, curveIndexName, evaluationTime, model);

			break;

		case MELTINGSIMMBUCKETS:   // Melting on SIMM Buckets
		case MELTINGSWAPRATEBUCKETS:   // Melting on SIMM Buckets
		case MELTINGLIBORBUCKETS:  // Melting on Libor Buckets
			// The time of the exact sensitivities being melted: Initial Melting Time
			double initialMeltingTime = evaluationTime < product.getMeltingResetTime(model) ? 0 : product.getMeltingResetTime(model);

			// The sensitivities obtained from getMeltedSensitivities are always on SIMM buckets
			maturityBucketSensis = getMeltedSensitivities(product, null /*given sensitivities*/, initialMeltingTime, evaluationTime, curveIndexName, riskClass);

			if (product instanceof SIMMBermudanSwaption) {

				maturityBucketSensis = ((SIMMBermudanSwaption) product).changeMeltedSensitivitiesOnExercisedPaths(evaluationTime, model, curveIndexName, maturityBucketSensis);
			}

			break;

		default:
			break;
		}

		return maturityBucketSensis;
	}

	@Override
	public RandomVariableInterface[] getExactDeltaSensitivities(AbstractSIMMProduct product, String curveIndexName, String riskClass,
			double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws SolverException, CloneNotSupportedException, CalculationException {

		//@Todo: Distinguish different risk classes. Works currently only for "INTEREST_RATE"
		return getSensitivitiesIRMarketRates(product, curveIndexName, evaluationTime, model);
	}

	/**
	 * Linear melting of the sensitivities given on the SIMM Buckets or Libor Buckets (dependig on the SensitivityMode).
	 * The melting on SIMM Buckets is perfromed linearly such that the time zero sensitivities on bucket with maturity
	 * N years have vanished after N years. After N years, half of the sensitivities which are originally on the 2N year
	 * bucket will have moved onto the N year bucket. Melting on the Libor Buckets means going forward in time while holding
	 * the Libor Bucket Sensitivities constant. The sensitivities are "melting" as less and less Libor Buckets are taken into
	 * account. The remaining Libor Buckets are converted into market-rate sensitivities and then allocated linearly onto the SIMM Buckets.
	 *
	 * @param product         The product whose sensitivities we melt
	 * @param sensitivities   The sensitivities on SIMM buckets or LiborPeriodDiscretization to be melted
	 * @param zeroMeltingTime The time at which the melting should start, i.e. time zero
	 * @param evaluationTime  The time at which the melted sensitivites are calculated
	 * @param curveIndexName  The name of the curve
	 * @param riskClass       The SIMM risk class of the product whose sensitivities we consider
	 * @return The melted sensitivities
	 * @throws CalculationException
	 * @throws CloneNotSupportedException
	 * @throws SolverException
	 */
	@Override
	public RandomVariableInterface[] getMeltedSensitivities(AbstractSIMMProduct product,
			RandomVariableInterface[] sensitivities, double meltingZeroTime,
			double evaluationTime, String curveIndexName, String riskClass) throws SolverException, CloneNotSupportedException, CalculationException {

		boolean isMarketRateSensi = sensitivityMode == SensitivityMode.MELTINGSIMMBUCKETS || sensitivityMode == SensitivityMode.MELTINGSWAPRATEBUCKETS;
		RandomVariableInterface[] meltedSensis = null;
		int[] riskFactorDays = null;
		// Get sensitivities to melt if not provided as input to the function
		if (sensitivities == null) {
			sensitivities = product.getExactDeltaFromCache(meltingZeroTime, riskClass, curveIndexName, isMarketRateSensi);

			if (sensitivityMode == SensitivityMode.MELTINGSIMMBUCKETS) {
				// Map sensitivities on SIMM buckets
				sensitivities = mapSensitivitiesOnBuckets(sensitivities, "INTEREST_RATE" /*riskClass*/, null, model);
			}
		}

		switch (sensitivityMode) {
		case MELTINGSIMMBUCKETS: // Melting of market-rate sensitivities on SIMM Buckets
		{
			int[] riskFactorsSIMM = riskClass == "INTEREST_RATE" ? new int[]{14, 30, 90, 180, 365, 730, 1095, 1825, 3650, 5475, 7300, 10950} : /*CREDIT*/ new int[]{365, 730, 1095, 1825, 3650};

			// Get new riskFactor times
			riskFactorDays = Arrays.stream(riskFactorsSIMM).filter(n -> n > (int) Math.round(365 * (evaluationTime - meltingZeroTime))).map(n -> n - (int) Math.round(365 * (evaluationTime - meltingZeroTime))).toArray();

			// Find first bucket later than evaluationTime
			int firstIndex = IntStream.range(0, riskFactorsSIMM.length)
					.filter(i -> riskFactorsSIMM[i] > (int) Math.round(365 * (evaluationTime - meltingZeroTime))).findFirst().getAsInt();

			//Calculate melted sensitivities
			meltedSensis = new RandomVariableInterface[sensitivities.length - firstIndex];

			for (int i = 0; i < meltedSensis.length; i++) {
				meltedSensis[i] = sensitivities[i + firstIndex].mult(1.0 - (double) Math.round(365 * (evaluationTime - meltingZeroTime)) / (double) riskFactorsSIMM[i + firstIndex]);
			}
		}
		break;

		case MELTINGSWAPRATEBUCKETS: // Melting of market-rate sensitivities on model buckets
		{
			int[] riskFactorDaysLIBOR = riskFactorDaysLibor(sensitivities, model);

			// Get new riskFactor times
			riskFactorDays = Arrays.stream(riskFactorDaysLIBOR).filter(n -> n > (int) Math.round(365 * (evaluationTime - meltingZeroTime))).map(n -> n - (int) Math.round(365 * (evaluationTime - meltingZeroTime))).toArray();

			// Find first bucket later than evaluationTime
			int firstIndex = IntStream.range(0, riskFactorDaysLIBOR.length).filter(i -> riskFactorDaysLIBOR[i] > (int) Math.round(365 * (evaluationTime - meltingZeroTime))).findFirst().getAsInt();

			//Calculate melted sensitivities
			meltedSensis = new RandomVariableInterface[sensitivities.length - firstIndex];

			for (int i = 0; i < meltedSensis.length; i++) {
				meltedSensis[i] = sensitivities[i + firstIndex].mult(1.0 - (double) Math.round(365 * (evaluationTime - meltingZeroTime)) / (double) riskFactorDaysLIBOR[i + firstIndex]);
			}

			boolean isPrintDebugSensiVector = false;
			if (isPrintDebugSensiVector) {
				System.out.print("M " + evaluationTime + "\t");
				for (int i = 0; i < meltedSensis.length; i++) {
					System.out.print(meltedSensis[i].get(0) + "\t");
				}
				System.out.print("\n");
			}
		}
		break;

		case MELTINGLIBORBUCKETS: // Melting of model sensitivities with subsequent mapping to market-rate sensitivities
		{
			// First index of Libor at evaluation time given the initial melting time
			int liborIndexAtInitialMeltingTime = model.getLiborPeriodIndex(meltingZeroTime);
			liborIndexAtInitialMeltingTime = liborIndexAtInitialMeltingTime < 0 ? -liborIndexAtInitialMeltingTime - 1 : liborIndexAtInitialMeltingTime;
			int liborIndexAtEval = model.getLiborPeriodIndex(evaluationTime);
			int oisIndex = (liborIndexAtEval < 0 && curveIndexName == "OIS") ? 1 : 0;
			liborIndexAtEval = liborIndexAtEval < 0 ? -liborIndexAtEval - 2 : liborIndexAtEval;
			int firstIndex = liborIndexAtEval - liborIndexAtInitialMeltingTime + oisIndex;
			// Retain all libor sensis after eval
			meltedSensis = ArrayUtils.subarray(sensitivities, firstIndex, sensitivities.length);
			// Map model sensis to market rate sensis
			meltedSensis = curveIndexName == "Libor6m" ? mapLiborToMarketRateSensitivities(evaluationTime, meltedSensis, model) :
				mapOISBondToMarketRateSensitivities(evaluationTime, meltedSensis, model);
		}
		break;
		default: {
			throw new IllegalArgumentException();
		}
		}

		return mapSensitivitiesOnBuckets(meltedSensis, riskClass, riskFactorDays, model);
	}

	/**
	 * Interpolates sensitivities on SIMM buckets linearly between two exact sensitivities obtained by AAD.
	 * Information of future sensitivities (after evaluation time) is used.
	 *
	 * @param product        The product whose sensitivities are interpolated
	 * @param riskClass      The risk class of the product
	 * @param curveIndexName The name of the index curve
	 * @param evaluationTime The time of evaluation
	 * @param model          The Libor market model
	 * @return The interpolated sensitivities on SIMM buckets at evaluation time
	 * @throws SolverException
	 * @throws CloneNotSupportedException
	 * @throws CalculationException
	 */
	private RandomVariableInterface[] getInterpolatedSensitivities(AbstractSIMMProduct product,
			String riskClass,
			String curveIndexName,
			double evaluationTime,
			LIBORModelMonteCarloSimulationInterface model) throws SolverException, CloneNotSupportedException, CalculationException {

		// time of initial and final sensitivities
		TimeDiscretizationInterface exactSensiTimes = new TimeDiscretization(0, 50, interpolationStep);
		int initialIndex = exactSensiTimes.getTimeIndexNearestLessOrEqual(evaluationTime);
		double initialTime = (exactSensiTimes.getTime(initialIndex) <= product.getMeltingResetTime(model)) && (exactSensiTimes.getTime(initialIndex + 1) > product.getMeltingResetTime(model)) ? product.getMeltingResetTime(model) : exactSensiTimes.getTime(initialIndex);
		double finalTime = initialTime < product.getMeltingResetTime(model) ? Math.min(product.getMeltingResetTime(model), exactSensiTimes.getTime(initialIndex + 1)) : exactSensiTimes.getTime(initialIndex + 1);

		// Get Sensitivities from exactDeltaCache
		RandomVariableInterface[] initialSensitivities = product.getExactDeltaFromCache(initialTime, riskClass, curveIndexName, true /*isMarketRateSensi*/);
		// Map sensitivities on SIMM buckets
		initialSensitivities = mapSensitivitiesOnBuckets(initialSensitivities, "INTEREST_RATE" /*riskClass*/, null, model);

		RandomVariableInterface[] finalSensitivities = product.getExactDeltaFromCache(finalTime, riskClass, curveIndexName, true /*isMarketRateSensi*/);
		// Map sensitivities on SIMM buckets
		finalSensitivities = mapSensitivitiesOnBuckets(finalSensitivities, "INTEREST_RATE" /*riskClass*/, null, model);

		// Perform linear interpolation
		double deltaT = finalTime - initialTime;
		double deltaTEval = evaluationTime - initialTime;

		if (deltaT == 0) {
			return finalSensitivities;
		}

		RandomVariableInterface[] interpolatedSensis = new RandomVariableInterface[initialSensitivities.length];
		for (int bucketIndex = 0; bucketIndex < initialSensitivities.length; bucketIndex++) {
			RandomVariableInterface slope = finalSensitivities[bucketIndex].sub(initialSensitivities[bucketIndex]).div(deltaT);
			interpolatedSensis[bucketIndex] = initialSensitivities[bucketIndex].add(slope.mult(deltaTEval));
		}

		return interpolatedSensis;
	}

	/**
	 * Returns the interpolation time step, i.e. the length of equidistant intervals of points at which we
	 * calculate the exact AAD sensitivities
	 *
	 * @return The interpolation step size
	 */
	public double getInterpolationStep() {
		return this.interpolationStep;
	}
}

