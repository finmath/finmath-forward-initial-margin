package net.finmath.lch.initialmargin.swapclear.libormodels;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.CurveName;
import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata.model.curves.CurveInterpolation.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationEntity;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationMethod;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.AbstractLIBORCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.models.covariance.BlendedLocalVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelPiecewiseConstant;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;



public class ModelFactory {
	
	// date up to which historical zero rate data is available
	public static final LocalDateTime REFERENCE_DATE =  LocalDate.of(2024,  Month.FEBRUARY, 5).atStartOfDay();
	public static final int DEFAULT_OBSERVATION_YEARS = 20;
	public static final double DEFAULT_MAX_TENOR = 60.0;
	private static final double LIBOR_PERIOD_LENGTH = 0.25;
	private static final double[] VOLATILITY_BUCKETS = { 0.0, 1.0, 2.0, 5.0, 10.0, 20.0, 30.0, 40.0 };
	private static final double CALIBRATED_MATURITY_HORIZON = 40.0;
	private static final double TIME_TICK_SIZE = 1.0 / (365.0 * 24.0 * 60.0 * 60.0);

	/**
	 * Baseline model with twenty years of weekday observations and rolling tenors
	 * up to sixty years. Full path simulation can require several GB of memory;
	 * use the explicit horizon overload for smaller experiments.
	 */
	public static LIBORModelMonteCarloSimulationModel getModel(CurveName curveName) throws CalculationException {
		return getModel(curveName, REFERENCE_DATE.toLocalDate().plusYears(DEFAULT_OBSERVATION_YEARS), DEFAULT_MAX_TENOR);
	}

	/** Uses the Monday-to-Friday calendar of the supplied historical workbook. */
	public static LIBORModelMonteCarloSimulationModel getModel(CurveName curveName, LocalDate simulationEndDate, double maxTenor) throws CalculationException {
		return getModel(curveName, simulationEndDate, maxTenor, new BusinessdayCalendarExcludingWeekends());
	}

	/**
	 * Creates a model covering observations through the specified date and
	 * maturities through observation time plus {@code maxTenor}. The calendar
	 * must also be used by the historical/simulated scenario sources.
	 *
	 * The supplied volatility parameters were calibrated on a forty-year maturity
	 * domain. Newly required cells use the last calibrated tenor bucket in their
	 * time row; time and tenor buckets beyond forty years are flat. This is an
	 * explicit extrapolation assumption, not a recalibration.
	 */
	public static LIBORModelMonteCarloSimulationModel getModel(CurveName curveName, LocalDate simulationEndDate, double maxTenor, BusinessdayCalendar calendar) throws CalculationException {
		return getModel(curveName, simulationEndDate, maxTenor, calendar, new RandomVariableFromArrayFactory());
	}

	/**
	 * Optional differentiable simulation for native-model sensitivity diagnostics.
	 * For AAD at future process nodes, supply a factory configured with
	 * {@code isGradientRetainsLeafNodesOnly=false}. Keeping the full simulation
	 * graph can require substantial memory; use an explicit, limited horizon.
	 * Local PAIRS zero-curve sensitivities do not require an AAD simulation.
	 */
	public static LIBORModelMonteCarloSimulationModel getModel(CurveName curveName, LocalDate simulationEndDate,
			double maxTenor, BusinessdayCalendar calendar, RandomVariableFactory randomVariableFactory) throws CalculationException {
		requirePositiveFinite(maxTenor, "Maximum tenor");
		TimeDiscretization observationTimes = createBusinessdayTimeDiscretization(REFERENCE_DATE.toLocalDate(), simulationEndDate, calendar);
		double observationHorizon = FloatingpointDate.getFloatingPointDateFromDate(REFERENCE_DATE.toLocalDate(), simulationEndDate);
		double maturityHorizon = Math.ceil((observationHorizon + maxTenor) / LIBOR_PERIOD_LENGTH) * LIBOR_PERIOD_LENGTH;
		// Include fixing times as well as observation dates: spot numéraires must
		// not obtain a fixing from a later Euler step.
		TimeDiscretization fixingTimes = new TimeDiscretizationFromArray(0.0, (int) Math.floor(observationHorizon / LIBOR_PERIOD_LENGTH), LIBOR_PERIOD_LENGTH);
		TimeDiscretization simulationTimes = observationTimes.union(fixingTimes);
		switch (curveName) {
		case EUR_EURIBOR_3M:
			final int numberOfPaths = 100;
			final int numberOfFactors = 1;
			final double[] zeroRateFixings = new double[] {0.0, 0.2493151, 0.4986301, 0.7479452, 1.0000000, 1.4986301, 2.0000000, 3.0000000, 4.0000000, 5.0000000, 6.0000000, 7.0000000, 8.0000000, 9.0000000, 10.0000000, 12.0000000, 15.0000000, 20.0000000, 25.0000000, 30.0000000, 35.0000000, 40.0000000, 50.0000000, 60.0000000};	
			final double[] zeroRates = new double[]{0.0, 0.039679454, 0.038092025, 0.036301049, 0.034411283, 0.03127043, 0.029134774, 0.026978123, 0.026084691, 0.02578301, 0.025724468, 0.025808707, 0.025986709, 0.026228892, 0.026532052, 0.02712804, 0.02767065, 0.027235979, 0.026004347, 0.024754381, 0.023677185, 0.022869038, 0.021163335, 0.019995148};
			double[] volatility = new double[] {0.018188757085591742, 0.023669136470620522, 0.018838312991427623, 0.017398721686694832, 0.016157134065584788, 0.012580974937947433, 0.03715426781325657, 0.005, 0.011789390800298272, 0.023835171110197697, 0.017407408943930395, 0.01647740433369315, 0.014694957571607822, 0.012702232360660029, -0.011628112608719269, 0.014145444499685772, 0.021064553112378974, 0.01704151002264071, 0.016478221525674207, 0.013046920133543867, 0.011028286318128655, 0.003453754464521545, -2.3004198146720653E-4, 0.027332804579421253, 0.014488506318889972, 0.013199204327977335, 0.008914059002711453, 0.008878468292101021, 0.012214531788801015, 0.004927497343985883, 0.030667639991621958, 0.01396130678001358, 0.0152163862643317, 0.008136714960180556, 0.002349377408815333, 0.005, -0.007070862361627261, -0.006351041032061278, 0.026574244710620715, 0.01169447786782754, 0.0042076388094646954, 0.005, 0.005, 0.005, 0.005, 0.005, 0.005, 0.005};
			double displacementParameter =  0.5077918637651763;
			final AnalyticModel curveModel = getAnalyticModel(curveName, zeroRateFixings, zeroRates);
			return createLIBORMarketModel(numberOfPaths, numberOfFactors, maturityHorizon, LIBOR_PERIOD_LENGTH, simulationTimes, curveModel, curveName, extendBaselineVolatility(volatility, maturityHorizon), displacementParameter, randomVariableFactory);
		// standard EUR curve should be used for Base & Floor IM
		// case EUR_EURIBOR: 
		default:
			throw new IllegalArgumentException("Curve " + curveName + " currently not implemented.");
		}

	}

	public static TimeDiscretization createBusinessdayTimeDiscretization(LocalDate referenceDate, LocalDate endDate) {
		return createBusinessdayTimeDiscretization(referenceDate, endDate, new BusinessdayCalendarExcludingWeekends());
	}

	/**
	 * Daily observation dates in ACT/365, plus the initial and terminal boundary
	 * even when either boundary is not a business day. Calendar holidays between
	 * these boundaries are included only if accepted by the supplied calendar.
	 */
	public static TimeDiscretization createBusinessdayTimeDiscretization(LocalDate referenceDate, LocalDate endDate, BusinessdayCalendar calendar) {
		Objects.requireNonNull(referenceDate, "Reference date");
		Objects.requireNonNull(endDate, "End date");
		Objects.requireNonNull(calendar, "Calendar");
		if (!endDate.isAfter(referenceDate)) {
			throw new IllegalArgumentException("Simulation end date must be after reference date.");
		}
		TreeSet<Double> times = new TreeSet<>();
		times.add(0.0);
		for (LocalDate date = referenceDate.plusDays(1); !date.isAfter(endDate); date = date.plusDays(1)) {
			if (calendar.isBusinessday(date) || date.equals(endDate)) {
				times.add(FloatingpointDate.getFloatingPointDateFromDate(referenceDate, date));
			}
		}
		return new TimeDiscretizationFromArray(times, TIME_TICK_SIZE);
	}

	private static double[] extendBaselineVolatility(double[] calibratedVolatility, double maturityHorizon) {
		List<Double> extended = new ArrayList<>();
		int calibratedIndex = 0;
		for (double time : VOLATILITY_BUCKETS) {
			double lastCalibrated = Double.NaN;
			for (double tenor : VOLATILITY_BUCKETS) {
				if (time + tenor <= CALIBRATED_MATURITY_HORIZON) {
					lastCalibrated = calibratedVolatility[calibratedIndex++];
				}
				if (time + tenor <= maturityHorizon) {
					extended.add(lastCalibrated);
				}
			}
		}
		return extended.stream().mapToDouble(Double::doubleValue).toArray();
	}
	
	
	public static LIBORModelMonteCarloSimulationModel createLIBORMarketModel(
			final int numberOfPaths, final int numberOfFactors, 
			final double liborRateTimeHorzion, final double liborPeriodLength, final double simulationTimeStep, 
			final AnalyticModel curveModel, CurveName curveName, double[] volatility, double displacementParameter) throws CalculationException {
		return createLIBORMarketModel(numberOfPaths, numberOfFactors, liborRateTimeHorzion, liborPeriodLength,
				simulationTimeStep, curveModel, curveName, volatility, displacementParameter, new RandomVariableFromArrayFactory());
	}

	/** Creates a regular-grid simulation using the supplied random-variable factory. */
	public static LIBORModelMonteCarloSimulationModel createLIBORMarketModel(
			final int numberOfPaths, final int numberOfFactors,
			final double liborRateTimeHorzion, final double liborPeriodLength, final double simulationTimeStep,
			final AnalyticModel curveModel, CurveName curveName, double[] volatility, double displacementParameter,
			RandomVariableFactory randomVariableFactory) throws CalculationException {
		requirePositiveFinite(liborRateTimeHorzion, "Maturity horizon");
		requirePositiveFinite(simulationTimeStep, "Simulation time step");
		TimeDiscretization simulationTimes = new TimeDiscretizationFromArray(0.0, liborRateTimeHorzion, simulationTimeStep, TimeDiscretizationFromArray.ShortPeriodLocation.SHORT_PERIOD_AT_END);
		return createLIBORMarketModel(numberOfPaths, numberOfFactors, liborRateTimeHorzion, liborPeriodLength, simulationTimes, curveModel, curveName, volatility, displacementParameter, randomVariableFactory);
	}

	/**
	 * Creates a model with independent observation and maturity grids. The tenor
	 * grid is rounded up to cover the requested maturity horizon. Volatility is
	 * either one constant or the parameter vector for the resulting finmath grid.
	 */
	public static LIBORModelMonteCarloSimulationModel createLIBORMarketModel(
			final int numberOfPaths, final int numberOfFactors,
			final double liborRateTimeHorizon, final double liborPeriodLength, final TimeDiscretization simulationTimes,
			final AnalyticModel curveModel, CurveName curveName, double[] volatility, double displacementParameter) throws CalculationException {
		return createLIBORMarketModel(numberOfPaths, numberOfFactors, liborRateTimeHorizon, liborPeriodLength,
				simulationTimes, curveModel, curveName, volatility, displacementParameter, new RandomVariableFromArrayFactory());
	}

	/**
	 * Uses one factory for the state, volatility and displacement parameters.
	 * Brownian increments remain independent, non-differentiable inputs. Future
	 * native-node AAD requires retaining intermediate nodes in the supplied factory.
	 */
	public static LIBORModelMonteCarloSimulationModel createLIBORMarketModel(
			final int numberOfPaths, final int numberOfFactors,
			final double liborRateTimeHorizon, final double liborPeriodLength, final TimeDiscretization simulationTimes,
			final AnalyticModel curveModel, CurveName curveName, double[] volatility, double displacementParameter,
			RandomVariableFactory randomVariableFactory) throws CalculationException {
		requirePositiveFinite(liborRateTimeHorizon, "Maturity horizon");
		requirePositiveFinite(liborPeriodLength, "LIBOR period length");
		Objects.requireNonNull(simulationTimes, "Simulation times");
		Objects.requireNonNull(randomVariableFactory, "Random-variable factory");
		if (numberOfPaths <= 0 || numberOfFactors <= 0) {
			throw new IllegalArgumentException("Number of paths and factors must be positive.");
		}
		if (simulationTimes.getNumberOfTimeSteps() < 1 || simulationTimes.getTime(0) != 0.0 || simulationTimes.getTime(simulationTimes.getNumberOfTimeSteps()) > liborRateTimeHorizon) {
			throw new IllegalArgumentException("Simulation grid must start at zero and end within the maturity horizon.");
		}

		final ForwardCurve forwardCurve = curveModel.getForwardCurve("ForwardCurveFromDiscountCurve("+ curveName.toString() +",3M)");
		final DiscountCurve discountCurve = curveModel.getDiscountCurve(curveName.toString());

		/*
		 * Create the libor tenor structure and the initial values
		 */
		TimeDiscretizationFromArray liborPeriodDiscretization = new TimeDiscretizationFromArray(0.0, (int) Math.ceil(liborRateTimeHorizon / liborPeriodLength), liborPeriodLength);

		/*
		 * Create Brownian motions
		 */
		final BrownianMotion brownianMotion = new BrownianMotionFromMersenneRandomNumbers(simulationTimes, numberOfFactors, numberOfPaths, 31415 /* seed */);

		// Create a volatility model: Piecewise constant volatility
		LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelPiecewiseConstant(randomVariableFactory, simulationTimes, liborPeriodDiscretization, new TimeDiscretizationFromArray(VOLATILITY_BUCKETS), new TimeDiscretizationFromArray(VOLATILITY_BUCKETS), volatility, false);

		// Create a correlation model
		LIBORCorrelationModel correlationModel = new LIBORCorrelationModelExponentialDecay(simulationTimes, liborPeriodDiscretization, numberOfFactors, 0.05, false);

		// Create a covariance model
		AbstractLIBORCovarianceModelParametric covarianceModelParametric = new LIBORCovarianceModelFromVolatilityAndCorrelation(simulationTimes, liborPeriodDiscretization, volatilityModel, correlationModel);

		// Blend the local volatility with the supplied displacement parameter.
		AbstractLIBORCovarianceModelParametric covarianceModelBlended = new BlendedLocalVolatilityModel(randomVariableFactory, covarianceModelParametric, displacementParameter, false);

		// Set model properties
		final Map<String, String> properties = new HashMap<>();

		// Choose the simulation measure
		properties.put("measure", LIBORMarketModelFromCovarianceModel.Measure.SPOT.name());

		// Evolve the forwards in normal state space.
		properties.put("stateSpace", LIBORMarketModelFromCovarianceModel.StateSpace.NORMAL.name());

		// Empty array of calibration items - hence, model will use given covariance
		final CalibrationProduct[] calibrationItems = new CalibrationProduct[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */		
		final LIBORMarketModel liborMarketModel = new LIBORMarketModelFromCovarianceModel(
				liborPeriodDiscretization, curveModel, forwardCurve, discountCurve, 
				randomVariableFactory, covarianceModelBlended, calibrationItems, properties);

		final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(liborMarketModel, brownianMotion);

		return new LIBORMonteCarloSimulationFromLIBORModel(process);
	}

	private static void requirePositiveFinite(double value, String name) {
		if (!Double.isFinite(value) || value <= 0.0) {
			throw new IllegalArgumentException(name + " must be finite and positive.");
		}
	}

	
	public static AnalyticModel getAnalyticModel(CurveName curveName, double[] zeroRateFixings, double[] zeroRates) {
		// Create a discount curve
		final DiscountCurveInterpolation discountCurveInterpolation = DiscountCurveInterpolation.createDiscountCurveFromZeroRates(
				curveName.toString() /* name */,
				REFERENCE_DATE.toLocalDate()	/* referenceDate */,
				zeroRateFixings	/* maturities */,
				zeroRates		/* rates */,
				InterpolationMethod.LINEAR,
				ExtrapolationMethod.CONSTANT,
				// Linear continuously compounded zero rates, with flat zero-rate
				// tails, as in StochasticCurve. finmath omits the dummy t=0 point
				// for this entity and still returns P(0)=1.
				InterpolationEntity.LOG_OF_VALUE_PER_TIME
				);
		
		// Single curve setup
		final ForwardCurve	forwardCurveFromDiscountCurve = new ForwardCurveFromDiscountCurve(
				discountCurveInterpolation.getName(),
				REFERENCE_DATE.toLocalDate(),
				"3M");
		
		return new AnalyticModelFromCurvesAndVols(new Curve[] { discountCurveInterpolation, forwardCurveFromDiscountCurve });
	}
	
}
