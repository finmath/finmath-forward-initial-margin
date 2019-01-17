package net.finmath.sensitivities.simm2.products;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.MonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.sensitivities.GradientProduct;
import net.finmath.sensitivities.simm2.MarginType;
import net.finmath.sensitivities.simm2.ProductClass;
import net.finmath.sensitivities.simm2.RiskClass;
import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.sensitivities.simm2.Vertex;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.FloatingpointDate;
import net.finmath.xva.tradespecifications.SIMMTradeSpecification;

/**
 * Approximates the sensitivity of a swap using the period length instead of the annuity.
 */
public class ApproximateAnnuity extends AbstractMonteCarloProduct implements GradientProduct<SimmCoordinate> {

	private SIMMTradeSpecification tradeSpec;

	public ApproximateAnnuity(SIMMTradeSpecification tradeSpec) {
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
	public Map<SimmCoordinate, RandomVariableInterface> getGradient(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
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

	private Pair<SimmCoordinate, RandomVariableInterface> getCoordinateSensitivityPair(RandomVariableInterface bpv, Map.Entry<Vertex, Double> vertexAndWeight) {
		return Pair.of(
				new SimmCoordinate(vertexAndWeight.getKey(), tradeSpec.getIRCurve().getName(), tradeSpec.getIRCurve().getCurrency(),
						RiskClass.INTEREST_RATE, MarginType.DELTA, ProductClass.RATES_FX),
				bpv.mult(vertexAndWeight.getValue()));
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, MonteCarloSimulationInterface model) throws CalculationException {
		double bpv = tradeSpec.getNotional() * (tradeSpec.getIRCurve().getPeriodLength() / tradeSpec.getIRCurve().getDayInYears()) / 10000.0;

		return model.getRandomVariableForConstant(bpv);
	}
}
