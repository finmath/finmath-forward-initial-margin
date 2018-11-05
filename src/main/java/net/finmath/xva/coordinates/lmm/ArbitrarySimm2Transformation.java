package net.finmath.xva.coordinates.lmm;

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
 * Provides a transformation from model sensitivities (with respect to the model quantities) to SIMM sensitivities.
 */
public class ArbitrarySimm2Transformation {
	private Set<ModelledMarketQuantity> marketQuantities;
	private Function<RandomVariableInterface[][], RandomVariableInterface[][]> pseudoInverter;
	private Set<AadCoordinate> modelQuantities;

	public ArbitrarySimm2Transformation(Set<ModelledMarketQuantity> marketQuantities, Set<AadCoordinate> modelQuantities) {
		this(modelQuantities, marketQuantities, TransformationAlgorithms::getPseudoInverseByParallelAcmSvd);
	}

	/**
	 * Creates a new SIMM transformation with the given model and market quantities, using the specified pseudo-inversion algorithm.
	 *
	 * @param modelQuantities  A set of model quantities, differentiable via {@link RandomVariableDifferentiableInterface}.
	 * @param marketQuantities A set of market quantities in the form of {@link ModelledMarketQuantity}s.
	 * @param pseudoInverter   A function that performs a pseudo-inversion on a random matrix.
	 */
	public ArbitrarySimm2Transformation(Set<AadCoordinate> modelQuantities, Set<ModelledMarketQuantity> marketQuantities, Function<RandomVariableInterface[][], RandomVariableInterface[][]> pseudoInverter) {
		this.marketQuantities = marketQuantities;
		this.modelQuantities = modelQuantities;
		this.pseudoInverter = pseudoInverter;
	}

	private RandomVariableDifferentiableInterface getValue(AbstractMonteCarloProduct product, double time, MonteCarloSimulationInterface simulation) {
		RandomVariableInterface x = null;
		try {
			x = product.getValue(time, simulation);
		} catch (CalculationException e) {
			throw new RuntimeException("Given model failed to deliver market quantity", e);
		}

		if (x instanceof RandomVariableDifferentiableInterface) {
			return (RandomVariableDifferentiableInterface) x;
		}

		throw new RuntimeException("Given model does not have automatic differentiation capabilities.");
	}

	public RandomVariableInterface[][] getTransformationMatrix(double time, LIBORModelMonteCarloSimulationInterface simulation) {

		Set<Long> modelQuantityIDs = modelQuantities.stream().
				flatMap(c -> c.getDomainVariables(simulation)).
				map(RandomVariableDifferentiableInterface::getID).
				collect(Collectors.toSet());

		final RandomVariableInterface[][] matrix = marketQuantities.stream().
				map(q -> getValue(q.getProduct(time), time, simulation).getGradient(modelQuantityIDs).values().toArray(new RandomVariableInterface[0])).toArray(RandomVariableInterface[][]::new);

		return pseudoInverter.apply(matrix);
	}
}
