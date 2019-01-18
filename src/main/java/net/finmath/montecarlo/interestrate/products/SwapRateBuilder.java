package net.finmath.montecarlo.interestrate.products;

import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.TimeDiscretization;

public final class SwapRateBuilder {

	private SwapRateBuilder() {}

	public interface HasStart {
		/**
		 * Specifies the end time directly.
		 * @param endTime The maturity timepoint as {@link net.finmath.time.FloatingpointDate}.
		 * @return A builder requiring further steps.
		 */
		HasEnd endingAt(double endTime);

		/**
		 * Specifies the end time through the tenor.
		 * @param tenor The time span from start to end time in {@link net.finmath.time.FloatingpointDate} units.
		 * @return A builder requiring further steps.
		 */
		HasEnd withTenor(double tenor);
	}

	public interface HasEnd {
		/**
		 * Specifies the frequency of floating rate payments.
		 * @param frequency The period between floating rate payments in {@link net.finmath.time.FloatingpointDate} units.
		 * @return A builder with optional steps.
		 */
		HasFloatFrequency floatPaysEvery(double frequency);
	}

	public interface HasFloatFrequency {
		/**
		 * Specifies the frequency of fixed rate payments.
		 * @param frequency The period between fixed rate payments in {@link net.finmath.time.FloatingpointDate} units.
		 * @return A builder with no further steps.
		 */
		HasFixFrequency fixPaysEvery(double frequency);

		/**
		 * Builds the swap rate product as specified before, with a fixed frequency of 1 year.
		 * @return A {@link SwapMarketRateProduct} instance with inferred time discretizations and stub period in front.
		 */
		SwapMarketRateProduct build();
	}

	public interface HasFixFrequency {
		/**
		 * Builds the swap rate product as specified before.
		 * @return A {@link SwapMarketRateProduct} instance with inferred time discretizations and stub period in front.
		 */
		SwapMarketRateProduct build();
	}

	/**
	 * Starts building a swap rate product with the given start time.
	 * @param startTime The start of the swap as {@link net.finmath.time.FloatingpointDate}.
	 * @return A builder with required further steps.
	 */
	public static HasStart startingAt(double startTime) {
		return new Intermediate(startTime);
	}

	/**
	 * Starts building a swap rate product with start time zero.
	 * @return A builder with required further steps.
	 */
	public static HasStart startingImmediately() {
		return startingAt(0.0);
	}

	private static class Intermediate implements HasStart, HasEnd, HasFloatFrequency, HasFixFrequency {

		private double startTime;
		private double endTime;
		private double floatFrequency;
		private double fixFrequency = 1.0;

		public Intermediate(double startTime) {
			this.startTime = startTime;
		}

		@Override
		public HasEnd endingAt(double endTime) {
			this.endTime = endTime;
			return this;
		}

		@Override
		public HasEnd withTenor(double tenor) {
			endTime = startTime + tenor;
			return this;
		}

		@Override
		public HasFloatFrequency floatPaysEvery(double frequency) {
			floatFrequency = frequency;
			return this;
		}

		@Override
		public HasFixFrequency fixPaysEvery(double frequency) {
			fixFrequency = frequency;
			return this;
		}

		@Override
		public SwapMarketRateProduct build() {
			return new SwapMarketRateProduct(getFloatTD(), getFixTD());
		}

		private TimeDiscretization getFixTD() {
			return new TimeDiscretizationFromArray(startTime, endTime, fixFrequency, TimeDiscretizationFromArray.ShortPeriodLocation.SHORT_PERIOD_AT_START);
		}

		private TimeDiscretization getFloatTD() {
			return new TimeDiscretizationFromArray(startTime, endTime, floatFrequency, TimeDiscretizationFromArray.ShortPeriodLocation.SHORT_PERIOD_AT_START);

		}
	}
}
