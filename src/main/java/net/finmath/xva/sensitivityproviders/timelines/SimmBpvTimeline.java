package net.finmath.xva.sensitivityproviders.timelines;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.MonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.FloatingpointDate;
import net.finmath.xva.coordinates.simm2.MarginType;
import net.finmath.xva.coordinates.simm2.ProductClass;
import net.finmath.xva.coordinates.simm2.RiskClass;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;
import net.finmath.xva.coordinates.simm2.Vertex;
import net.finmath.xva.tradespecifications.SIMMTradeSpecification;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Approximates the sensitivity of a swap using its base point value.
 */
public class SimmBpvTimeline extends AbstractMonteCarloProduct implements SimmSensitivityTimeline {

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
		RandomVariableInterface bpv;
		try {
			bpv = getValue(evaluationTime, model);
		} catch (CalculationException e) {
			throw new RuntimeException("No SIMM sensitivity deliverable as BPV calculation failed", e);
		}

		return Vertex.splitYearCountFraction(Math.max(0.0, tradeSpec.getMaxTimeToMaturity() - evaluationTime)).entrySet().stream().
				map(vertexAndWeight -> getCoordinateSensitivityPair(bpv, vertexAndWeight)).
				collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	private Pair<Simm2Coordinate, RandomVariableInterface> getCoordinateSensitivityPair(RandomVariableInterface bpv, Map.Entry<Vertex, Double> vertexAndWeight) {
		return Pair.of(
				new Simm2Coordinate(vertexAndWeight.getKey(), tradeSpec.getIRCurve().getName(), tradeSpec.getIRCurve().getCurrency(),
						RiskClass.INTEREST_RATE, MarginType.DELTA, ProductClass.RATES_FX),
				bpv.mult(vertexAndWeight.getValue()));
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, MonteCarloSimulationInterface model) throws CalculationException {
		double bpv = tradeSpec.getNotional() * (tradeSpec.getIRCurve().getPeriodLength() / tradeSpec.getIRCurve().getDayInYears()) / 10000.0;

		return model.getRandomVariableForConstant(bpv);
	}
}
