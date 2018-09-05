package net.finmath.xva.sensitivityproviders.simmsensitivityproviders;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.sensitivityproviders.modelsensitivityproviders.ModelSensitivityProviderInterface;
import net.finmath.xva.tradespecifications.SIMMSensitivityKey;
import net.finmath.xva.tradespecifications.SIMMTradeSpecification;

public class SIMMTradeSensitivityProvider implements SIMMSensitivityProviderInterface {

    ModelSensitivityProviderInterface   modelSensitivityProviderInterface;
    SIMMTradeSpecification simmTradeSpecification;

    public SIMMTradeSensitivityProvider(ModelSensitivityProviderInterface modelSensitivityProviderInterface, SIMMTradeSpecification simmTradeSpecification) {
        this.modelSensitivityProviderInterface = modelSensitivityProviderInterface;
        this.simmTradeSpecification = simmTradeSpecification;
    }

    public ModelSensitivityProviderInterface getModelSensitivityProviderInterface() {
        return modelSensitivityProviderInterface;
    }

    public SIMMTradeSpecification getSimmTradeSpecification() {
        return simmTradeSpecification;
    }

    public RandomVariableInterface getSIMMSensitivity(SIMMSensitivityKey key, // null if riskClass is not IR
                                                      double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {

        /*
        - Check remaining maturity
        - Look in Trade Spec whether
        - Ask Model Provider for Model Sensis





         */
        return null;
    }


}
