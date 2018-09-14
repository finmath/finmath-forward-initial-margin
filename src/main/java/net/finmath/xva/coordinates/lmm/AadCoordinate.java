package net.finmath.xva.coordinates.lmm;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;

/**
 * Defines methods to retrieve sensitivities in a Monte Carlo model via automatic differentiation.
 */
public interface AadCoordinate {
	/**
	 * Returns the random variable with regard to which the differentiation shall happen.
	 * @param evaluationTime The evaluation time.
	 * @return A {@link RandomVariableDifferentiableInterface} that can be used to retrieve to identify the derivative in the gradient.
	 * @throws CalculationException Gets propagated from the Monte Carlo simulation.
	 */
	RandomVariableDifferentiableInterface getDomainVariable(double evaluationTime) throws CalculationException;
}
