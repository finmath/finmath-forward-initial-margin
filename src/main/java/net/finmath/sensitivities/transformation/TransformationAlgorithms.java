package net.finmath.sensitivities.transformation;

import java.util.stream.IntStream;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.Scalar;

public final class TransformationAlgorithms {
	private TransformationAlgorithms() {}

	/**
	 * Calculates the pseudo-inverse of a matrix by calling Apache Commons Math's SVD algorithm parallel on each path.
	 * @param matrix A random matrix, represented by a jagged array (row-column) of {@link RandomVariableInterface}s.
	 * @return The pseudo-inverse, using the same convention as the input matrix.
	 */
	public static RandomVariableInterface[][] getPseudoInverseByParallelAcmSvd(RandomVariableInterface[][] matrix) {

		//Assume that all random variable entries have the same path count and filtration time
		//This might break if we have deterministic values mixed with sampled ones (or even worse different path counts)
		int numberOfPaths = matrix[0][0].size();
		double filtrationTime = matrix[0][0].getFiltrationTime();


		final int rowCount = matrix.length;
		final int columnCount = matrix[0].length;

		//Row-column-path array of the inverse (row count = column count from input)
		double[][][] resultByRowColPath = new double[columnCount][rowCount][numberOfPaths];

		//Warning: parallelism via stream.parallel; check for thread pool clash
		IntStream.range(0, numberOfPaths).parallel().forEach(pathIndex -> {
			double[][] matrixOnPath = new double[rowCount][columnCount];
			for (int i = 0; i < rowCount; i++) {
				for (int j = 0; j < columnCount; j++) {
					matrixOnPath[i][j] = matrix[i][j] == null ? 0 : matrix[i][j].get(pathIndex);
				}
			}

			//Use SVD...getInverse() on each path
			RealMatrix pseudoInverse = new SingularValueDecomposition(MatrixUtils.createRealMatrix(matrixOnPath)).getSolver().getInverse();
			for (int j = 0; j < pseudoInverse.getColumnDimension(); j++) {
				double[] columnValues = pseudoInverse.getColumn(j);
				for (int i = 0; i < pseudoInverse.getRowDimension(); i++) {
					resultByRowColPath[i][j][pathIndex] = columnValues[i];
				}
			}
		});

		RandomVariableInterface[][] pseudoInverse = new RandomVariableInterface[columnCount][rowCount];
		for (int i = 0; i < pseudoInverse.length; i++) {
			for (int j = 0; j < pseudoInverse[0].length; j++) {
				pseudoInverse[i][j] = new RandomVariable(filtrationTime, resultByRowColPath[i][j]);
			}
		}

		return pseudoInverse;
	}

	public static RandomVariableInterface[] multiplyVectorMatrix(RandomVariableInterface[] vector, RandomVariableInterface[][] matrix) {
		final int columnCount = matrix[0].length;
		RandomVariableInterface[] product = new RandomVariableInterface[columnCount];

		for (int column = 0; column < columnCount; column++) {
			product[column] = new Scalar(0.0);
			for (int row = 0; row < vector.length; row++) {
				RandomVariableInterface currentEntry;

				if (vector[row] == null || matrix[row][column] == null) {
					currentEntry = new Scalar(0.0);
				} else {
					currentEntry = vector[row].mult(matrix[row][column]);
				}
				product[column] = product[column].add(currentEntry);
			}
		}
		return product;
	}
}
