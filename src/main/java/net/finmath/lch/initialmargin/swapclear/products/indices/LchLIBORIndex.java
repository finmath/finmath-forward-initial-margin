package net.finmath.lch.initialmargin.swapclear.products.indices;

/*
 * Created on 15.09.2006
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import net.finmath.lch.initialmargin.simulation.modeldata.StochasticCurve;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar.DateRollConvention;

/**
 * A (floating) forward rate index for a given period start offset (offset from fixing) and period length.
 *
 * @author Christian Fries
 * @version 1.1
 */
public class LchLIBORIndex extends LchAbstractIndex {

	private static final long serialVersionUID = 1L;

	private final String paymentOffsetCode;
	private final BusinessdayCalendar paymentBusinessdayCalendar;
	private final BusinessdayCalendar.DateRollConvention paymentDateRollConvention;

	private final double periodStartOffset;
	private final double periodLength;



	public LchLIBORIndex(final String name, final String currency, final String paymentOffsetCode,
			final BusinessdayCalendar paymentBusinessdayCalendar, final DateRollConvention paymentDateRollConvention) {
		super(name, currency);
		this.paymentOffsetCode = paymentOffsetCode;
		this.paymentBusinessdayCalendar = paymentBusinessdayCalendar;
		this.paymentDateRollConvention = paymentDateRollConvention;
		periodStartOffset = 0.0;
		periodLength = Double.NaN;
	}

	/**
	 * Creates a forward rate index for a given period start offset (offset from fixing) and period length.
	 *
	 * @param name The name of an index. Used to map an index on a curve.
	 * @param periodStartOffset An offset added to the fixing to define the period start.
	 * @param periodLength The period length
	 */
	public LchLIBORIndex(final String name, final double periodStartOffset, final double periodLength) {
		super(name, null);
		paymentOffsetCode = null;
		paymentBusinessdayCalendar = null;
		paymentDateRollConvention = null;
		this.periodStartOffset = periodStartOffset;
		this.periodLength = periodLength;
	}

	/**
	 * Creates a forward rate index for a given period start offset (offset from fixing) and period length.
	 *
	 * @param periodStartOffset An offset added to the fixing to define the period start.
	 * @param periodLength The period length
	 */
	public LchLIBORIndex(final double periodStartOffset, final double periodLength) {
		this(null, periodStartOffset, periodLength);
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		// Check if model provides this index
		if(getName() != null && model.getModel().getForwardRateCurve().getName() != null) {
			// Check if analytic adjustment would be possible
			if(!model.getModel().getForwardRateCurve().getName().equals(getName()) && (model.getModel().getAnalyticModel() != null && model.getModel().getAnalyticModel().getForwardCurve(getName()) == null)) {
				throw new IllegalArgumentException("No curve for index " + getName() + " found in model.");
			}
		}

		// If evaluationTime < 0 take fixing from curve (note: this is a fall-back, fixing should be provided by product, if possible).
		if(evaluationTime < 0) {
			return model.getRandomVariableForConstant(model.getModel().getForwardRateCurve().getForward(model.getModel().getAnalyticModel(), evaluationTime+periodStartOffset));
		}

		/*
		 * The periodLength may be a given float or (more exact) derived from the rolling convetions.
		 */
		final double periodLength = getPeriodLength(model, evaluationTime+periodStartOffset);

		/*
		 * Fetch forward rate from model
		 */
		RandomVariable forwardRate = model.getForwardRate(evaluationTime, evaluationTime+periodStartOffset, evaluationTime+periodStartOffset+periodLength);

		if(getName() != null && !model.getModel().getForwardRateCurve().getName().equals(getName())) {
			// Perform a multiplicative adjustment on the forward bonds
			final AnalyticModel analyticModel = model.getModel().getAnalyticModel();
			if(analyticModel == null) {
				throw new IllegalArgumentException("Index " + getName() + " does not aggree with model curve " + model.getModel().getForwardRateCurve().getName() + " and requires analytic model for adjustment. The analyticModel is null.");
			}
			final ForwardCurve indexForwardCurve = analyticModel.getForwardCurve(getName());
			final ForwardCurve modelForwardCurve = model.getModel().getForwardRateCurve();
			final double adjustment = (1.0 + indexForwardCurve.getForward(analyticModel, evaluationTime+periodStartOffset, periodLength) * periodLength) / (1.0 + modelForwardCurve.getForward(analyticModel, evaluationTime+periodStartOffset, periodLength) * periodLength);
			forwardRate = forwardRate.mult(periodLength).add(1.0).mult(adjustment).sub(1.0).div(periodLength);
		}

		return forwardRate;
	}

	
	@Override
	public RandomVariable getValue(LocalDateTime evaluationDate, LocalDateTime fixingDate, ZeroRateModel curveModel, StochasticCurve curveShifts, Integer pathOrState) throws CalculationException {
		double periodLength = getPeriodLength(curveModel, fixingDate);
		RandomVariable value;
		// if index has been already fixed in the past, we use the zero curve of the fixing date, which has not been shifted since the shift is a MPR forward scenario
		if(fixingDate.isBefore(evaluationDate) || fixingDate.isEqual(evaluationDate)) {
			value = curveModel.getForwardRate(fixingDate, periodStartOffset, periodStartOffset + periodLength, null, pathOrState);
		} else {
			// if not, we use the curve scenario and value the swap analytically under that curve
			// when valuing the swap in the future and using the fixingTimes of the schedule which is based on the reference date of the schedule (t=0)
			// the fixings on the zero curve are the schedule fixings minus the offSet time 
			double fixing = FloatingpointDate.getFloatingPointDateFromDate(evaluationDate, fixingDate); // distance from current time to fixing time
			value = curveModel.getForwardRate(evaluationDate, fixing + periodStartOffset, fixing + periodStartOffset + periodLength, curveShifts, pathOrState);
		}
		return value;
	}
	
	
	/**
	 * Returns the periodStartOffset as an act/365 daycount.
	 *
	 * @return the periodStartOffset
	 */
	@Override
	public double getPeriodStartOffset() {
		return periodStartOffset;
	}

	
	public double getPeriodLength(final TermStructureMonteCarloSimulationModel model, final double fixingTime) {
		if(paymentOffsetCode != null) {
			final LocalDateTime referenceDate = model.getReferenceDate();
			final LocalDateTime fixingDate = FloatingpointDate.getDateFromFloatingPointDate(referenceDate, fixingTime); 
			final LocalDate paymentDate = paymentBusinessdayCalendar.getAdjustedDate(fixingDate.toLocalDate(), paymentOffsetCode, paymentDateRollConvention);
			final double paymentTime = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, LocalDateTime.of(paymentDate, fixingDate.toLocalTime()));

			return paymentTime - fixingTime;
		}
		else {
			return periodLength;
		}
	}


	public double getPeriodLength(final ZeroRateModel model, LocalDateTime fixingDate) {
		if(paymentOffsetCode != null) {
			final LocalDate paymentDate = paymentBusinessdayCalendar.getAdjustedDate(fixingDate.toLocalDate(), paymentOffsetCode, paymentDateRollConvention);
			return FloatingpointDate.getFloatingPointDateFromDate(fixingDate, LocalDateTime.of(paymentDate, fixingDate.toLocalTime()));
		}
		else {
			return periodLength;
		}
	}
	
	
	@Override
	public double getPeriodLength() {

		return periodLength;
	}

	@Override
	public Set<String> queryUnderlyings() {
		final Set<String> underlyingNames = new HashSet<>();
		underlyingNames.add(getName());
		return underlyingNames;
	}

	@Override
	public String toString() {
		return "LchLIBORIndex [periodStartOffset=" + periodStartOffset
				+ ", periodLength=" + periodLength + ", toString()="
				+ super.toString() + "]";
	}
}
