package net.finmath.xva.coordinates.lmm;

import java.util.stream.Stream;

import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;

/**
 * Allows to retrieve the model quantities as a coordinate system for the calculation of sensitivities in a Monte Carlo model via automatic differentiation.
 */
public interface AadCoordinate {
	/**
	 * Returns the random variables with regard to which the differentiation shall happen.
	 * @param simulation The {@link LIBORModelMonteCarloSimulationInterface} holding the differentiable simulation.
	 * @param evaluationTime The evaluation time.
	 * @return A stream of {@link RandomVariableDifferentiableInterface}s that can be used to identify the derivatives in the gradient vector.
	 */
	Stream<RandomVariableDifferentiableInterface> getDomainVariables(LIBORModelMonteCarloSimulationInterface simulation, double evaluationTime);

	/**
	 * Returns the random variables with regard to which the differentiation shall happen, evaluated at time zero.
	 * @param simulation The {@link LIBORModelMonteCarloSimulationInterface} holding the differentiable simulation.
	 * @return A stream of {@link RandomVariableDifferentiableInterface}s that can be used to identify the derivatives in the gradient vector.
	 */
	default Stream<RandomVariableDifferentiableInterface> getDomainVariables(LIBORModelMonteCarloSimulationInterface simulation) {
		return getDomainVariables(simulation, 0.0);
	}
}
