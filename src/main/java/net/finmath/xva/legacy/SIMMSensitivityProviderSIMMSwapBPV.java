package net.finmath.xva.legacy;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.coordinates.simm2.MarginType;
import net.finmath.xva.coordinates.simm2.ProductClass;
import net.finmath.xva.coordinates.simm2.RiskClass;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;
import net.finmath.xva.sensitivityproviders.modelsensitivityproviders.ModelSensitivityProviderInterface;

import java.util.Map;
import java.util.Optional;

@Deprecated
public class SIMMSensitivityProviderSIMMSwapBPV {

	final ProductClass productClass = ProductClass.RATES_FX;
	final RiskClass riskClass = RiskClass.INTEREST_RATE;
	final MarginType riskType = MarginType.DELTA;

	Map<Simm2Coordinate, Double> notionalMap;

	ModelSensitivityProviderInterface forwardSensitivityProvider;

	public SIMMSensitivityProviderSIMMSwapBPV(ModelSensitivityProviderInterface forwardSensiProvider) throws Exception {

		// BUILD UP MAP
		// For Each Key: Build a ForwardSensitivityProvider
		this.forwardSensitivityProvider = forwardSensiProvider;
	}

	public RandomVariableInterface getSIMMSensitivity(String productClass,
													  String riskClass,
													  String riskType,
													  String bucketKey,      // currency for IR otherwise bucket number
													  String maturityBucket, // only for IR and Credit risk class, null otherwise
													  String curveIndexName, // null if riskClass is not IR
													  double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {

		Optional<Simm2Coordinate> optional = notionalMap.keySet().stream().filter(key -> key.getRiskClass().equals(riskClass) && key.getProductClass().equals(productClass) && key.getRiskType().equals(riskType) && key.getBucketKey().equals(bucketKey)).findAny();
		if (optional.isPresent()) {
			double effectiveMaturity = 0.0;//maturityMap.get(curveIndexName);
			double notional = notionalMap.get(curveIndexName);
			double bpvProxyAtZero = effectiveMaturity * notional / 10000.;
			return new RandomVariable(evaluationTime, model.getNumberOfPaths(), bpvProxyAtZero);
		} else
			return model.getRandomVariableForConstant(0.0);
	}
}
