package net.finmath.lch.initialmargin.simulation.modeldata;

import java.time.LocalDate;

import net.finmath.exception.CalculationException;
import net.finmath.stochastic.RandomVariable;

/**
 * Supplies continuously compounded zero rates for one specified PAIRS risk curve.
 * The tenor is a strictly positive, finite time to maturity in years, held constant
 * across observation dates. Historical observations may be deterministic; simulated
 * observations must preserve the same paths and ordering across dates and tenors.
 * The source determines the economic curve being represented, independently of
 * whether its underlying model uses forward rates or short rates.
 */
@FunctionalInterface
public interface ZeroRateSource {

    RandomVariable getZeroRate(LocalDate observationDate, double tenorYears) throws CalculationException;
}
