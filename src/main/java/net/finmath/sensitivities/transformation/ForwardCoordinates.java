package net.finmath.sensitivities.transformation;

import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariable;

/**
 * Provides the model quantities of the forward rate coordinates in a simulated Libor Market Model with automatic differentiation.
 */
public class ForwardCoordinates implements AadCoordinate {

	private RandomVariableDifferentiable castToAad(RandomVariable x) {
		if (x instanceof RandomVariableDifferentiable) {
			return (RandomVariableDifferentiable) x;
		}

		throw new UnsupportedOperationException("Given model does not support automatic differentiation.");
	}

	@Override
	public Stream<RandomVariableDifferentiable> getDomainVariables(LIBORModelMonteCarloSimulationInterface simulation, double evaluationTime) {
		int evaluationTimeIndex = simulation.getTimeIndex(evaluationTime);

		return IntStream.range(0, simulation.getLiborPeriodDiscretization().getNumberOfTimes()).mapToObj(i -> {
			try {
				return castToAad(simulation.getLIBOR(evaluationTimeIndex, i));
			} catch (CalculationException e) {
				return null;
			}
		}).filter(Objects::nonNull);
	}
}
