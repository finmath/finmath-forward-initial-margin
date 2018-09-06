package net.finmath.xva.initialmargin;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.coordinates.simm2.MarginType;
import net.finmath.xva.coordinates.simm2.ProductClass;
import net.finmath.xva.coordinates.simm2.RiskClass;
import net.finmath.xva.sensitivityproviders.simmsensitivityproviders.SIMMSensitivityProviderInterface;

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

	public SimmProduct(double marginCalculationTime, SIMMSensitivityProviderInterface provider, SimmModality modality) {
		this.modality = modality;
		this.marginCalculationTime = marginCalculationTime;
		this.simmSensitivityProvider = provider;
		this.helper = null;
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
		Set<String> riskClassList = helper.getRiskClassKeysForProductClass(productClass.name(), evaluationTime);

		RandomVariableInterface[] contributions = Arrays.stream(RiskClass.values()).map(rc -> {
			if (riskClassList.contains(rc.name())) {
				return getSimmForRiskClass(rc, productClass, evaluationTime, model);
			}

			return model.getRandomVariableForConstant(0.0);
		}).toArray(RandomVariableInterface[]::new);

		RandomVariableInterface simmProductClass = helper.getVarianceCovarianceAggregation(contributions, getModality().getParameterSet().CrossRiskClassCorrelationMatrix);
		return simmProductClass;
	}

	public RandomVariableInterface getSimmForRiskClass(RiskClass riskClass, ProductClass productClass, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		return getDeltaMargin(riskClass, productClass, evaluationTime, model).add(
				getVegaMargin(riskClass, productClass, evaluationTime, model));
	}

	public RandomVariableInterface getDeltaMargin(RiskClass riskClass, ProductClass productClass, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		RandomVariableInterface deltaMargin = null;

		String riskTypeKey = MarginType.DELTA.name();
		if (riskClass == RiskClass.INTEREST_RATE) {
			SIMMProductIRDelta DeltaScheme = new SIMMProductIRDelta(this.simmSensitivityProvider, productClass.name(), this.getModality().getParameterSet(), evaluationTime);
			deltaMargin = DeltaScheme.getValue(evaluationTime, model);
		} else {
			SIMMProductNonIRDeltaVega DeltaScheme = new SIMMProductNonIRDeltaVega(this.simmSensitivityProvider, riskClass.name(), productClass.name(), riskTypeKey, this.getModality().getParameterSet(), this.getModality().getCalculationCurrency(), evaluationTime);
			deltaMargin = DeltaScheme.getValue(evaluationTime, model);
		}
		return deltaMargin;
	}

	public RandomVariableInterface getVegaMargin(RiskClass riskClass, ProductClass productClass, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		SIMMProductNonIRDeltaVega VegaScheme = new SIMMProductNonIRDeltaVega(this.simmSensitivityProvider, riskClass.name(), productClass.name(), MarginType.VEGA.name(), this.getModality().getParameterSet(), this.getModality().getCalculationCurrency(), evaluationTime);
		return VegaScheme.getValue(evaluationTime, model);
	}

	public RandomVariableInterface getCurvatureMargin(RiskClass riskClass, ProductClass productClass, double atTime) {
		throw new RuntimeException();
	}

	public SimmModality getModality() {
		return modality;
	}
}
