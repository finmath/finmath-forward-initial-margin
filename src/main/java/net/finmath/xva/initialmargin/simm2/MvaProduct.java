package net.finmath.xva.initialmargin.simm2;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.sensitivities.GradientProduct;
import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Calculates the margin valuation adjustments by calculating the initial margins on a time discretization and integrating them.
 */
public class MvaProduct extends AbstractLIBORMonteCarloProduct {
	private Set<SimmProduct> initialMargins;

	public MvaProduct(GradientProduct<SimmCoordinate> gradientProduct, SimmModality modality, TimeDiscretizationInterface times) {
		this.initialMargins = IntStream.range(0, times.getNumberOfTimes())
				.mapToObj(timeIndex -> new SimmProduct(times.getTime(timeIndex), gradientProduct, modality))
				.collect(Collectors.toSet());
	}

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
