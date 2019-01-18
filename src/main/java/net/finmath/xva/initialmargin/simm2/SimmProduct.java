package net.finmath.xva.initialmargin.simm2;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.sensitivities.GradientProduct;
import net.finmath.sensitivities.simm2.MarginType;
import net.finmath.sensitivities.simm2.RiskClass;
import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.xva.initialmargin.simm2.calculation.SimmCurvatureScheme;
import net.finmath.xva.initialmargin.simm2.calculation.SimmIRScheme;
import net.finmath.xva.initialmargin.simm2.calculation.SimmNonIRScheme;

/**
 * A product whose value represents the total initial margin to be posted at a fixed time according to SIMM.
 */
public class SimmProduct extends AbstractLIBORMonteCarloProduct {
	private GradientProduct<SimmCoordinate> gradientProduct;
	private double marginCalculationTime;
	private SimmModality modality;
	private SimmIRScheme irScheme;
	private SimmNonIRScheme nonIRScheme;
	private SimmCurvatureScheme curvatureScheme;

	public SimmProduct(double marginCalculationTime, GradientProduct<SimmCoordinate> gradientProduct, SimmModality modality) {
		this.modality = modality;
		this.marginCalculationTime = marginCalculationTime;
		this.gradientProduct = gradientProduct;
		this.irScheme = new SimmIRScheme(modality.getParams());
		this.nonIRScheme = new SimmNonIRScheme(modality.getParams());
		this.curvatureScheme = new SimmCurvatureScheme(modality.getParams());
	}

	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		if (evaluationTime > marginCalculationTime) {
			return model.getRandomVariableForConstant(0.0);
		}

		final RandomVariable simmValue = gradientProduct.getGradient(evaluationTime, model).entrySet().stream().
				collect(Collectors.groupingBy(e -> e.getKey().getProductClass())).entrySet().stream().
				map(group -> getSimmForProductClass(group.getValue())).
				reduce(model.getRandomVariableForConstant(0.0), RandomVariable::add);

		RandomVariable numeraireAtEval = model.getNumeraire(evaluationTime);
		return simmValue.sub(this.getModality().getPostingThreshold()).floor(0.0).mult(numeraireAtEval);
	}

	private RandomVariable getSimmForProductClass(List<Map.Entry<SimmCoordinate, RandomVariable>> sensitivities) {
		final Map<RiskClass, RandomVariable> marginByRiskClass = sensitivities.stream().
				collect(Collectors.groupingBy(e -> e.getKey().getRiskClass())).entrySet().stream().
				map(group -> Pair.of(group.getKey(), getSimmForRiskClass(
						group.getKey(),
						group.getValue().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
						))).
				collect(Collectors.toMap(Pair::getKey, Pair::getValue));

		return marginByRiskClass.entrySet().stream().
				flatMap(im1 -> marginByRiskClass.entrySet().stream().
						map(im2 -> im1.getValue().mult(im2.getValue()).mult(modality.getParams().getRiskClassCorrelation(im1.getKey(), im2.getKey())))
						).reduce(new Scalar(0.0), RandomVariable::add).sqrt();
	}

	private RandomVariable getSimmForRiskClass(RiskClass riskClass, Map<SimmCoordinate, RandomVariable> gradient) {
		final Map<MarginType, Map<SimmCoordinate, RandomVariable>> gradientsByMarginType = gradient.entrySet().stream().
				collect(Collectors.groupingBy(e -> e.getKey().getMarginType(), Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

		return gradientsByMarginType.entrySet().stream().reduce((RandomVariable)new Scalar(0.0),
				(accum, e) -> {
					if (e.getKey() == MarginType.CURVATURE) {
						return accum.add(curvatureScheme.getMargin(riskClass, gradient));
					}
					if (riskClass == RiskClass.INTEREST_RATE) {
						return accum.add(irScheme.getMargin(gradient));
					}

					return accum.add(nonIRScheme.getMargin(riskClass, gradient));
				}, RandomVariable::add);
	}

	public SimmModality getModality() {
		return modality;
	}
}
