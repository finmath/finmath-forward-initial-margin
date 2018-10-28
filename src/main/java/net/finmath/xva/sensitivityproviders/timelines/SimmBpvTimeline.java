package net.finmath.xva.sensitivityproviders.timelines;

import com.google.common.collect.ImmutableMap;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.FloatingpointDate;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;
import net.finmath.xva.tradespecifications.SIMMTradeSpecification;

import java.util.Map;

/**
 * Approximates the sensitivity of a swap using its base point value.
 */
public class SimmBpvTimeline implements SimmSensitivityTimeline {

	private SIMMTradeSpecification tradeSpec;

	public SimmBpvTimeline(SIMMTradeSpecification tradeSpec) {
		this.tradeSpec = tradeSpec;
	}

	/**
	 * Returns all the sensitivities that are available from this source.
	 *
	 * @param evaluationTime The time as {@link FloatingpointDate}.
	 * @param model
	 * @return A map from coordinates to sensitivity values.
	 */
	@Override
	public Map<Simm2Coordinate, RandomVariableInterface> getSimmSensitivities(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		double bpv = tradeSpec.getNotional() * (tradeSpec.getIRCurve().getPeriodLength() / tradeSpec.getIRCurve().getDayInYears()) / 10000.0;

		return ImmutableMap.of(null, //TODO convert spec to coordinate
				model.getRandomVariableForConstant(bpv));
	}
}
