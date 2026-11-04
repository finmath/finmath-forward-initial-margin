package net.finmath.lch.initialmargin.valuation;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Test;

import net.finmath.lch.initialmargin.simulation.modeldata.StochasticCurve;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGrid;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.Currency;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.CurveName;
import net.finmath.lch.initialmargin.swapclear.products.LchSwap;
import net.finmath.lch.initialmargin.swapclear.products.components.LchNotionalFromConstant;
import net.finmath.lch.initialmargin.swapclear.products.components.LchSwapLeg;
import net.finmath.lch.initialmargin.swapclear.products.indices.LchLIBORIndex;
import net.finmath.lch.initialmargin.swapclear.sensitivities.AadZeroRateSensitivities;
import net.finmath.lch.initialmargin.swapclear.sensitivities.SensitivityMatrix;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.Period;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleFromPeriods;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;
import net.finmath.time.daycount.DayCountConvention_ACT_365;

public class CurveValuationTest {
    private static final LocalDate REFERENCE = LocalDate.of(2024, 2, 5);

    @Test
    public void sameSwapValuesAndDifferentiatesThroughCommonContract() throws Exception {
        ZeroRateModel model = model();
        LocalDateTime date = REFERENCE.atStartOfDay();
        Schedule schedule = new ScheduleFromPeriods(REFERENCE, new DayCountConvention_ACT_365(),
                new Period(REFERENCE.plusDays(30), REFERENCE.plusDays(133),
                        REFERENCE.plusDays(32), REFERENCE.plusDays(124)));
        LchSwapLeg floating = new LchSwapLeg(schedule, new LchNotionalFromConstant(10000),
                new LchLIBORIndex(2.0 / 365.0, 0.25), 0.002, false, false);
        LchSwapLeg fixed = new LchSwapLeg(schedule, new LchNotionalFromConstant(10000),
                null, 0.03, true, false);
        LchSwap swap = new LchSwap(Currency.EUR, CurveName.EUR_EURIBOR_3M, floating, fixed);
        CurveValuation valuation = swap;
        StochasticCurve shocks = new StochasticCurve();
        shocks.addRate(0.1, new Scalar(0.001));
        shocks.addRate(1.0, new Scalar(-0.002));
        RandomVariable value = valuation.getValue(date, model, shocks, null);
        RandomVariable expected = floating.getValue(date, model, shocks, null)
                .add(fixed.getValue(date, model, shocks, null));
        for (int path = 0; path < 2; path++) {
            assertEquals(expected.get(path), value.get(path), 1E-12);
            // Scalar and vector arithmetic may round the coupon ratio differently.
            assertEquals(value.get(path), valuation.getValue(date, model, shocks, path).get(0), 1E-10);
        }
        SensitivityMatrix generic = new AadZeroRateSensitivities(valuation, model, grid()).getDeltaSensitivities(date);
        SensitivityMatrix legacy = new AadZeroRateSensitivities(List.of(floating, fixed), model, grid()).getDeltaSensitivities(date);
        for (double bucket : grid().getFixings()) {
            for (int path = 0; path < 2; path++) {
                assertEquals(legacy.getSensitivityMatrix().get(bucket).get(bucket).get(path),
                        generic.getSensitivityMatrix().get(bucket).get(bucket).get(path), 1E-12);
            }
        }
    }

    @Test
    public void arbitraryCurveValuationSupportsAadAndPortfolioComposition() throws Exception {
        ZeroRateModel model = model();
        LocalDateTime date = REFERENCE.atStartOfDay();
        // Deliberately no LchSwapLeg: an independent payment adapter exercises the generic seam.
        CurveValuation payment = (evaluation, market, shocks, path) ->
                market.getDiscountFactor(evaluation, 0.55, shocks, path).mult(100.0);
        List<CurveValuation> entries = new ArrayList<>(List.of(payment, payment));
        CurveValuation portfolio = CurveValuation.sum(entries);
        entries.clear(); // The portfolio retains its supplied positions.
        RandomVariable value = portfolio.getValue(date, model, null, null);
        SensitivityMatrix delta = new AadZeroRateSensitivities(portfolio, model, grid()).getDeltaSensitivities(date);
        for (int path = 0; path < 2; path++) {
            double expected = -0.55 * value.get(path) * 0.5 * 1E-4;
            assertEquals(expected, delta.getSensitivityMatrix().get(0.1).get(0.1).get(path), 1E-12);
            assertEquals(expected, delta.getSensitivityMatrix().get(1.0).get(1.0).get(path), 1E-12);
        }
        assertEquals(0.0, CurveValuation.sum(List.of()).getValue(date, model, null, null).getAverage(), 0.0);
    }

    private static TenorGrid grid() {
        LinkedHashMap<Double, String> tenors = new LinkedHashMap<>();
        tenors.put(0.1, "short");
        tenors.put(1.0, "long");
        return new TenorGrid(tenors);
    }

    private static ZeroRateModel model() throws Exception {
        return new ZeroRateModel(null, (date, tenor) -> new RandomVariableFromDoubleArray(0.0,
                new double[] {0.02 + 0.001 * tenor, 0.04 + 0.002 * tenor}), REFERENCE,
                new BusinessdayCalendarExcludingWeekends(), 2, 0.0);
    }
}
