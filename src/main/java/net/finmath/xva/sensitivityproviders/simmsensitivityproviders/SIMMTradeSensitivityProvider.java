package net.finmath.xva.sensitivityproviders.simmsensitivityproviders;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.sensitivityproviders.modelsensitivityproviders.ModelSensitivityProviderInterface;
import net.finmath.xva.tradespecifications.SIMMTradeSpecification;

public class SIMMTradeSensitivityProvider implements SIMMSensitivityProviderInterface {

	ModelSensitivityProviderInterface modelSensitivityProvider;
	SIMMTradeSpecification simmTradeSpecification;

	public SIMMTradeSensitivityProvider(ModelSensitivityProviderInterface modelSensitivityProvider, SIMMTradeSpecification simmTradeSpecification) {
		this.modelSensitivityProvider = modelSensitivityProvider;
		this.simmTradeSpecification = simmTradeSpecification;
	}

	public ModelSensitivityProviderInterface getModelSensitivityProvider() {
		return modelSensitivityProvider;
	}

	public SIMMTradeSpecification getSimmTradeSpecification() {
		return simmTradeSpecification;
	}

	/**
	 * @param coordinate     Determines which sensitivity (with respect to risk class, curve and bucket) shall be retrieved.
	 * @param evaluationTime The time of the forward sensitivity.
	 * @param model          The model that is used to calculate model sensitivities.
	 * @return A forward sensitivity according to the ISDA SIMM framework.
	 */
	@Override
	public RandomVariableInterface getSIMMSensitivity(SimmSensitivityCoordinate coordinate, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		RandomVariableInterface modelSensi = modelSensitivityProvider.getValue(evaluationTime, model, "CURVE-KEY-MUST-BE-DETERMINED");
		//TODO: do the actual SIMM mapping.
		return modelSensi;
	}
}
