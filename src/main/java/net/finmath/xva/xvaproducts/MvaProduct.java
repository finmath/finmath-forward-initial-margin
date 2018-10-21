package net.finmath.xva.xvaproducts;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;
import net.finmath.xva.coordinates.lmm.ArbitrarySimm2Transformation;
import net.finmath.xva.coordinates.lmm.ForwardCoordinates;
import net.finmath.xva.coordinates.lmm.IborSwapMarketQuantity;
import net.finmath.xva.coordinates.simm2.ProductClass;
import net.finmath.xva.coordinates.simm2.Vertex;
import net.finmath.xva.initialmargin.SimmProduct;
import net.finmath.xva.initialmargin.SimmModality;
import net.finmath.xva.sensitivityproviders.simmsensitivityproviders.SIMMSensitivityProviderInterface;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Calculates the margin valuation adjustments by calculating the initial margins on a time discretization and integrating them.
 */
public class MvaProduct extends AbstractLIBORMonteCarloProduct {
	private Set<SimmProduct> initialMargins;
	private final ArbitrarySimm2Transformation deltaTransformation;
	private final ArbitrarySimm2Transformation vegaTransformation;

	public MvaProduct(SIMMSensitivityProviderInterface sensitivityProvider, SimmModality modality, TimeDiscretizationInterface times) {
		this.deltaTransformation = getDeltaTransformation();
		this.vegaTransformation = getVegaTransformation();
		this.initialMargins = IntStream.range(0, times.getNumberOfTimes())
				.mapToObj(timeIndex -> new SimmProduct(times.getTime(timeIndex), sensitivityProvider, modality,
						deltaTransformation, vegaTransformation))
				.collect(Collectors.toSet());
	}

	private ArbitrarySimm2Transformation getVegaTransformation() {
		//TODO return a vega transformation once the coordinate are there
		return null;
	}

	private ArbitrarySimm2Transformation getDeltaTransformation() {
		return new ArbitrarySimm2Transformation(
				ImmutableList.of(new IborSwapMarketQuantity(Vertex.Y1, "EUR", ProductClass.RATES_FX, "Libor3m", 0.25, 0.5)),
				ImmutableSet.of(new ForwardCoordinates()));
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
