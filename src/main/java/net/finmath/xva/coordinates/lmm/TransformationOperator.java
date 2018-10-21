package net.finmath.xva.coordinates.lmm;

import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

import java.util.Map;

/**
 * Provides a method to perform the transformation of a random variable's gradient.
 */
public interface TransformationOperator {
	/**
	 * Returns the transformed sensitivities in the SIMM framework.
	 * @param evaluationTime The time of the sensitivity as {@link net.finmath.time.FloatingpointDate}.
	 * @param product The product whose sensitivities are requested.
	 * @return A map from {@link Simm2Coordinate}s to the transformed sensitivity values.
	 */
	Map<Simm2Coordinate, RandomVariableInterface> apply(double evaluationTime, AbstractLIBORMonteCarloProduct product);
}
