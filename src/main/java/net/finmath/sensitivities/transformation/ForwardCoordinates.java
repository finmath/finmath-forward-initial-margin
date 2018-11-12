package net.finmath.sensitivities.transformation;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Provides the model quantities of the forward rate coordinates in a simulated Libor Market Model with automatic differentiation.
 */
public class ForwardCoordinates implements AadCoordinate {

	private RandomVariableDifferentiableInterface castToAad(RandomVariableInterface x) {
		if (x instanceof RandomVariableDifferentiableInterface) {
			return (RandomVariableDifferentiableInterface) x;
		}

		throw new UnsupportedOperationException("Given model does not support automatic differentiation.");
	}

	@Override
	public Stream<RandomVariableDifferentiableInterface> getDomainVariables(LIBORModelMonteCarloSimulationInterface simulation, double evaluationTime) {
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
