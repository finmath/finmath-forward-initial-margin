package net.finmath.xva.initialmargin;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.sensitivityproviders.simmsensitivityproviders.SIMMSensitivityProviderInterface;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/**
 * Calculates the initial margin as required by ISDA SIMM at a certain time.
 */
public class SIMMProduct extends AbstractLIBORMonteCarloProduct {

	final SIMMHelper helper;
	private String calculationCCY;
	private double thresholdAmount;
	private SIMMParameter parameterSet;
	private SIMMSensitivityProviderInterface simmSensitivityProvider;
	private double marginCalculationTime;


	public SIMMProduct(double marginCalculationTime,
					   SIMMSensitivityProviderInterface simmSensitivityProvider,
					   SIMMParameter parameterSet,
					   String calculationCCY,
					   double thresholdAmount) {
		this.marginCalculationTime = marginCalculationTime;
		this.thresholdAmount = thresholdAmount;
		this.calculationCCY = calculationCCY;
		this.parameterSet = parameterSet;
		this.simmSensitivityProvider = simmSensitivityProvider;
		this.helper = null;//new SIMMHelper(simmSensitivityProvider.getTradeSpecs());
	}

	public String getCalculationCCY() {
		return this.calculationCCY;
	}

	public SIMMParameter getParameterSet() {
		return parameterSet;
	}

	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		if (evaluationTime > marginCalculationTime) {
			return model.getRandomVariableForConstant(0.0);
		}

		RandomVariableInterface SIMMValue = Arrays.stream(SIMMParameter.getProductClassKeys())
				.map(productClass -> getSIMMForProductClass(productClass, evaluationTime, model))
				.filter(Objects::nonNull)
				.reduce(RandomVariableInterface::add)
				.orElse(model.getRandomVariableForConstant(0.0));

		System.out.println("SIMMProduct: SIMMValue at " + DecimalFormat.getNumberInstance().format(evaluationTime) + ": " + DecimalFormat.getCurrencyInstance().format(SIMMValue.getAverage()));
		RandomVariableInterface SIMMValueAfterThreshold = this.thresholdAmount != 0.0 ? SIMMValue.sub(this.thresholdAmount).floor(0.0) : SIMMValue;
		RandomVariableInterface numeraireAtEval = model.getNumeraire(evaluationTime);
		RandomVariableInterface numeraireAtFlow = model.getNumeraire(marginCalculationTime);
		return SIMMValueAfterThreshold.mult(numeraireAtEval).div(numeraireAtFlow);
	}

	public RandomVariableInterface getSIMMForProductClass(String productClass, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {

		Set<String> riskClassList = helper.getRiskClassKeysForProductClass(productClass, evaluationTime);

		RandomVariableInterface[] contributions = Arrays.stream(SIMMParameter.getRiskClassKeys())
				.map(riskClass -> riskClassList.contains(riskClass) ? getSIMMForRiskClass(riskClass, productClass, evaluationTime, model) : model.getRandomVariableForConstant(0.0))
				.toArray(RandomVariableInterface[]::new);
		return SIMMHelper.getVarianceCovarianceAggregation(contributions, parameterSet.CrossRiskClassCorrelationMatrix);
	}

	public RandomVariableInterface getSIMMForRiskClass(String riskClassKey, String productClass, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		RandomVariableInterface deltaMargin = this.getDeltaMargin(riskClassKey, productClass, evaluationTime, model);
		RandomVariableInterface vegaMargin = this.getVegaMargin(riskClassKey, productClass, evaluationTime, model);
		//RandomVariableInterface    curatureMargin = this.getDeltaMargin(riskClassKey,productClass, atTime);

		return deltaMargin.add(vegaMargin);

	}

	public RandomVariableInterface getDeltaMargin(String riskClassKey, String productClassKey, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		RandomVariableInterface deltaMargin = null;

		String riskTypeKey = SIMMParameter.RiskType.Delta.name();
		if (riskClassKey.equals(SIMMParameter.RiskClass.InterestRate.name())) {
			SIMMProductIRDelta DeltaScheme = new SIMMProductIRDelta(this.simmSensitivityProvider, productClassKey, this.parameterSet, evaluationTime);
			deltaMargin = DeltaScheme.getValue(evaluationTime, model);
		} else {
			SIMMProductNonIRDeltaVega DeltaScheme = new SIMMProductNonIRDeltaVega(this.simmSensitivityProvider, riskClassKey, productClassKey, riskTypeKey, this.parameterSet, this.calculationCCY, evaluationTime);
			deltaMargin = DeltaScheme.getValue(evaluationTime, model);
		}
		return deltaMargin;
	}

	public RandomVariableInterface getVegaMargin(String riskClassKey, String productClassKey, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		SIMMProductNonIRDeltaVega VegaScheme = new SIMMProductNonIRDeltaVega(this.simmSensitivityProvider, riskClassKey, productClassKey, SIMMParameter.RiskType.Vega.name(), this.parameterSet, this.calculationCCY, evaluationTime);
		return VegaScheme.getValue(evaluationTime, model);
	}

	public RandomVariableInterface getCurvatureMargin(String riskClassKey, String productClassKey, double atTime) {
		throw new RuntimeException();
	}


}
