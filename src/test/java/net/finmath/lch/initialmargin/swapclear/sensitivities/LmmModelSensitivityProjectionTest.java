package net.finmath.lch.initialmargin.swapclear.sensitivities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.lch.initialmargin.simulation.modeldata.SimulatedZeroRateSource;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGrid;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.swapclear.products.components.LchNotionalFromConstant;
import net.finmath.lch.initialmargin.swapclear.products.components.LchSwapLeg;
import net.finmath.lch.initialmargin.swapclear.products.indices.LchAbstractIndex;
import net.finmath.lch.initialmargin.swapclear.products.indices.LchLIBORIndex;
import net.finmath.lch.initialmargin.swapclear.sensitivities.LmmModelSensitivityProjection.Result;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.CurveInterpolation.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationEntity;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.montecarlo.BrownianMotionLazyInit;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.HullWhiteModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelExponentialForm5Param;
import net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModelAsGiven;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.Period;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleFromPeriods;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;
import net.finmath.time.daycount.DayCountConvention_ACT_365;

public class LmmModelSensitivityProjectionTest {

    private static final LocalDate REFERENCE = LocalDate.of(2024, 2, 5);
    private static final int PATHS = 8;

    @Test
    public void alignedNativeAndZeroGridsRecoverBondDeltaPathwise() throws CalculationException {
        SimulatedZeroRateSource source = source(true, false);
        Result result = diagnostic(source, List.of(leg(365, null, 1.0)), grid(0.25, 0.5, 0.75, 1.0))
                .calculate(REFERENCE.atStartOfDay());
        for (int path = 0; path < PATHS; path++) {
            assertEquals(4, result.getRanks()[path]);
            assertEquals(100.0 * Math.exp(-0.03), result.getValue().get(path), 1E-10);
            assertEquals(-result.getValue().get(path) * 1E-4, result.getProjectedDeltas().get(1.0).get(path), 1E-12);
            assertEquals(0.0, result.getRelativeNativeGradientResidual().get(path), 1E-12);
            for (double tenor : new double[] {0.25, 0.5, 0.75}) {
                assertEquals(0.0, result.getProjectedDeltas().get(tenor).get(path), 1E-12);
            }
            for (RandomVariable residual : result.getUnrepresentedShockNorms().values()) {
                assertEquals(0.0, residual.get(path), 1E-12);
            }
        }
        assertThrows(UnsupportedOperationException.class, () -> result.getProjectedDeltas().clear());
        int[] ranks = result.getRanks();
        ranks[0] = 0;
        assertEquals(4, result.getRanks()[0]);
    }

    @Test
    public void offGridOldStubIsExcludedAndMissingRiskIsReported() throws CalculationException {
        SimulatedZeroRateSource source = source(true, false);
        Result result = diagnostic(source, List.of(leg(365, null, 1.0)), grid(0.05, 0.15, 0.25, 0.5))
                .calculate(REFERENCE.plusDays(36).atStartOfDay());
        assertFalse(result.getNativeLiborIndices().containsValue(0));
        assertEquals(3, result.getNativeLiborIndices().size());
        assertTrue(result.getValue().getVariance() > 0.0);
        for (int path = 0; path < PATHS; path++) {
            assertEquals(2, result.getRanks()[path]);
            for (double tenor : new double[] {0.05, 0.15}) {
                assertEquals(1.0, result.getUnrepresentedShockNorms().get(tenor).get(path), 1E-12);
                assertEquals(0.0, result.getProjectedDeltas().get(tenor).get(path), 1E-12);
            }
            // The payment also depends on the last native forward, which the
            // truncated target grid cannot observe. This must not look exact.
            assertTrue(result.getRelativeNativeGradientResidual().get(path) > 0.01);
        }
        long previous = Long.MIN_VALUE;
        for (long id : result.getNativeLiborIndices().keySet()) {
            assertTrue(id > previous);
            previous = id;
        }
    }

    @Test
    public void sameDayFixingIsConstantWhileItsDiscountCurveMoves() throws CalculationException {
        SimulatedZeroRateSource source = source(true, false);
        double payment = 91.0 / 365.0;
        Result result = diagnostic(source, List.of(leg(91, new LchLIBORIndex(0.0, 0.25), 0.0)),
                grid(payment, 0.5, 0.75, 1.0)).calculate(REFERENCE.atStartOfDay());
        assertTrue(result.getNativeLiborIndices().containsValue(0));
        for (int path = 0; path < PATHS; path++) {
            assertEquals(-payment * result.getValue().get(path) * 1E-4,
                    result.getProjectedDeltas().get(payment).get(path), 1E-12);
            for (double tenor : new double[] {0.5, 0.75, 1.0}) {
                assertEquals(0.0, result.getProjectedDeltas().get(tenor).get(path), 1E-12);
            }
        }
    }

    @Test
    public void moreZeroBucketsThanNativeCoordinatesAreNotIndependent() throws CalculationException {
        SimulatedZeroRateSource source = source(true, false);
        Result result = diagnostic(source, List.of(leg(365, null, 1.0)), grid(1.0 / 12.0, 0.25, 0.5, 0.75, 1.0))
                .calculate(REFERENCE.atStartOfDay());
        assertEquals(4, result.getRanks()[0]);
        assertTrue(result.getUnrepresentedShockNorms().get(1.0 / 12.0).get(0) > 0.1);
        assertEquals(0.0, result.getRelativeNativeGradientResidual().get(0), 1E-12);
    }

    @Test
    public void expiredProductsHaveZeroGradientsWithoutMissingIdFailures() throws CalculationException {
        SimulatedZeroRateSource source = source(true, false);
        Result result = diagnostic(source, List.of(leg(30, null, 1.0)), grid(0.05, 0.25, 0.5))
                .calculate(REFERENCE.plusDays(36).atStartOfDay());
        assertEquals(0.0, result.getValue().getAverage(), 0.0);
        for (RandomVariable delta : result.getProjectedDeltas().values()) {
            assertEquals(0.0, delta.abs().getMax(), 0.0);
        }
        assertEquals(0.0, result.getRelativeNativeGradientResidual().getMax(), 0.0);
    }

    @Test
    public void directBucketAadExposesShortEndRiskMissingFromNativeProjection() throws CalculationException {
        SimulatedZeroRateSource source = source(true, false);
        ZeroRateModel pricing = new ZeroRateModel(source, new BusinessdayCalendarExcludingWeekends());
        double shortTenor = 30.0 / 365.0;
        TenorGrid targets = grid(shortTenor, 0.25, 0.5, 0.75);
        List<LchSwapLeg> legs = List.of(leg(66, null, 1.0));
        java.time.LocalDateTime date = REFERENCE.plusDays(36).atStartOfDay();
        Result projection = new LmmModelSensitivityProjection(legs, pricing, source, targets).calculate(date);
        RandomVariable direct = new AadZeroRateSensitivities(legs, pricing, targets).getDeltaSensitivities(date)
                .getSensitivityMatrix().get(shortTenor).get(shortTenor);
        for (int path = 0; path < PATHS; path++) {
            assertEquals(-shortTenor * projection.getValue().get(path) * 1E-4, direct.get(path), 1E-12);
            assertEquals(0.0, projection.getProjectedDeltas().get(shortTenor).get(path), 1E-12);
            assertEquals(1.0, projection.getUnrepresentedShockNorms().get(shortTenor).get(path), 1E-12);
            assertTrue(Math.abs(direct.get(path)) > 1E-8);
        }
    }

    @Test
    public void rejectsInsufficientHorizonNonAadAndDiscardedIntermediateNodes() throws CalculationException {
        SimulatedZeroRateSource aad = source(true, false);
        assertThrows(IllegalArgumentException.class, () -> diagnostic(aad, Collections.emptyList(), grid(0.25, 60.0))
                .calculate(REFERENCE.atStartOfDay()));
        for (SimulatedZeroRateSource unsupported : new SimulatedZeroRateSource[] {source(false, false), source(true, true)}) {
            assertThrows(IllegalArgumentException.class, () -> diagnostic(unsupported, Collections.emptyList(), grid(0.25))
                    .calculate(REFERENCE.plusDays(36).atStartOfDay()));
        }
    }

    @Test
    public void rejectsHullWhiteEvenWhenItUsesALiborSimulationWrapper() throws CalculationException {
        DiscountCurve discount = DiscountCurveInterpolation.createDiscountCurveFromZeroRates("discount", REFERENCE,
                new double[] {0.0, 1.0}, new double[] {0.03, 0.03}, InterpolationMethod.LINEAR,
                ExtrapolationMethod.CONSTANT, InterpolationEntity.LOG_OF_VALUE);
        ForwardCurve forward = ForwardCurveInterpolation.createForwardCurveFromForwards("forward",
                new double[] {0.0, 1.0}, new double[] {0.03, 0.03}, 0.25);
        AnalyticModel curves = new AnalyticModelFromCurvesAndVols(new Curve[] {discount, forward});
        TimeDiscretization tenors = new TimeDiscretizationFromArray(0.0, 4, 0.25);
        HullWhiteModel model = new HullWhiteModel(new RandomVariableDifferentiableAADFactory(
                Map.of("isGradientRetainsLeafNodesOnly", false)), tenors, curves, forward, discount,
                new ShortRateVolatilityModelAsGiven(new TimeDiscretizationFromArray(0.0),
                        new double[] {0.01}, new double[] {0.1}), Collections.emptyMap());
        EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(model,
                new BrownianMotionLazyInit(tenors, 2, PATHS, 6147));
        SimulatedZeroRateSource source = new SimulatedZeroRateSource(
                new LIBORMonteCarloSimulationFromLIBORModel(model, process), REFERENCE, 1.0);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> diagnostic(source, Collections.emptyList(), grid(0.25)));
        assertTrue(exception.getMessage().contains("LIBOR market model"));
    }

    @Test
    public void rejectsDifferentCurveLevelsAtTheSameReferenceDate() throws CalculationException {
        SimulatedZeroRateSource source = source(true, false);
        ZeroRateModel wrong = new ZeroRateModel(null, (date, tenor) -> new Scalar(0.04), REFERENCE,
                new BusinessdayCalendarExcludingWeekends(), PATHS, 1E-7);
        LmmModelSensitivityProjection diagnostic = new LmmModelSensitivityProjection(Collections.emptyList(), wrong, source, grid(0.25));
        assertThrows(CalculationException.class, () -> diagnostic.calculate(REFERENCE.atStartOfDay()));
    }

    private LmmModelSensitivityProjection diagnostic(SimulatedZeroRateSource source, List<LchSwapLeg> legs,
            TenorGrid grid) throws CalculationException {
        return new LmmModelSensitivityProjection(legs,
                new ZeroRateModel(source, new BusinessdayCalendarExcludingWeekends()), source, grid);
    }

    private TenorGrid grid(double... tenors) {
        LinkedHashMap<Double, String> points = new LinkedHashMap<>();
        for (double tenor : tenors) {
            points.put(tenor, Double.toString(tenor));
        }
        return new TenorGrid(points);
    }

    private LchSwapLeg leg(int paymentDays, LchAbstractIndex index, double spread) {
        LocalDate payment = REFERENCE.plusDays(paymentDays);
        Schedule schedule = new ScheduleFromPeriods(REFERENCE, new DayCountConvention_ACT_365(),
                new Period(REFERENCE, payment, REFERENCE, payment));
        return new LchSwapLeg(schedule, new LchNotionalFromConstant(100.0), index, spread, false, false);
    }

    private SimulatedZeroRateSource source(boolean aad, boolean leafNodesOnly) throws CalculationException {
        DiscountCurve discount = DiscountCurveInterpolation.createDiscountCurveFromZeroRates("discount", REFERENCE,
                new double[] {0.0, 1.0}, new double[] {0.03, 0.03}, InterpolationMethod.LINEAR,
                ExtrapolationMethod.CONSTANT, InterpolationEntity.LOG_OF_VALUE);
        double forward = Math.expm1(0.03 * 0.25) / 0.25;
        ForwardCurve forwards = ForwardCurveInterpolation.createForwardCurveFromForwards("forward",
                new double[] {0.0, 1.0}, new double[] {forward, forward}, 0.25);
        AnalyticModel curves = new AnalyticModelFromCurvesAndVols(new Curve[] {discount, forwards});
        TimeDiscretization tenors = new TimeDiscretizationFromArray(0.0, 4, 0.25);
        TimeDiscretization times = new TimeDiscretizationFromArray(IntStream.rangeClosed(0, 36)
                .mapToDouble(day -> day / 365.0)).union(tenors);
        LIBORMarketModelFromCovarianceModel model = new LIBORMarketModelFromCovarianceModel(tenors, curves,
                forwards, discount, aad ? new RandomVariableDifferentiableAADFactory(
                        Map.of("isGradientRetainsLeafNodesOnly", leafNodesOnly)) : new RandomVariableFromArrayFactory(),
                new LIBORCovarianceModelExponentialForm5Param(times, tenors, 1,
                        new double[] {0.1, 0.0, 0.1, 0.0, 0.0}), new CalibrationProduct[0], Collections.emptyMap());
        EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(model,
                new BrownianMotionLazyInit(times, 1, PATHS, 6147));
        return new SimulatedZeroRateSource(new LIBORMonteCarloSimulationFromLIBORModel(model, process), REFERENCE, 1.0);
    }
}
