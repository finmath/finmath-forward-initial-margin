package net.finmath.xva.coordinates.lmm;

import java.util.Arrays;
import java.util.stream.IntStream;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.AnalyticDiscountZeroCouponBond;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Calculates the par-swap rate for a given float versus fix tenor structure.
 */
public class SwapMarketRateProduct extends AbstractLIBORMonteCarloProduct {
	private final TimeDiscretizationInterface floatTenor;
	private final TimeDiscretizationInterface fixTenor;
	private final AbstractLIBORMonteCarloProduct[] floatTenorDiscountFactors;
	private final AbstractLIBORMonteCarloProduct[]fixTenorDiscountFactors;

	public SwapMarketRateProduct(TimeDiscretizationInterface floatTenor, TimeDiscretizationInterface fixTenor) {
		this.floatTenor = floatTenor;
		this.fixTenor = fixTenor;
		floatTenorDiscountFactors = Arrays.stream(floatTenor.getAsDoubleArray()).
				mapToObj(AnalyticDiscountZeroCouponBond::new).
				toArray(AbstractLIBORMonteCarloProduct[]::new);
		fixTenorDiscountFactors = Arrays.stream(fixTenor.getAsDoubleArray()).
				mapToObj(AnalyticDiscountZeroCouponBond::new).
				toArray(AbstractLIBORMonteCarloProduct[]::new);
	}

	public TimeDiscretizationInterface getFloatTenor() {
		return floatTenor;
	}

	public TimeDiscretizationInterface getFixTenor() {
		return fixTenor;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface simulation)
			throws CalculationException {
		RandomVariableInterface numerator = IntStream.range(0, floatTenor.getNumberOfTimeSteps()).
				mapToObj(i -> getFloatPaymentSummand(i, evaluationTime, simulation)).
				reduce(simulation.getRandomVariableForConstant(0.0), RandomVariableInterface::add);

		RandomVariableInterface denominator = IntStream.range(0, fixTenor.getNumberOfTimeSteps()).
				mapToObj(i -> getFixPaymentSummand(i, evaluationTime, simulation)).
				reduce(simulation.getRandomVariableForConstant(0.0), RandomVariableInterface::add);

		return numerator.div(denominator);
	}

	private RandomVariableInterface getFixPaymentSummand(int i, double evaluationTime, LIBORModelMonteCarloSimulationInterface simulation) {
		double periodStartTime = fixTenor.getTime(i);
		double periodEndTime = fixTenor.getTime(i+1);
		double periodLength = periodEndTime - periodStartTime;
		try {
			return fixTenorDiscountFactors[i+1].getValue(evaluationTime, simulation).mult(periodLength);
		} catch (CalculationException e) {
			throw new RuntimeException(e);
		}
	}

	private RandomVariableInterface getFloatPaymentSummand(int i, double evaluationTime, LIBORModelMonteCarloSimulationInterface simulation) {
		double periodStartTime = floatTenor.getTime(i);
		double periodEndTime = floatTenor.getTime(i+1);
		double periodLength = periodEndTime - periodStartTime;

		try {
			//floating rate is paid out at periodEndTime
			RandomVariableInterface discountFactorToPeriodEnd = floatTenorDiscountFactors[i+1].getValue(evaluationTime, simulation);

			return simulation.getLIBOR(evaluationTime, periodStartTime, periodEndTime).mult(discountFactorToPeriodEnd).mult(periodLength);
		} catch (CalculationException e) {
			throw new RuntimeException(e);
		}
	}
}
