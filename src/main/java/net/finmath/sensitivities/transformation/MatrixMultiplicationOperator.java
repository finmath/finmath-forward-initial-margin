package net.finmath.sensitivities.transformation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Streams;

import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;

class MatrixMultiplicationOperator<C> implements TransformationOperator<C> {
	private final RandomVariable[][] matrix;
	private final Set<Long> modelVariableIDs;
	private final List<C> targetCoordinates;
	private final LIBORModelMonteCarloSimulationModel simulation;

	MatrixMultiplicationOperator(RandomVariable[][] matrix, Set<Long> modelVariableIDs, List<C> targetCoordinates, LIBORModelMonteCarloSimulationModel simulation) {
		this.simulation = simulation;
		this.matrix = matrix;
		this.modelVariableIDs = modelVariableIDs;
		this.targetCoordinates = targetCoordinates;
	}

	@Override
	public Map<C, RandomVariable> apply(double evaluationTime, AbstractLIBORMonteCarloProduct product) {
		final RandomVariableDifferentiable productValue = SvdTransformation.getValueAsDifferentiable(product, evaluationTime, simulation);

		final RandomVariable[] vector = productValue.getGradient(modelVariableIDs).entrySet().stream().
				sorted(Comparator.comparing(Map.Entry::getKey)).
				map(Map.Entry::getValue).
				toArray(RandomVariable[]::new);

		return Streams.zip(targetCoordinates.stream(),
				Arrays.stream(TransformationAlgorithms.multiplyVectorMatrix(vector, matrix)),
				Pair::of).
				collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}
}
