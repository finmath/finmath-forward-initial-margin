package net.finmath.xva.sensitivityproviders.timelines;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.FloatingpointDate;
import net.finmath.xva.coordinates.lmm.ArbitrarySimm2Transformation;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

import java.util.Map;

/**
 * Provides SIMM sensitivities from a model-to-market transformation.
 */
public class SimmTransformationTimeline implements SimmSensitivityTimeline {
	private ArbitrarySimm2Transformation transformation;
	private AbstractLIBORMonteCarloProduct marginedProduct;

	public SimmTransformationTimeline(ArbitrarySimm2Transformation transformation, AbstractLIBORMonteCarloProduct marginedProduct) {
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
	public Map<Simm2Coordinate, RandomVariableInterface> getSimmSensitivities(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		//TODO Rethink whether the operator itself has to know the product and thus the time
		return transformation.getTransformationOperator(evaluationTime, model).apply(evaluationTime, marginedProduct);
	}
}
