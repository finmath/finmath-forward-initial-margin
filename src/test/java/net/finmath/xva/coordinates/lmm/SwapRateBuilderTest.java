package net.finmath.xva.coordinates.lmm;

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class SwapRateBuilderTest {

	@Test
	public void testThatTimeDiscretizationsAreNotNull() {
		final SwapMarketRateProduct builtRate = SwapRateBuilder.startingAt(1.0).
				withTenor(10.0).
				floatPaysEvery(0.25).
				build();
		assertThat(
				Stream.of(builtRate.getFixTenor(), builtRate.getFloatTenor()).collect(Collectors.toList()),
				everyItem(is(notNullValue())));
	}

	@Test
	public void testSinglePeriodDiscretization() {
		final SwapMarketRateProduct builtRate = SwapRateBuilder.startingAt(1.0).
				withTenor(1.0).
				floatPaysEvery(1.0).
				build();
		assertThat(
				Stream.of(builtRate.getFixTenor().getAsArrayList(), builtRate.getFloatTenor().getAsArrayList()).collect(Collectors.toList()),
				everyItem(contains(1.0, 2.0)));
	}

	@Test
	public void testCustomFixFrequency() {
		final SwapMarketRateProduct builtRate = SwapRateBuilder.startingAt(1.0).
				withTenor(1.0).
				floatPaysEvery(1.0).
				fixPaysEvery(0.5).
				build();
		assertThat(
				builtRate.getFixTenor().getAsArrayList(),
				contains(1.0, 1.5, 2.0));
	}
}