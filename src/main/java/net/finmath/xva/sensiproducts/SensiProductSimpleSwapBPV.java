package net.finmath.xva.sensiproducts;


import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.tradespecifications.SIMMTradeSpecification;

/**
 * Approximates the sensitivity of a swap using its base point value.
 */
public class SensiProductSimpleSwapBPV extends AbstractLIBORMonteCarloProduct {

	private SIMMTradeSpecification tradeSpecification;

	public SensiProductSimpleSwapBPV(SIMMTradeSpecification tradeSpecification) {
		this.tradeSpecification = tradeSpecification;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		double bpv = tradeSpecification.getNotional() * (tradeSpecification.getMaxTimeToMaturity() - evaluationTime) / 10000;
		return model.getRandomVariableForConstant(bpv);
	}
}
