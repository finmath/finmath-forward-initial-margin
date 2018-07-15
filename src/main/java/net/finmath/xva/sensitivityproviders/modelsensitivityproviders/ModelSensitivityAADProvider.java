package net.finmath.xva.sensitivityproviders.modelsensitivityproviders;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Provides model sensitivities from a product able to generate them.
 */
public class ModelSensitivityAADProvider implements ModelSensitivityProviderInterface {
	private AbstractLIBORMonteCarloProduct sensitvityProduct;

	/**
	 * Creates a model sensitivity provider which will return the value of a Monte Carlo product, interpreted as model sensitivity.
	 * @param sensitvityProduct A product whose value is the forward sensitivity.
	 */
	public ModelSensitivityAADProvider(AbstractLIBORMonteCarloProduct sensitvityProduct) {
		this.sensitvityProduct = sensitvityProduct;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model, String curveKey) {
		//TODO ask the product for its value
		return null;
	}
}
