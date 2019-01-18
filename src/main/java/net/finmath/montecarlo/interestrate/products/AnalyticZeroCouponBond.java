package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * Representing an analytically priced zero coupon bond in the LIBOR Market Model.
 * @implNote The calculation happens using the simulated forward LIBOR rates, so no conditional expectations are needed.
 */
public class AnalyticZeroCouponBond extends AbstractLIBORMonteCarloProduct {
	private double maturity;

	/**
	 * Creates an {@link AnalyticZeroCouponBond} using the forward rate of the Libor Market Model.
	 * @param maturity The maturity of the zero coupon bond as a floating point time.
	 */
	public AnalyticZeroCouponBond(double maturity) {
		this.maturity = maturity;
	}

	public double getMaturity() {
		return maturity;
	}

	@Override
	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationModel simulation)
			throws CalculationException {
		if (evaluationTime > maturity) {
			return new Scalar(0.0);
		}

		int firstLiborIndex = simulation.getLiborPeriodIndex(evaluationTime);
		int lastLiborIndex = simulation.getLiborPeriodIndex(maturity);

		if (firstLiborIndex == lastLiborIndex) {
			return simulation.getLIBOR(evaluationTime, evaluationTime, maturity).mult(maturity - evaluationTime).add(1.0).pow(-1.0);
		}

		int initialIndex = firstLiborIndex < 0 ? -firstLiborIndex - 1 : firstLiborIndex;
		int finalIndex = lastLiborIndex < 0 ? -lastLiborIndex - 2 : lastLiborIndex;

		double firstLiborTime = simulation.getLiborPeriod(initialIndex);
		double lastLiborTime = simulation.getLiborPeriod(finalIndex);
		RandomVariable bond = simulation.getRandomVariableForConstant(1.0);

		RandomVariable firstBond = firstLiborIndex < 0 ? simulation.getLIBOR(evaluationTime, evaluationTime, firstLiborTime).mult(firstLiborTime - evaluationTime).add(1.0).pow(-1.0) : new RandomVariableFromDoubleArray(1.0);
		RandomVariable lastBond = lastLiborIndex < 0 ? simulation.getLIBOR(evaluationTime, lastLiborTime, maturity).mult(maturity - lastLiborTime).add(1.0).pow(-1.0) : new RandomVariableFromDoubleArray(1.0);

		for (int i = initialIndex; i < finalIndex; i++) {
			double liborPeriodLength = simulation.getLiborPeriod(i + 1) - simulation.getLiborPeriod(i);
			RandomVariable factor = simulation.getLIBOR(simulation.getTimeDiscretization().getTimeIndexNearestLessOrEqual(evaluationTime), i).mult(liborPeriodLength).add(1.0).pow(-1.0);
			bond = bond.mult(factor);
		}

		return bond.mult(firstBond).mult(lastBond);
	}
}
