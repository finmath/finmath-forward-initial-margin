package net.finmath.xva.sensitivityproviders.timelines;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.Scalar;
import net.finmath.time.FloatingpointDate;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Composes the sensitivities obtained by underlying sensitivity providers into a single one.
 */
public class SimmCompositeTimeline implements SimmSensitivityTimeline {
	private Set<SimmSensitivityTimeline> underlyingTimelines;

	/**
	 * @param underlyingTimelines A set of sensitivity providers responsible for calculating sensitivities of the portfolio constituents.
	 */
	public SimmCompositeTimeline(Set<SimmSensitivityTimeline> underlyingTimelines) {
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
	public Map<Simm2Coordinate, RandomVariableInterface> getSimmSensitivities(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		return underlyingTimelines.stream().
				flatMap(timeline -> timeline.getSimmSensitivities(evaluationTime, model).entrySet().stream()).
				collect(Collectors.groupingBy(Map.Entry::getKey)).entrySet().stream().
				map(SimmCompositeTimeline::aggregateGroupedSensitivities).
				collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	private static Pair<Simm2Coordinate, RandomVariableInterface> aggregateGroupedSensitivities(Map.Entry<Simm2Coordinate, List<Map.Entry<Simm2Coordinate, RandomVariableInterface>>> group) {
		return Pair.of(group.getKey(), group.getValue().stream().
				map(Map.Entry::getValue).
				reduce(new Scalar(0.0), RandomVariableInterface::add));
	}
}
