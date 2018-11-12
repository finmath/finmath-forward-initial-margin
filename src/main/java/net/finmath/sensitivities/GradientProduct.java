package net.finmath.sensitivities;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

import java.util.Map;

public interface GradientProduct<C> {

	/**
	 * Returns all the sensitivities that are available from this source.
	 * @param evaluationTime The time as {@link net.finmath.time.FloatingpointDate}.
	 * @param model
	 * @return A map from coordinates to sensitivity values.
	 */
	Map<C, RandomVariableInterface> getGradient(double evaluationTime, LIBORModelMonteCarloSimulationInterface model);
}
