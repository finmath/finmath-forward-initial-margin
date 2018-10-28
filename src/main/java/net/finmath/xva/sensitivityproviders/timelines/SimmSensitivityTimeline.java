package net.finmath.xva.sensitivityproviders.timelines;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

import java.util.Map;

/**
 * Provides SIMM sensitivities for multiple times.
 *
 * Sensitivities might come from single instruments or portfolios, they might be calculated from model quantities or directly given or approximated.
 */
public interface SimmSensitivityTimeline {


	/**
	 * @deprecated
	 */
	default RandomVariableInterface getSIMMSensitivity(Simm2Coordinate key, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		return getSimmSensitivities(evaluationTime, model).get(key);
	}

	/**
	 * Returns all the sensitivities that are available from this source.
	 * @param evaluationTime The time as {@link net.finmath.time.FloatingpointDate}.
	 * @param model
	 * @return A map from coordinates to sensitivity values.
	 */
	Map<Simm2Coordinate, RandomVariableInterface> getSimmSensitivities(double evaluationTime, LIBORModelMonteCarloSimulationInterface model);
}

