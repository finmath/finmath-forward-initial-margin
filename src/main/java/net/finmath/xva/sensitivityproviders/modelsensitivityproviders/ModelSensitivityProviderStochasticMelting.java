package net.finmath.xva.sensitivityproviders.modelsensitivityproviders;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

public class ModelSensitivityProviderStochasticMelting implements ModelSensitivityProviderInterface {
	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model, String curveKey) {
		//TODO: melt down the annuity and use the numéraire of the model
		return null;
	}
}
