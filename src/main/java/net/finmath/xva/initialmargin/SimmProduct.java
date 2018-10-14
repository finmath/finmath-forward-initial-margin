package net.finmath.xva.initialmargin;

import com.google.common.collect.ImmutableSet;
import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.coordinates.lmm.AadCoordinate;
import net.finmath.xva.coordinates.lmm.ArbitrarySimm2Transformation;
import net.finmath.xva.coordinates.lmm.ForwardCoordinates;
import net.finmath.xva.coordinates.lmm.IborSwapMarketQuantity;
import net.finmath.xva.coordinates.simm2.MarginType;
import net.finmath.xva.coordinates.simm2.ProductClass;
import net.finmath.xva.coordinates.simm2.RiskClass;
import net.finmath.xva.coordinates.simm2.Vertex;
import net.finmath.xva.sensitivityproviders.simmsensitivityproviders.SIMMSensitivityProviderInterface;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Set;

/**
 * A product whose value represents the initial margin accordin to be posted at a fixed time according to SIMM.
 */
public class SimmProduct extends AbstractLIBORMonteCarloProduct {
	private SIMMSensitivityProviderInterface simmSensitivityProvider;
	private double marginCalculationTime;
	private SimmModality modality;
	private SIMMHelper helper;
	private ArbitrarySimm2Transformation transformation;

	public SimmProduct(double marginCalculationTime, SIMMSensitivityProviderInterface provider, SimmModality modality) {
		this.modality = modality;
		this.marginCalculationTime = marginCalculationTime;
		this.simmSensitivityProvider = provider;
		this.helper = null;
		this.transformation = getTransformation();
	}

	private ArbitrarySimm2Transformation getTransformation() {
		return new ArbitrarySimm2Transformation(
				ImmutableSet.of(new IborSwapMarketQuantity(Vertex.Y1, "EUR", ProductClass.RATES_FX, "Libor3m", 0.25, 0.5)),
				ImmutableSet.of(new ForwardCoordinates()));
	}

	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		if (evaluationTime > marginCalculationTime) {
			return model.getRandomVariableForConstant(0.0);
		}

		RandomVariableInterface simmValue = Arrays.stream(ProductClass.values()).
				map(pc -> getSimmForProductClass(pc, evaluationTime, model)).
				reduce(model.getRandomVariableForConstant(0.0), RandomVariableInterface::add);

		System.out.println("SimmProduct: simmValue at " + DecimalFormat.getNumberInstance().format(evaluationTime) + ": " + DecimalFormat.getCurrencyInstance().format(simmValue.getAverage()));

		RandomVariableInterface numeraireAtEval = model.getNumeraire(evaluationTime);
		return simmValue.sub(this.getModality().getPostingThreshold()).floor(0.0).mult(numeraireAtEval);
	}

	public RandomVariableInterface getSimmForProductClass(ProductClass productClass, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		Set<RiskClass> riskClassList = helper.getRiskClassesForProductClass(productClass, evaluationTime);

		RandomVariableInterface[] contributions = Arrays.stream(RiskClass.values()).map(rc -> {
			if (riskClassList.contains(rc)) {
				try {
					return getSimmForRiskClass(rc, productClass, evaluationTime, model);
				} catch (CalculationException e) {
					throw new RuntimeException(e);
				}
			}

			return model.getRandomVariableForConstant(0.0);
		}).toArray(RandomVariableInterface[]::new);

		RandomVariableInterface simmProductClass = helper.getVarianceCovarianceAggregation(contributions, getModality().getParameterSet().CrossRiskClassCorrelationMatrix);
		return simmProductClass;
	}

	public RandomVariableInterface getSimmForRiskClass(RiskClass riskClass, ProductClass productClass, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		return getDeltaMargin(riskClass, productClass, evaluationTime, model).add(
				getVegaMargin(riskClass, productClass, evaluationTime, model));
	}

	public RandomVariableInterface getDeltaMargin(RiskClass riskClass, ProductClass productClass, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		return getDeltaScheme(riskClass, productClass, evaluationTime).getValue(evaluationTime, model);
	}

	private AbstractLIBORMonteCarloProduct getDeltaScheme(RiskClass riskClass, ProductClass productClass, double evaluationTime) { ;
		if (riskClass == RiskClass.INTEREST_RATE) {
			return new SIMMProductIRDelta(this.simmSensitivityProvider, productClass.name(), this.getModality().getParameterSet(), evaluationTime);
		}

		return new SIMMProductNonIRDeltaVega(this.simmSensitivityProvider, riskClass, productClass, MarginType.DELTA, modality, evaluationTime);
	}

	public RandomVariableInterface getVegaMargin(RiskClass riskClass, ProductClass productClass, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		SIMMProductNonIRDeltaVega VegaScheme = new SIMMProductNonIRDeltaVega(this.simmSensitivityProvider, riskClass, productClass, MarginType.VEGA, getModality(), evaluationTime);
		return VegaScheme.getValue(evaluationTime, model);
	}

	public RandomVariableInterface getCurvatureMargin(RiskClass riskClass, ProductClass productClass, double atTime) {
		throw new RuntimeException();
	}

	public SimmModality getModality() {
		return modality;
	}
}
