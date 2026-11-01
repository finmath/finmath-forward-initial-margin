package net.finmath.lch.initialmargin.simulation.ratetransformations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.lch.initialmargin.simulation.ratetransformations.RateTransformationReturns.ReturnType;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

public class RateTransformationsTest {

	private static final LocalDateTime START = LocalDateTime.of(2024, 1, 2, 0, 0);
	private static final double TOLERANCE = 1E-12;

	@Test
	public void absoluteReturnsUseEndDatesAndAllowNegativeRates() throws CalculationException {
		TransformationContext context = new TransformationContext(scalarSeries(-0.02, -0.01, 0.005, -0.015, 0.01));

		new RateTransformationReturns(2, ReturnType.ABSOLUTE).applyTransformation(context);

		assertEquals(Arrays.asList(START.plusDays(2), START.plusDays(3), START.plusDays(4)),
				Arrays.asList(context.getPrimaryData().keySet().toArray(new LocalDateTime[0])));
		assertValues(context, 2, 0.025);
		assertValues(context, 3, -0.005);
		assertValues(context, 4, 0.005);
	}

	@Test
	public void relativeReturnsPreservePathsAndRejectAZeroDenominatorOnAnyPath() throws CalculationException {
		TransformationContext context = new TransformationContext(series(paths(100, 80), paths(110, 60)));
		RateTransformationReturns transformation = new RateTransformationReturns(1, ReturnType.RELATIVE);

		transformation.applyTransformation(context);

		assertValues(context, 1, 0.1, -0.25);
		TransformationContext zeroDenominator = new TransformationContext(series(paths(100, 0), paths(110, 1)));
		assertThrows(CalculationException.class, () -> transformation.applyTransformation(zeroDenominator));
	}

	@Test
	public void ewmaMatchesHandCalculatedSampleSeedAndRecursionOnEachPath() throws CalculationException {
		TransformationContext context = new TransformationContext(series(paths(1, 2), paths(2, 4), paths(4, 8), paths(3, 6)));

		new RateTransformationEWMA(3, 0.5).applyTransformation(context);

		// Seed at the first return, followed by the recursion starting with the second return.
		double[] expectedVariances = {7.0 / 3, 19.0 / 6, 115.0 / 12, 223.0 / 24};
		for (int day = 0; day < expectedVariances.length; day++) {
			double volatility = Math.sqrt(expectedVariances[day]);
			assertValues(context, day, volatility, 2 * volatility);
		}
	}

	@Test
	public void constantSmallReturnsHaveAnExactlyZeroSeedAndFiniteFollowingVolatilities() throws CalculationException {
		double[] returns = new double[60];
		Arrays.fill(returns, 0.0001);
		TransformationContext context = new TransformationContext(scalarSeries(returns));

		new RateTransformationEWMA(60, 0.992).applyTransformation(context);

		assertEquals(0.0, context.getPrimaryData().firstEntry().getValue().get(0), 0.0);
		for (RandomVariable volatility : context.getPrimaryData().values()) {
			assertTrue(Double.isFinite(volatility.get(0)));
			assertTrue(volatility.get(0) >= 0.0);
		}
		assertTrue(context.getPrimaryData().lastEntry().getValue().get(0) > 0.0);
	}

	@Test
	public void midVolatilityScalingMatchesHandCalculatedReturns() throws CalculationException {
		TransformationContext context = scalingContext(scalarSeries(2, 1, 4), scalarSeries(1, -2, 3));

		new RateTransformationMidVolatilityScaling("Returns").applyTransformation(context);

		assertValues(context, 0, 1.5);
		assertValues(context, 1, -5);
		assertValues(context, 2, 3);
	}

	@Test
	public void zeroReturnWithZeroVolatilityScalesToZeroWithoutContaminatingOtherPaths() throws CalculationException {
		TransformationContext context = scalingContext(
				series(paths(0, 2, 0), paths(4, 4, 0)),
				series(paths(0, 1, 0), paths(2, 3, 0)));

		new RateTransformationMidVolatilityScaling("Returns").applyTransformation(context);

		assertValues(context, 0, 0, 1.5, 0);
		assertValues(context, 1, 2, 3, 0);
	}

	@Test
	public void nonzeroReturnWithZeroVolatilityIsRejectedEvenOnOnlyOnePath() {
		TransformationContext context = scalingContext(
				series(paths(1, 0), paths(2, 1)),
				series(paths(0.1, 0.2), paths(0.3, 0.4)));

		assertThrows(CalculationException.class,
				() -> new RateTransformationMidVolatilityScaling("Returns").applyTransformation(context));
	}

	@Test
	public void scalingRejectsMissingReturnsOrDifferentDates() {
		NavigableMap<LocalDateTime, RandomVariable> returns = scalarSeries(1, 2);
		returns.put(START.plusDays(2), returns.remove(START));
		TransformationContext mismatchedDates = scalingContext(scalarSeries(2, 3), returns);
		TransformationContext missingReturns = new TransformationContext(scalarSeries(2, 3));
		RateTransformationMidVolatilityScaling transformation = new RateTransformationMidVolatilityScaling("Returns");

		assertThrows(CalculationException.class, () -> transformation.applyTransformation(mismatchedDates));
		assertThrows(CalculationException.class, () -> transformation.applyTransformation(missingReturns));
	}

	@Test
	public void invalidLagSeedAndDecayParametersAreRejected() {
		for (int lag : new int[] {-1, 0}) {
			assertThrows(IllegalArgumentException.class, () -> new RateTransformationReturns(lag, ReturnType.ABSOLUTE));
		}
		for (int seedPeriod : new int[] {-1, 0, 1}) {
			assertThrows(IllegalArgumentException.class, () -> new RateTransformationEWMA(seedPeriod, 0.992));
		}
		for (double decay : new double[] {-0.1, 1.0, 1.1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
			assertThrows(IllegalArgumentException.class, () -> new RateTransformationEWMA(60, decay));
		}
	}

	@Test
	public void appendingAStochasticDayRescalesHistoricalScenariosOnTheSamePaths() throws CalculationException {
		TransformationContext historical = applyPipeline(scalarSeries(1, 2, 4, 8));
		NavigableMap<LocalDateTime, RandomVariable> extendedRates = scalarSeries(1, 2, 4, 8);
		extendedRates.put(START.plusDays(4), paths(11, 9));

		TransformationContext extended = applyPipeline(extendedRates);

		assertValues(historical, 1, 0.5 * (1 + Math.sqrt(115.0 / 28)));
		assertValues(extended, 1, 0.5 * (1 + Math.sqrt(223.0 / 56)), 0.5 * (1 + Math.sqrt(127.0 / 56)));
		assertValues(extended, 4, 3, 1);
	}

	@Test
	public void contextRejectsNonfiniteValuesInAdditionalTransformationResults() {
		TransformationContext context = new TransformationContext(series(paths(1, 2)));

		assertThrows(IllegalArgumentException.class,
				() -> context.addTransformationResult("Returns", series(paths(1, Double.NaN))));
	}

	@Test
	public void contextRejectsDifferentPathCountsBetweenPrimaryAndAdditionalSeries() {
		TransformationContext context = new TransformationContext(series(paths(1, 2)));

		assertThrows(IllegalArgumentException.class,
				() -> context.addTransformationResult("Returns", series(paths(1, 2, 3))));
	}

	private static TransformationContext applyPipeline(NavigableMap<LocalDateTime, RandomVariable> rates) throws CalculationException {
		TransformationContext context = new TransformationContext(rates);
		new RateTransformationReturns(1, ReturnType.ABSOLUTE).applyTransformation(context);
		new RateTransformationEWMA(3, 0.5).applyTransformation(context);
		new RateTransformationMidVolatilityScaling("Returns").applyTransformation(context);
		return context;
	}

	private static TransformationContext scalingContext(NavigableMap<LocalDateTime, RandomVariable> volatilities,
			NavigableMap<LocalDateTime, RandomVariable> returns) {
		TransformationContext context = new TransformationContext(volatilities);
		context.addTransformationResult("Returns", returns);
		return context;
	}

	private static NavigableMap<LocalDateTime, RandomVariable> scalarSeries(double... values) {
		RandomVariable[] rates = new RandomVariable[values.length];
		for (int i = 0; i < values.length; i++) {
			rates[i] = new Scalar(values[i]);
		}
		return series(rates);
	}

	private static NavigableMap<LocalDateTime, RandomVariable> series(RandomVariable... values) {
		NavigableMap<LocalDateTime, RandomVariable> series = new TreeMap<>();
		for (int i = 0; i < values.length; i++) {
			series.put(START.plusDays(i), values[i]);
		}
		return series;
	}

	private static RandomVariable paths(double... values) {
		return new RandomVariableFromDoubleArray(0.0, values);
	}

	private static void assertValues(TransformationContext context, int day, double... expected) {
		RandomVariable actual = context.getPrimaryData().get(START.plusDays(day));
		for (int path = 0; path < expected.length; path++) {
			assertTrue("Non-finite value on path " + path, Double.isFinite(actual.get(path)));
			assertEquals("Value on path " + path, expected[path], actual.get(path), TOLERANCE);
		}
	}
}
