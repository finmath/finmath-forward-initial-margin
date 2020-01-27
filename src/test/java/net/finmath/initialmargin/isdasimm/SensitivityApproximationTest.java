package net.finmath.initialmargin.isdasimm;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.exception.CalculationException;
import net.finmath.initialmargin.isdasimm.products.AbstractSIMMProduct;
import net.finmath.initialmargin.isdasimm.products.AbstractSIMMProduct.MVAMode;
import net.finmath.initialmargin.isdasimm.products.SIMMBermudanSwaption;
import net.finmath.initialmargin.isdasimm.products.SIMMBermudanSwaption.ExerciseType;
import net.finmath.initialmargin.isdasimm.products.SIMMSimpleSwap;
import net.finmath.initialmargin.isdasimm.products.SIMMSwaption;
import net.finmath.initialmargin.isdasimm.products.SIMMSwaption.DeliveryType;
import net.finmath.initialmargin.isdasimm.sensitivity.AbstractSIMMSensitivityCalculation.SensitivityMode;
import net.finmath.initialmargin.isdasimm.sensitivity.AbstractSIMMSensitivityCalculation.WeightMode;
import net.finmath.initialmargin.isdasimm.test.SIMMTest;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * @author Mario Viehmann
 * @author Christian Fries
 */
@RunWith(Parameterized.class)
public class SensitivityApproximationTest {

	/**
	 * The parameters for this test, that is an error consisting of
	 * { numberOfPaths, setup }.
	 *
	 * @return Array of parameters.
	 */
	@Parameters(name = "{0}")
	public static Collection<Object[]> generateData() {
		return Arrays.asList(new Object[][]{
			{TestProductType.SWAPS, WeightMode.TIMEDEPENDENT },
			{TestProductType.SWAPTIONS, WeightMode.TIMEDEPENDENT },
			{TestProductType.BERMUDANCALLABLE, WeightMode.TIMEDEPENDENT },
			{TestProductType.BERMUDANCANCELABLE, WeightMode.TIMEDEPENDENT },
		});
	}

	static final DecimalFormat formatterTime = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
	{
		formatterTime.applyPattern("0");
	}
	static final DecimalFormat formatterReal1 = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
	{
		formatterReal1.applyPattern("0.00");
	}
	static final DecimalFormat formatterPercent = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
	{
		formatterPercent.applyPattern("0.0%");
	}

	private static class MvaResult {
		private double mva;
		private Map<Double, Double> expectedIM;
		public MvaResult(double mva, Map<Double, Double> expectedIM) {
			super();
			this.mva = mva;
			this.expectedIM = expectedIM;
		}
	}

	// Model Paths
	static final int numberOfPaths = 1000; // Use 1000 or more for results in publication.
	static final double simulationTimeDt = 0.1;        // Value is higher to let unit test run on low mem. Set this to 0.1 for better results.
	static final double notional = 100;
	static final boolean isPrintProfile = false;
	static final double fundingSpread = 0.005; // For MVA
	static final double periodLength = 0.5;

	public enum TestProductType {
		SWAPS,
		SWAPTIONS,
		BERMUDANCALLABLE,
		BERMUDANCANCELABLE
	}

	// Selected TestProducts
	private final TestProductType testProductType;
	private final WeightMode weightMode;

	public static void main(String[] args) throws CalculationException {
		SensitivityApproximationTest sat = new SensitivityApproximationTest(TestProductType.BERMUDANCALLABLE, WeightMode.TIMEDEPENDENT);
		sat.test();
	}

	public SensitivityApproximationTest(TestProductType testProductType, WeightMode weightMode) {
		super();
		this.testProductType = testProductType;
		this.weightMode = weightMode;
	}

	@Test
	@Ignore
	public void test() throws CalculationException {

		/*
		 *
		 *  Create a Indices market Model
		 *
		 */
		RandomVariableFactory abstractRandomVariableFactory = SIMMTest.createRandomVariableFactoryAAD();

		// CurveFromInterpolationPoints Data as of December 8, 2017
		DiscountCurveInterpolation discountCurve = DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors("OIS",
				// Times
				new double[]{0, 0.02739726, 0.065753425, 0.095890411, 0.178082192, 0.254794521, 0.345205479, 0.421917808, 0.506849315, 0.594520548, 0.673972603, 0.764383562, 0.843835616, 0.926027397, 1.01369863, 1.254794521, 1.512328767, 2.01369863, 3.010958904, 4.010958904, 5.010958904, 6.010958904, 7.019178082, 8.016438356, 9.01369863, 10.01369863, 11.01643836, 12.02191781, 15.01917808, 18.02465753, 20.02191781, 25.02739726, 30.03287671, 40.04109589, 50.04109589},
				// Discount Factors
				new double[]{1, 0.942220253, 1.14628676, 0.973644156, 0.989291916, 0.988947387, 0.989030365, 0.989540089, 0.989760412, 0.990003764, 0.990397338, 0.990628687, 0.990878391, 0.991165682, 0.991574886, 0.992229531, 0.993347703, 0.993022409, 0.992927371, 0.990353891, 0.98534136, 0.977964157, 0.968209156, 0.956438149, 0.942562961, 0.927724566, 0.911915214, 0.895097576, 0.84499878, 0.798562566, 0.769568088, 0.707863301, 0.654037617, 0.562380546, 0.496026132}
				);

		ForwardCurveInterpolation forwardCurve = ForwardCurveInterpolation.createForwardCurveFromForwards("Libor6m",
				// Fixings of the forward
				new double[]{0.504109589, 1.504109589, 2.509589041, 3.506849315, 4.506849315, 5.506849315, 6.509589041, 7.515068493, 8.512328767, 9.509589041, 10.51232877, 11.51232877, 12.51232877, 13.51780822, 14.51506849, 15.51506849, 16.51506849, 17.51506849, 18.52328767, 19.52054795, 20.51780822, 21.51780822, 22.52054795, 23.52054795, 24.5260274, 25.52328767, 26.52328767, 27.52328767, 28.52328767, 29.52328767, 34.52876712, 39.53150685, 44.53424658, 49.5369863, 54.54246575, 59.54520548},
				// Forward Rates
				new double[]{-0.002630852, -6.82E-04, 0.002757708, 0.005260602, 0.007848164, 0.010749576, 0.012628982, 0.014583704, 0.017103188, 0.017791957, 0.01917447, 0.019788258, 0.020269155, 0.02327218, 0.01577317, 0.026503375, 0.017980753, 0.016047889, 0.024898978, 0.010798547, 0.027070148, 0.014816786, 0.018220786, 0.016549747, 0.008028913, 0.020022068, 0.015134412, 0.016604122, 0.014386016, 0.026732673, 0.003643934, 0.024595029, 0.002432369, 0.02233176, 0.003397059, 0.020576206},
				0.5/* tenor / period length */);

		LIBORModelMonteCarloSimulationModel model = SIMMTest.createLIBORMarketModel(false, abstractRandomVariableFactory, numberOfPaths, 1 /*numberOfFactors*/,
				discountCurve,
				forwardCurve,
				simulationTimeDt);

		double[] exerciseDates = null;
		int[] numberOfPeriods = null;

		// Define further parameters
		boolean isConsiderOISSensis = true;
		double interpolationStep = 1.0;

		// Specify test products
		switch (testProductType) {
		case SWAPS:
			exerciseDates = new double[] { 0.0 };
			if(isPrintProfile) {
				numberOfPeriods = new int[] { 20 };
			}
			else {
				numberOfPeriods = new int[] { 10, 20, 24, 30, 40 };
			}
			break;
		case SWAPTIONS:
			exerciseDates = new double[] { 5.0, 10.0 };
			if(isPrintProfile) {
				numberOfPeriods = new int[] { 10, 16 };
			}
			else {
				numberOfPeriods = new int[] { 4, 8, 10, 12, 16 };
			}
			break;
		case BERMUDANCALLABLE:
		case BERMUDANCANCELABLE:
			exerciseDates = new double[] { 5.0, 8.0, 10.0 };
			//			exerciseDates = new double[] { 10.0 };
			numberOfPeriods = new int[] { 20 };
			break;
		default:
			break;
		}

		System.out.println("Product....................: " + testProductType.name());
		System.out.println("Sensitivity Transformation.: " + weightMode.name());
		System.out.println();

		// Create test products
		AbstractSIMMProduct[] products = createProducts(testProductType, exerciseDates, periodLength, numberOfPeriods, forwardCurve, discountCurve);

		// Execute test function
		testSIMMProductApproximation(products, exerciseDates, numberOfPeriods, forwardCurve, discountCurve, model, isConsiderOISSensis, interpolationStep, simulationTimeDt, weightMode);
	}

	public static void testSIMMProductApproximation(AbstractSIMMProduct[] product, double[] exerciseDates,
			int[] numberOfPeriods, ForwardCurveInterpolation forwardCurve, DiscountCurveInterpolation discountCurve, LIBORModelMonteCarloSimulationModel model, boolean isConsiderOISSensis, double interpolationStep, double simulationTimeDt, WeightMode weightMode) throws CalculationException {

		double timeStep = simulationTimeDt;
		boolean isUseAnalyticSwapSensis = false;

		System.out.println("Exercise" + "\t" + "Maturity" + "\t" + "Time Exact" + "\t" + "Time Melting" + "\t" + "Time Interpol." + "\t" + "Time Exact C" + "\t" + "Time Melting C" + "\t" + "Time Interpol C" + "\t" + "MVA Exact" + "\t" + "MVA Dev Melting" + "\t" + "MVA Dev Interpol" + "\t" + "MVA Exact C" + "\t" + "MVA Dev Melting C" + "\t" + "MVA Dev Interpol C" + "\t" + "MVA Det Funding");

		int productIndex = 0;
		for (int exerciseIndex = 0; exerciseIndex < exerciseDates.length; exerciseIndex++) {
			for (int swapPeriodsIndex = 0; swapPeriodsIndex < numberOfPeriods.length; swapPeriodsIndex++) {

				double finalIMTime = exerciseDates[exerciseIndex] + model.getLiborPeriodDiscretization().getTimeStep(0) * numberOfPeriods[swapPeriodsIndex];
				/*
				 * Calculate IM
				 */

				/*
				 * 1) Exact
				 */
				long timeStart = System.currentTimeMillis();
				MvaResult mvaExact = getMVA(model, product[productIndex], finalIMTime, timeStep, fundingSpread, MVAMode.EXACT, SensitivityMode.EXACT, WeightMode.TIMEDEPENDENT, isUseAnalyticSwapSensis, isConsiderOISSensis);
				long timeEnd = System.currentTimeMillis();

				MvaResult mvaApproximation = getMVA(model, product[productIndex], finalIMTime, timeStep, fundingSpread, MVAMode.APPROXIMATION, SensitivityMode.EXACT, WeightMode.TIMEDEPENDENT, isUseAnalyticSwapSensis, isConsiderOISSensis);

				/*
				 * 1) Exact Const
				 */
				long timeStartConstantWeights = System.currentTimeMillis();
				MvaResult mvaExactConstantWeights = getMVA(model, product[productIndex], finalIMTime, timeStep, fundingSpread, MVAMode.EXACT, SensitivityMode.EXACT, WeightMode.CONSTANT, isUseAnalyticSwapSensis, isConsiderOISSensis);
				long timeEndConstantWeights = System.currentTimeMillis();

				/*
				 * 2) Melting (on SIMM buckets)
				 */
				long timeStartMelting = System.currentTimeMillis();
				MvaResult mvaMelting = getMVA(model, product[productIndex], finalIMTime, timeStep, fundingSpread, MVAMode.EXACT, SensitivityMode.MELTINGSIMMBUCKETS, WeightMode.TIMEDEPENDENT, isUseAnalyticSwapSensis, isConsiderOISSensis);
				long timeEndMelting = System.currentTimeMillis();

				long timeStartMeltingConstant = System.currentTimeMillis();
				MvaResult mvaMeltingConstant = getMVA(model, product[productIndex], finalIMTime, timeStep, fundingSpread, MVAMode.EXACT, SensitivityMode.MELTINGSIMMBUCKETS, WeightMode.CONSTANT, isUseAnalyticSwapSensis, isConsiderOISSensis);
				long timeEndMeltingConstant = System.currentTimeMillis();

				/*
				 * 3) Interpolation
				 */
				long timeStartInterpolation = System.currentTimeMillis();
				MvaResult mvaInterpolation = getMVA(model, product[productIndex], finalIMTime, timeStep, fundingSpread, MVAMode.EXACT, SensitivityMode.INTERPOLATION, WeightMode.TIMEDEPENDENT, isUseAnalyticSwapSensis, isConsiderOISSensis);
				long timeEndInterpolation = System.currentTimeMillis();

				long timeStartInterpolationConstant = System.currentTimeMillis();
				MvaResult mvaInterpolationConstant = getMVA(model, product[productIndex], finalIMTime, timeStep, fundingSpread, MVAMode.EXACT, SensitivityMode.INTERPOLATION, WeightMode.CONSTANT, isUseAnalyticSwapSensis, isConsiderOISSensis);
				long timeEndInterpolationConstant = System.currentTimeMillis();

				boolean isPrintProfileConstant = true;
				// Print Result and calculate Deviations
				if (isPrintProfile) {
					System.out.println("time" + "\t" + "exact" + "\t" + "melting" + "\t" + "interpolation");
					for (int i = 0; i < finalIMTime / timeStep + 1; i++) {
						if(isPrintProfileConstant) {
							System.out.println((i*timeStep) + "\t" + mvaExactConstantWeights.expectedIM.get(i*timeStep).doubleValue() + "\t" + mvaMeltingConstant.expectedIM.get(i*timeStep).doubleValue() + "\t" + mvaInterpolationConstant.expectedIM.get(i*timeStep).doubleValue());
						}
						else {
							System.out.println((i*timeStep) + "\t" + mvaExact.expectedIM.get(i*timeStep).doubleValue() + "\t" + mvaMelting.expectedIM.get(i*timeStep).doubleValue() + "\t" + mvaInterpolation.expectedIM.get(i*timeStep).doubleValue());
						}
					}
				}

				System.out.println(exerciseDates[exerciseIndex] + "\t" + (numberOfPeriods[swapPeriodsIndex]*periodLength) +
						"\t" + formatterTime.format((timeEnd - timeStart) / 1000.0) + "s" +
						"\t" + formatterTime.format((timeEndMelting - timeStartMelting) / 1000.0) + "s" +
						"\t" + formatterTime.format((timeEndInterpolation - timeStartInterpolation) / 1000.0) + "s" +
						"\t" + formatterTime.format((timeEndConstantWeights - timeStartConstantWeights) / 1000.0) + "s" +
						"\t" + formatterTime.format((timeEndMeltingConstant - timeStartMeltingConstant) / 1000.0) + "s" +
						"\t" + formatterTime.format((timeEndInterpolationConstant - timeStartInterpolationConstant) / 1000.0) + "s" +
						"\t" + formatterReal1.format(mvaExact.mva*100) +
						"\t" + formatterPercent.format((mvaMelting.mva - mvaExact.mva)/mvaExact.mva) +
						"\t" + formatterPercent.format((mvaInterpolation.mva - mvaExact.mva)/mvaExact.mva) +
						"\t" + formatterReal1.format(mvaExactConstantWeights.mva*100) +
						"\t" + formatterPercent.format((mvaMeltingConstant.mva - mvaExact.mva)/mvaExact.mva) +
						"\t" + formatterPercent.format((mvaInterpolationConstant.mva - mvaExact.mva)/mvaExact.mva) +
						"\t" + formatterReal1.format(mvaApproximation.mva*100)
						);

				productIndex++;
			}
		}

		System.out.println("\n");
	}

	/*
	 * @TODO HARDCODED PERIOD LENGTH: BAD. Fix.
	 */
	public static AbstractSIMMProduct[] createProducts(TestProductType type, double[] exerciseDates, double periodLength, int[] numberOfPeriods, ForwardCurveInterpolation forwardCurve, DiscountCurveInterpolation discountCurve) throws CalculationException {

		ArrayList<AbstractSIMMProduct> products = new ArrayList<>();
		double[] fixingDates;
		double[] paymentDates;
		double[] swapRates;
		double[] swapTenor;
		double[] periodLengths;
		double[] periodNotionals;
		boolean[] isPeriodStartDateExerciseDate;

		switch (type) {

		case SWAPS:

			// 1) Swap Input
			double startTime = 0.0;    // Exercise date
			for (int i = 0; i < numberOfPeriods.length; i++) {
				fixingDates = new double[numberOfPeriods[i]];
				paymentDates = new double[numberOfPeriods[i]];
				swapRates = new double[numberOfPeriods[i]];
				swapTenor = new double[numberOfPeriods[i] + 1];

				// Fill data
				fixingDates = IntStream.range(0, fixingDates.length).mapToDouble(n -> startTime + n * periodLength).toArray();
				paymentDates = IntStream.range(0, paymentDates.length).mapToDouble(n -> startTime + (n + 1) * periodLength).toArray();
				swapTenor = IntStream.range(0, numberOfPeriods[i] + 1).mapToDouble(n -> startTime + n * periodLength).toArray();
				Arrays.fill(swapRates, SIMMTest.getParSwaprate(forwardCurve, discountCurve, swapTenor));

				products.add(new SIMMSimpleSwap(fixingDates, paymentDates, swapRates, true /*isPayFix*/, notional, new String[]{"OIS", "Libor6m"}, "EUR"));
			}
			break;

		case SWAPTIONS:

			for (int i = 0; i < exerciseDates.length; i++) {
				for (int j = 0; j < numberOfPeriods.length; j++) {

					fixingDates = new double[numberOfPeriods[j]];
					paymentDates = new double[numberOfPeriods[j]];
					swapRates = new double[numberOfPeriods[j]];
					swapTenor = new double[numberOfPeriods[j] + 1];

					periodLengths = new double[paymentDates.length];
					periodNotionals = new double[periodLengths.length];

					int index = i;
					// Set values
					fixingDates = IntStream.range(0, fixingDates.length).mapToDouble(n -> exerciseDates[index] + n * periodLength).toArray();
					paymentDates = IntStream.range(0, paymentDates.length).mapToDouble(n -> exerciseDates[index] + (n + 1) * periodLength).toArray();
					swapTenor = IntStream.range(0, numberOfPeriods[j] + 1).mapToDouble(n -> exerciseDates[index] + n * periodLength).toArray();
					Arrays.fill(periodLengths, periodLength);
					Arrays.fill(periodNotionals, notional);
					Arrays.fill(swapRates, SIMMTest.getParSwaprate(forwardCurve, discountCurve, swapTenor));

					products.add(new SIMMSwaption(exerciseDates[index], fixingDates, paymentDates, swapRates, notional,
							DeliveryType.Physical, new String[]{"OIS", "Libor6m"}, "EUR"));
				}
			}
			break;

		case BERMUDANCALLABLE:

			for (int i = 0; i < exerciseDates.length; i++) {
				for (int j = 0; j < numberOfPeriods.length; j++) {

					fixingDates = new double[numberOfPeriods[j]];
					paymentDates = new double[numberOfPeriods[j]];
					swapRates = new double[numberOfPeriods[j]];
					swapTenor = new double[numberOfPeriods[j] + 1];

					periodLengths = new double[paymentDates.length];
					periodNotionals = new double[periodLengths.length];
					isPeriodStartDateExerciseDate = new boolean[periodLengths.length];
					Arrays.fill(isPeriodStartDateExerciseDate, false);
					isPeriodStartDateExerciseDate[0] = true;
					isPeriodStartDateExerciseDate[4] = true;
					isPeriodStartDateExerciseDate[8] = true;
					isPeriodStartDateExerciseDate[12] = true;

					int index = i;
					// Set values
					fixingDates = IntStream.range(0, fixingDates.length).mapToDouble(n -> exerciseDates[index] + n * periodLength).toArray();
					paymentDates = IntStream.range(0, paymentDates.length).mapToDouble(n -> exerciseDates[index] + (n + 1) * periodLength).toArray();
					swapTenor = IntStream.range(0, numberOfPeriods[j] + 1).mapToDouble(n -> exerciseDates[index] + n * periodLength).toArray();
					Arrays.fill(periodLengths, periodLength);
					Arrays.fill(periodNotionals, notional);
					Arrays.fill(swapRates, SIMMTest.getParSwaprate(forwardCurve, discountCurve, swapTenor));
					products.add(new SIMMBermudanSwaption(fixingDates, periodLengths, paymentDates, periodNotionals,
							swapRates, isPeriodStartDateExerciseDate, ExerciseType.Callable, new String[]{"OIS", "Libor6m"}, "EUR"));
				}
			}
			break;

		case BERMUDANCANCELABLE:

			for (int i = 0; i < exerciseDates.length; i++) {
				for (int j = 0; j < numberOfPeriods.length; j++) {

					fixingDates = new double[numberOfPeriods[j]];
					paymentDates = new double[numberOfPeriods[j]];
					swapRates = new double[numberOfPeriods[j]];
					swapTenor = new double[numberOfPeriods[j] + 1];

					periodLengths = new double[paymentDates.length];
					periodNotionals = new double[periodLengths.length];
					isPeriodStartDateExerciseDate = new boolean[periodLengths.length];
					Arrays.fill(isPeriodStartDateExerciseDate, false);
					isPeriodStartDateExerciseDate[0] = true;
					isPeriodStartDateExerciseDate[4] = true;
					isPeriodStartDateExerciseDate[8] = true;
					isPeriodStartDateExerciseDate[12] = true;

					int index = i;
					// Set values
					fixingDates = IntStream.range(0, fixingDates.length).mapToDouble(n -> exerciseDates[index] + n * periodLength).toArray();
					paymentDates = IntStream.range(0, paymentDates.length).mapToDouble(n -> exerciseDates[index] + (n + 1) * periodLength).toArray();
					swapTenor = IntStream.range(0, numberOfPeriods[j] + 1).mapToDouble(n -> exerciseDates[index] + n * periodLength).toArray();
					Arrays.fill(periodLengths, periodLength);
					Arrays.fill(periodNotionals, notional);
					Arrays.fill(swapRates, SIMMTest.getParSwaprate(forwardCurve, discountCurve, swapTenor));
					products.add(new SIMMBermudanSwaption(fixingDates, periodLengths, paymentDates, periodNotionals,
							swapRates, isPeriodStartDateExerciseDate, ExerciseType.Cancelable, new String[]{"OIS", "Libor6m"}, "EUR"));
				}
			}
			break;

		default:
			break;
		}
		return products.stream().toArray(AbstractSIMMProduct[]::new);
	}

	public static MvaResult getMVA(LIBORModelMonteCarloSimulationModel model, AbstractSIMMProduct product, double finalIMTime, double timeStep, double fundingSpread, MVAMode mvaMode, SensitivityMode sensitivityMode, WeightMode weightMode, boolean isUseAnalyticSwapSensis, boolean isConsiderOISSensis) throws CalculationException {

		Map<Double, Double> expectedIM = new HashMap<Double, Double>();

		RandomVariable forwardBond;
		RandomVariable MVA = new RandomVariableFromDoubleArray(0.0);
		for (int i = 0; i < (int) (finalIMTime / timeStep)+1; i++) {
			double time = i * timeStep;
			double timeNext = (i+1) * timeStep;
			RandomVariable initialMargin = product.getInitialMargin(time, model, "EUR", sensitivityMode, weightMode, 1.0, isUseAnalyticSwapSensis, isConsiderOISSensis);
			forwardBond = model.getNumeraire((i + 1) * timeStep).mult(Math.exp(timeNext * fundingSpread)).invert();
			forwardBond = forwardBond.sub(model.getNumeraire(time).mult(Math.exp(time * fundingSpread)).invert());
			if (mvaMode == MVAMode.APPROXIMATION) {
				initialMargin = initialMargin.average();
			}
			MVA = MVA.add(forwardBond.mult(initialMargin));
			expectedIM.put(time, initialMargin.getAverage());
		}

		return new MvaResult(-MVA.getAverage(), expectedIM);
	}
}
