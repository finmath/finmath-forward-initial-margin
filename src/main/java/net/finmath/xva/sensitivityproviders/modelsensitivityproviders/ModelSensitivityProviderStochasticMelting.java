package net.finmath.xva.sensitivityproviders.modelsensitivityproviders;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

import java.util.Map;

public class ModelSensitivityProviderStochasticMelting implements ModelSensitivityProviderInterface{
    @Override
    public Map<String, RandomVariableInterface> getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model, String curveKey) {
        return null;
    }
}
