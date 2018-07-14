package net.finmath.xva.sensitivityproviders.simmsensitivityproviders;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Defines a method to provide a sensitivity in the ISDA SIMM framework.
 */
public interface SIMMSensitivityProviderInterface {

	RandomVariableInterface getSIMMSensitivity(SimmSensitivityCoordinate coordinate, double evaluationTime, LIBORModelMonteCarloSimulationInterface model);//throws SolverException, CloneNotSupportedException, CalculationException;
}
