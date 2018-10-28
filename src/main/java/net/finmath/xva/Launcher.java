package net.finmath.xva;

import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.initialmargin.SIMMParameter;
import net.finmath.xva.initialmargin.SimmModality;
import net.finmath.xva.initialmargin.SimmProduct;
import net.finmath.xva.sensitivityproviders.timelines.SimmBpvTimeline;
import net.finmath.xva.sensitivityproviders.timelines.SimmCompositeTimeline;
import net.finmath.xva.sensitivityproviders.timelines.SimmSensitivityTimeline;
import net.finmath.xva.tradespecifications.Indices;
import net.finmath.xva.tradespecifications.SIMMTradeSpecification;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Launcher {

	public static void main(String[] args) throws Exception {

		SIMMParameter parameterSet = new SIMMParameter();
		String calculationCCY = "EUR";

		SIMMTradeSpecification trade = new SIMMTradeSpecification(1.0E6, 10.0, Indices.getLibor("EUR", "6M"));
		SIMMTradeSpecification trade2 = new SIMMTradeSpecification(1.0E6, 20.0, Indices.getLibor("EUR", "3M"));
		Set<SIMMTradeSpecification> tradeSet = new HashSet<>();
		tradeSet.add(trade);
		tradeSet.add(trade2);

		Stream<SimmSensitivityTimeline> tradeSensiProviders = tradeSet.stream().map(SimmBpvTimeline::new);

		SimmSensitivityTimeline portfolioSensiProvider = new SimmCompositeTimeline(tradeSensiProviders.collect(Collectors.toSet()));

		double marginCalculationTime = 5.0;
		SimmProduct product = new SimmProduct(marginCalculationTime, portfolioSensiProvider, new SimmModality(parameterSet, calculationCCY, 0.0), null, null);
		LIBORMarketModel model = null;
		LIBORModelMonteCarloSimulation simulation = null;
		RandomVariableInterface result = product.getValue(4.0, simulation);
	}
}
