package net.finmath.lch.initialmargin.swapclear.products;
/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28.02.2015
 */

import java.time.LocalDateTime;

import net.finmath.lch.initialmargin.simulation.modeldata.StochasticCurve;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.Currency;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.CurveName;
import net.finmath.lch.initialmargin.swapclear.products.components.LchNotional;
import net.finmath.lch.initialmargin.swapclear.products.components.LchSwapLeg;
import net.finmath.lch.initialmargin.swapclear.products.indices.LchAbstractIndex;
import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.AbstractTermStructureMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.Schedule;

/**
 * Create a swap from schedules, notional, indices and spreads (fixed coupons).
 *
 * The getValue method of this class simple returns
 * <code>
 * 	legReceiver.getValue(evaluationTime, model).sub(legPayer.getValue(evaluationTime, model))
 * </code>
 * where <code>legReceiver</code> and <code>legPayer</code> are {@link net.finmath.montecarlo.interestrate.products.SwapLeg}s.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class LchSwap extends AbstractTermStructureMonteCarloProduct {

	private final LchSwapLeg 						legReceiver;
	private final LchSwapLeg 						legPayer;
	private final Currency							currency;
	private final CurveName							curveName;
	
	
	/**
	 * Create a swap which values as <code>legReceiver - legPayer</code>.
	 *
	 * @param legReceiver The receiver leg.
	 * @param legPayer The payer leg.
	 */
	public LchSwap(final Currency currency, final CurveName curveName, final LchSwapLeg legReceiver, final LchSwapLeg legPayer) {
		super();
		this.currency = currency;
		this.curveName = curveName;
		this.legReceiver = legReceiver;
		this.legPayer = legPayer;
	}
	
	/**
	 * Create a swap from schedules, notional, indices and spreads (fixed coupons). 
	 * Each LchSwap is directly set up with its own model, as the underlying forward curve of the swap indirectly determines the model to be used.
	 * 
	 * @param notional The notional.
	 * @param scheduleReceiveLeg The period schedule for the receiver leg.
	 * @param indexReceiveLeg The index of the receiver leg, may be null if no index is received.
	 * @param spreadReceiveLeg The constant spread or fixed coupon rate of the receiver leg.
	 * @param schedulePayLeg The period schedule for the payer leg.
	 * @param indexPayLeg The index of the payer leg, may be null if no index is paid.
	 * @param spreadPayLeg The constant spread or fixed coupon rate of the payer leg.
	 */
	public LchSwap(final Currency currency, final CurveName curveName, final LchNotional notional,
			final Schedule scheduleReceiveLeg, final LchAbstractIndex indexReceiveLeg, final double spreadReceiveLeg,
			final Schedule schedulePayLeg, final LchAbstractIndex indexPayLeg, final double spreadPayLeg) {
		super();
		this.currency = currency;
		this.curveName = curveName;
		if (currency == null || curveName == null) {
			throw new IllegalArgumentException("Can't set up swap without currency or curve specification. Current currency: " + currency + " and curve: " + curveName);
		}
		legReceiver = new LchSwapLeg(scheduleReceiveLeg, notional, indexReceiveLeg, spreadReceiveLeg, false /*isPayLeg = false */, false);
		legPayer = new LchSwapLeg(schedulePayLeg, notional, indexPayLeg, spreadPayLeg, true /*isPayLeg = true */, false); 
	}


	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		RandomVariable value = legReceiver.getValue(evaluationTime, model);
		if(legPayer != null) {
			value = value.add(legPayer.getValue(evaluationTime, model));
		}
		return value;
	}
	

	// Added method: Values the swap under the zero rate model with curveshifts if provided
	public RandomVariable getValue(final LocalDateTime evaluationDate, final ZeroRateModel forwardCurveModel, final StochasticCurve forwardCurveShifts, final ZeroRateModel discountCurveModel, final StochasticCurve discountCurveShifts, Integer pathOrState) throws CalculationException {
		RandomVariable value = ((LchSwapLeg) legReceiver).getValue(evaluationDate, forwardCurveModel, forwardCurveShifts, discountCurveModel, discountCurveShifts, pathOrState);
		if(legPayer != null) {
			value = value.add(((LchSwapLeg) legPayer).getValue(evaluationDate, forwardCurveModel, forwardCurveShifts, discountCurveModel, discountCurveShifts, pathOrState));
		}
		return value;
	}
	
	
	
	// Added method: Returns the receiver leg
	public LchSwapLeg getLegReceiver() {
		return legReceiver;
	}
	
	
	// Added method: Returns the payer leg
	public LchSwapLeg getLegPayer() {
		return legPayer;
	}
		
	
	// Last payment date of the swap
	public double getLastPaymentDate() {
		return Math.max(legPayer.getLastPaymentDate(), legReceiver.getLastPaymentDate());
	}
	
	
	// Added method
	public CurveName getCurveName() {
		return curveName;
	}

	
	// Added method
	public Currency getLchCurrency() {
		return currency;
	}


	@Override
	public String toString() {
		return "Swap [legReceiver=" + legReceiver + ", legPayer=" + legPayer + "]";
	}
}
