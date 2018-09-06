package net.finmath.xva.sensitivityproviders.modelsensitivityproviders;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

public interface ModelSensitivityProviderInterface {

	//@TODO specify curveKey and return type.

	/**
	 * @param evaluationTime
	 * @param model
	 * @param curveKey
	 * @return
	 */
	RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model, String curveKey);
}
