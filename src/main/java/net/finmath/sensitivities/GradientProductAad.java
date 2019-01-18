package net.finmath.sensitivities;

import java.util.Map;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.sensitivities.transformation.Transformation;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.FloatingpointDate;

/**
 * Provides gradients from AAD model gradients undergone a transformation.
 */
public class GradientProductAad<C> implements GradientProduct<C> {
	private Transformation<C> transformation;
	private AbstractLIBORMonteCarloProduct marginedProduct;

	public GradientProductAad(Transformation<C> transformation, AbstractLIBORMonteCarloProduct marginedProduct) {
		this.transformation = transformation;
		this.marginedProduct = marginedProduct;
	}

	/**
	 * Returns all the sensitivities that are available from this source.
	 *
	 * @param evaluationTime The time as {@link FloatingpointDate}.
	 * @param model
	 * @return A map from coordinates to sensitivity values.
	 */
	@Override
	public Map<C, RandomVariable> getGradient(double evaluationTime, LIBORModelMonteCarloSimulationModel model) {
		//TODO Rethink whether the operator itself has to know the product and thus the time
		return transformation.getTransformationOperator(evaluationTime, model).apply(evaluationTime, marginedProduct);
	}
}
