package net.finmath.lch.initialmargin.swapclear.products.indices;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.lch.initialmargin.simulation.modeldata.StochasticCurve;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.swapclear.products.components.LchNotionalFromConstant;
import net.finmath.lch.initialmargin.swapclear.products.components.LchPeriod;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAAD;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;

public class LchLinearCombinationIndexTest {

    private static final LocalDate REFERENCE = LocalDate.of(2024, 2, 5);
    private static final double[] ZERO_RATES = {0.02, 0.04};
    private static final double NOTIONAL = 1_000_000.0;
    private static final double TENOR = 0.5;
    private static final double FLOAT_SCALE = 2.0;
    private static final double FIXED_SCALE = -3.0;
    private static final double FIXED_RATE = 0.01;

    @Test
    public void scaledCouponPriceAndAadDeltaMatchIndependentFormula() throws CalculationException {
        assertPriceAndDelta(1.0, false);
    }

    @Test
    public void alreadyFixedScaledCouponHasOnlyDiscountRisk() throws CalculationException {
        assertPriceAndDelta(0.0, true);
    }

    private void assertPriceAndDelta(double fixingTime, boolean fixed) throws CalculationException {
        ZeroRateModel model = new ZeroRateModel(null, (date, tenor) -> new RandomVariableFromDoubleArray(
                FloatingpointDate.getFloatingPointDateFromDate(REFERENCE, date), ZERO_RATES),
                REFERENCE, new BusinessdayCalendarExcludingWeekends(), ZERO_RATES.length, 1E-7);
        LchLinearCombinationIndex index = new LchLinearCombinationIndex(FLOAT_SCALE, new LchLIBORIndex(0.0, TENOR),
                FIXED_SCALE, new LchFixedCoupon(FIXED_RATE));
        double paymentTime = fixingTime + TENOR;
        LchPeriod period = new LchPeriod(REFERENCE.atStartOfDay(), fixingTime, paymentTime, fixingTime, paymentTime,
                new LchNotionalFromConstant(NOTIONAL), index, TENOR, true, false, false, false);
        RandomVariableDifferentiable shift = new RandomVariableDifferentiableAAD(0.0);
        StochasticCurve shifts = new StochasticCurve();
        shifts.addRate(1.0 / 365.0, shift);
        shifts.addRate(2.0, shift);
        LocalDateTime date = REFERENCE.atStartOfDay();
        RandomVariableDifferentiable value = (RandomVariableDifferentiable)
                period.getValue(date, date, model, shifts, model, shifts, null);
        RandomVariable delta = value.getGradient().get(shift.getID());
        for (int path = 0; path < ZERO_RATES.length; path++) {
            double zero = ZERO_RATES[path];
            double forward = Math.expm1(zero * TENOR) / TENOR;
            double coupon = FLOAT_SCALE * forward + FIXED_SCALE * FIXED_RATE;
            double discount = Math.exp(-zero * paymentTime);
            double expectedPrice = NOTIONAL * TENOR * coupon * discount;
            double expectedDelta = NOTIONAL * TENOR * discount
                    * ((fixed ? 0.0 : FLOAT_SCALE * Math.exp(zero * TENOR)) - paymentTime * coupon);
            assertEquals(expectedPrice, value.get(path), 1E-8);
            assertEquals(expectedDelta, delta.get(path), 1E-8);
        }
    }
}
