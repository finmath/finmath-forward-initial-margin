/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 28.02.2015
 */

package net.finmath.initialmargin.regression.products;

import java.util.ArrayList;
import java.util.Collection;

import net.finmath.exception.CalculationException;
import net.finmath.initialmargin.regression.products.components.AbstractNotional;
import net.finmath.initialmargin.regression.products.components.AbstractProductComponent;
import net.finmath.initialmargin.regression.products.components.AccruingNotional;
import net.finmath.initialmargin.regression.products.components.Period;
import net.finmath.initialmargin.regression.products.components.ProductCollection;
import net.finmath.initialmargin.regression.products.indices.AbstractIndex;
import net.finmath.initialmargin.regression.products.indices.FixedCoupon;
import net.finmath.initialmargin.regression.products.indices.LinearCombinationIndex;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.Schedule;

/**
 * @author Christian Fries
 */
public class SwapLeg extends AbstractLIBORMonteCarloRegressionProduct {

	private final Schedule legSchedule;
	private final AbstractNotional notional;
	private final AbstractIndex index;
	private final double spread;
	private final boolean couponFlow;
	private final boolean isNotionalExchanged;
	private final boolean isNotionalAccruing;

	private final ProductCollection components;

	/**
	 * Creates a swap leg. The swap leg is build from elementary components.
	 *
	 * @param legSchedule         ScheduleFromPeriods of the leg.
	 * @param notional            The notional.
	 * @param index               The index.
	 * @param spread              Fixed spread on the forward or fix rate.
	 * @param couponFlow          If true, the coupon is payed. If false, the coupon is not payed, but may still be part of an accruing notional, see <code>isNotionalAccruing</code>.
	 * @param isNotionalExchanged If true, the leg will pay notional at the beginning of the swap and receive notional at the end of the swap.
	 * @param isNotionalAccruing  If true, the notional is accruing, that is, the notional of a period is given by the notional of the previous period, accrued with the coupon of the previous period.
	 */
	public SwapLeg(Schedule legSchedule, AbstractNotional notional, AbstractIndex index, double spread, boolean couponFlow, boolean isNotionalExchanged, boolean isNotionalAccruing) {
		super();
		this.legSchedule = legSchedule;
		this.notional = notional;
		this.index = index;
		this.spread = spread;
		this.couponFlow = couponFlow;
		this.isNotionalExchanged = isNotionalExchanged;
		this.isNotionalAccruing = isNotionalAccruing;

		/*
		 * Create components.
		 *
		 * The interesting part here is, that the creation of the components implicitly
		 * constitutes the (traditional) pricing algorithms (e.g., loop over all periods).
		 * Hence, the definition of the product is the definition of the pricing algorithm.
		 */
		Collection<AbstractProductComponent> periods = new ArrayList<AbstractProductComponent>();
		for (int periodIndex = 0; periodIndex < legSchedule.getNumberOfPeriods(); periodIndex++) {
			double fixingDate = legSchedule.getFixing(periodIndex);
			double paymentDate = legSchedule.getPayment(periodIndex);
			double periodLength = legSchedule.getPeriodLength(periodIndex);

			/*
			 * We do not count empty periods.
			 * Since empty periods are an indication for a ill-specified
			 * product, it might be reasonable to throw an illegal argument exception instead.
			 */
			if (periodLength == 0) {
				continue;
			}

			AbstractIndex coupon;
			if (index != null) {
				if (spread != 0) {
					coupon = new LinearCombinationIndex(1, index, 1, new FixedCoupon(spread));
				} else {
					coupon = index;
				}
			} else {
				coupon = new FixedCoupon(spread);
			}

			Period period = new Period(fixingDate, paymentDate, fixingDate, paymentDate, notional, coupon, periodLength, couponFlow, isNotionalExchanged, false);
			periods.add(period); //fill arrayList of AbstractProductComponents with Periods

			if (isNotionalAccruing) {
				notional = new AccruingNotional(notional, period);
			}
		}

		components = new ProductCollection(periods);
	}

	/**
	 * Creates a swap leg. The swap leg is build from elementary components
	 *
	 * @param legSchedule         ScheduleFromPeriods of the leg.
	 * @param notional            The notional.
	 * @param index               The index.
	 * @param spread              Fixed spread on the forward or fix rate.
	 * @param isNotionalExchanged If true, the leg will pay notional at the beginning of the swap and receive notional at the end of the swap.
	 */
	public SwapLeg(Schedule legSchedule, AbstractNotional notional, AbstractIndex index, double spread, boolean isNotionalExchanged) {
		this(legSchedule, notional, index, spread, true, isNotionalExchanged, false);
	}

	@Override
	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {
		return components.getValue(evaluationTime, model);
	}

	@Override
	public RandomVariable getCF(double initialTime, double finalTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {
		return components.getCF(initialTime, finalTime, model);
	}
}
