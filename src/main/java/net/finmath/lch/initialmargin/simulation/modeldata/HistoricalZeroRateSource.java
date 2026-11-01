package net.finmath.lch.initialmargin.simulation.modeldata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;

import net.finmath.exception.CalculationException;
import net.finmath.stochastic.RandomVariable;

/**
 * Immutable snapshot of deterministic, continuously compounded historical curves.
 * Input dates must be daily observations at midnight. No dates are filled or
 * interpolated. Zero rates are interpolated linearly between original tenor
 * points and extrapolated constantly outside them, without caching new points.
 * A raw zero-tenor point is allowed, but requested tenors must be positive.
 */
public final class HistoricalZeroRateSource implements ZeroRateSource {

    private final NavigableMap<LocalDate, NavigableMap<Double, RandomVariable>> curves;

    public HistoricalZeroRateSource(NavigableMap<LocalDateTime, StochasticCurve> history) {
        Objects.requireNonNull(history, "history");
        if (history.isEmpty()) {
            throw new IllegalArgumentException("Historical zero-rate curves must not be empty.");
        }
        NavigableMap<LocalDate, NavigableMap<Double, RandomVariable>> snapshot = new TreeMap<>();
        for (Map.Entry<LocalDateTime, StochasticCurve> entry : history.entrySet()) {
            LocalDateTime dateTime = Objects.requireNonNull(entry.getKey(), "Historical observation date");
            if (!dateTime.toLocalTime().equals(LocalTime.MIDNIGHT)) {
                throw new IllegalArgumentException("Historical observation must be at midnight: " + dateTime);
            }
            LocalDate date = dateTime.toLocalDate();
            StochasticCurve inputCurve = Objects.requireNonNull(entry.getValue(), "Historical curve at " + date);
            NavigableMap<Double, RandomVariable> points = new TreeMap<>();
            for (Map.Entry<Double, RandomVariable> point : inputCurve.getCurve().entrySet()) {
                Double tenor = point.getKey();
                if (tenor == null || !Double.isFinite(tenor) || tenor < 0.0) {
                    throw new IllegalArgumentException("Historical tenor must be finite and nonnegative at " + date);
                }
                RandomVariable rate = point.getValue();
                if (rate == null || !rate.isDeterministic() || !Double.isFinite(rate.get(0))) {
                    throw new IllegalArgumentException("Historical zero rate must be finite and deterministic at " + date + ", tenor " + tenor);
                }
                double normalizedTenor = tenor == 0.0 ? 0.0 : tenor;
                if (points.put(normalizedTenor, rate) != null) {
                    throw new IllegalArgumentException("Duplicate historical tenor at " + date + ": " + tenor);
                }
            }
            if (points.isEmpty()) {
                throw new IllegalArgumentException("Historical curve must not be empty at " + date);
            }
            if (snapshot.put(date, Collections.unmodifiableNavigableMap(points)) != null) {
                throw new IllegalArgumentException("Duplicate historical observation day: " + date);
            }
        }
        curves = Collections.unmodifiableNavigableMap(snapshot);
    }

    @Override
    public RandomVariable getZeroRate(LocalDate observationDate, double tenorYears) throws CalculationException {
        Objects.requireNonNull(observationDate, "observationDate");
        if (!Double.isFinite(tenorYears) || tenorYears <= 0.0) {
            throw new IllegalArgumentException("Requested tenor must be finite and strictly positive.");
        }
        NavigableMap<Double, RandomVariable> curve = curves.get(observationDate);
        if (curve == null) {
            throw new CalculationException("No historical zero-rate curve for " + observationDate);
        }
        Map.Entry<Double, RandomVariable> lower = curve.floorEntry(tenorYears);
        Map.Entry<Double, RandomVariable> upper = curve.ceilingEntry(tenorYears);
        if (lower == null) {
            return curve.firstEntry().getValue();
        }
        if (upper == null || lower.getKey().equals(upper.getKey())) {
            return lower.getValue();
        }
        double weight = (tenorYears - lower.getKey()) / (upper.getKey() - lower.getKey());
        return lower.getValue().mult(1.0 - weight).add(upper.getValue().mult(weight));
    }

    public LocalDate getFirstDate() {
        return curves.firstKey();
    }

    public LocalDate getLastDate() {
        return curves.lastKey();
    }

    public NavigableSet<Double> getTenors(LocalDate observationDate) {
        Objects.requireNonNull(observationDate, "observationDate");
        NavigableMap<Double, RandomVariable> curve = curves.get(observationDate);
        if (curve == null) {
            throw new IllegalArgumentException("No historical zero-rate curve for " + observationDate);
        }
        return Collections.unmodifiableNavigableSet(curve.navigableKeySet());
    }

    public NavigableSet<LocalDate> getObservationDates() {
        return Collections.unmodifiableNavigableSet(curves.navigableKeySet());
    }
}
