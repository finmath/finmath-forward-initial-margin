package net.finmath.xva.sensitivityproviders.simmsensitivityproviders;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

import java.util.Set;

public class SIMMPortfolioSensitivityProvider implements SIMMSensitivityProviderInterface {
    private Set<? extends SIMMSensitivityProviderInterface> underlyingSensiProviders;

    public SIMMPortfolioSensitivityProvider(Set<? extends SIMMSensitivityProviderInterface> underlyingSensiProviders) {
        this.underlyingSensiProviders = underlyingSensiProviders;
    }

    @Override
    public RandomVariableInterface getSIMMSensitivity(String productClass, String riskClass, String riskType, String bucketKey, String maturityBucket, String curveIndexName, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
        return underlyingSensiProviders.stream()
                .map(u -> u.getSIMMSensitivity(productClass, riskClass, riskType, bucketKey, maturityBucket, curveIndexName, evaluationTime, model))
                .reduce(RandomVariableInterface::add).orElse(model.getRandomVariableForConstant(0.0));
    }
}
