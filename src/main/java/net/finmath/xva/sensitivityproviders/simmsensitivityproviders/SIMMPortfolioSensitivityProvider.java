package net.finmath.xva.sensitivityproviders.simmsensitivityproviders;

import java.util.Set;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Composes the sensitivities obtained by underlying sensitivity providers into a single one.
 */
public class SIMMPortfolioSensitivityProvider implements SIMMSensitivityProviderInterface {
	private Set<? extends SIMMSensitivityProviderInterface> underlyingSensiProviders;

	/**
	 * @param underlyingSensiProviders A set of sensitivity providers responsible for calculating sensitivities of the portfolio constituents.
	 */
	public SIMMPortfolioSensitivityProvider(Set<? extends SIMMSensitivityProviderInterface> underlyingSensiProviders) {
		this.underlyingSensiProviders = underlyingSensiProviders;
	}

	/**
	 * Delegates the sensitivity request to the underlying sensitivity providers and returns the sum.
	 *
	 * @param coordinate
	 * @param evaluationTime
	 * @param model
	 * @return The sum of the sensitivities in this portfolio.
	 */
	@Override
	public RandomVariableInterface getSIMMSensitivity(SimmSensitivityCoordinate coordinate, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		return underlyingSensiProviders.stream()
				.map(u -> u.getSIMMSensitivity(coordinate, evaluationTime, model))
				.reduce(RandomVariableInterface::add).orElse(model.getRandomVariableForConstant(0.0));
	}
}
