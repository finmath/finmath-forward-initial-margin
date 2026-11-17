package net.finmath.lch.initialmargin.simulation.modeldata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import net.finmath.exception.CalculationException;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.Currency;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.CurveName;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;

/**
 * One PAIRS risk curve, composed of historical and/or simulated zero-rate sources.
 * The historical closing curve owns the reference date; only later observations
 * come from the simulation. Both sources must describe the same curve, units and
 * constant-tenor convention. There is no implicit rebasing or index conversion.
 *
 * Scenario histories contain every configured business day, independently of
 * previous pricing calls. Original historical curves are never changed. The
 * compatibility pricing methods derive forwards and discounts from this same
 * curve; OIS and tenor-basis add-ons are outside this single-curve module.
 */
public class ZeroRateModel implements ZeroRateSource {

    /** Absolute zero-rate tolerance: 0.001 basis points, allowing rounded input quotes. */
    public static final double DEFAULT_INITIAL_CURVE_TOLERANCE = 1E-7;

    private final HistoricalZeroRateSource historicalSource;
    private final ZeroRateSource simulatedSource;
    private final LocalDate referenceDate;
    private final LocalDate firstObservationDate;
    private final BusinessdayCalendar calendar;
    private final int numberOfPaths;
    private final double initialCurveTolerance;
    private final Set<Double> alignedTenors = new HashSet<>();
    // Only scenario requests populate this cache, never arbitrary payment tenors.
    private final Map<Double, NavigableMap<LocalDate, RandomVariable>> scenarioObservations = new HashMap<>();

    /**
     * Legacy file-based entry point. New callers can inject either source directly.
     * The bundled history contains all weekdays, including TARGET holidays, so
     * this entry point uses a Monday-Friday observation calendar. For other data
     * sets inject an explicit calendar. Currency/curveName identify caller-supplied
     * data; they do not convert or rebootstrap it.
     */
    public ZeroRateModel(Currency currency, CurveName curveName, LIBORModelMonteCarloSimulationModel processModel, String inputDataFilePath) throws CalculationException {
        this(new HistoricalZeroRateSource(new ZeroCurveHistory(inputDataFilePath).buildZeroCurveHistory()),
                new SimulatedZeroRateSource(processModel, processModel.getReferenceDate().toLocalDate(), processModel.getLiborPeriodDiscretization().getLastTime()),
                new BusinessdayCalendarExcludingWeekends());
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(curveName, "curveName");
    }

    /** Historical and simulated data for one curve, with a checked common initial state. */
    public ZeroRateModel(HistoricalZeroRateSource historicalSource, SimulatedZeroRateSource simulatedSource, BusinessdayCalendar calendar) throws CalculationException {
        this(historicalSource, simulatedSource, simulatedSource.getReferenceDate(), calendar, simulatedSource.getNumberOfPaths(), DEFAULT_INITIAL_CURVE_TOLERANCE);
    }

    /** A purely historical curve; forward history requests beyond its last date fail. */
    public ZeroRateModel(HistoricalZeroRateSource historicalSource, BusinessdayCalendar calendar) throws CalculationException {
        this(historicalSource, null, historicalSource.getLastDate(), calendar, 1, DEFAULT_INITIAL_CURVE_TOLERANCE);
    }

    /** A purely simulated curve. Scenario windows require an actual simulation warm-up. */
    public ZeroRateModel(SimulatedZeroRateSource simulatedSource, BusinessdayCalendar calendar) throws CalculationException {
        this(null, simulatedSource, simulatedSource.getReferenceDate(), calendar, simulatedSource.getNumberOfPaths(), DEFAULT_INITIAL_CURVE_TOLERANCE);
    }

    /**
     * General source composition, also usable for a separately constructed PAIRS
     * standard curve. At least one source is required. Custom simulated sources
     * must use this reference date and the same ordered paths as product valuation.
     * The tolerance checks initial levels only; it never shifts either source.
     * Initial alignment queries the simulation at time zero. A simulation which
     * precalculates all process times may therefore allocate its full path storage
     * already during construction; choose its observation horizon accordingly.
     */
    public ZeroRateModel(HistoricalZeroRateSource historicalSource, ZeroRateSource simulatedSource, LocalDate referenceDate, BusinessdayCalendar calendar, int numberOfPaths, double initialCurveTolerance) throws CalculationException {
        if (historicalSource == null && simulatedSource == null) {
            throw new IllegalArgumentException("At least one zero-rate source is required.");
        }
        this.referenceDate = Objects.requireNonNull(referenceDate, "referenceDate");
        this.calendar = Objects.requireNonNull(calendar, "calendar");
        if (!calendar.isBusinessday(referenceDate)) {
            throw new IllegalArgumentException("The reference date must be an observation business day: " + referenceDate);
        }
        if (numberOfPaths <= 0) {
            throw new IllegalArgumentException("numberOfPaths must be positive.");
        }
        if (!Double.isFinite(initialCurveTolerance) || initialCurveTolerance < 0.0) {
            throw new IllegalArgumentException("initialCurveTolerance must be finite and nonnegative.");
        }
        if (simulatedSource instanceof SimulatedZeroRateSource) {
            SimulatedZeroRateSource source = (SimulatedZeroRateSource) simulatedSource;
            if (!referenceDate.equals(source.getReferenceDate()) || numberOfPaths != source.getNumberOfPaths()) {
                throw new IllegalArgumentException("Simulated source reference date or path count does not match.");
            }
        }
        this.historicalSource = historicalSource;
        this.simulatedSource = simulatedSource;
        this.numberOfPaths = numberOfPaths;
        this.initialCurveTolerance = initialCurveTolerance;
        firstObservationDate = historicalSource == null ? referenceDate : historicalSource.getFirstDate();

        if (historicalSource != null) {
            if (firstObservationDate.isAfter(referenceDate)) {
                throw new IllegalArgumentException("Historical observations must start on or before the reference date.");
            }
            for (LocalDate date = firstObservationDate; !date.isAfter(referenceDate); date = date.plusDays(1)) {
                if (calendar.isBusinessday(date) && !historicalSource.getObservationDates().contains(date)) {
                    throw new CalculationException("Missing historical business-day observation at " + date + ".");
                }
            }
            // Check supported initial knots now; every requested tenor is checked on demand.
            // An unused long historical tenor must not prevent use of a shorter model.
            if (simulatedSource != null) {
                for (double tenor : historicalSource.getTenors(referenceDate)) {
                    if (tenor > 0.0 && (!(simulatedSource instanceof SimulatedZeroRateSource) || tenor <= ((SimulatedZeroRateSource) simulatedSource).getMaturityHorizon())) {
                        ensureInitialCurveAlignment(tenor);
                    }
                }
            }
        }
    }

    @Override
    public RandomVariable getZeroRate(LocalDate observationDate, double tenorYears) throws CalculationException {
        Objects.requireNonNull(observationDate, "observationDate");
        if (!Double.isFinite(tenorYears) || tenorYears <= 0.0) {
            throw new IllegalArgumentException("tenorYears must be finite and strictly positive.");
        }
        if (observationDate.isBefore(firstObservationDate)) {
            throw new CalculationException("Observation " + observationDate + " precedes available data at " + firstObservationDate + ".");
        }
        if (!observationDate.isAfter(referenceDate) && historicalSource != null) {
            // A frozen historical prefix must not trigger queries into a simulated future.
            if (observationDate.equals(referenceDate)) {
                ensureInitialCurveAlignment(tenorYears);
            }
            return checkedValue(historicalSource.getZeroRate(observationDate, tenorYears), observationDate, true);
        }
        if (simulatedSource == null) {
            throw new CalculationException("No simulated source is configured after " + referenceDate + ".");
        }
        ensureInitialCurveAlignment(tenorYears);
        return checkedValue(simulatedSource.getZeroRate(observationDate, tenorYears), observationDate, false);
    }

    /**
     * Compatibility curve access. Shifts are applied to the common zero curve
     * before selecting a path, so they affect forward and discount roles together.
     */
    public RandomVariable getZeroRate(LocalDateTime evaluationDate, double periodLength, StochasticCurve curveShifts, Integer pathOrState) throws CalculationException {
        LocalDate date = observationDate(evaluationDate);
        RandomVariable rate = getZeroRate(date, periodLength);
        if (curveShifts != null) {
            rate = checkedValue(rate.add(curveShifts.getRate(periodLength, null)), date, false);
        }
        if (pathOrState != null) {
            if (pathOrState < 0 || pathOrState >= numberOfPaths) {
                throw new IllegalArgumentException("Path index outside [0, " + numberOfPaths + "): " + pathOrState);
            }
            return new Scalar(rate.get(pathOrState));
        }
        return rate;
    }

    public RandomVariable getDiscountFactor(LocalDateTime evaluationDate, double periodLength, StochasticCurve curveShifts, Integer pathOrState) throws CalculationException {
        if (!Double.isFinite(periodLength) || periodLength < 0.0) {
            throw new IllegalArgumentException("Discount maturity must be finite and nonnegative.");
        }
        if (periodLength == 0.0) {
            return new Scalar(1.0);
        }
        return getZeroRate(evaluationDate, periodLength, curveShifts, pathOrState).mult(-periodLength).exp();
    }

    public RandomVariable getForwardRate(LocalDateTime evaluationDate, double periodStart, double periodEnd, StochasticCurve curveShifts, Integer pathOrState) throws CalculationException {
        if (!Double.isFinite(periodStart) || !Double.isFinite(periodEnd) || periodStart < 0.0 || periodEnd <= periodStart) {
            throw new IllegalArgumentException("Forward period must satisfy 0 <= start < end with finite times.");
        }
        return getDiscountFactor(evaluationDate, periodStart, curveShifts, pathOrState).div(getDiscountFactor(evaluationDate, periodEnd, curveShifts, pathOrState)).sub(1.0).div(periodEnd - periodStart);
    }

    /**
     * Complete history at a constant tenor. Nonbusiness evaluation dates use the
     * latest preceding business-day observation; no extra return observation is
     * inserted for a weekend/holiday. Missing required source observations fail.
     * The returned map is a snapshot; future calls cannot modify earlier results.
     */
    public NavigableMap<LocalDateTime, RandomVariable> getRateSeries(LocalDateTime evaluationDate, double periodLength) throws CalculationException {
        LocalDate cutoff = observationDate(evaluationDate);
        if (!Double.isFinite(periodLength) || periodLength <= 0.0) {
            throw new IllegalArgumentException("periodLength must be finite and strictly positive.");
        }
        if (cutoff.isBefore(firstObservationDate)) {
            throw new CalculationException("Scenario cutoff precedes available observations: " + cutoff);
        }
        NavigableMap<LocalDate, RandomVariable> cache = scenarioObservations.computeIfAbsent(periodLength, key -> new TreeMap<>());
        NavigableMap<LocalDateTime, RandomVariable> result = new TreeMap<>();
        for (LocalDate date = firstObservationDate; !date.isAfter(cutoff); date = date.plusDays(1)) {
            if (calendar.isBusinessday(date)) {
                RandomVariable rate = cache.get(date);
                if (rate == null) {
                    rate = getZeroRate(date, periodLength);
                    cache.put(date, rate);
                }
                result.put(date.atStartOfDay(), rate);
            }
        }
        if (result.isEmpty()) {
            throw new CalculationException("No business-day observations on or before " + cutoff + ".");
        }
        return Collections.unmodifiableNavigableMap(result);
    }

    public LocalDateTime getReferenceDate() {
        return referenceDate.atStartOfDay();
    }

    public int getNumberOfPaths() {
        return numberOfPaths;
    }

    /** @return The declared absolute maturity horizon of the simulated source. */
    public double getModelMaturity() {
        if (simulatedSource instanceof SimulatedZeroRateSource) {
            return ((SimulatedZeroRateSource) simulatedSource).getMaturityHorizon();
        }
        throw new UnsupportedOperationException("This source does not declare a model maturity horizon.");
    }

    private void ensureInitialCurveAlignment(double tenor) throws CalculationException {
        if (historicalSource == null || simulatedSource == null || alignedTenors.contains(tenor)) {
            return;
        }
        RandomVariable historical = checkedValue(historicalSource.getZeroRate(referenceDate, tenor), referenceDate, true);
        RandomVariable simulated = checkedValue(simulatedSource.getZeroRate(referenceDate, tenor), referenceDate, false);
        double difference = simulated.sub(historical).abs().getMax();
        if (difference > initialCurveTolerance) {
            throw new CalculationException("Initial zero curves disagree at " + referenceDate + ", tenor " + tenor
                    + ": maximum absolute difference " + difference + " exceeds tolerance " + initialCurveTolerance
                    + ". Align curve definitions and initial calibration; no automatic rebasing is applied.");
        }
        alignedTenors.add(tenor);
    }

    private RandomVariable checkedValue(RandomVariable value, LocalDate date, boolean historical) throws CalculationException {
        if (value == null || value.size() < 1) {
            throw new CalculationException("Missing zero-rate value at " + date + ".");
        }
        if (historical && !value.isDeterministic()) {
            throw new CalculationException("Historical rates must be deterministic at " + date + ".");
        }
        if (!value.isDeterministic() && value.size() != numberOfPaths) {
            throw new CalculationException("Inconsistent path count at " + date + ": expected " + numberOfPaths + ", got " + value.size());
        }
        for (int path = 0; path < value.size(); path++) {
            if (!Double.isFinite(value.get(path))) {
                throw new CalculationException("Nonfinite zero rate at " + date + ", path " + path + ".");
            }
        }
        if (!historical) {
            double time = FloatingpointDate.getFloatingPointDateFromDate(referenceDate.atStartOfDay(), date.atStartOfDay());
            if (value.getFiltrationTime() > time + 1E-10 || Double.isNaN(value.getFiltrationTime())) {
                throw new CalculationException("Zero rate at " + date + " depends on a future model state.");
            }
        }
        return value;
    }

    private static LocalDate observationDate(LocalDateTime date) {
        Objects.requireNonNull(date, "evaluationDate");
        if (!date.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            throw new IllegalArgumentException("Daily zero-rate observations require dates at midnight: " + date);
        }
        return date.toLocalDate();
    }
}
