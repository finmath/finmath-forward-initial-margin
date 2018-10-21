package net.finmath.xva.coordinates.lmm;

import com.google.common.collect.Streams;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class TransformationOperatorImpl implements TransformationOperator {
	private final RandomVariableInterface[][] transformationMatrix;
	private final Set<Long> modelVariableIDs;
	private final List<Simm2Coordinate> targetCoordinates;
	private final LIBORModelMonteCarloSimulationInterface simulation;

	TransformationOperatorImpl(RandomVariableInterface[][] transformationMatrix, Set<Long> modelVariableIDs, List<Simm2Coordinate> targetCoordinates, LIBORModelMonteCarloSimulationInterface simulation) {
		this.simulation = simulation;
		this.transformationMatrix = transformationMatrix;
		this.modelVariableIDs = modelVariableIDs;
		this.targetCoordinates = targetCoordinates;
	}

	@Override
	public Map<Simm2Coordinate, RandomVariableInterface> apply(double evaluationTime, AbstractLIBORMonteCarloProduct product) {
		final RandomVariableDifferentiableInterface productValue = ArbitrarySimm2Transformation.getValueAsDifferentiable(product, evaluationTime, simulation);

		final RandomVariableInterface[] vector = productValue.getGradient(modelVariableIDs).entrySet().stream().
				sorted(Comparator.comparing(Map.Entry::getKey)).
				map(Map.Entry::getValue).
				toArray(RandomVariableInterface[]::new);

		return Streams.zip(targetCoordinates.stream(),
				Arrays.stream(TransformationAlgorithms.multiplyVectorMatrix(vector, transformationMatrix)),
				(c, s) -> Pair.of((Simm2Coordinate)null, s)).
				collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}
}
