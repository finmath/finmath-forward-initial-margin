package net.finmath.xva.initialmargin.simm2.calculation;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.Scalar;
import net.finmath.xva.initialmargin.simm2.specs.ParameterSet;

/**
 * Represents a product that returns the initial margin to be posted at a fixed time according to SIMM.
 * This product will consider the non-IR Delta and Vega risk contributions to the total margin.
 */
public class SimmNonIRScheme extends SimmBaseScheme {

	public SimmNonIRScheme(ParameterSet parameter) {
		super(parameter);
	}

	/**
	 * Calculates the weighted sensitivity of a trade.
	 *
	 * @param coordinate The coordinate describing what kind of sensitivity (with respect to which risk factor) x is.
	 * @param x          The value of the sensitivity as a random variable.
	 * @return The {@link WeightedSensitivity} object representing the computation result.
	 */
	public WeightedSensitivity getWeightedSensitivity(SimmCoordinate coordinate, RandomVariableInterface x) {
		//Additional weighting:
		//Vega: For non-IR, non-CR we have a non-trivial volatility weight to multiply
		//This is not included in the risk weight (the risk weight is excluded from the concentration factor)
		RandomVariableInterface a = x.mult(parameter.getAdditionalWeight(coordinate));

		final double riskWeight = parameter.getRiskWeight(coordinate);
		final double threshold = parameter.getConcentrationThreshold(coordinate);
		//not for credit -- here we have to sum up all sensitivities belonging to the same issuer/seniority; todo.
		final RandomVariableInterface concentrationRiskFactor = a.abs().div(threshold).sqrt().cap(1.0);

		return new WeightedSensitivity(coordinate, concentrationRiskFactor, a.mult(riskWeight).mult(concentrationRiskFactor));
	}

	public BucketResult getBucketAggregation(String bucketName, Map<SimmCoordinate, RandomVariableInterface> gradient) {
		final Set<WeightedSensitivity> weightedSensitivities = gradient.entrySet().stream().map(e -> getWeightedSensitivity(e.getKey(), e.getValue())).collect(Collectors.toSet());

		RandomVariableInterface k = gradient.entrySet().stream().map(e -> getWeightedSensitivity(e.getKey(), e.getValue())).
				flatMap(w -> weightedSensitivities.stream().map(v -> w.getCrossTermNonIR(v, parameter))).
				reduce(new Scalar(0.0), RandomVariableInterface::add).sqrt();

		return new BucketResult(bucketName, weightedSensitivities, k);

	}

	/**
	 * Strips the vertices of the gradient summing the sensitivities of different vertices together.
	 * Vertex-level sensitivities are neither needed for vega margins (only the sum across vertices is needed) nor for delta margins (since this scheme does not handle IR delta).
	 * @param gradient The gradient which may contain coordinates with vertices.
	 * @return A stream of vertex-less coordinates paired with sensitivities.
	 */
	@Override
	protected Stream<Map.Entry<SimmCoordinate, RandomVariableInterface>> streamGradient(Map<SimmCoordinate, RandomVariableInterface> gradient) {
		return gradient.entrySet().stream().
				map(z -> Pair.of(z.getKey().stripVertex(), z.getValue())).
				collect(Collectors.groupingBy(Pair::getKey)).entrySet().stream().
				map(group -> Pair.of(
						group.getKey(),
						group.getValue().stream().map(Pair::getValue).reduce(new Scalar(0.0), RandomVariableInterface::add)
						));
	}
}
