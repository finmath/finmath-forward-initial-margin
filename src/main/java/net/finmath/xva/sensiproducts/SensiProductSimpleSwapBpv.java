package net.finmath.xva.sensiproducts;


import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.MonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.tradespecifications.SIMMTradeSpecification;

/**
 * Approximates the sensitivity of a swap using its base point value.
 */
public class SensiProductSimpleSwapBpv extends AbstractMonteCarloProduct {

	private SIMMTradeSpecification tradeSpec;

	public SensiProductSimpleSwapBpv(SIMMTradeSpecification tradeSpec) {
		this.tradeSpec = tradeSpec;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, MonteCarloSimulationInterface model) throws CalculationException {
		double bpv = tradeSpec.getNotional() * (tradeSpec.getIRCurve().getPeriodLength() / tradeSpec.getIRCurve().getDayInYears()) / 10000.0;
		return model.getRandomVariableForConstant(bpv);
	}
}
