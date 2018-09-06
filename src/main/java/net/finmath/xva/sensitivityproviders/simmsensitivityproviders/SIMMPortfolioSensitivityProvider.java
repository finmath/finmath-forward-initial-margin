package net.finmath.xva.sensitivityproviders.simmsensitivityproviders;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

import java.util.Set;

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
	public RandomVariableInterface getSIMMSensitivity(Simm2Coordinate coordinate, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		return underlyingSensiProviders.stream()
				.map(u -> u.getSIMMSensitivity(coordinate, evaluationTime, model))
				.reduce(RandomVariableInterface::add).orElse(model.getRandomVariableForConstant(0.0));
	}
}
