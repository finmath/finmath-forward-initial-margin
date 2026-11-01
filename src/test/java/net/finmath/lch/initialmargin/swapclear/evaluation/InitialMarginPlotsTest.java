package net.finmath.lch.initialmargin.swapclear.evaluation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.junit.Test;

import net.finmath.lch.initialmargin.swapclear.libormodels.ModelFactory;
import net.finmath.time.FloatingpointDate;

public class InitialMarginPlotsTest {

	@Test
	public void fractionalPlotTimeUsesPrecedingDailyObservation() {
		LocalDateTime observation = InitialMarginPlots.getObservationDate(0.125);
		assertEquals(LocalDateTime.of(2024, 3, 21, 0, 0), observation);
		assertTrue(FloatingpointDate.getFloatingPointDateFromDate(ModelFactory.REFERENCE_DATE, observation) <= 0.125);
	}

	@Test
	public void weekendAndTargetHolidaysUsePreviousBusinessDayAtMidnight() {
		for (LocalDateTime date : new LocalDateTime[] {
				LocalDateTime.of(2024, 3, 29, 15, 0),
				LocalDateTime.of(2024, 3, 30, 12, 0),
				LocalDateTime.of(2024, 4, 1, 18, 0) }) {
			double time = FloatingpointDate.getFloatingPointDateFromDate(ModelFactory.REFERENCE_DATE, date);
			assertEquals(LocalDateTime.of(2024, 3, 28, 0, 0), InitialMarginPlots.getObservationDate(time));
		}
	}
}
