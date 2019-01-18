package net.finmath.xva.coordinates.lmm;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.sensitivities.transformation.TransformationAlgorithms;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

@RunWith(Theories.class)
public class TransformationAlgorithmsTest {

	@DataPoint
	public static final double[] Diagonal1 = new double[]{ 1, 2, 3, 4, 5 };

	@DataPoint
	public static final double[] Diagonal2 = new double[] { 0.5, 1.0, 1.5 };

	@DataPoint
	public static final double[] DiagonalBig = new double[] { 1E6, 2E7, 3E9, 4E12, 5E8 };

	@DataPoint
	public static final double[] DiagonalTiny = new double[] { 1E-6, 1E-10, 2E-8 };

	@Theory
	public void testGetPseudoInverseByParallelAcmSvdWithDeterministicDiagonalMatrix(double[] diagonal) {

		RandomVariable[][] randomMatrix = new RandomVariable[diagonal.length][diagonal.length];

		for (int i = 0; i < diagonal.length; i++) {
			randomMatrix[i] = new RandomVariable[diagonal.length];

			for (int j = 0; j < diagonal.length; j++) {
				randomMatrix[i][j] = new Scalar(i == j ? diagonal[i] : 0.0);
			}
		}

		final RandomVariable[][] pseudoInverse = TransformationAlgorithms.getPseudoInverseByParallelAcmSvd(randomMatrix);

		assertThat(
				IntStream.range(0, diagonal.length).
				mapToObj(i -> pseudoInverse[i][i].get(0) - 1.0/diagonal[i]).
				collect(Collectors.toList()),
				everyItem(is(closeTo(0.0, 1E-3)))
				);
	}

	@Theory
	public void testGetPseudoInverseByParallelAcmSvdWithTwoStateDiagonalMatrix(double[] diagonal1, double[] diagonal2) {

		int dimension = Math.max(diagonal1.length, diagonal2.length);

		RandomVariable[][] randomMatrix = new RandomVariable[dimension][dimension];

		double[] nonDiagonal = new double[] { 0.0, 0.0 };

		for (int i = 0; i < dimension; i++) {
			randomMatrix[i] = new RandomVariable[dimension];

			for (int j = 0; j < dimension; j++) {
				randomMatrix[i][j] = new RandomVariableFromDoubleArray(Double.NEGATIVE_INFINITY, i == j ? new double[] {
						i < diagonal1.length ? diagonal1[i] : 0.0,
								i < diagonal2.length ? diagonal2[i] : 0.0,
				} : nonDiagonal);
			}
		}

		final RandomVariable[][] pseudoInverse = TransformationAlgorithms.getPseudoInverseByParallelAcmSvd(randomMatrix);

		assertThat(
				IntStream.range(0, dimension).
				mapToObj(i -> pseudoInverse[i][i].get(0) - (i < diagonal1.length ? 1.0/diagonal1[i] : 0.0)).
				collect(Collectors.toList()),
				everyItem(is(closeTo(0.0, 1E-3)))
				);

		assertThat(
				IntStream.range(0, dimension).
				mapToObj(i -> pseudoInverse[i][i].get(1) - (i < diagonal2.length ? 1.0/diagonal2[i] : 0.0)).
				collect(Collectors.toList()),
				everyItem(is(closeTo(0.0, 1E-3)))
				);
	}
}