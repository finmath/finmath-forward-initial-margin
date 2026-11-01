package net.finmath.lch.initialmargin.simulation.modeldata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.lch.initialmargin.simulation.scenarios.ScenarioFactory;
import net.finmath.lch.initialmargin.simulation.scenarios.ScenarioFactory.Simulation;
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
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.TermStructureModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationFromTermStructureModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.models.HullWhiteModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelExponentialForm5Param;
import net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModelAsGiven;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;

public class SimulatedZeroRateSourceTest {

    private static final LocalDate REFERENCE_DATE = LocalDate.of(2024, 2, 5);
    private static final double FLAT_ZERO_RATE = 0.03;
    private static final int PATHS = 32;
    private static final double HORIZON = 2.0;

    @Test
    public void lmmPreservesPathwiseForwardImpliedCurve() throws CalculationException {
        checkRealModel(createSimulation(false));
    }

    @Test
    public void hullWhiteWorksWithGenericWrapperAndExplicitReferenceDate() throws CalculationException {
        TermStructureMonteCarloSimulationModel simulation = createSimulation(true);
        assertThrows(UnsupportedOperationException.class, simulation::getReferenceDate);
        checkRealModel(simulation);
        assertThrows(IllegalArgumentException.class,
                () -> new SimulatedZeroRateSource(simulation, REFERENCE_DATE.plusDays(1), HORIZON));
    }

    @Test
    public void hullWhiteExtendsHistoricalCurvesWithoutAnInitialJump() throws CalculationException {
        SimulatedZeroRateSource simulated = new SimulatedZeroRateSource(createSimulation(true), REFERENCE_DATE, HORIZON);
        BusinessdayCalendar calendar = new BusinessdayCalendarExcludingWeekends();
        NavigableMap<LocalDateTime, StochasticCurve> history = new TreeMap<>();
        for (LocalDate date = REFERENCE_DATE; history.size() < 2505; date = date.minusDays(1)) {
            if (calendar.isBusinessday(date)) {
                StochasticCurve curve = new StochasticCurve();
                double rate = FLAT_ZERO_RATE + 0.0001 * Math.sin(history.size());
                for (double tenor : new double[] {0.25, 1.0, HORIZON}) {
                    curve.addRate(tenor, new Scalar(rate));
                }
                history.put(date.atStartOfDay(), curve);
            }
        }
        ZeroRateModel combined = new ZeroRateModel(new HistoricalZeroRateSource(history), simulated, calendar);
        NavigableMap<LocalDateTime, RandomVariable> frozen = combined.getRateSeries(REFERENCE_DATE.atStartOfDay(), 1.0);
        LocalDate cutoff = REFERENCE_DATE.plusDays(7);
        NavigableMap<LocalDateTime, RandomVariable> moving = combined.getRateSeries(cutoff.atStartOfDay(), 1.0);
        assertEquals(2505, frozen.size());
        assertEquals(2510, moving.size());
        assertEquals(FLAT_ZERO_RATE, moving.get(REFERENCE_DATE.atStartOfDay()).get(0), 1E-12);
        RandomVariable finalRate = simulated.getZeroRate(cutoff, 1.0);
        RandomVariableSeries scenarios = new ScenarioFactory(combined, Simulation.VALUE_AT_RISK)
                .getTenorPointScenarios(cutoff.atStartOfDay(), 1.0);
        assertEquals(2500, scenarios.getSeries().size());
        RandomVariable lastShock = scenarios.getValue(cutoff.atStartOfDay());
        for (int path = 0; path < PATHS; path++) {
            assertEquals(finalRate.get(path), moving.lastEntry().getValue().get(path), 1E-12);
            // Seven calendar days are five observations: this return starts at the historical closing curve.
            assertEquals(finalRate.get(path) - FLAT_ZERO_RATE, lastShock.get(path), 1E-12);
        }
        assertEquals(REFERENCE_DATE.atStartOfDay(), frozen.lastKey());
    }

    private void checkRealModel(TermStructureMonteCarloSimulationModel simulation) throws CalculationException {
        SimulatedZeroRateSource source = new SimulatedZeroRateSource(simulation, REFERENCE_DATE, HORIZON);
        assertSame(simulation, source.getSimulation());
        assertEquals(REFERENCE_DATE, source.getReferenceDate());
        assertEquals(PATHS, source.getNumberOfPaths());
        assertEquals(HORIZON, source.getMaturityHorizon(), 0.0);
        assertEquals(FLAT_ZERO_RATE, source.getZeroRate(REFERENCE_DATE, 1.0).getAverage(), 1E-11);
        for (int days : new int[] {31, 62, 93}) {
            LocalDate date = REFERENCE_DATE.plusDays(days);
            double time = FloatingpointDate.getFloatingPointDateFromDate(REFERENCE_DATE, date);
            time = simulation.getTimeDiscretization().getTime(simulation.getTimeDiscretization().getTimeIndex(time));
            RandomVariable actual = source.getZeroRate(date, 1.0);
            RandomVariable forward = simulation.getForwardRate(time, time, time + 1.0);
            assertEquals(PATHS, actual.size());
            assertTrue(actual.getVariance() > 0.0);
            assertTrue(actual.getFiltrationTime() <= time + 1E-10);
            for (int path = 0; path < PATHS; path++) {
                assertEquals(Math.log1p(forward.get(path)), actual.get(path), 1E-12);
            }
        }
    }

    @Test
    public void rejectsUnavailableDatesInvalidTenorsAndMaturities() throws CalculationException {
        SimulatedZeroRateSource source = new SimulatedZeroRateSource(createSimulation(false), REFERENCE_DATE, HORIZON);
        assertThrows(IllegalArgumentException.class, () -> source.getZeroRate(REFERENCE_DATE.minusDays(1), 1.0));
        assertThrows(IllegalArgumentException.class, () -> source.getZeroRate(REFERENCE_DATE.plusDays(125), 1.0));
        assertThrows(IllegalArgumentException.class, () -> source.getZeroRate(REFERENCE_DATE.plusDays(155), 1.0));
        assertThrows(IllegalArgumentException.class, () -> source.getZeroRate(REFERENCE_DATE.plusDays(31), HORIZON));
        for (double tenor : new double[] {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> source.getZeroRate(REFERENCE_DATE, tenor));
        }
        assertEquals(FLAT_ZERO_RATE, source.getZeroRate(REFERENCE_DATE, HORIZON).getAverage(), 1E-11);
    }

    @Test
    public void preservesAadDerivativesAndPathOrder() throws CalculationException {
        double time = 31.0 / 365.0;
        double tenor = 0.5;
        RandomVariableDifferentiable forward = new RandomVariableDifferentiableAADFactory()
                .createRandomVariable(time, new double[] {0.02, 0.04});
        SimulatedZeroRateSource source = new SimulatedZeroRateSource(
                simulation(forward, new TimeDiscretizationFromArray(0.0, time), 2, null), REFERENCE_DATE, HORIZON);
        RandomVariable rate = source.getZeroRate(REFERENCE_DATE.plusDays(31), tenor);
        assertTrue(rate instanceof RandomVariableDifferentiable);
        RandomVariable derivative = ((RandomVariableDifferentiable) rate).getGradient().get(forward.getID());
        for (int path = 0; path < 2; path++) {
            assertEquals(Math.log1p(tenor * forward.get(path)) / tenor, rate.get(path), 1E-12);
            assertEquals(1.0 / (1.0 + tenor * forward.get(path)), derivative.get(path), 1E-12);
        }
    }

    @Test
    public void acceptsOnlyNumericalTimeRounding() throws CalculationException {
        double time = 31.0 / 365.0;
        TimeDiscretization roundedGrid = new TimeDiscretizationFromArray(DoubleStream.of(0.0, time + 1E-12), 1E-13);
        SimulatedZeroRateSource source = new SimulatedZeroRateSource(
                simulation(new Scalar(0.03), roundedGrid, 2, null), REFERENCE_DATE, HORIZON);
        assertEquals(Math.log1p(0.03), source.getZeroRate(REFERENCE_DATE.plusDays(31), 1.0).getAverage(), 1E-12);
        TimeDiscretization offGrid = new TimeDiscretizationFromArray(DoubleStream.of(0.0, time + 1E-5), 1E-13);
        SimulatedZeroRateSource offGridSource = new SimulatedZeroRateSource(
                simulation(new Scalar(0.03), offGrid, 2, null), REFERENCE_DATE, HORIZON);
        assertThrows(IllegalArgumentException.class, () -> offGridSource.getZeroRate(REFERENCE_DATE.plusDays(31), 1.0));
    }

    @Test
    public void rejectsNonpositiveOrNonfiniteAccumulationAndInconsistentPaths() {
        TimeDiscretization grid = new TimeDiscretizationFromArray(0.0);
        for (RandomVariable forward : new RandomVariable[] {
                new Scalar(-1.0), new Scalar(-2.0), new Scalar(Double.NaN), new Scalar(Double.POSITIVE_INFINITY),
                new RandomVariableFromDoubleArray(0.0, new double[] {0.02, 0.03, 0.04}),
                new RandomVariableFromDoubleArray(1.0, new double[] {0.02, 0.03})}) {
            SimulatedZeroRateSource source = new SimulatedZeroRateSource(
                    simulation(forward, grid, 2, null), REFERENCE_DATE, HORIZON);
            assertThrows(IllegalArgumentException.class, () -> source.getZeroRate(REFERENCE_DATE, 1.0));
        }
    }

    @Test
    public void validatesReferenceDateHorizonAndPathCount() {
        TimeDiscretization grid = new TimeDiscretizationFromArray(0.0);
        TermStructureMonteCarloSimulationModel simulation = simulation(new Scalar(0.03), grid, 2, null);
        for (double horizon : new double[] {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new SimulatedZeroRateSource(simulation, REFERENCE_DATE, horizon));
        }
        assertThrows(IllegalArgumentException.class, () -> new SimulatedZeroRateSource(
                simulation(new Scalar(0.03), grid, 0, null), REFERENCE_DATE, HORIZON));
        assertThrows(IllegalArgumentException.class, () -> new SimulatedZeroRateSource(
                simulation(new Scalar(0.03), grid, 2, REFERENCE_DATE.atTime(12, 0)), REFERENCE_DATE, HORIZON));
    }

    private TermStructureMonteCarloSimulationModel simulation(RandomVariable forward, TimeDiscretization grid,
            int paths, LocalDateTime modelReferenceDate) {
        return (TermStructureMonteCarloSimulationModel) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {TermStructureMonteCarloSimulationModel.class},
                (proxy, method, arguments) -> {
                    switch (method.getName()) {
                    case "getNumberOfPaths": return paths;
                    case "getTimeDiscretization": return grid;
                    case "getForwardRate": return forward;
                    case "getModel": return null;
                    case "getReferenceDate":
                        if (modelReferenceDate != null) {
                            return modelReferenceDate;
                        }
                        throw new UnsupportedOperationException("No model reference date.");
                    default: throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    private TermStructureMonteCarloSimulationModel createSimulation(boolean hullWhite) throws CalculationException {
        DiscountCurve discountCurve = DiscountCurveInterpolation.createDiscountCurveFromZeroRates(
                "flat", REFERENCE_DATE, new double[] {0.0, HORIZON}, new double[] {FLAT_ZERO_RATE, FLAT_ZERO_RATE},
                InterpolationMethod.LINEAR, ExtrapolationMethod.CONSTANT, InterpolationEntity.LOG_OF_VALUE);
        // Match the model's quarter-year tenor. Calendar 3M forwards would be
        // re-interpolated by HullWhite's DiscountCurveFromForwardCurve and change z(0,T).
        double flatForward = Math.expm1(FLAT_ZERO_RATE * 0.25) / 0.25;
        ForwardCurve forwardCurve = ForwardCurveInterpolation.createForwardCurveFromForwards("forward",
                new double[] {0.0, HORIZON}, new double[] {flatForward, flatForward}, 0.25);
        AnalyticModel analyticModel = new AnalyticModelFromCurvesAndVols(new Curve[] {discountCurve, forwardCurve});
        TimeDiscretization tenors = new TimeDiscretizationFromArray(0.0, 8, 0.25);
        // The LMM requires its fixing times as process times, in addition to observation dates.
        TimeDiscretization times = new TimeDiscretizationFromArray(IntStream.rangeClosed(0, 124)
                .mapToDouble(day -> day / 365.0)).union(tenors);
        if (hullWhite) {
            TermStructureModel model = new HullWhiteModel(tenors, analyticModel, forwardCurve, discountCurve,
                    new ShortRateVolatilityModelAsGiven(new TimeDiscretizationFromArray(0.0),
                            new double[] {0.01}, new double[] {0.1}), Collections.emptyMap());
            EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(model,
                    new BrownianMotionLazyInit(times, 2, PATHS, 3141));
            return new TermStructureMonteCarloSimulationFromTermStructureModel(model, process);
        }
        LIBORMarketModelFromCovarianceModel model = new LIBORMarketModelFromCovarianceModel(tenors, analyticModel,
                forwardCurve, discountCurve, new RandomVariableFromArrayFactory(),
                new LIBORCovarianceModelExponentialForm5Param(times, tenors, 1,
                        new double[] {0.1, 0.0, 0.1, 0.0, 0.0}), new CalibrationProduct[0], Collections.emptyMap());
        EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(model,
                new BrownianMotionLazyInit(times, 1, PATHS, 3141));
        return new LIBORMonteCarloSimulationFromLIBORModel(model, process);
    }
}
