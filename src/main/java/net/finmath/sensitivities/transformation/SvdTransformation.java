package net.finmath.sensitivities.transformation;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.MonteCarloProduct;
import net.finmath.montecarlo.MonteCarloSimulationModel;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * Provides a SVD transformation from model gradients (with respect to the model quantities) to target gradients (for example with respect to market quantities).
 */
public class SvdTransformation<C> implements Transformation {
	private List<TargetQuantity<C>> targetQuantities;
	private Function<RandomVariable[][], RandomVariable[][]> pseudoInverter;
	private Set<AadCoordinate> modelQuantities;

	/**
	 * Creates a new SIMM transformation with the given model and market quantities, using the specified pseudo-inversion algorithm.
	 *
	 * @param modelQuantities  A set of model quantities, differentiable via {@link RandomVariableDifferentiable}.
	 * @param targetQuantities A set of target quantities in the form of {@link TargetQuantity}s.
	 */
	public SvdTransformation(Set<AadCoordinate> modelQuantities, List<TargetQuantity<C>> targetQuantities) {
		this.targetQuantities = targetQuantities;
		this.modelQuantities = modelQuantities;
		this.pseudoInverter = TransformationAlgorithms::getPseudoInverseByParallelAcmSvd;
	}

	static RandomVariableDifferentiable getValueAsDifferentiable(MonteCarloProduct product, double time, MonteCarloSimulationModel simulation) {
		RandomVariable x;
		try {
			x = product.getValue(time, simulation);
		} catch (CalculationException e) {
			throw new RuntimeException("Given model failed to deliver target quantity", e);
		}

		if (x instanceof RandomVariableDifferentiable) {
			return (RandomVariableDifferentiable) x;
		}

		throw new RuntimeException("Given model does not have automatic differentiation capabilities.");
	}

	private RandomVariable[][] getTransformationMatrix(double time, LIBORModelMonteCarloSimulationModel simulation) {

		Set<Long> modelQuantityIDs = modelQuantities.stream().
				flatMap(c -> c.getDomainVariables(simulation)).
				map(RandomVariableDifferentiable::getID).
				collect(Collectors.toSet());

		final RandomVariable[][] matrix = targetQuantities.stream().
				map(q -> getValueAsDifferentiable(q.getProduct(time), time, simulation).getGradient(modelQuantityIDs).values().toArray(new RandomVariable[0])).toArray(RandomVariable[][]::new);

		return pseudoInverter.apply(matrix);
	}

	@Override
	public TransformationOperator<C> getTransformationOperator(double time, LIBORModelMonteCarloSimulationModel simulation) {
		Set<Long> modelVariableIDs = modelQuantities.stream().
				flatMap(x -> x.getDomainVariables(simulation)).
				map(RandomVariableDifferentiable::getID).
				collect(Collectors.toSet());

		List<C> targetCoordinates = targetQuantities.stream().
				map(TargetQuantity::getCoordinate).
				collect(Collectors.toList());

		return new MatrixMultiplicationOperator<>(getTransformationMatrix(time, simulation), modelVariableIDs, targetCoordinates, simulation);
	}
}
