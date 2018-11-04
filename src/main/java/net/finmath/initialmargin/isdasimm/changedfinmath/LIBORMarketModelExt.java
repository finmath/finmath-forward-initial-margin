/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.initialmargin.isdasimm.changedfinmath;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.marketdata.model.volatilities.AbstractSwaptionMarketData;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModel;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

public class LIBORMarketModelExt extends net.finmath.montecarlo.interestrate.LIBORMarketModel implements LIBORMarketModelInterface, net.finmath.montecarlo.interestrate.LIBORMarketModelInterface {

	public LIBORMarketModelExt(TimeDiscretizationInterface liborPeriodDiscretization,
			AnalyticModelInterface analyticModel, ForwardCurveInterface forwardRateCurve,
			DiscountCurveInterface discountCurve, AbstractLIBORCovarianceModel covarianceModel,
			CalibrationItem[] calibrationItems, Map<String, ?> properties) throws CalculationException {
		super(liborPeriodDiscretization, analyticModel, forwardRateCurve, discountCurve, covarianceModel, calibrationItems,
				properties);
		// TODO Auto-generated constructor stub
	}

	public LIBORMarketModelExt(TimeDiscretizationInterface liborPeriodDiscretization,
			AnalyticModelInterface analyticModel, ForwardCurveInterface forwardRateCurve,
			DiscountCurveInterface discountCurve, AbstractRandomVariableFactory randomVariableFactory,
			AbstractLIBORCovarianceModel covarianceModel, CalibrationItem[] calibrationItems, Map<String, ?> properties)
			throws CalculationException {
		super(liborPeriodDiscretization, analyticModel, forwardRateCurve, discountCurve, randomVariableFactory, covarianceModel,
				calibrationItems, properties);
		// TODO Auto-generated constructor stub
	}

	public LIBORMarketModelExt(TimeDiscretizationInterface liborPeriodDiscretization,
			ForwardCurveInterface forwardRateCurve, AbstractLIBORCovarianceModel covarianceModel,
			AbstractSwaptionMarketData swaptionMarketData) throws CalculationException {
		super(liborPeriodDiscretization, forwardRateCurve, covarianceModel, swaptionMarketData);
		// TODO Auto-generated constructor stub
	}

	public LIBORMarketModelExt(TimeDiscretizationInterface liborPeriodDiscretization,
			ForwardCurveInterface forwardRateCurve, AbstractLIBORCovarianceModel covarianceModel)
			throws CalculationException {
		super(liborPeriodDiscretization, forwardRateCurve, covarianceModel);
		// TODO Auto-generated constructor stub
	}

	public LIBORMarketModelExt(TimeDiscretizationInterface liborPeriodDiscretization,
			ForwardCurveInterface forwardRateCurve, DiscountCurveInterface discountCurve,
			AbstractLIBORCovarianceModel covarianceModel, AbstractSwaptionMarketData swaptionMarketData,
			Map<String, ?> properties) throws CalculationException {
		super(liborPeriodDiscretization, forwardRateCurve, discountCurve, covarianceModel, swaptionMarketData, properties);
		// TODO Auto-generated constructor stub
	}

	public LIBORMarketModelExt(TimeDiscretizationInterface liborPeriodDiscretization,
			ForwardCurveInterface forwardRateCurve, DiscountCurveInterface discountCurve,
			AbstractLIBORCovarianceModel covarianceModel, AbstractSwaptionMarketData swaptionMarketData)
			throws CalculationException {
		super(liborPeriodDiscretization, forwardRateCurve, discountCurve, covarianceModel, swaptionMarketData);
		// TODO Auto-generated constructor stub
	}

	public LIBORMarketModelExt(TimeDiscretizationInterface liborPeriodDiscretization,
			ForwardCurveInterface forwardRateCurve, DiscountCurveInterface discountCurve,
			AbstractLIBORCovarianceModel covarianceModel, CalibrationItem[] calibrationItems, Map<String, ?> properties)
			throws CalculationException {
		super(liborPeriodDiscretization, forwardRateCurve, discountCurve, covarianceModel, calibrationItems, properties);
		// TODO Auto-generated constructor stub
	}

	public LIBORMarketModelExt(TimeDiscretizationInterface liborPeriodDiscretization,
			ForwardCurveInterface forwardRateCurve, DiscountCurveInterface discountCurve,
			AbstractLIBORCovarianceModel covarianceModel) throws CalculationException {
		super(liborPeriodDiscretization, forwardRateCurve, discountCurve, covarianceModel);
		// TODO Auto-generated constructor stub
	}

	@Override
	public RandomVariableInterface getNumeraireOISAdjustmentFactor(double time) throws CalculationException {
		if (this.getNumeraireAdjustments().containsKey(time)) {
			return this.getNumeraireAdjustments().get(time);
		}

		// Get unadjusted Numeraire
		int timeIndex = getLiborPeriodIndex(time);
		RandomVariableInterface numeraireUnadjusted;

		if (timeIndex < 0) {
			// Interpolation of Numeraire: log linear interpolation.
			int upperIndex = -timeIndex - 1;
			int lowerIndex = upperIndex - 1;
			if (lowerIndex < 0) {
				throw new IllegalArgumentException("Numeraire requested for time " + time + ". Unsupported");
			}

			double alpha = (time - getLiborPeriod(lowerIndex)) / (getLiborPeriod(upperIndex) - getLiborPeriod(lowerIndex));
			numeraireUnadjusted = getNumerairetUnAdjustedAtLIBORIndex(upperIndex).log().mult(alpha).add(getNumerairetUnAdjustedAtLIBORIndex(lowerIndex).log().mult(1.0 - alpha)).exp();
		} else {
			numeraireUnadjusted = getNumerairetUnAdjustedAtLIBORIndex(timeIndex);
		}

		RandomVariableInterface adjustment = getRandomVariableForConstant(numeraireUnadjusted.invert().getAverage()).div(getDiscountCurve().getDiscountFactor(time));

		return adjustment;
	}

	/**
	 * Calculate P(T;t) directly from Libors without using conditional expectation
	 *
	 * @param T final time
	 * @param t initial time
	 * @return P(T ; t)
	 * @throws CalculationException
	 */
	@Override
	public RandomVariableInterface getForwardBondLibor(double T, double t) throws CalculationException {

		int firstLiborIndex = getLiborPeriodIndex(t);
		int lastLiborIndex = getLiborPeriodIndex(T);

		if (firstLiborIndex == lastLiborIndex) {
			return getLIBOR(t, t, T).mult(T - t).add(1.0).pow(-1.0);
		}

		int initialIndex = firstLiborIndex < 0 ? -firstLiborIndex - 1 : firstLiborIndex;
		int finalIndex = lastLiborIndex < 0 ? -lastLiborIndex - 2 : lastLiborIndex;

		double firstLiborTime = getLiborPeriod(initialIndex);
		double lastLiborTime = getLiborPeriod(finalIndex);
		RandomVariableInterface bond = getRandomVariableForConstant(1.0);

		RandomVariableInterface firstBond = firstLiborIndex < 0 ? getLIBOR(t, t, firstLiborTime).mult(firstLiborTime - t).add(1.0).pow(-1.0) : new RandomVariable(1.0);
		RandomVariableInterface lastBond = lastLiborIndex < 0 ? getLIBOR(t, lastLiborTime, T).mult(T - lastLiborTime).add(1.0).pow(-1.0) : new RandomVariable(1.0);

		for (int i = initialIndex; i < finalIndex; i++) {
			double liborPeriodLength = getLiborPeriod(i + 1) - getLiborPeriod(i);
			RandomVariableInterface factor = getLIBOR(getTimeDiscretization().getTimeIndexNearestLessOrEqual(t), i).mult(liborPeriodLength).add(1.0).pow(-1.0);
			bond = bond.mult(factor);
		}

		return bond.mult(firstBond).mult(lastBond);
	}

	/**
	 * Returns the forward bond P(T;t) from on the OIS curve for a given Libor market model
	 *
	 * @param T     The maturity of the forward bond
	 * @param t     The inception of the forward bond
	 * @param model The Libor market model
	 * @return The forward bond P(T;t) on the OIS curve
	 * @throws CalculationException
	 */
	@Override
	public RandomVariableInterface getForwardBondOIS(double T, double t) throws CalculationException {

		// Get bondOIS = P^OIS(T;t) = P^L(T;t)*a_t/a_T
		RandomVariableInterface adjustment_t = getNumeraireOISAdjustmentFactor(t);
		RandomVariableInterface adjustment_T = getNumeraireOISAdjustmentFactor(T);

		return getForwardBondLibor(T, t).mult(adjustment_t).div(adjustment_T);
	}
}


