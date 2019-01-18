package net.finmath.sensitivities.transformation;

import java.util.Map;

import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.stochastic.RandomVariable;

/**
 * Provides a method to perform the transformation of a random variable's gradient.
 */
public interface TransformationOperator<C> {
	/**
	 * Returns the transformed sensitivities in the SIMM framework.
	 * @param evaluationTime The time of the sensitivity as {@link net.finmath.time.FloatingpointDate}.
	 * @param product The product whose sensitivities are requested.
	 * @return A map from {@link SimmCoordinate}s to the transformed sensitivity values.
	 */
	Map<C, RandomVariable> apply(double evaluationTime, AbstractLIBORMonteCarloProduct product);
}
