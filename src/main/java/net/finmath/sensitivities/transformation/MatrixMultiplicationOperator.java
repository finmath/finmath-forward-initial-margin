package net.finmath.sensitivities.transformation;

import com.google.common.collect.Streams;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class MatrixMultiplicationOperator<C> implements TransformationOperator<C> {
	private final RandomVariableInterface[][] matrix;
	private final Set<Long> modelVariableIDs;
	private final List<C> targetCoordinates;
	private final LIBORModelMonteCarloSimulationInterface simulation;

	MatrixMultiplicationOperator(RandomVariableInterface[][] matrix, Set<Long> modelVariableIDs, List<C> targetCoordinates, LIBORModelMonteCarloSimulationInterface simulation) {
		this.simulation = simulation;
		this.matrix = matrix;
		this.modelVariableIDs = modelVariableIDs;
		this.targetCoordinates = targetCoordinates;
	}

	@Override
	public Map<C, RandomVariableInterface> apply(double evaluationTime, AbstractLIBORMonteCarloProduct product) {
		final RandomVariableDifferentiableInterface productValue = SvdTransformation.getValueAsDifferentiable(product, evaluationTime, simulation);

		final RandomVariableInterface[] vector = productValue.getGradient(modelVariableIDs).entrySet().stream().
				sorted(Comparator.comparing(Map.Entry::getKey)).
				map(Map.Entry::getValue).
				toArray(RandomVariableInterface[]::new);

		return Streams.zip(targetCoordinates.stream(),
				Arrays.stream(TransformationAlgorithms.multiplyVectorMatrix(vector, matrix)),
				Pair::of).
				collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}
}
