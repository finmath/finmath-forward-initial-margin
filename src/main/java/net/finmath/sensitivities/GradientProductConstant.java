package net.finmath.sensitivities;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * Provides a fixed gradient throughout all times.
 */
public class GradientProductConstant<C> implements GradientProduct<C> {

	private Map<C, RandomVariable> sensitivitiyMap;

	private GradientProductConstant(Map<C, RandomVariable> sensitivityMap) {
		this.sensitivitiyMap = sensitivityMap;
	}

	public static <C> GradientProductConstant<C> fromDouble(Map<C, Double> sensitivities) {
		return fromRandom(
				sensitivities.entrySet().stream().
				map(e -> Pair.of(e.getKey(), new Scalar(e.getValue()))).
				collect(Collectors.toMap(Pair::getKey, Pair::getValue)));
	}

	public static <C> GradientProductConstant<C> fromRandom(Map<C, RandomVariable> sensitivities) {
		return new GradientProductConstant(sensitivities);
	}

	/**
	 * Returns the constant sensitivities that were given upon construction.
	 *
	 * @param evaluationTime The time which will not be evaluated.
	 * @param model The model which will not be evaluated.
	 * @return The constant map from coordinates to sensitivity values.
	 */
	@Override
	public Map<C, RandomVariable> getGradient(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		return sensitivitiyMap;
	}
}
