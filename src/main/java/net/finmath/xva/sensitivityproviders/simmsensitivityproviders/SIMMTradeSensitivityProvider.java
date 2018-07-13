package net.finmath.xva.sensitivityproviders.simmsensitivityproviders;

import net.finmath.xva.sensitivityproviders.modelsensitivityproviders.ModelSensitivityProviderInterface;
import net.finmath.xva.tradespecifications.SIMMTradeSpecification;

public class SIMMTradeSensitivityProvider {

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
}
