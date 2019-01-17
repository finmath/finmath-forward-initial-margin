package net.finmath.sensitivities.simm2;

import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class VertexTest {

	@Test
	public void testSplitYearCountFractionWithOneWeek() {
		assertThat(Vertex.splitYearCountFraction(0.02), hasEntry(equalTo(Vertex.W2), closeTo(1.0, 1E-16)));
	}

	@Test
	public void testSplitYearCountFractionWithFourYears() {
		assertThat(Vertex.splitYearCountFraction(4.0),
				allOf(
						hasEntry(equalTo(Vertex.Y3), closeTo(0.5, 1E-16)),
						hasEntry(equalTo(Vertex.Y5), closeTo(0.5, 1E-16)))
				);
	}

	@Test
	public void testSplitYearCountFractionWithFiveYears() {
		assertThat(Vertex.splitYearCountFraction(5.0), hasEntry(equalTo(Vertex.Y5), closeTo(1.0, 1E-16)));
	}

	@Test
	public void testSplitYearCountFractionWithFourtYears() {
		assertThat(Vertex.splitYearCountFraction(40.0), hasEntry(equalTo(Vertex.Y30), closeTo(1.0, 1E-16)));
	}
}