package net.finmath.xva.sensitivityproviders.modelsensitivityproviders;

import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

public class ModelSensitivitySimpleMeltingProvider implements ModelSensitivityProviderInterface {

    double maturity;
    private AbstractMonteCarloProduct product;
    public ModelSensitivitySimpleMeltingProvider(AbstractMonteCarloProduct product, double maturity)
    {

    }

   @Override
    public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model, String curveKey) {

        double ttmRatio = (maturity-evaluationTime)/evaluationTime;




        return null;
    }
}
