package net.finmath.sensitivities;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.Scalar;
import net.finmath.time.FloatingpointDate;

/**
 * Composes the sensitivities obtained by underlying sensitivity providers into a single one.
 */
public class GradientProductComposite<C> implements GradientProduct<C> {
	private Set<GradientProduct<C>> underlyingTimelines;

	/**
	 * @param underlyingTimelines A set of sensitivity providers responsible for calculating sensitivities of the portfolio constituents.
	 */
	public GradientProductComposite(Set<GradientProduct<C>> underlyingTimelines) {
		this.underlyingTimelines = underlyingTimelines;
	}

	/**
	 * Returns all the aggregation of the sensitivities in the single timelines.
	 *
	 * @param evaluationTime The time as {@link FloatingpointDate}.
	 * @param model
	 * @return A map from coordinates to sensitivity values.
	 */
	@Override
	public Map<C, RandomVariableInterface> getGradient(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		return underlyingTimelines.stream().
				flatMap(timeline -> timeline.getGradient(evaluationTime, model).entrySet().stream()).
				collect(Collectors.groupingBy(Map.Entry::getKey)).entrySet().stream().
				map(GradientProductComposite::aggregateGroupedSensitivities).
				collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	private static <C> Pair<C, RandomVariableInterface> aggregateGroupedSensitivities(Map.Entry<C, List<Map.Entry<C, RandomVariableInterface>>> group) {
		return Pair.of(group.getKey(), group.getValue().stream().
				map(Map.Entry::getValue).
				reduce(new Scalar(0.0), RandomVariableInterface::add));
	}
}
