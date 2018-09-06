package net.finmath.xva.sensitivityproviders.simmsensitivityproviders;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

public interface SIMMSensitivityProviderInterface {

	public RandomVariableInterface getSIMMSensitivity(Simm2Coordinate key, double evaluationTime, LIBORModelMonteCarloSimulationInterface model);//throws SolverException, CloneNotSupportedException, CalculationException;
}

