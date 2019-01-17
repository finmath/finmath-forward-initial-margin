package net.finmath.xva;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation;
import net.finmath.sensitivities.GradientProduct;
import net.finmath.sensitivities.GradientProductComposite;
import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.sensitivities.simm2.products.ApproximateAnnuity;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.initialmargin.simm2.SimmModality;
import net.finmath.xva.initialmargin.simm2.SimmProduct;
import net.finmath.xva.tradespecifications.Indices;
import net.finmath.xva.tradespecifications.SIMMTradeSpecification;

public class Launcher {

	public static void main(String[] args) throws Exception {

		String calculationCCY = "EUR";

		SIMMTradeSpecification trade = new SIMMTradeSpecification(1.0E6, 10.0, Indices.getLibor("EUR", "6M"));
		SIMMTradeSpecification trade2 = new SIMMTradeSpecification(1.0E6, 20.0, Indices.getLibor("EUR", "3M"));
		Set<SIMMTradeSpecification> tradeSet = new HashSet<>();
		tradeSet.add(trade);
		tradeSet.add(trade2);

		Stream<GradientProduct<SimmCoordinate>> tradeSensiProviders = tradeSet.stream().map(ApproximateAnnuity::new);

		GradientProduct<SimmCoordinate> portfolioSensiProvider = new GradientProductComposite<>(tradeSensiProviders.collect(Collectors.toSet()));

		double marginCalculationTime = 5.0;
		SimmProduct product = new SimmProduct(marginCalculationTime, portfolioSensiProvider, new SimmModality(calculationCCY, 0.0));
		LIBORMarketModel model = null;
		LIBORModelMonteCarloSimulation simulation = null;
		RandomVariableInterface result = product.getValue(4.0, simulation);
	}
}
