package net.finmath.xva.coordinates.lmm;

import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;

/**
 * Defines methods to retrieve sensitivities in a Monte Carlo model via automatic differentiation.
 */
public interface AadCoordinate {
	RandomVariableDifferentiableInterface getWrt();
}
