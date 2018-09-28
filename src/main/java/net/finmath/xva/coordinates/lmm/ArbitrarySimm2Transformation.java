package net.finmath.xva.coordinates.lmm;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.stochastic.RandomVariableInterface;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Provides a transformation from model sensitivities (with respect to the model quantities) to SIMM sensitivities.
 */
public class ArbitrarySimm2Transformation {
	private Set<RandomVariableDifferentiableInterface> modelQuantities;
	private Set<ModelledMarketQuantity> marketQuantities;
	private Function<RandomVariableInterface[][], RandomVariableInterface[][]> pseudoInverter;
	private Set<Long> modelQuantityIDs;

	/**
	 * Creates a new SIMM transformation with the given model and market quantities, using getPseudoInverseByParallelAcmSvd for inversion.
	 * @param modelQuantities A set of model quantities, differentiable via {@link RandomVariableDifferentiableInterface}.
	 * @param marketQuantities A set of market quantities in the form of {@link ModelledMarketQuantity}s.
	 */
	public ArbitrarySimm2Transformation(Set<RandomVariableDifferentiableInterface> modelQuantities, Set<ModelledMarketQuantity> marketQuantities, Set<Long> modelQuantityIDs) {
		this(modelQuantities, marketQuantities, ArbitrarySimm2Transformation::getPseudoInverseByParallelAcmSvd);
	}

	/**
	 * Creates a new SIMM transformation with the given model and market quantities, using the specified pseudo-inversion algorithm.
	 * @param modelQuantities A set of model quantities, differentiable via {@link RandomVariableDifferentiableInterface}.
	 * @param marketQuantities A set of market quantities in the form of {@link ModelledMarketQuantity}s.
	 * @param pseudoInverter A function that performs a pseudo-inversion on a random matrix.
	 */
	public ArbitrarySimm2Transformation(Set<RandomVariableDifferentiableInterface> modelQuantities, Set<ModelledMarketQuantity> marketQuantities, Function<RandomVariableInterface[][], RandomVariableInterface[][]> pseudoInverter) {
		this.modelQuantities = modelQuantities;
		this.marketQuantities = marketQuantities;
		this.modelQuantityIDs = modelQuantities.stream().map(RandomVariableDifferentiableInterface::getID
		).collect(Collectors.toSet());
		this.pseudoInverter = pseudoInverter;
	}

	private RandomVariableDifferentiableInterface getValue(AbstractMonteCarloProduct product, double time) {
		RandomVariableInterface x = null;
		try {
			x = product.getValue(time, null);
		} catch (CalculationException e) {
			throw new RuntimeException("Given model failed to deliver market quantity", e);
		}

		if (x instanceof RandomVariableDifferentiableInterface) {
			return (RandomVariableDifferentiableInterface) x;
		}

		throw new RuntimeException("Given model does not have automatic differentiation capabilities.");
	}

	public RandomVariableInterface[][] getTransformationMatrix(double time) {
		final RandomVariableInterface[][] matrix = marketQuantities.stream().
				map(q -> getValue(q.getProduct(), time).getGradient(modelQuantityIDs).values().toArray(new RandomVariableInterface[0])).toArray(RandomVariableInterface[][]::new);

		return pseudoInverter.apply(matrix);
	}

	/**
	 * Calculates the pseudo-inverse of a matrix by calling Apache Commons Math's SVD algorithm parallel on each path.
	 * @param matrix A random matrix, represented by a jagged array (row-column) of {@link RandomVariableInterface}s.
	 * @return The pseudo-inverse, using the same convention as the input matrix.
	 */
	public static RandomVariableInterface[][] getPseudoInverseByParallelAcmSvd(RandomVariableInterface[][] matrix) {

		//Assume that all random variable entries have the same path count and filtration time
		//This might break if we have deterministic values mixed with sampled ones
		int numberOfPaths = matrix[0][0].size();
		double filtrationTime = matrix[0][0].getFiltrationTime();


		final int rowCount = matrix.length;
		final int columnCount = matrix[0].length;

		//Row-column-path array of the inverse
		double[][][] resultByRowColPath = new double[columnCount][rowCount][numberOfPaths];

		//Warning: parallelism via stream.parallel; check for thread pool clash
		IntStream.range(0, numberOfPaths).parallel().forEach(pathIndex -> {
			double[][] matrixOnPath = new double[rowCount][columnCount];
			for (int i = 0; i < rowCount; i++) {
				for (int j = 0; j < columnCount; j++) {
					matrixOnPath[i][j] = matrix[i][j] == null ? 0 : matrix[i][j].get(pathIndex);
				}
			}

			// Get Pseudo Inverse
			RealMatrix pseudoInverse = new SingularValueDecomposition(MatrixUtils.createRealMatrix(matrixOnPath)).getSolver().getInverse();
			for (int j = 0; j < pseudoInverse.getColumnDimension(); j++) {
				double[] columnValues = pseudoInverse.getColumn(j);
				for (int i = 0; i < pseudoInverse.getRowDimension(); i++) {
					resultByRowColPath[i][j][pathIndex] = columnValues[i];
				}
			}
		});

		// Wrap to RandomVariableInterface[][]
		RandomVariableInterface[][] pseudoInverse = new RandomVariableInterface[columnCount][rowCount];
		for (int i = 0; i < pseudoInverse.length; i++) {
			for (int j = 0; j < pseudoInverse[0].length; j++) {
				pseudoInverse[i][j] = new RandomVariable(filtrationTime, resultByRowColPath[i][j]);
			}
		}

		return pseudoInverse;
	}
}
