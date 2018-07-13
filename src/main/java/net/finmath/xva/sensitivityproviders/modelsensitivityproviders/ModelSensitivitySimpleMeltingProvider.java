package net.finmath.xva.sensitivityproviders.modelsensitivityproviders;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;

import java.util.HashMap;
import java.util.Map;

public class ModelSensitivitySimpleMeltingProvider implements ModelSensitivityProviderInterface {

    double maturity;
    private AbstractLIBORMonteCarloProduct product;
    public ModelSensitivitySimpleMeltingProvider(AbstractLIBORMonteCarloProduct product, double maturity)
    {

    }

   @Override
    public Map<String, RandomVariableInterface> getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model, String curveKey) {

        double ttmRatio = (maturity-evaluationTime)/evaluationTime;




        return new HashMap<>();
    }
}
