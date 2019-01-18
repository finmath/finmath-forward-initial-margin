package net.finmath.sensitivities;

import java.util.Map;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

public interface GradientProduct<C> {

	/**
	 * Returns all the sensitivities that are available from this source.
	 * @param evaluationTime The time as {@link net.finmath.time.FloatingpointDate}.
	 * @param model
	 * @return A map from coordinates to sensitivity values.
	 */
	Map<C, RandomVariable> getGradient(double evaluationTime, LIBORModelMonteCarloSimulationModel model);
}
