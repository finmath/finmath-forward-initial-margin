package net.finmath.initialmargin.isdasimm.test;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import net.finmath.exception.CalculationException;
import net.finmath.initialmargin.isdasimm.changedfinmath.LIBORModelMonteCarloSimulation;
import net.finmath.initialmargin.isdasimm.changedfinmath.LIBORModelMonteCarloSimulationInterface;
import net.finmath.initialmargin.isdasimm.products.AbstractSIMMProduct;
import net.finmath.initialmargin.isdasimm.products.SIMMBermudanSwaption;
import net.finmath.initialmargin.isdasimm.products.SIMMBermudanSwaption.ExerciseType;
import net.finmath.initialmargin.isdasimm.products.SIMMPortfolio;
import net.finmath.initialmargin.isdasimm.products.SIMMSimpleSwap;
import net.finmath.initialmargin.isdasimm.products.SIMMSwaption;
import net.finmath.initialmargin.isdasimm.products.SIMMSwaption.DeliveryType;
import net.finmath.initialmargin.isdasimm.sensitivity.AbstractSIMMSensitivityCalculation.SensitivityMode;
import net.finmath.initialmargin.isdasimm.sensitivity.AbstractSIMMSensitivityCalculation.WeightMode;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.BrownianMotionLazyInit;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAAD;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.modelplugins.AbstractLIBORCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.models.modelplugins.BlendedLocalVolatilityModel;
import net.finmath.montecarlo.interestrate.models.modelplugins.LIBORCorrelationModel;
import net.finmath.montecarlo.interestrate.models.modelplugins.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.modelplugins.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.modelplugins.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.models.modelplugins.LIBORVolatilityModelPiecewiseConstant;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.SwaptionSimple;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.optimizer.OptimizerFactory;
import net.finmath.optimizer.OptimizerFactoryLevenbergMarquardt;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.businessdaycalendar.AbstractBusinessdayCalendar;
import net.finmath.time.daycount.DayCountConvention;

public class SIMMTest {
	static final DecimalFormat formatterTime = new DecimalFormat("0.000");
	static final double upperQuantile = 0.025;
	static final double lowerQuantile = 0.975;

	static final boolean isPrintAverage = true;
	static final boolean isPrintQuantile = false;
	static final boolean isPrintPaths = false;

	static final boolean isCalculatePortfolio = false;
	static final boolean isCalculateSwap = false;
	static final boolean isCalculateSwaption = false;
	static final boolean isCalculateBermudan = true;

	// Model Paths
	static final int numberOfPaths = 500;//1000;

	public static void main(String[] args) throws CalculationException {

		/*
		 *  Create a LIBOR market Model
		 */
		AbstractRandomVariableFactory randomVariableFactory = createRandomVariableFactoryAAD();

		// Curve Data as of December 8, 2017
		DiscountCurve discountCurve = DiscountCurve.createDiscountCurveFromDiscountFactors("OIS",
				// Times
				new double[]{0, 0.02739726, 0.065753425, 0.095890411, 0.178082192, 0.254794521, 0.345205479, 0.421917808, 0.506849315, 0.594520548, 0.673972603, 0.764383562, 0.843835616, 0.926027397, 1.01369863, 1.254794521, 1.512328767, 2.01369863, 3.010958904, 4.010958904, 5.010958904, 6.010958904, 7.019178082, 8.016438356, 9.01369863, 10.01369863, 11.01643836, 12.02191781, 15.01917808, 18.02465753, 20.02191781, 25.02739726, 30.03287671, 40.04109589, 50.04109589},
				// Discount Factors
				new double[]{1, 0.942220253, 1.14628676, 0.973644156, 0.989291916, 0.988947387, 0.989030365, 0.989540089, 0.989760412, 0.990003764, 0.990397338, 0.990628687, 0.990878391, 0.991165682, 0.991574886, 0.992229531, 0.993347703, 0.993022409, 0.992927371, 0.990353891, 0.98534136, 0.977964157, 0.968209156, 0.956438149, 0.942562961, 0.927724566, 0.911915214, 0.895097576, 0.84499878, 0.798562566, 0.769568088, 0.707863301, 0.654037617, 0.562380546, 0.496026132}
				);

		ForwardCurve forwardCurve = ForwardCurve.createForwardCurveFromForwards("Libor6m",
				// Fixings of the forward
				new double[]{0.504109589, 1.504109589, 2.509589041, 3.506849315, 4.506849315, 5.506849315, 6.509589041, 7.515068493, 8.512328767, 9.509589041, 10.51232877, 11.51232877, 12.51232877, 13.51780822, 14.51506849, 15.51506849, 16.51506849, 17.51506849, 18.52328767, 19.52054795, 20.51780822, 21.51780822, 22.52054795, 23.52054795, 24.5260274, 25.52328767, 26.52328767, 27.52328767, 28.52328767, 29.52328767, 34.52876712, 39.53150685, 44.53424658, 49.5369863, 54.54246575, 59.54520548},
				// Forward Rates
				new double[]{-0.002630852, -6.82E-04, 0.002757708, 0.005260602, 0.007848164, 0.010749576, 0.012628982, 0.014583704, 0.017103188, 0.017791957, 0.01917447, 0.019788258, 0.020269155, 0.02327218, 0.01577317, 0.026503375, 0.017980753, 0.016047889, 0.024898978, 0.010798547, 0.027070148, 0.014816786, 0.018220786, 0.016549747, 0.008028913, 0.020022068, 0.015134412, 0.016604122, 0.014386016, 0.026732673, 0.003643934, 0.024595029, 0.002432369, 0.02233176, 0.003397059, 0.020576206},
				0.5/* tenor / period length */);

		LIBORModelMonteCarloSimulationInterface model = createLIBORMarketModel(false, randomVariableFactory, numberOfPaths, 1 /*numberOfFactors*/,
				discountCurve,
				forwardCurve);

		LIBORModelMonteCarloSimulationInterface zeroVolatilityModel = getZeroVolatilityModel(model);


		/*
		 *  Create Products.
		 */
		// 1) Swap Input
		double startTime = 0.0;    // Exercise date
		double constantSwapRateSwap = 0.013;
		int numberOfPeriodsSwap = 20;
		double notionalSwap = 100;
		double[] fixingDatesSwap = new double[numberOfPeriodsSwap];
		double[] paymentDatesSwap = new double[numberOfPeriodsSwap];
		double[] swapRatesSwap = new double[numberOfPeriodsSwap];

		// Fill data
		fixingDatesSwap = IntStream.range(0, fixingDatesSwap.length).mapToDouble(i -> startTime + i * 0.5).toArray();
		paymentDatesSwap = IntStream.range(0, paymentDatesSwap.length).mapToDouble(i -> startTime + (i + 1) * 0.5).toArray();
		Arrays.fill(swapRatesSwap, constantSwapRateSwap);

		// 2) Swaption Input
		double exerciseTime = 8.0;
		int numberOfPeriods = 12;
		double notional = 100;
		double[] fixingDates = new double[numberOfPeriods];
		double[] paymentDates = new double[numberOfPeriods];
		double[] periodLength = new double[paymentDates.length];
		double[] periodNotionals = new double[periodLength.length];
		double[] swapRates = new double[numberOfPeriods];
		double[] swapTenor = new double[numberOfPeriods + 1];

		// Set values
		fixingDates = IntStream.range(0, fixingDates.length).mapToDouble(i -> exerciseTime + i * 0.5).toArray();
		paymentDates = IntStream.range(0, paymentDates.length).mapToDouble(i -> exerciseTime + (i + 1) * 0.5).toArray();
		swapTenor = IntStream.range(0, numberOfPeriods + 1).mapToDouble(i -> exerciseTime + i * 0.5).toArray();
		Arrays.fill(periodLength, 0.5);
		Arrays.fill(periodNotionals, notional);
		Arrays.fill(swapRates, getParSwaprate(forwardCurve, discountCurve, swapTenor)); // 0.0193

		// 3) Bermudan Input
		ExerciseType bermudanExerciseType = ExerciseType.Callable; // Callable or Cancelable
		double exerciseTimeB = 8.0;
		int numberOfPeriodsB = 20;
		double constantSwapRateBermudan = 0.02;
		double notionalB = 100;
		double[] fixingDatesB = new double[numberOfPeriodsB];
		double[] paymentDatesB = new double[numberOfPeriodsB];
		double[] periodLengthB = new double[paymentDatesB.length];
		double[] periodNotionalsB = new double[periodLengthB.length];
		double[] swapRatesB = new double[numberOfPeriodsB];
		double[] swapTenorB = new double[numberOfPeriodsB + 1];
		boolean[] isPeriodStartDateExerciseDate = new boolean[periodLengthB.length]; // for Bermudan

		// Set values
		fixingDatesB = IntStream.range(0, fixingDatesB.length).mapToDouble(i -> exerciseTimeB + i * 0.5).toArray();
		paymentDatesB = IntStream.range(0, paymentDatesB.length).mapToDouble(i -> exerciseTimeB + (i + 1) * 0.5).toArray();
		swapTenorB = IntStream.range(0, numberOfPeriodsB + 1).mapToDouble(i -> exerciseTimeB + i * 0.5).toArray();
		Arrays.fill(periodLengthB, 0.5);
		Arrays.fill(periodNotionalsB, notionalB);
		Arrays.fill(swapRatesB, constantSwapRateBermudan);//getParSwaprate(forwardCurve, discountCurve, swapTenorB));
		Arrays.fill(isPeriodStartDateExerciseDate, false);
		isPeriodStartDateExerciseDate[0] = true;
		isPeriodStartDateExerciseDate[4] = true;
		isPeriodStartDateExerciseDate[8] = true;
		isPeriodStartDateExerciseDate[12] = true;
		isPeriodStartDateExerciseDate[16] = true;

		// Second Swaption
		double exerciseTime2 = 5.0;    // Exercise date
		double constantSwapRate2 = 0.017;
		double notional2 = 100;
		double[] fixingDates2 = new double[numberOfPeriods];
		double[] paymentDates2 = new double[numberOfPeriods];
		double[] periodLength2 = new double[paymentDates.length];
		double[] periodNotionals2 = new double[periodLength.length];
		double[] swapRates2 = new double[numberOfPeriods];

		// Set values
		fixingDates2 = IntStream.range(0, fixingDates2.length).mapToDouble(i -> exerciseTime2 + i * 0.5).toArray();
		paymentDates2 = IntStream.range(0, paymentDates2.length).mapToDouble(i -> exerciseTime2 + (i + 1) * 0.5).toArray();
		Arrays.fill(periodLength2, 0.5);
		Arrays.fill(periodNotionals2, notional2);
		Arrays.fill(swapRates2, constantSwapRate2);


		/*
		 *  Create SIMMProducts and a SIMMPortfolio
		 */
		AbstractSIMMProduct SIMMSwap = new SIMMSimpleSwap(fixingDatesSwap, paymentDatesSwap, swapRatesSwap, true /*isPayFix*/, notionalSwap, new String[]{"OIS", "Libor6m"}, "EUR");

		AbstractSIMMProduct SIMMSwaption = new SIMMSwaption(exerciseTime, fixingDates, paymentDates, swapRates, notional,
				DeliveryType.Physical, new String[]{"OIS", "Libor6m"}, "EUR");

		AbstractSIMMProduct SIMMSwaption2 = new SIMMSwaption(exerciseTime2, fixingDates2, paymentDates2, swapRates2, notional2,
				DeliveryType.CashSettled, new String[]{"OIS", "Libor6m"}, "EUR");

		AbstractSIMMProduct SIMMBermudan = new SIMMBermudanSwaption(fixingDatesB, periodLengthB, paymentDatesB, periodNotionalsB,
				swapRatesB, isPeriodStartDateExerciseDate, bermudanExerciseType, new String[]{"OIS", "Libor6m"}, "EUR");

		SIMMPortfolio SIMMPortfolio = new SIMMPortfolio(new AbstractSIMMProduct[]{SIMMSwaption, SIMMSwap}, "EUR");

		/*
		 *  Set calculation parameters
		 */
		WeightMode weightMode = WeightMode.TIMEDEPENDENT; // TimeDependent or Constant
		double timeStep = 0.1;
		double interpolationStep = 1.0;
		boolean isUseAnalyticSwapSensis = false;
		boolean isConsiderOISSensis = true;

		// Final IM times
		double finalIMTimeSwap = model.getLiborPeriodDiscretization().getTimeStep(0) * numberOfPeriodsSwap;
		double finalIMTimeSwaption = exerciseTime + model.getLiborPeriodDiscretization().getTimeStep(0) * numberOfPeriods;
		double finalIMTimeBermudan = exerciseTimeB + model.getLiborPeriodDiscretization().getTimeStep(0) * numberOfPeriodsB;
		double finalIMTimePortfolio = Math.max(finalIMTimeSwaption, finalIMTimeSwap);

		// time measurement variables
		long timeStart;
		long timeEnd;


		/*
		 * Perform calculations
		 */

		// Portfolio

		if (isCalculatePortfolio) {
			RandomVariable[][] valuesPortfolio = new RandomVariable[3][(int) (finalIMTimePortfolio / timeStep) + 1];

			timeStart = System.currentTimeMillis();
			for (int i = 0; i < finalIMTimePortfolio / timeStep + 1; i++) {
				valuesPortfolio[0][i] = SIMMPortfolio.getInitialMargin(i * timeStep, model, "EUR", SensitivityMode.EXACT, WeightMode.TIMEDEPENDENT, 1.0, isUseAnalyticSwapSensis, isConsiderOISSensis);
			}
			timeEnd = System.currentTimeMillis();

			System.out.println("Time for Portfolio, Exact: " + formatterTime.format((timeEnd - timeStart) / 1000.0) + " s");

			timeStart = System.currentTimeMillis();
			for (int i = 0; i < finalIMTimePortfolio / timeStep + 1; i++) {
				valuesPortfolio[1][i] = SIMMPortfolio.getInitialMargin(i * timeStep, model, "EUR", SensitivityMode.MELTINGSIMMBUCKETS, WeightMode.TIMEDEPENDENT, 1.0, isUseAnalyticSwapSensis, isConsiderOISSensis);
			}
			timeEnd = System.currentTimeMillis();

			System.out.println("Time for Portfolio, Melting: " + formatterTime.format((timeEnd - timeStart) / 1000.0) + " s");

			timeStart = System.currentTimeMillis();
			for (int i = 0; i < finalIMTimePortfolio / timeStep + 1; i++) {
				valuesPortfolio[2][i] = SIMMPortfolio.getInitialMargin(i * timeStep, model, "EUR", SensitivityMode.INTERPOLATION, WeightMode.TIMEDEPENDENT, interpolationStep, isUseAnalyticSwapSensis, isConsiderOISSensis);
			}
			timeEnd = System.currentTimeMillis();

			System.out.println("Time for Portfolio, Interpolation: " + formatterTime.format((timeEnd - timeStart) / 1000.0) + " s");

			if (isPrintAverage) {
				System.out.println("Expected Forward IM for Portfolio");
				System.out.println("Exact" + "\t" + "Melting " + "\t" + "Interpolation");
				for (int i = 0; i < finalIMTimePortfolio / timeStep + 1; i++) {
					System.out.println(valuesPortfolio[0][i].getAverage() + "\t" + valuesPortfolio[1][i].getAverage() + "\t" + valuesPortfolio[2][i].getAverage());
				}
			}
			if (isPrintQuantile) {
				System.out.println("Quantiles Forward IM for Portfolio");
				System.out.println("Upper Bound" + "\t" + "Lower Bound");
				for (int i = 0; i < finalIMTimePortfolio / timeStep + 1; i++) {
					System.out.println(valuesPortfolio[0][i].getQuantile(upperQuantile) + "\t" + valuesPortfolio[0][i].getQuantile(lowerQuantile));
				}
			}
			if (isPrintPaths) {
				System.out.println("Some paths of Forward IM for Portfolio");

				for (int i = 0; i < finalIMTimePortfolio / timeStep + 1; i++) {
					for (int j = 0; j < 10; j++) {

						System.out.print(valuesPortfolio[0][i].get(j) + "\t");
					}
					System.out.println();
				}
			}
		}

		// Swap

		if (isCalculateSwap) {
			RandomVariable[][] valuesSwap = new RandomVariable[4][(int) (finalIMTimeSwap / timeStep) + 1];

			timeStart = System.currentTimeMillis();
			for (int i = 0; i < finalIMTimeSwap / timeStep + 1; i++) {
				valuesSwap[0][i] = SIMMSwap.getInitialMargin(i * timeStep, model, "EUR", SensitivityMode.EXACT, weightMode, 1.0, isUseAnalyticSwapSensis, isConsiderOISSensis);
			}
			timeEnd = System.currentTimeMillis();

			System.out.println("Time for SWAP, Exact: " + formatterTime.format((timeEnd - timeStart) / 1000.0) + " s");

			timeStart = System.currentTimeMillis();
			for (int i = 0; i < finalIMTimeSwap / timeStep + 1; i++) {
				valuesSwap[1][i] = SIMMSwap.getInitialMargin(i * timeStep, model, "EUR", SensitivityMode.MELTINGSIMMBUCKETS, weightMode, 1.0, isUseAnalyticSwapSensis, isConsiderOISSensis);
			}
			timeEnd = System.currentTimeMillis();

			System.out.println("Time for SWAP, Melting: " + formatterTime.format((timeEnd - timeStart) / 1000.0) + "s");

			timeStart = System.currentTimeMillis();
			for (int i = 0; i < finalIMTimeSwap / timeStep + 1; i++) {
				valuesSwap[2][i] = SIMMSwap.getInitialMargin(i * timeStep, model, "EUR", SensitivityMode.INTERPOLATION, weightMode, interpolationStep, isUseAnalyticSwapSensis, isConsiderOISSensis);
			}
			timeEnd = System.currentTimeMillis();

			System.out.println("Time for SWAP, Interpolation with step " + interpolationStep + ": " + formatterTime.format((timeEnd - timeStart) / 1000.0) + "s");

			timeStart = System.currentTimeMillis();
			for (int i = 0; i < finalIMTimeSwap / timeStep + 1; i++) {
				valuesSwap[3][i] = SIMMSwap.getInitialMargin(i * timeStep, zeroVolatilityModel, "EUR", SensitivityMode.EXACT, weightMode, interpolationStep, isUseAnalyticSwapSensis, isConsiderOISSensis);
			}
			timeEnd = System.currentTimeMillis();

			System.out.println("Time for SWAP, one path ageing " + formatterTime.format((timeEnd - timeStart) / 1000.0) + "s");

			if (isPrintAverage) {
				System.out.println("Expected Forward IM for Swap");
				System.out.println("Exact" + "\t" + "\t" + "Melting " + "\t" + "Interpolation" + "\t" + "OnePathAgeing");
				for (int i = 0; i < finalIMTimeSwap / timeStep + 1; i++) {
					System.out.println(valuesSwap[0][i].getAverage() + "\t" + valuesSwap[1][i].getAverage() + "\t" + valuesSwap[2][i].getAverage() + "\t" + valuesSwap[3][i].getAverage());
				}
			}
			if (isPrintQuantile) {
				System.out.println("Quantiles Forward IM for Swap");
				System.out.println("Upper Bound" + "\t" + "Lower Bound");
				for (int i = 0; i < finalIMTimeSwap / timeStep + 1; i++) {
					System.out.println(valuesSwap[0][i].getQuantile(upperQuantile) + "\t" + valuesSwap[0][i].getQuantile(lowerQuantile));
				}
			}
			if (isPrintPaths) {
				System.out.println("Some paths of Forward IM for Swap");

				for (int i = 0; i < finalIMTimeSwap / timeStep + 1; i++) {
					for (int j = 0; j < 10; j++) {

						System.out.print(valuesSwap[0][i].get(j) + "\t");
					}
					System.out.println();
				}
			}
		}

		// Swaption

		if (isCalculateSwaption) {
			RandomVariable[][] valuesSwaption = new RandomVariable[3][(int) (finalIMTimeSwaption / timeStep) + 1];

			timeStart = System.currentTimeMillis();
			for (int i = 0; i < finalIMTimeSwaption / timeStep + 1; i++) {
				valuesSwaption[0][i] = SIMMSwaption.getInitialMargin(i * timeStep, model, "EUR", SensitivityMode.EXACT, weightMode, 1.0, isUseAnalyticSwapSensis, isConsiderOISSensis);
			}
			timeEnd = System.currentTimeMillis();

			System.out.println("Time for SWAPTION, Exact: " + formatterTime.format((timeEnd - timeStart) / 1000.0) + "s");

			timeStart = System.currentTimeMillis();
			for (int i = 0; i < finalIMTimeSwaption / timeStep + 1; i++) {
				valuesSwaption[1][i] = SIMMSwaption.getInitialMargin(i * timeStep, model, "EUR", SensitivityMode.MELTINGSIMMBUCKETS, weightMode, 1.0, isUseAnalyticSwapSensis, isConsiderOISSensis);
			}
			timeEnd = System.currentTimeMillis();

			System.out.println("Time for SWAPTION, Melting: " + formatterTime.format((timeEnd - timeStart) / 1000.0) + "s");

			timeStart = System.currentTimeMillis();
			for (int i = 0; i < finalIMTimeSwaption / timeStep + 1; i++) {
				valuesSwaption[2][i] = SIMMSwaption.getInitialMargin(i * timeStep, model, "EUR", SensitivityMode.INTERPOLATION, weightMode, interpolationStep, isUseAnalyticSwapSensis, isConsiderOISSensis);
			}
			timeEnd = System.currentTimeMillis();

			System.out.println("Time for SWAPTION, Interpolation with step " + interpolationStep + ": " + formatterTime.format((timeEnd - timeStart) / 1000.0) + "s");

			if (isPrintAverage) {
				System.out.println("Expected Forward IM for Swaption");
				System.out.println("Exact, constant weights" + "\t" + "Melting " + "\t" + "Interpolation" + "\t");
				for (int i = 0; i < finalIMTimeSwaption / timeStep + 1; i++) {
					System.out.println(valuesSwaption[0][i].getAverage() + "\t" + valuesSwaption[1][i].getAverage() + "\t" + valuesSwaption[2][i].getAverage());
				}
			}
			if (isPrintQuantile) {
				System.out.println("Quantiles Forward IM for Swaption");
				System.out.println("Upper Bound" + "\t" + "Lower Bound");
				for (int i = 0; i < finalIMTimeSwaption / timeStep + 1; i++) {
					System.out.println(valuesSwaption[0][i].getQuantile(upperQuantile) + "\t" + valuesSwaption[0][i].getQuantile(lowerQuantile));
				}
			}
			if (isPrintPaths) {
				System.out.println("Some paths of Forward IM for Swaption");

				for (int i = 0; i < finalIMTimeSwaption / timeStep + 1; i++) {
					for (int j = 0; j < 10; j++) {

						System.out.print(valuesSwaption[0][i].get(j) + "\t");
					}
					System.out.println();
				}
			}
		}

		// Bermudan

		if (isCalculateBermudan) {
			RandomVariable[][] valuesBermudan = new RandomVariable[3][(int) (finalIMTimeBermudan / timeStep) + 1];

			timeStart = System.currentTimeMillis();
			for (int i = 0; i < finalIMTimeBermudan / timeStep + 1; i++) {
				valuesBermudan[0][i] = SIMMBermudan.getInitialMargin(i * timeStep, model, "EUR", SensitivityMode.EXACT, weightMode, 1.0, true, isConsiderOISSensis);
			}
			timeEnd = System.currentTimeMillis();

			System.out.println("Time for BERMUDAN, Exact: " + formatterTime.format((timeEnd - timeStart) / 1000.0) + "s");

			timeStart = System.currentTimeMillis();
			for (int i = 0; i < finalIMTimeBermudan / timeStep + 1; i++) {
				valuesBermudan[1][i] = SIMMBermudan.getInitialMargin(i * timeStep, model, "EUR", SensitivityMode.MELTINGSIMMBUCKETS, weightMode, 1.0, true, isConsiderOISSensis);
			}
			timeEnd = System.currentTimeMillis();

			System.out.println("Time for BERMUDAN, Melting: " + formatterTime.format((timeEnd - timeStart) / 1000.0) + "s");

			timeStart = System.currentTimeMillis();
			for (int i = 0; i < finalIMTimeBermudan / timeStep + 1; i++) {
				valuesBermudan[2][i] = SIMMBermudan.getInitialMargin(i * timeStep, model, "EUR", SensitivityMode.INTERPOLATION, weightMode, interpolationStep, true, isConsiderOISSensis);
			}
			timeEnd = System.currentTimeMillis();

			System.out.println("Time for BERMUDAN, Interpolation with step " + interpolationStep + ": " + formatterTime.format((timeEnd - timeStart) / 1000.0) + "s");

			if (isPrintAverage) {
				System.out.println("Expected Forward IM for Bermudan");
				System.out.println("Exact" + "\t" + "Melting " + "\t" + "Interpolation");
				for (int i = 0; i < finalIMTimeBermudan / timeStep + 1; i++) {
					System.out.println(valuesBermudan[0][i].getAverage() + "\t" + valuesBermudan[1][i].getAverage() + "\t" + valuesBermudan[2][i].getAverage());
				}
			}
			if (isPrintQuantile) {
				System.out.println("Quantiles Forward IM for Bermudan");
				System.out.println("Upper Bound" + "\t" + "Lower Bound");
				for (int i = 0; i < finalIMTimeBermudan / timeStep + 1; i++) {
					System.out.println(valuesBermudan[0][i].getQuantile(upperQuantile) + "\t" + valuesBermudan[0][i].getQuantile(lowerQuantile));
				}
			}
			if (isPrintPaths) {
				System.out.println("Some paths of Forward IM for Bermudan");

				for (int i = 0; i < finalIMTimeBermudan / timeStep + 1; i++) {
					for (int j = 0; j < 20; j++) {

						System.out.print(valuesBermudan[0][i].get(j) + "\t");
					}
					System.out.println();
				}
			}
		}
	}

	public static LIBORModelMonteCarloSimulationInterface createLIBORMarketModel(boolean isUseTenorRefinement,
			AbstractRandomVariableFactory randomVariableFactory,
			int numberOfPaths, int numberOfFactors, DiscountCurveInterface discountCurve, ForwardCurve forwardCurve) throws CalculationException {
		return createLIBORMarketModel(isUseTenorRefinement, randomVariableFactory, numberOfPaths, numberOfFactors, discountCurve, forwardCurve, 0.1);
	}

	public static LIBORModelMonteCarloSimulationInterface createLIBORMarketModel(boolean isUseTenorRefinement,
			AbstractRandomVariableFactory randomVariableFactory,
			int numberOfPaths, int numberOfFactors, DiscountCurveInterface discountCurve, ForwardCurve forwardCurve, double simulationTimeDt) throws CalculationException {

		/*
		 * Create a simulation time discretization
		 */
		// If simulation time is below libor time, exceptions will be hard to track.
		double lastTime = 30.0;
		double dt = simulationTimeDt;
		TimeDiscretizationFromArray timeDiscretizationFromArray = new TimeDiscretizationFromArray(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create the libor tenor structure and the initial values
		 */
		double liborPeriodLength = 0.5;
		double liborRateTimeHorzion = 30.0;
		TimeDiscretizationFromArray liborPeriodDiscretization = new TimeDiscretizationFromArray(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		/*
		 * Create Brownian motions
		 */
		final BrownianMotion brownianMotion = new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray, numberOfFactors, numberOfPaths, 31415 /* seed */, new RandomVariableFactory(false));

		// Create a volatility model: Piecewise constant volatility calibrated to Swaption Normal implied volatility of December 8, 2017
		double[] volatility = new double[]{
				0.0035380523915104246,
				0.004191317634739811,
				0.008841173374561527,
				0.010367178689341235,
				0.009683514692001837,
				0.00881065062410322,
				0.005,
				0.005553622644567465,
				0.006240047498020553,
				0.008993528127078064,
				0.009894813615533201,
				0.009632854002962725,
				0.009785740680837035,
				0.0037906111648575865,
				0.014616121514330995,
				0.011590302354861921,
				0.012136753578600236,
				0.009878601075748226,
				0.008283683236194047,
				0.01158663971536579,
				0.011596322104781735,
				0.010557210170556731,
				0.011936780200631499,
				0.007661672888823457,
				0.003682948768971966,
				0.004960044546431093,
				0.01198892262469119,
				0.007086263635340348,
				0.01173222179819021,
				0.008696172864293873,
				0.005,
				0.007599201382279569,
				0.007148972951937137,
				0.006788680889933558,
				0.011140027803944855,
				0.005,
				0.005};

		//		LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelPiecewiseConstantLegacy(randomVariableFactory,timeDiscretizationFromArray, liborPeriodDiscretization, new TimeDiscretizationFromArray(0.00, 1.0, 2.0, 5.0, 10.0, 20.0, 30.0), new TimeDiscretizationFromArray(0.00, 1.0, 2.0, 5.0, 10.0, 20.0, 30.0), volatility, false);
		LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelPiecewiseConstant(randomVariableFactory, timeDiscretizationFromArray, liborPeriodDiscretization, new TimeDiscretizationFromArray(0.00, 1.0, 2.0, 5.0, 10.0, 20.0, 30.0), new TimeDiscretizationFromArray(0.00, 1.0, 2.0, 5.0, 10.0, 20.0, 30.0), volatility, false);
		//		LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelPiecewiseConstant(randomVariableFactory,timeDiscretizationFromArray, liborPeriodDiscretization, new TimeDiscretizationFromArray(0.00, 1.0, 2.0, 5.0, 10.0, 20.0, 30.0), new TimeDiscretizationFromArray(0.00, 1.0, 2.0, 5.0, 10.0, 20.0, 30.0), /*new double[]{0.01}*/volatility, false);
		//		LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelPiecewiseConstant(randomVariableFactory,timeDiscretizationFromArray, liborPeriodDiscretization, new TimeDiscretizationFromArray(0.00, 1.0, 2.0, 5.0, 10.0, 20.0, 30.0), new TimeDiscretizationFromArray(0.00, 1.0, 2.0, 5.0, 10.0, 20.0, 30.0), new double[]{0.00}, false);

		// Create a correlation model
		LIBORCorrelationModel correlationModel = new LIBORCorrelationModelExponentialDecay(timeDiscretizationFromArray, liborPeriodDiscretization, numberOfFactors, 0.04 /*correlationDecayParameter*/, false);

		// Create a covariance model
		AbstractLIBORCovarianceModelParametric covarianceModelParametric = new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretizationFromArray, liborPeriodDiscretization, volatilityModel, correlationModel);

		// Create blended local volatility model with fixed parameter 0.0 (that is "lognormal").
		double displacementParameter = 0.5880313623110442;
		AbstractLIBORCovarianceModelParametric covarianceModelBlended = new BlendedLocalVolatilityModel(randomVariableFactory, covarianceModelParametric, displacementParameter, false);

		// Set model properties
		Map<String, Object> properties = new HashMap<String, Object>();

		// Choose the simulation measure
		properties.put("measure", LIBORMarketModelFromCovarianceModel.Measure.SPOT.name());

		// Choose normal state space for the Euler scheme (the covariance model above carries a linear local volatility model, such that the resulting model is log-normal).
		properties.put("stateSpace", LIBORMarketModelFromCovarianceModel.StateSpace.NORMAL.name());

		// Empty array of calibration items - hence, model will use given covariance
		CalibrationProduct[] calibrationItems = new CalibrationProduct[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */

		EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(brownianMotion, EulerSchemeFromProcessModel.Scheme.EULER_FUNCTIONAL);

		LIBORMarketModel liborMarketModel = new LIBORMarketModelFromCovarianceModel(liborPeriodDiscretization, new AnalyticModel(new CurveInterface[]{new DiscountCurveFromForwardCurve(forwardCurve), discountCurve}), forwardCurve, discountCurve, randomVariableFactory, covarianceModelBlended, calibrationItems, properties);

		return new LIBORModelMonteCarloSimulation(liborMarketModel, process);
	}

	public static AbstractRandomVariableFactory createRandomVariableFactoryAAD() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("isGradientRetainsLeafNodesOnly", new Boolean(false));
		return new RandomVariableDifferentiableAADFactory(new RandomVariableFactory(false), properties);
	}

	public static RandomVariable[] getRVAAD(double[] rates) {
		RandomVariable[] rv = new RandomVariable[rates.length];
		for (int i = 0; i < rv.length; i++) {
			rv[i] = new RandomVariableDifferentiableAAD(rates[i]);
		}
		return rv;
	}

	public static LIBORModelMonteCarloSimulationInterface getZeroVolatilityModel(LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		AbstractRandomVariableFactory randomVariableFactory = createRandomVariableFactoryAAD();

		// Set brownian motion with one path
		BrownianMotion originalBM = model.getBrownianMotion();
		BrownianMotion brownianMotion = new BrownianMotionLazyInit(originalBM.getTimeDiscretization(), originalBM.getNumberOfFactors(), 1 /* numberOfPaths */, 3141);

		// Get process
		EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(brownianMotion, EulerSchemeFromProcessModel.Scheme.EULER_FUNCTIONAL);

		// Create zero volatility model
		LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelPiecewiseConstant(randomVariableFactory, model.getTimeDiscretization(), model.getLiborPeriodDiscretization(), new TimeDiscretizationFromArray(0.00, 1.0, 2.0, 5.0, 10.0, 20.0, 30.0), new TimeDiscretizationFromArray(0.00, 1.0, 2.0, 5.0, 10.0, 20.0, 30.0), new double[]{0.0}/*volatility*/, false);

		//Create a correlation model rho_{i,j} = exp(-a * abs(T_i-T_j))
		LIBORCorrelationModelExponentialDecay correlationModel = new LIBORCorrelationModelExponentialDecay(model.getTimeDiscretization(), model.getLiborPeriodDiscretization(), model.getNumberOfFactors(), 0);

		//Combine volatility model and correlation model to a covariance model
		LIBORCovarianceModelFromVolatilityAndCorrelation covarianceModel =
				new LIBORCovarianceModelFromVolatilityAndCorrelation(model.getTimeDiscretization(),
						model.getLiborPeriodDiscretization(), volatilityModel, correlationModel);

		AbstractLIBORCovarianceModelParametric covarianceModelBlended = new BlendedLocalVolatilityModel(covarianceModel, 0.0/*displacementParameter*/, false);

		Map<String, Object> dataModified = new HashMap<>();
		dataModified.put("covarianceModel", covarianceModelBlended);
		return new LIBORModelMonteCarloSimulation((LIBORModel) model.getModel().getCloneWithModifiedData(dataModified), process);
	}

	public static Map<String, Object> getModelPropertiesMap(LIBORMarketModelFromCovarianceModel.Measure measure, LIBORMarketModelFromCovarianceModel.StateSpace stateSpace) {
		Map<String, Object> properties = new HashMap<String, Object>();

		// simulation measure
		properties.put("measure", measure.name());

		// Choose normal state space for the Euler scheme since we use a blended local volatiltiy model
		properties.put("stateSpace", stateSpace.name());

		return properties;
	}

	public static double getParSwaprate(ForwardCurveInterface forwardCurve, DiscountCurveInterface discountCurve, double[] swapTenor) {
		return net.finmath.marketdata.products.Swap.getForwardSwapRate(new TimeDiscretizationFromArray(swapTenor), new TimeDiscretizationFromArray(swapTenor), forwardCurve, discountCurve);
	}

	/*
	 *
	 *
	 *  Some functions for LMM Calibration (used in the spreadsheet SIMMProductTest)
	 *
	 *
	 */
	public static CalibrationProduct createCalibrationItem(double weight, double exerciseDate, double swapPeriodLength, int numberOfPeriods, double moneyness, double targetVolatility, String targetVolatilityType, ForwardCurveInterface forwardCurve, DiscountCurveInterface discountCurve) throws CalculationException {

		double[] fixingDates = new double[numberOfPeriods];
		double[] paymentDates = new double[numberOfPeriods];
		double[] swapTenor = new double[numberOfPeriods + 1];

		for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
			fixingDates[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
			paymentDates[periodStartIndex] = exerciseDate + (periodStartIndex + 1) * swapPeriodLength;
			swapTenor[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
		}
		swapTenor[numberOfPeriods] = exerciseDate + numberOfPeriods * swapPeriodLength;

		// Swaptions swap rate
		double swaprate = moneyness + getParSwaprate(forwardCurve, discountCurve, swapTenor);

		// Set swap rates for each period
		double[] swaprates = new double[numberOfPeriods];
		Arrays.fill(swaprates, swaprate);

		/*
		 * We use Monte-Carlo calibration on implied volatility.
		 * Alternatively you may change here to Monte-Carlo valuation on price or
		 * use an analytic approximation formula, etc.
		 */
		SwaptionSimple swaptionMonteCarlo = new SwaptionSimple(swaprate, swapTenor, SwaptionSimple.ValueUnit.valueOf(targetVolatilityType));
		//		double targetValuePrice = AnalyticFormulas.blackModelSwaptionValue(swaprate, targetVolatility, fixingDates[0], swaprate, getSwapAnnuity(discountCurve, swapTenor));
		return new CalibrationProduct(swaptionMonteCarlo, targetVolatility, weight);
	}

	public static CalibrationProduct[] createCalibrationItems(ForwardCurveInterface forwardCurve, DiscountCurveInterface discountCurve, String[] atmExpiries, String[] atmTenors, double[] atmNormalVolatilities, LocalDate referenceDate, AbstractBusinessdayCalendar cal, DayCountConvention modelDC, double swapPeriodLength) throws CalculationException {

		final ArrayList<CalibrationProduct> calibrationProducts = new ArrayList<CalibrationProduct>();

		for (int i = 0; i < atmNormalVolatilities.length; i++) {

			LocalDate exerciseDate = cal.getDateFromDateAndOffsetCode(referenceDate, atmExpiries[i]);
			LocalDate tenorEndDate = cal.getDateFromDateAndOffsetCode(exerciseDate, atmTenors[i]);
			double exercise = modelDC.getDaycountFraction(referenceDate, exerciseDate);
			double tenor = modelDC.getDaycountFraction(exerciseDate, tenorEndDate);

			// We consider an idealized tenor grid (alternative: adapt the model grid)
			exercise = Math.round(exercise / 0.25) * 0.25;
			tenor = Math.round(tenor / 0.25) * 0.25;

			if (exercise < 1.0) {
				continue;
			}

			int numberOfPeriods = (int) Math.round(tenor / swapPeriodLength);

			double moneyness = 0.0;
			double targetVolatility = atmNormalVolatilities[i];

			String targetVolatilityType = "VOLATILITYNORMAL";

			double weight = 1.0;

			calibrationProducts.add(createCalibrationItem(weight, exercise, swapPeriodLength, numberOfPeriods, moneyness, targetVolatility, targetVolatilityType, forwardCurve, discountCurve));
		}
		CalibrationProduct[] calibrationItemsLMM = new CalibrationProduct[calibrationProducts.size()];
		for (int i = 0; i < calibrationProducts.size(); i++) {
			calibrationItemsLMM[i] = new CalibrationProduct(calibrationProducts.get(i).getProduct(), calibrationProducts.get(i).getTargetValue(), calibrationProducts.get(i).getWeight());
		}

		return calibrationItemsLMM;
	}

	public static Map<String, Object> getModelCalibrationPropertiesMap(double accuracy, double parameterStep, int maxIterations, int numberOfThreads, BrownianMotion brownianMotion) {
		// Set model properties
		Map<String, Object> properties = new HashMap<String, Object>();

		// Choose the simulation measure
		properties.put("measure", LIBORMarketModelFromCovarianceModel.Measure.SPOT.name());

		// Choose normal state space for the Euler scheme (the covariance model above carries a linear local volatility model, such that the resulting model is log-normal).
		properties.put("stateSpace", LIBORMarketModelFromCovarianceModel.StateSpace.NORMAL.name());

		// Set calibration properties (should use our brownianMotion for calibration - needed to have to right correlation).
		OptimizerFactory optimizerFactory = new OptimizerFactoryLevenbergMarquardt(maxIterations, accuracy, numberOfThreads);

		// Set calibration properties (should use our brownianMotion for calibration - needed to have to right correlation).
		Map<String, Object> calibrationParameters = new HashMap<String, Object>();
		calibrationParameters.put("accuracy", new Double(accuracy));
		calibrationParameters.put("brownianMotion", brownianMotion);
		calibrationParameters.put("optimizerFactory", optimizerFactory);
		calibrationParameters.put("parameterStep", new Double(parameterStep));
		properties.put("calibrationParameters", calibrationParameters);

		return properties;
	}

	public static double[] getCalibratedParameters(LIBORModel liborMarketModelCalibrated) {
		return ((AbstractLIBORCovarianceModelParametric) ((LIBORMarketModelFromCovarianceModel) liborMarketModelCalibrated).getCovarianceModel()).getParameter();
	}

	public static double[] getTargetValuesUnderCalibratedModel(LIBORModel liborMarketModelCalibrated, BrownianMotion brownianMotion, CalibrationProduct[] calibrationItems) {
		EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(brownianMotion);
		LIBORModelMonteCarloSimulationInterface simulationCalibrated = new LIBORModelMonteCarloSimulation(liborMarketModelCalibrated, process);

		double[] valueModel = new double[calibrationItems.length];
		for (int i = 0; i < calibrationItems.length; i++) {
			AbstractLIBORMonteCarloProduct calibrationProduct = calibrationItems[i].getProduct();
			try {
				valueModel[i] = calibrationProduct.getValue(simulationCalibrated);
			} catch (Exception e) {
			}
		}
		return valueModel;
	}

	public static double[] getCalibratedVolatilities(LIBORModel liborMarketModelCalibrated) {
		double[] calibratedParameters = getCalibratedParameters(liborMarketModelCalibrated);
		double[] calibratedVols = new double[calibratedParameters.length - 2];
		for (int i = 0; i < calibratedVols.length; i++) {
			calibratedVols[i] = calibratedParameters[i];
		}
		return calibratedVols;
	}

	public static double getCalibratedBlendingParameter(LIBORModel liborMarketModelCalibrated) {
		double[] calibratedParameters = getCalibratedParameters(liborMarketModelCalibrated);
		return calibratedParameters[calibratedParameters.length - 1];
	}
}
