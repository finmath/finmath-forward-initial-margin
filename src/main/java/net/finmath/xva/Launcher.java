package net.finmath.xva;

import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.initialmargin.SIMMParameter;
import net.finmath.xva.initialmargin.SIMMProduct;
import net.finmath.xva.sensiproducts.SensiProductSimpleSwapBPV;
import net.finmath.xva.sensitivityproviders.modelsensitivityproviders.ModelSensitivitySimpleMeltingProvider;
import net.finmath.xva.sensitivityproviders.simmsensitivityproviders.SIMMPortfolioSensitivityProvider;
import net.finmath.xva.sensitivityproviders.simmsensitivityproviders.SIMMSensitivityProviderInterface;
import net.finmath.xva.sensitivityproviders.simmsensitivityproviders.SIMMTradeSensitivityProvider;
import net.finmath.xva.tradespecifications.SIMMTradeSpecification;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Launcher {

    public static void main(String[] args) throws Exception{

        SIMMParameter parameterSet = new SIMMParameter();
        String calculationCCY = "EUR";


        SIMMTradeSpecification trade = new SIMMTradeSpecification(1.0E6,10.0, "Libor6M");
        SIMMTradeSpecification trade2 = new SIMMTradeSpecification(1.0E6,20.0, "Libor3M");
        Set<SIMMTradeSpecification> tradeSet = new HashSet<>();
        tradeSet.add(trade);
        tradeSet.add(trade2);

        Stream<SIMMSensitivityProviderInterface> tradeSensiProviders = tradeSet.stream()
                .map(tradeSpec -> (SIMMSensitivityProviderInterface) new SIMMTradeSensitivityProvider(
                        new ModelSensitivitySimpleMeltingProvider(
                                new SensiProductSimpleSwapBPV(tradeSpec), tradeSpec.getMaxTimeToMaturity()
                        ),
                        tradeSpec));

        SIMMSensitivityProviderInterface portfolioSensiProvider = new SIMMPortfolioSensitivityProvider(tradeSensiProviders.collect(Collectors.toSet()));

        double marginCalculationTime = 5.0;
        SIMMProduct product = new SIMMProduct(marginCalculationTime,portfolioSensiProvider,parameterSet,calculationCCY,0.0,null);
        LIBORMarketModel model = null;
        LIBORModelMonteCarloSimulation simulation = null;
        RandomVariableInterface result = product.getValue(4.0,simulation);


    }
}
