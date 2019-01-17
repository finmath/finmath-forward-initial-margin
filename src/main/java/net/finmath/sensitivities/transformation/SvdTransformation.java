package net.finmath.sensitivities.transformation;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.MonteCarloSimulationInterface;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Provides a SVD transformation from model gradients (with respect to the model quantities) to target gradients (for example with respect to market quantities).
 */
public class SvdTransformation<C> implements Transformation {
	private List<TargetQuantity<C>> targetQuantities;
	private Function<RandomVariableInterface[][], RandomVariableInterface[][]> pseudoInverter;
	private Set<AadCoordinate> modelQuantities;

	/**
	 * Creates a new SIMM transformation with the given model and market quantities, using the specified pseudo-inversion algorithm.
	 *
	 * @param modelQuantities  A set of model quantities, differentiable via {@link RandomVariableDifferentiableInterface}.
	 * @param targetQuantities A set of target quantities in the form of {@link TargetQuantity}s.
	 */
	public SvdTransformation(Set<AadCoordinate> modelQuantities, List<TargetQuantity<C>> targetQuantities) {
		this.targetQuantities = targetQuantities;
		this.modelQuantities = modelQuantities;
		this.pseudoInverter = TransformationAlgorithms::getPseudoInverseByParallelAcmSvd;
	}

	static RandomVariableDifferentiableInterface getValueAsDifferentiable(AbstractMonteCarloProduct product, double time, MonteCarloSimulationInterface simulation) {
		RandomVariableInterface x;
		try {
			x = product.getValue(time, simulation);
		} catch (CalculationException e) {
			throw new RuntimeException("Given model failed to deliver target quantity", e);
		}

		if (x instanceof RandomVariableDifferentiableInterface) {
			return (RandomVariableDifferentiableInterface) x;
		}

		throw new RuntimeException("Given model does not have automatic differentiation capabilities.");
	}

	private RandomVariableInterface[][] getTransformationMatrix(double time, LIBORModelMonteCarloSimulationInterface simulation) {

		Set<Long> modelQuantityIDs = modelQuantities.stream().
				flatMap(c -> c.getDomainVariables(simulation)).
				map(RandomVariableDifferentiableInterface::getID).
				collect(Collectors.toSet());

		final RandomVariableInterface[][] matrix = targetQuantities.stream().
				map(q -> getValueAsDifferentiable(q.getProduct(time), time, simulation).getGradient(modelQuantityIDs).values().toArray(new RandomVariableInterface[0])).toArray(RandomVariableInterface[][]::new);

		return pseudoInverter.apply(matrix);
	}

	@Override
	public TransformationOperator<C> getTransformationOperator(double time, LIBORModelMonteCarloSimulationInterface simulation) {
		Set<Long> modelVariableIDs = modelQuantities.stream().
				flatMap(x -> x.getDomainVariables(simulation)).
				map(RandomVariableDifferentiableInterface::getID).
				collect(Collectors.toSet());

		List<C> targetCoordinates = targetQuantities.stream().
				map(TargetQuantity::getCoordinate).
				collect(Collectors.toList());

		return new MatrixMultiplicationOperator<>(getTransformationMatrix(time, simulation), modelVariableIDs, targetCoordinates, simulation);
	}
}
