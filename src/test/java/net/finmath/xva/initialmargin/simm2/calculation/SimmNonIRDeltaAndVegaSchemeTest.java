package net.finmath.xva.initialmargin.simm2.calculation;

import com.google.common.collect.ImmutableMap;
import net.finmath.sensitivities.simm2.MarginType;
import net.finmath.sensitivities.simm2.ProductClass;
import net.finmath.sensitivities.simm2.RiskClass;
import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.Scalar;
import net.finmath.xva.initialmargin.simm2.specs.ParameterSet;
import net.finmath.xva.initialmargin.simm2.specs.Simm2_0;
import org.junit.Test;

import java.util.Map;

import static net.finmath.functions.NormalDistribution.inverseCumulativeDistribution;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.IsCloseTo.closeTo;

public class SimmNonIRDeltaAndVegaSchemeTest {

	@Test
	public void getMarginForSingleStockDelta() {

		final ParameterSet parameters = new Simm2_0();
		final SimmNonIRDeltaAndVegaScheme scheme = new SimmNonIRDeltaAndVegaScheme(parameters);
		final SimmCoordinate coordinate = new SimmCoordinate(null, "DAX", "11", RiskClass.EQUITY, MarginType.DELTA, ProductClass.EQUITY);
		final double riskWeight = parameters.getRiskWeightWithScaling(coordinate);

		//By choosing a sensitivity above the threshold we don't have to care about it in the assertion
		final double marketSensitivity = parameters.getConcentrationThreshold(coordinate)*2.0;

		Map<SimmCoordinate, RandomVariableInterface> gradient = ImmutableMap.of(
				coordinate,
				new Scalar(marketSensitivity)
		);

		final RandomVariableInterface result = scheme.getMargin(RiskClass.EQUITY, gradient);

		//For a single weighted sensitivity (one stock)
		//the result should be the weighted sensitivity

		assertThat(result.getAverage(), is(closeTo(marketSensitivity*riskWeight, 1E-8)));

	}

	@Test
	public void getMarginForSingleStockVega() {

		final ParameterSet parameters = new Simm2_0();
		final SimmNonIRDeltaAndVegaScheme scheme = new SimmNonIRDeltaAndVegaScheme(parameters);
		final SimmCoordinate coordinate = new SimmCoordinate(null, "DAX", "11", RiskClass.EQUITY, MarginType.VEGA, ProductClass.EQUITY);
		final double vegaRiskWeight = parameters.getRiskWeightWithScaling(coordinate);

		//By choosing a sensitivity above the threshold we don't have to care about it in the assertion
		final double marketSensitivity = parameters.getConcentrationThreshold(coordinate)*10000.0;

		Map<SimmCoordinate, RandomVariableInterface> gradient = ImmutableMap.of(
				coordinate,
				new Scalar(marketSensitivity)
		);

		final RandomVariableInterface result = scheme.getMargin(RiskClass.EQUITY, gradient);

		//For a single weighted sensitivity (one stock)
		//the result should be the vega risk (VRW * HVR * RW * scale * marketSensitivity)

		assertThat(result.getAverage(), is(closeTo(marketSensitivity*vegaRiskWeight, 1E-8)));

	}
}