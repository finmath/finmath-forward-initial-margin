package net.finmath.xva.sensitivityproviders.simmsensitivityproviders;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Defines a method to provide a sensitivity in the ISDA SIMM framework.
 */
public interface SIMMSensitivityProviderInterface {

	/**
	 * @param coordinate Determines which sensitivity (with respect to risk class, curve and bucket) shall be retrieved.
	 * @param evaluationTime The time of the forward sensitivity.
	 * @param model The model that is used to calculate model sensitivities.
	 * @return A forward sensitivity according to the ISDA SIMM framework.
	 */
	RandomVariableInterface getSIMMSensitivity(SimmSensitivityCoordinate coordinate, double evaluationTime, LIBORModelMonteCarloSimulationInterface model);//throws SolverException, CloneNotSupportedException, CalculationException;
}
