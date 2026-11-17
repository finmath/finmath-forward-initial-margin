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
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
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
    public void scaledCouponPriceAndParallelCurveBumpMatchIndependentFormula() throws CalculationException {
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
        LocalDateTime date = REFERENCE.atStartOfDay();
        RandomVariable value = price(period, date, model, 0.0);
        double bump = 1E-5;
        RandomVariable deltaPerBp = price(period, date, model, bump).sub(price(period, date, model, -bump))
                .mult(1E-4 / (2.0 * bump));
        for (int path = 0; path < ZERO_RATES.length; path++) {
            double zero = ZERO_RATES[path];
            double forward = Math.expm1(zero * TENOR) / TENOR;
            double coupon = FLOAT_SCALE * forward + FIXED_SCALE * FIXED_RATE;
            double discount = Math.exp(-zero * paymentTime);
            double expectedPrice = NOTIONAL * TENOR * coupon * discount;
            double expectedDelta = NOTIONAL * TENOR * discount
                    * ((fixed ? 0.0 : FLOAT_SCALE * Math.exp(zero * TENOR)) - paymentTime * coupon);
            assertEquals(expectedPrice, value.get(path), 1E-8);
            assertEquals(expectedDelta * 1E-4, deltaPerBp.get(path), 3E-7);
        }
    }

    private static RandomVariable price(LchPeriod period, LocalDateTime date, ZeroRateModel model,
            double parallelShift) throws CalculationException {
        StochasticCurve shifts = new StochasticCurve();
        shifts.addRate(1.0 / 365.0, new Scalar(parallelShift));
        shifts.addRate(2.0, new Scalar(parallelShift));
        return period.getValue(date, date, model, shifts, model, shifts, null);
    }
}
