package net.finmath.lch.initialmargin.simulation.modeldata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.TimeDiscretization;

/**
 * The PAIRS single risk curve implied by a simulation's forward rates.
 * The continuously compounded zero rate is log(1 + tenor * forward) / tenor.
 * This curve is used for both projection and discounting in the PAIRS single-curve
 * approximation; it is not a separate collateral discount curve.
 *
 * Observation dates must be present on the simulation grid. Only numerical
 * rounding is tolerated: this source never interpolates between process states.
 * Random-variable operations retain the simulation's paths and AAD graph.
 */
public final class SimulatedZeroRateSource implements ZeroRateSource {

    private static final double TIME_TOLERANCE = 1E-10;

    private final TermStructureMonteCarloSimulationModel simulation;
    private final LocalDate referenceDate;
    private final double maturityHorizon;
    private final int numberOfPaths;

    /**
     * @param simulation The simulation supplying the common forward-derived curve.
     * @param referenceDate Calendar date corresponding to model time zero.
     * @param maturityHorizon Last supported absolute maturity in model years, not
     *        the last observation time or a rolling maximum tenor.
     */
    public SimulatedZeroRateSource(TermStructureMonteCarloSimulationModel simulation, LocalDate referenceDate, double maturityHorizon) {
        this.simulation = Objects.requireNonNull(simulation, "simulation");
        this.referenceDate = Objects.requireNonNull(referenceDate, "referenceDate");
        if (!Double.isFinite(maturityHorizon) || maturityHorizon <= 0.0) {
            throw new IllegalArgumentException("Maturity horizon must be finite and positive.");
        }
        this.maturityHorizon = maturityHorizon;
        numberOfPaths = simulation.getNumberOfPaths();
        if (numberOfPaths <= 0) {
            throw new IllegalArgumentException("Simulation must have at least one path.");
        }
        try {
            checkReferenceDate(simulation.getReferenceDate());
        } catch (UnsupportedOperationException exception) {
            // Some finmath simulation wrappers require an explicit reference date.
        }
        if (simulation.getModel() != null) {
            try {
                checkReferenceDate(simulation.getModel().getReferenceDate());
            } catch (UnsupportedOperationException exception) {
                // The supplied reference date also supports models without dates.
            }
        }
    }

    @Override
    public RandomVariable getZeroRate(LocalDate observationDate, double tenorYears) throws CalculationException {
        Objects.requireNonNull(observationDate, "observationDate");
        if (observationDate.isBefore(referenceDate)) {
            throw new IllegalArgumentException("Simulated observation date precedes the reference date.");
        }
        if (!Double.isFinite(tenorYears) || tenorYears <= 0.0) {
            throw new IllegalArgumentException("Tenor must be finite and positive.");
        }
        double dateTime = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, observationDate);
        double time = getObservationTime(dateTime, observationDate);
        double maturity = time + tenorYears;
        if (!Double.isFinite(maturity) || maturity > maturityHorizon) {
            throw new IllegalArgumentException("Observation time plus tenor exceeds maturity horizon " + maturityHorizon + ".");
        }
        RandomVariable accumulation = simulation.getForwardRate(time, time, maturity).mult(tenorYears).add(1.0);
        if (!accumulation.isDeterministic() && accumulation.size() != numberOfPaths) {
            throw new IllegalArgumentException("Forward rate path count differs from the simulation.");
        }
        if (Double.isNaN(accumulation.getFiltrationTime()) || accumulation.getFiltrationTime() > dateTime + TIME_TOLERANCE) {
            throw new IllegalArgumentException("Forward rate uses information after the observation date.");
        }
        for (int path = 0; path < accumulation.size(); path++) {
            double value = accumulation.get(path);
            if (!Double.isFinite(value) || value <= 0.0) {
                throw new IllegalArgumentException("Forward accumulation must be finite and positive on every path.");
            }
        }
        RandomVariable zeroRate = accumulation.log().div(tenorYears);
        for (int path = 0; path < zeroRate.size(); path++) {
            if (!Double.isFinite(zeroRate.get(path))) {
                throw new IllegalArgumentException("Simulated zero rate must be finite on every path.");
            }
        }
        return zeroRate;
    }

    private double getObservationTime(double time, LocalDate date) {
        TimeDiscretization grid = simulation.getTimeDiscretization();
        int index = grid.getTimeIndex(time);
        if (index < 0) {
            index = -index - 1;
            if (index >= grid.getNumberOfTimes() || Math.abs(grid.getTime(index) - time) > TIME_TOLERANCE) {
                index--;
            }
        }
        if (index < 0 || index >= grid.getNumberOfTimes() || !Double.isFinite(grid.getTime(index)) || Math.abs(grid.getTime(index) - time) > TIME_TOLERANCE) {
            throw new IllegalArgumentException("Observation date " + date + " is not on the simulation time grid.");
        }
        return grid.getTime(index);
    }

    private void checkReferenceDate(LocalDateTime modelReferenceDate) {
        if (modelReferenceDate != null && !modelReferenceDate.equals(referenceDate.atStartOfDay())) {
            throw new IllegalArgumentException("Explicit reference date differs from the simulation/model reference date.");
        }
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }

    public int getNumberOfPaths() {
        return numberOfPaths;
    }

    public double getMaturityHorizon() {
        return maturityHorizon;
    }

    public TermStructureMonteCarloSimulationModel getSimulation() {
        return simulation;
    }
}
