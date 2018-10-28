package net.finmath.xva.sensitivityproviders.timelines;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.Scalar;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.stream.Collectors;

public class SimmConstantTimeline implements SimmSensitivityTimeline {

	private Map<Simm2Coordinate, RandomVariableInterface> sensitivitiyMap;

	private SimmConstantTimeline(Map<Simm2Coordinate, RandomVariableInterface> sensitivityMap) {
		this.sensitivitiyMap = sensitivityMap;
	}

	public static SimmConstantTimeline fromDouble(Map<Simm2Coordinate, Double> sensitivities) {
		return fromRandom(
				sensitivities.entrySet().stream().
						map(e -> Pair.of(e.getKey(), new Scalar(e.getValue()))).
						collect(Collectors.toMap(Pair::getKey, Pair::getValue)));
	}

	public static SimmConstantTimeline fromRandom(Map<Simm2Coordinate, RandomVariableInterface> sensitivities) {
		return new SimmConstantTimeline(sensitivities);
	}

	/**
	 * Returns the constant sensitivities that were given upon construction.
	 *
	 * @param evaluationTime The time which will not be evaluated.
	 * @param model The model which will not be evaluated.
	 * @return The constant map from coordinates to sensitivity values.
	 */
	@Override
	public Map<Simm2Coordinate, RandomVariableInterface> getSimmSensitivities(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		return sensitivitiyMap;
	}
}
