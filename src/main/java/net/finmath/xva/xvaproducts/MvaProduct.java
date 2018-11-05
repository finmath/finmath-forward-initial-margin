package net.finmath.xva.xvaproducts;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;
import net.finmath.xva.initialmargin.SimmModality;
import net.finmath.xva.initialmargin.SimmProduct;
import net.finmath.xva.sensitivityproviders.simmsensitivityproviders.SIMMSensitivityProviderInterface;

/**
 * Calculates the margin valuation adjustments by calculating the initial margins on a time discretization and integrating them.
 */
public class MvaProduct extends AbstractLIBORMonteCarloProduct {
	private Set<SimmProduct> initialMargins;

	public MvaProduct(SIMMSensitivityProviderInterface sensitivityProvider, SimmModality modality, TimeDiscretizationInterface times) {
		initialMargins = IntStream.range(0, times.getNumberOfTimes())
				.mapToObj(timeIndex -> new SimmProduct(times.getTime(timeIndex), sensitivityProvider, new SimmModality(null, "EUR", 0.0)))
				.collect(Collectors.toSet());
	}

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model          The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		return initialMargins.stream()
				.map(im -> {
					try {
						return im.getValue(evaluationTime, model);
					} catch (CalculationException e) {
						return model.getRandomVariableForConstant(Double.NaN);
					}
				})
				.reduce(RandomVariableInterface::add)
				.orElse(model.getRandomVariableForConstant(0.0));
	}
}
