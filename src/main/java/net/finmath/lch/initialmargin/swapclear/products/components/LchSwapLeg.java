/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28.02.2015
 */

package net.finmath.lch.initialmargin.swapclear.products.components;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;

import net.finmath.lch.initialmargin.simulation.modeldata.StochasticCurve;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.swapclear.products.indices.LchAbstractIndex;
import net.finmath.lch.initialmargin.swapclear.products.indices.LchFixedCoupon;
import net.finmath.lch.initialmargin.swapclear.products.indices.LchLinearCombinationIndex;
import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.AbstractTermStructureMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.Schedule;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public class LchSwapLeg extends AbstractTermStructureMonteCarloProduct {

	private final LchProductCollection				components;
	private final Schedule 							legSchedule;

	/**
	 * Creates a swap leg. The swap leg is build from elementary components.
	 *
	 * @param legSchedule ScheduleFromPeriods of the leg.
	 * @param notional The notional.
	 * @param index The index.
	 * @param spread Fixed spread on the forward or fix rate.
	 * @param couponFlow If true, the coupon is payed. If false, the coupon is not payed, but may still be part of an accruing notional, see <code>isNotionalAccruing</code>.
	 * @param isPayLeg If true, the leg pays the rate.
	 * @param isNotionalExchanged If true, the leg will pay notional at the beginning of the swap and receive notional at the end of the swap.
	 * @param isNotionalAccruing If true, the notional is accruing, that is, the notional of a period is given by the notional of the previous period, accrued with the coupon of the previous period.
	 */
	public LchSwapLeg(final Schedule legSchedule, LchNotional notional, final LchAbstractIndex index, final double spread, final boolean couponFlow, final boolean isPayLeg, final boolean isNotionalExchanged, final boolean isNotionalAccruing) {
		super();
		this.legSchedule = legSchedule;
		
		final LocalDateTime referenceDate = legSchedule.getReferenceDate() != null ? legSchedule.getReferenceDate().atStartOfDay() : null;

		/*
		 * Create components.
		 *
		 * The interesting part here is, that the creation of the components implicitly
		 * constitutes the (traditional) pricing algorithms (e.g., loop over all periods).
		 * Hence, the definition of the product is the definition of the pricing algorithm.
		 */
		final Collection<AbstractProductComponent> periods = new ArrayList<>();
		for(int periodIndex=0; periodIndex<legSchedule.getNumberOfPeriods(); periodIndex++) {
			final double fixingDate	= legSchedule.getFixing(periodIndex);
			final double paymentDate	= legSchedule.getPayment(periodIndex);
			final double periodLength	= legSchedule.getPeriodLength(periodIndex);

			/*
			 * We do not count empty periods.
			 * Since empty periods are an indication for a ill-specified
			 * product, it might be reasonable to throw an illegal argument exception instead.
			 */
			if(periodLength == 0) {
				continue;
			}

			LchAbstractIndex coupon;
			if(index != null) {
				if(spread != 0) {
					coupon = new LchLinearCombinationIndex(1, index, 1, new LchFixedCoupon(spread));
				} else {
					coupon = index;
				}
			}
			else {
				coupon = new LchFixedCoupon(spread);
			}

			final LchPeriod period = new LchPeriod(referenceDate, fixingDate, paymentDate, fixingDate, paymentDate, notional, coupon, periodLength, couponFlow, isNotionalExchanged, isPayLeg, false);
			periods.add(period);

//			if(isNotionalAccruing) {
//				notional = new AccruingNotional(notional, period);
//			}
		}

		components = new LchProductCollection(periods);
	}

	/**
	 * Creates a swap leg. The swap leg is build from elementary components.
	 *
	 * @param legSchedule ScheduleFromPeriods of the leg.
	 * @param notionals An array of notionals for each period in the schedule.
	 * @param index The index.
	 * @param spreads Fixed spreads on the forward or fix rate.
	 * @param couponFlow If true, the coupon is payed. If false, the coupon is not payed, but may still be part of an accruing notional, see <code>isNotionalAccruing</code>.
	 * @param isNotionalExchanged If true, the leg will pay notional at the beginning of the swap and receive notional at the end of the swap.
	 */
	public LchSwapLeg(final Schedule legSchedule, final LchNotional[] notionals, final LchAbstractIndex index, final double[] spreads, final boolean couponFlow, final boolean isPayLeg, final boolean isNotionalExchanged) {
		super();
		this.legSchedule = legSchedule;

		
		if(legSchedule.getNumberOfPeriods() != notionals.length) {
			throw new IllegalArgumentException("Number of notionals ("+notionals.length+") must match number of periods ("+legSchedule.getNumberOfPeriods()+").");
		}

		/*
		 * Create components.
		 *
		 * The interesting part here is, that the creation of the components implicitly
		 * constitutes the (traditional) pricing algorithms (e.g., loop over all periods).
		 * Hence, the definition of the product is the definition of the pricing algorithm.
		 */
		final Collection<AbstractProductComponent> periods = new ArrayList<>();
		for(int periodIndex=0; periodIndex<legSchedule.getNumberOfPeriods(); periodIndex++) {
			final LocalDateTime referenceDate = LocalDateTime.of(legSchedule.getReferenceDate(), LocalTime.of(0, 0));

			final double periodStart	= legSchedule.getPeriodStart(periodIndex);
			final double periodEnd	= legSchedule.getPeriodEnd(periodIndex);
			final double fixingDate	= legSchedule.getFixing(periodIndex);
			final double paymentDate	= legSchedule.getPayment(periodIndex);
			final double periodLength	= legSchedule.getPeriodLength(periodIndex);

			/*
			 * We do not count empty periods.
			 * Since empty periods are an indication for a ill-specified
			 * product, it might be reasonable to throw an illegal argument exception instead.
			 */
			if(periodLength == 0) {
				continue;
			}

			LchAbstractIndex coupon;
			if(index != null) {
				if(spreads[periodIndex] != 0) {
					coupon = new LchLinearCombinationIndex(1, index, 1, new LchFixedCoupon(spreads[periodIndex]));
				} else {
					coupon = index;
				}
			}
			else {
				coupon = new LchFixedCoupon(spreads[periodIndex]);
			}

			final LchPeriod period = new LchPeriod(referenceDate, periodStart, periodEnd, fixingDate, paymentDate, notionals[periodIndex], coupon,
					periodLength, couponFlow, isNotionalExchanged, isPayLeg, false /* isExcludeAccruedInterest */);
			periods.add(period);
		}

		components = new LchProductCollection(periods);
	}

	/**
	 * Creates a swap leg. The swap leg is build from elementary components
	 *
	 * @param legSchedule ScheduleFromPeriods of the leg.
	 * @param notional The notional.
	 * @param index The index.
	 * @param spread Fixed spread on the forward or fix rate.
	 * @param isNotionalExchanged If true, the leg will pay notional at the beginning of the swap and receive notional at the end of the swap.
	 */
	public LchSwapLeg(final Schedule legSchedule, final LchNotional notional, final LchAbstractIndex index, final double spread, final boolean isPayLeg, final boolean isNotionalExchanged) {
		this(legSchedule, notional, index, spread, true, isPayLeg ,isNotionalExchanged, false);

	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		return components.getValue(evaluationTime, model);
	}
	
	
	// Swap value under the zero rate model with separate models for forward rates and discount factors
	public RandomVariable getValue(final LocalDateTime evaluationDate, final ZeroRateModel forwardCurveModel, final StochasticCurve forwardCurveShifts, final ZeroRateModel discountCurveModel, final StochasticCurve discountCurveShifts, Integer pathOrState) throws CalculationException {
		return components.getValue(evaluationDate, legSchedule.getReferenceDate().atStartOfDay(), forwardCurveModel, forwardCurveShifts, discountCurveModel, discountCurveShifts, pathOrState);
	}
	
	
	// Swap value for single curve setup
	public RandomVariable getValue(final LocalDateTime evaluationDate, final ZeroRateModel zeroRateModel, final StochasticCurve curveShifts, Integer pathOrState) throws CalculationException {
		return components.getValue(evaluationDate, legSchedule.getReferenceDate().atStartOfDay(), zeroRateModel, curveShifts, zeroRateModel, curveShifts, pathOrState);
	}
	
	
	// Return components 
	public LchProductCollection getComponents() {
		return components;
	}
	
	
	// Reference date of swap leg schedule
	public LocalDateTime getReferenceDate() {
		return legSchedule.getReferenceDate().atStartOfDay();
	}

	
	// Last payment date of the swap
	public double getLastPaymentDate() {
		return legSchedule.getPayment(legSchedule.getNumberOfPeriods() - 1);
	}
	
	// Leg schedule of the swap
	public Schedule getSchedule() {
		return legSchedule;
	}
	
	
}
