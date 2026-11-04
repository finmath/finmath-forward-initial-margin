/*
 * Created on 22.11.2009
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.lch.initialmargin.swapclear.products.components;

import java.time.LocalDateTime;

import net.finmath.lch.initialmargin.simulation.modeldata.StochasticCurve;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.swapclear.products.indices.LchAbstractIndex;
import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.FloatingpointDate;

/**
 * A period. A period has references to the index (coupon) and the notional.
 * It provides the fixing date for the index, the period length, and the payment date.
 *
 * @author Christian Fries
 * @version 1.1
 */
public class LchPeriod extends LchAbstractPeriod {

	private static final long serialVersionUID = -7107623461781510475L;
	private final boolean couponFlow;
	private final boolean notionalFlow;
	private final boolean payer;
	private final boolean isExcludeAccruedInterest;
	private final LchAbstractIndex index;

	/**
	 * Create a simple period with notional and index (coupon) flow.
	 *
	 * @param referenceDate The date corresponding to time \( t = 0 \).
	 * @param periodStart The period start.
	 * @param periodEnd The period end.
	 * @param fixingDate The fixing date (as double).
	 * @param paymentDate The payment date (as double).
	 * @param notional The notional object relevant for this period.
	 * @param index The index (used for coupon calculation) associated with this period.
	 * @param daycountFraction The daycount fraction (<code>coupon = index(fixingDate) * daycountFraction</code>).
	 * @param couponFlow If true, the coupon will be payed. Otherwise there will be not coupon flow.
	 * @param notionalFlow If true, there will be a positive notional flow at period start (but only if peirodStart &gt; evaluationTime) and a negative notional flow at period end (but only if periodEnd &gt; evaluationTime). Otherwise there will be no notional flows.
	 * @param payer If true, the period will be a payer period, i.e. notional and coupon at period end are payed (negative). Otherwise it is a receiver period.
	 * @param isExcludeAccruedInterest If the true, the valuation will exclude accrued interest, if any.
	 */
	public LchPeriod(final LocalDateTime referenceDate, final double periodStart, final double periodEnd, final double fixingDate,
			final double paymentDate, final LchNotional notional, final LchAbstractIndex index, final double daycountFraction,
			final boolean couponFlow, final boolean notionalFlow, final boolean payer, final boolean isExcludeAccruedInterest) {
		super(referenceDate, periodStart, periodEnd, fixingDate, paymentDate, notional, index, daycountFraction);
		this.couponFlow = couponFlow;
		this.notionalFlow = notionalFlow;
		this.payer = payer;
		this.isExcludeAccruedInterest = isExcludeAccruedInterest;
		this.index = index;
	}

	/**
	 * Create a simple period with notional and index (coupon) flow.
	 *
	 * @param periodStart The period start.
	 * @param periodEnd The period end.
	 * @param fixingDate The fixing date (as double).
	 * @param paymentDate The payment date (as double).
	 * @param notional The notional object relevant for this period.
	 * @param index The index (used for coupon calculation) associated with this period.
	 * @param daycountFraction The daycount fraction (<code>coupon = index(fixingDate) * daycountFraction</code>).
	 * @param couponFlow If true, the coupon will be payed. Otherwise there will be not coupon flow.
	 * @param notionalFlow If true, there will be a positive notional flow at period start (but only if peirodStart &gt; evaluationTime) and a negative notional flow at period end (but only if periodEnd &gt; evaluationTime). Otherwise there will be no notional flows.
	 * @param payer If true, the period will be a payer period, i.e. notional and coupon at period end are payed (negative). Otherwise it is a receiver period.
	 * @param isExcludeAccruedInterest If the true, the valuation will exclude accrued interest, if any.
	 */
	public LchPeriod(final double periodStart, final double periodEnd, final double fixingDate,
			final double paymentDate, final LchNotional notional, final LchAbstractIndex index, final double daycountFraction,
			final boolean couponFlow, final boolean notionalFlow, final boolean payer, final boolean isExcludeAccruedInterest) {
		this(null, periodStart, periodEnd, fixingDate, paymentDate, notional, index, daycountFraction, couponFlow, notionalFlow, payer, isExcludeAccruedInterest);
	}

	/**
	 * Create a simple period with notional and index (coupon) flow.
	 *
	 * The valuation does not exclude the accrued interest, i.e., the valuation reports a so called dirty price.
	 *
	 * @param periodStart The period start.
	 * @param periodEnd The period end.
	 * @param fixingDate The fixing date (as double).
	 * @param paymentDate The payment date (as double).
	 * @param notional The notional object relevant for this period.
	 * @param index The index (used for coupon calculation) associated with this period.
	 * @param daycountFraction The daycount fraction (<code>coupon = index(fixingDate) * daycountFraction</code>).
	 * @param couponFlow If true, the coupon will be payed. Otherwise there will be not coupon flow.
	 * @param notionalFlow If true, there will be a positive notional flow at period start (but only if peirodStart &gt; evaluationTime) and a negative notional flow at period end (but only if periodEnd &gt; evaluationTime). Otherwise there will be no notional flows.
	 * @param payer If true, the period will be a payer period, i.e. notional and coupon at period end are payed (negative). Otherwise it is a receiver period.
	 */
	public LchPeriod(final double periodStart, final double periodEnd, final double fixingDate,
			final double paymentDate, final LchNotional notional, final LchAbstractIndex index, final double daycountFraction,
			final boolean couponFlow, final boolean notionalFlow, final boolean payer) {
		this(periodStart, periodEnd, fixingDate, paymentDate, notional, index, daycountFraction, couponFlow, notionalFlow, payer, false);
	}

	/**
	 * Create a simple period with notional and index (coupon) flow.
	 *
	 * The valuation does not exclude the accrued interest, i.e., the valuation reports a so called dirty price.
	 *
	 * @param periodStart The period start.
	 * @param periodEnd The period end.
	 * @param fixingDate The fixing date (as double).
	 * @param paymentDate The payment date (as double).
	 * @param notional The notional object relevant for this period.
	 * @param index The index (coupon) associated with this period.
	 * @param couponFlow If true, the coupon will be payed. Otherwise there will be not coupon flow.
	 * @param notionalFlow If true, there will be a positive notional flow at period start (but only if peirodStart &gt; evaluationTime) and a negative notional flow at period end (but only if periodEnd &gt; evaluationTime). Otherwise there will be no notional flows.
	 * @param payer If true, the period will be a payer period, i.e. notional and coupon at period end are payed (negative). Otherwise it is a receiver period.
	 */
	public LchPeriod(final double periodStart, final double periodEnd, final double fixingDate,
			final double paymentDate, final LchNotional notional, final LchAbstractIndex index,
			final boolean couponFlow, final boolean notionalFlow, final boolean payer) {
		this(periodStart, periodEnd, fixingDate, paymentDate, notional, index, periodEnd-periodStart, couponFlow, notionalFlow, payer);
	}

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		double productToModelTimeOffset = 0;
		try {
			if(this.getReferenceDate() != null && model.getReferenceDate() != null) {
				productToModelTimeOffset = FloatingpointDate.getFloatingPointDateFromDate(model.getReferenceDate(), this.getReferenceDate());
			}
		}
		catch(final UnsupportedOperationException e) {
			// @TODO Models that do not provide a reference date will become disfunctional in future releases.
		}

		if(evaluationTime >= productToModelTimeOffset + getPaymentDate()) {
			return new RandomVariableFromDoubleArray(0.0);
		}

		// Get random variables
		final RandomVariable	notionalAtPeriodStart	= getNotional().getNotionalAtPeriodStart(this, model);
		final RandomVariable	numeraireAtEval			= model.getNumeraire(evaluationTime);
		final RandomVariable	numeraire				= model.getNumeraire(productToModelTimeOffset + getPaymentDate());
		// @TODO Add support for weighted Monte-Carlo.
		//        RandomVariable	monteCarloProbabilities	= model.getMonteCarloWeights(getPaymentDate());

		RandomVariable values;

		// Calculate numeraire relative value of coupon flows
		if(couponFlow) {
			values = getCoupon(productToModelTimeOffset + getFixingDate(), model);
			values = values.mult(notionalAtPeriodStart);
			values = values.div(numeraire);
			if(isExcludeAccruedInterest && evaluationTime >= productToModelTimeOffset + getPeriodStart() && evaluationTime < productToModelTimeOffset + getPeriodEnd()) {
				final double nonAccruedInterestRatio = (productToModelTimeOffset + getPeriodEnd() - evaluationTime) / (getPeriodEnd() - getPeriodStart());
				values = values.mult(nonAccruedInterestRatio);
			}
		}
		else {
			values = new RandomVariableFromDoubleArray(0.0,0.0);
		}

		// Apply notional exchange
		if(notionalFlow) {
			final RandomVariable	notionalAtPeriodEnd		= getNotional().getNotionalAtPeriodEnd(this, model);

			if(getPeriodStart() > evaluationTime) {
				final RandomVariable	numeraireAtPeriodStart	= model.getNumeraire(getPeriodStart());
				values = values.subRatio(notionalAtPeriodStart, numeraireAtPeriodStart);
			}

			if(getPeriodEnd() > evaluationTime) {
				final RandomVariable	numeraireAtPeriodEnd	= model.getNumeraire(getPeriodEnd());
				values = values.addRatio(notionalAtPeriodEnd, numeraireAtPeriodEnd);
			}
		}

		if(payer) {
			values = values.mult(-1.0);
		}

		values = values.mult(numeraireAtEval);

		// Return values
		return values;
	}
		
	
	// Added method to calculate the value under the zero rate model with a shifted curve if provided
	public RandomVariable getValue(final LocalDateTime evaluationDate, final LocalDateTime scheduleReferenceDate, final ZeroRateModel forwardCurveModel, final StochasticCurve forwardCurveShifts, final ZeroRateModel discountCurveModel, final StochasticCurve discountCurveShifts, Integer pathOrState) throws CalculationException {
		if (notionalFlow || isExcludeAccruedInterest) {
			throw new UnsupportedOperationException("The PAIRS curve valuation supports coupon flows without notional exchanges or accrued-interest exclusion.");
		}

		LocalDateTime paymentDate = FloatingpointDate.getDateFromFloatingPointDate(scheduleReferenceDate, getPaymentDate());
		
		if(paymentDate.isBefore(evaluationDate) || paymentDate.isEqual(evaluationDate)) {
			return new RandomVariableFromDoubleArray(0.0);
		}
		RandomVariable values;
		if(couponFlow) {
			LocalDateTime fixingDate = FloatingpointDate.getDateFromFloatingPointDate(scheduleReferenceDate, getFixingDate());
			values = index.getValue(evaluationDate, fixingDate, forwardCurveModel, forwardCurveShifts, pathOrState);
			final RandomVariable notional = getNotional().getConstantNotional();
			double scheduleOffSet = FloatingpointDate.getFloatingPointDateFromDate(scheduleReferenceDate, evaluationDate);
			final RandomVariable discountFactor = discountCurveModel.getDiscountFactor(evaluationDate, getPaymentDate() - scheduleOffSet, discountCurveShifts, pathOrState);

			values = values.mult(notional).mult(getDaycountFraction()).mult(discountFactor);
		}
		else {
			values = new RandomVariableFromDoubleArray(0.0,0.0);
		}
		
		if(payer) {
			values = values.mult(-1.0);
		}
		return values;
	}
	

	@Override
	public RandomVariable getCoupon(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		// Calculate percentage value of coupon (not multiplied with notional, not discounted)
		RandomVariable values = getIndex().getValue(evaluationTime, model);
		// Apply daycount fraction
		final double periodDaycountFraction = getDaycountFraction();
		values = values.mult(periodDaycountFraction);

		return values;
	}
	
	// Added method
	public boolean isCouponFlow() {
		return couponFlow;
	}

	public boolean isNotionalFlow() {
		return notionalFlow;
	}

	public boolean isExcludeAccruedInterest() {
		return isExcludeAccruedInterest;
	}
	
	// Added method
	public LchAbstractIndex getLchIndex() {
		return index;
	}
	
	// Added method
	public boolean isPayer() {
		return payer;
	}

	
	@Override
	public String toString() {
		return "Period [couponFlow=" + couponFlow + ", notionalFlow="
				+ notionalFlow + ", payer=" + payer + ", toString()="
				+ super.toString() + "]";
	}
}
