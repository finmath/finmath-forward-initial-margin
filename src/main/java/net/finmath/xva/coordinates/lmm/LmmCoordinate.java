package net.finmath.xva.coordinates.lmm;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.initialmargin.LIBORMarketModel;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Holds a coordinate (tenor index for rate or volatility) in a simulated Libor Market Model with automatic differentiation.
 */
public class LmmCoordinate implements AadCoordinate {
	private LIBORModelMonteCarloSimulationInterface simulation;
	private SensitivityType sensitivityType;
	private int discretizationIndex;

	public LmmCoordinate(LIBORModelMonteCarloSimulationInterface simulation, SensitivityType sensitivityType, int discretizationIndex) {
		this.simulation = simulation;
		this.sensitivityType = sensitivityType;
		this.discretizationIndex = discretizationIndex;
	}

	/**
	 * Describes with regard to what the sensitivity is.
	 */
	enum SensitivityType {
		RATE,
		VOLATILITY
	}

	private RandomVariableDifferentiableInterface castToAad(RandomVariableInterface x) {
		if (x instanceof RandomVariableDifferentiableInterface) {
			return (RandomVariableDifferentiableInterface)x;
		}

		throw new UnsupportedOperationException("Given model does not support automatic differentiation.");
	}

	@Override
	public RandomVariableDifferentiableInterface getDomainVariable(double evaluationTime) throws CalculationException {
		int evaluationTimeIndex = simulation.getTimeIndex(evaluationTime);

		switch (sensitivityType) {
			case RATE:
				return castToAad(simulation.getLIBOR(evaluationTimeIndex, discretizationIndex));
			default:
				throw new UnsupportedOperationException("Vola coordinate cannot be retrieved yet.");
		}
	}
}
