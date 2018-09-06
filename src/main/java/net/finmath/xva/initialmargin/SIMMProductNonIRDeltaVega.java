package net.finmath.xva.initialmargin;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.coordinates.simm2.MarginType;
import net.finmath.xva.coordinates.simm2.ProductClass;
import net.finmath.xva.coordinates.simm2.RiskClass;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;
import net.finmath.xva.sensitivityproviders.simmsensitivityproviders.SIMMSensitivityProviderInterface;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SIMMProductNonIRDeltaVega extends AbstractLIBORMonteCarloProduct {
	private SimmModality modality;
	ProductClass productClass;
	RiskClass riskClass;
	String[] activeBucketKeys;
	MarginType marginType;
	final SIMMHelper helper;
	private SIMMSensitivityProviderInterface simmSensitivitivityProvider;

	public SIMMProductNonIRDeltaVega(SIMMSensitivityProviderInterface simmSensitivitivityProvider,
									 RiskClass riskClass,
									 ProductClass productClass,
									 MarginType marginType, SimmModality modality, double atTime) {
		this.modality = modality;
		this.helper = null;//new SIMMHelper(simmSensitivitivityProvider.getTradeSpecs());
		this.simmSensitivitivityProvider = simmSensitivitivityProvider;
		this.riskClass = riskClass;
		this.productClass = productClass;
		this.marginType = marginType;
		this.activeBucketKeys = helper.getBucketsByRiskClass(this.marginType, atTime).get(riskClass).stream().filter(e -> !e.equals("Residual")).toArray(String[]::new);
	}

	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {

		RandomVariableInterface deltaMargin = model.getRandomVariableForConstant(0.0);
		Double[][] correlationMatrix = getModality().getParameterSet().MapRiskClassCorrelationCrossBucketMap.get(this.riskClass);

		int length = correlationMatrix.length == 1 ? this.activeBucketKeys.length : correlationMatrix.length;

		if (this.activeBucketKeys.length > 0) {
			RandomVariableInterface[] KContributions = new RandomVariableInterface[length];
			RandomVariableInterface[] S1Contributions = new RandomVariableInterface[length];
			for (int iBucket = 0; iBucket < activeBucketKeys.length; iBucket++) {
				String bucketKey = null;
				int bucketIndex = 0;
				if (riskClass.equals(RiskClass.EQUITY) || riskClass.equals(RiskClass.COMMODITY)) {
					bucketKey = new Integer(activeBucketKeys[iBucket]).toString();
					bucketIndex = Integer.parseInt(activeBucketKeys[iBucket]);
				} else {
					bucketKey = this.activeBucketKeys[iBucket];
					bucketIndex = iBucket;
				}

				/*Check whether we have risk factors in that bucket*/
				Set<String> activeRiskFactorKeys = helper.getRiskFactorKeysByRiskClass(this.marginType, bucketKey, evaluationTime).get(riskClass);
				if (activeRiskFactorKeys != null && activeRiskFactorKeys.size() > 0) {
					Map<String, RandomVariableInterface> netSensitivityMap = this.getRiskFactorNetSensitivityMap(bucketKey, activeRiskFactorKeys, evaluationTime, model);
					RandomVariableInterface K1 = getAggregatedSensitivityForBucket(bucketKey, netSensitivityMap, evaluationTime);
					RandomVariableInterface sumWeigthedNetSensi = this.getRiskFactorWeightedNetSensitivityMap(bucketKey, netSensitivityMap, evaluationTime).values().stream().reduce(RandomVariableInterface::add).orElseGet(() -> new RandomVariable(evaluationTime, model.getNumberOfPaths(), 0.0));//this.getWeightedSensitivitySum(bucketKey, weightedNetSensitivities, evaluationTime);
					RandomVariableInterface S1 = K1.barrier(sumWeigthedNetSensi.sub(K1), K1, sumWeigthedNetSensi);
					RandomVariableInterface KNegative = K1.mult(-1);
					S1 = S1.barrier(S1.sub(KNegative), S1, KNegative);
					S1Contributions[bucketIndex] = S1;
					KContributions[bucketIndex] = K1;
				}
			}

			RandomVariableInterface VarCovar = helper.getVarianceCovarianceAggregation(S1Contributions, correlationMatrix);

			if (VarCovar == null) {
				return deltaMargin;
			} else {
				/*Adjustment on Diagonal*/
				VarCovar = VarCovar.squared();
				RandomVariableInterface SSumSQ = null;
				RandomVariableInterface KSumSQ = null;
				for (int k = 0; k < S1Contributions.length; k++) {
					if (S1Contributions[k] != null) {
						SSumSQ = SSumSQ == null ? SSumSQ = S1Contributions[k].squared() : SSumSQ.add(S1Contributions[k].squared());
						KSumSQ = KSumSQ == null ? KSumSQ = KContributions[k].squared() : KSumSQ.add(KContributions[k].squared());
					}
				}
				VarCovar = VarCovar.sub(SSumSQ).add(KSumSQ);
				deltaMargin = VarCovar.sqrt();
			}
		}

		/* RESIDUAL TERM*/
		if (!this.riskClass.equals(RiskClass.FX)) {
			String bucketKey = "Residual";
			Set<String> activeRiskFactorKeys = this.helper.getRiskFactorKeysByRiskClass(this.marginType, bucketKey, evaluationTime).get(riskClass);
			if (activeRiskFactorKeys != null && activeRiskFactorKeys.size() > 0) {
				Map<String, RandomVariableInterface> netSensitivityMap = this.getRiskFactorNetSensitivityMap(bucketKey, activeRiskFactorKeys, evaluationTime, model);
				Map<String, RandomVariableInterface> weightedNetSensitivityMap = this.getRiskFactorWeightedNetSensitivityMap(bucketKey, netSensitivityMap, evaluationTime);
				RandomVariableInterface KResidual = getAggregatedSensitivityForBucket(bucketKey, weightedNetSensitivityMap, evaluationTime);
				deltaMargin = deltaMargin.add(KResidual);
			}
		}

		return deltaMargin;
	}

	private Map<String, RandomVariableInterface> getRiskFactorNetSensitivityMap(String bucketKey, Set<String> activeRiskFactorKeys, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		return activeRiskFactorKeys.stream().collect(Collectors.toMap(activeRiskFactorKey -> activeRiskFactorKey, activeRiskFactor -> this.getNetSensitivity(activeRiskFactor, bucketKey, evaluationTime, model)));
	}

	private Map<String, RandomVariableInterface> getConcentrationFactorMap(String bucketKey, Map<String, RandomVariableInterface> riskFactorNetSensitivityMap, double atTime) {

		return
				riskFactorNetSensitivityMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> this.getConcentrationRiskFactor(entry.getValue(), entry.getKey(), bucketKey, atTime)));
	}

	private Map<String, RandomVariableInterface> getRiskFactorWeightedNetSensitivityMap(String bucketKey, Map<String, RandomVariableInterface> riskFactorNetSensitivityMap, double evaluationTime) {

		Map<String, RandomVariableInterface> concentrationFactors = this.getConcentrationFactorMap(bucketKey, riskFactorNetSensitivityMap, evaluationTime);

		return riskFactorNetSensitivityMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
			RandomVariableInterface netSensi = entry.getValue();
			RandomVariableInterface concentrationRiskFactor = concentrationFactors.get(entry.getKey());
			return this.getWeightedNetSensitivity(netSensi, concentrationRiskFactor, entry.getKey(), bucketKey, evaluationTime);
		}));
	}

	private RandomVariableInterface getAggregatedSensitivityForBucket(String bucketKey, Map<String, RandomVariableInterface> netSensitivityMap, double evaluationTime) {
		RandomVariableInterface aggregatedSensi = null;
		Double[][] correlationMatrix = new Double[netSensitivityMap.size()][netSensitivityMap.size()];
		Map<String, RandomVariableInterface> weightedNetSensitivityMap = this.getRiskFactorWeightedNetSensitivityMap(bucketKey, netSensitivityMap, evaluationTime);
		String[] activeRiskFactorKeys = netSensitivityMap.keySet().toArray(new String[0]);
		if (riskClass.equals(RiskClass.INTEREST_RATE) && marginType.equals(MarginType.VEGA)) {
			correlationMatrix = getModality().getParameterSet().MapRiskClassCorrelationIntraBucketMap.get("InterestRate_Tenor");
			RandomVariableInterface[] contributionsReDim = new RandomVariableInterface[correlationMatrix.length];
			int nTenors = getModality().getParameterSet().IRMaturityBuckets.length;
			for (String activeRiskFactorKey : activeRiskFactorKeys) {
				for (int i = 0; i < nTenors; i++) {
					if (activeRiskFactorKey.equals(getModality().getParameterSet().IRMaturityBuckets[i])) {
						contributionsReDim[i] = weightedNetSensitivityMap.get(activeRiskFactorKey);
					}
					if (activeRiskFactorKey.equals(SIMMParameter.inflationKey)) {
						contributionsReDim[contributionsReDim.length - 2] = weightedNetSensitivityMap.get(activeRiskFactorKey);
					}
				}
			}
			aggregatedSensi = helper.getVarianceCovarianceAggregation(contributionsReDim, correlationMatrix);
		} else {
			Double correlation = 0.0;
			if (riskClass.equals(RiskClass.FX)) {
				correlation = getModality().getParameterSet().MapRiskClassCorrelationIntraBucketMap.get(this.riskClass)[0][0];
			} else if (riskClass.equals(RiskClass.CREDIT_Q) || riskClass.equals(RiskClass.CREDIT_NON_Q)) {
				correlation = getModality().getParameterSet().MapRiskClassCorrelationIntraBucketMap.get(this.riskClass)[0][1];
			} else {
				int bucketNr = 0;
				try {
					bucketNr = (int) Double.parseDouble(bucketKey);
					correlation = getModality().getParameterSet().MapRiskClassCorrelationIntraBucketMap.get(this.riskClass)[0][bucketNr];
				} catch (Exception e) {
					bucketNr = getModality().getParameterSet().MapRiskClassCorrelationIntraBucketMap.get(this.riskClass)[0].length - 1;
					correlation = getModality().getParameterSet().MapRiskClassCorrelationIntraBucketMap.get(this.riskClass)[0][bucketNr];
				}
			}
			Map<String, RandomVariableInterface> concentrationFactors = this.getConcentrationFactorMap(bucketKey, netSensitivityMap, evaluationTime);
			RandomVariableInterface[] weightedNetSensitivitesArray = new RandomVariableInterface[activeRiskFactorKeys.length];
			for (int i = 0; i < activeRiskFactorKeys.length; i++) {
				String iRiskFactorKey = activeRiskFactorKeys[i];
				weightedNetSensitivitesArray[i] = netSensitivityMap.get(iRiskFactorKey);
				for (int j = 0; j < activeRiskFactorKeys.length; j++) {
					if (i != j) {
						String jRiskFactorKey = activeRiskFactorKeys[j];
						correlationMatrix[i][j] = getParameterF(concentrationFactors.get(iRiskFactorKey), concentrationFactors.get(jRiskFactorKey)).getAverage() * correlation;
					}
				}
			}
			aggregatedSensi = helper.getVarianceCovarianceAggregation(weightedNetSensitivitesArray, correlationMatrix);
		}

		return aggregatedSensi;
	}

	private RandomVariableInterface getWeightedNetSensitivity(RandomVariableInterface netSensi, RandomVariableInterface concentrationRiskFactor, String riskFactorKey, String bucketKey, double atTime) {
		int bucketIndex = 0;
		try {
			bucketIndex = (int) Double.parseDouble(bucketKey);
		} catch (NumberFormatException e) {
			bucketIndex = getModality().getParameterSet().MapRiskClassRiskweightMap.get(this.marginType).get(riskClass).entrySet().iterator().next().getValue()[0].length - 1;
		}
		bucketIndex = Math.max(0, bucketIndex);

		Double[][] riskWeights = getModality().getParameterSet().MapRiskClassRiskweightMap.get(this.marginType).get(riskClass).entrySet().iterator().next().getValue();
		double riskWeight = riskWeights[0][bucketIndex];

		double riskWeightAdjustment = this.getRiskWeightAdjustment(bucketIndex);

		return netSensi != null ? netSensi.mult(riskWeight).mult(riskWeightAdjustment).mult(concentrationRiskFactor) : null;
	}

	public RandomVariableInterface getParameterF(RandomVariableInterface CR1, RandomVariableInterface CR2) {
		RandomVariableInterface min = CR1.barrier(CR1.sub(CR2), CR2, CR1);
		RandomVariableInterface max = CR1.barrier(CR1.sub(CR2), CR1, CR2);
		return min.div(max);
	}

	public RandomVariableInterface getConcentrationRiskFactor(RandomVariableInterface netSensi, String riskFactorKey, String bucketKey, double atTime) {

		double concentrationThreshold = 1.0E12;
		int bucketIndex = 0;
		if (riskClass == RiskClass.FX) {
			Map<String, String> FXMap = getModality().getParameterSet().MapFXCategory;
			String category = null;
			String defaultCategory = "Category3";
			if (riskFactorKey.length() == 3) {
				category = FXMap.containsKey(riskFactorKey) ? FXMap.get(riskFactorKey) : defaultCategory;
			} else /* Usually Vega Case */ {
				String str1 = riskFactorKey.substring(0, 3);
				String str2 = riskFactorKey.substring(3, 6);
				String category1 = FXMap.containsKey(str1) ? FXMap.get(str1) : defaultCategory;
				String category2 = FXMap.containsKey(str2) ? FXMap.get(str2) : defaultCategory;
				category = category1 + "-" + category2;
			}
			concentrationThreshold = getModality().getParameterSet().MapRiskClassThresholdMap.get(marginType).get(riskClass).get(category)[0][0];
		} else {
			try {
				bucketIndex = (int) Double.parseDouble(bucketKey);
				concentrationThreshold = getModality().getParameterSet().MapRiskClassThresholdMap.get(marginType).get(riskClass).entrySet().iterator().next().getValue()[0][bucketIndex];
			} catch (Exception e) {
				if (bucketKey.equals("Residual")) { //!NumberUtils.isNumber(bucketKey))/*Usually RESIDUAL*/ {
					bucketIndex = getModality().getParameterSet().MapRiskClassThresholdMap.get(marginType).get(riskClass).entrySet().iterator().next().getValue()[0].length - 1;
					concentrationThreshold = getModality().getParameterSet().MapRiskClassThresholdMap.get(marginType).get(riskClass).entrySet().iterator().next().getValue()[0][bucketIndex];
				} else {
					String key = getModality().getParameterSet().IRCurrencyMap.get(bucketKey);
					if (key == null) {
						key = "High_Volatility_Currencies";
					}
					try {
						concentrationThreshold = getModality().getParameterSet().MapRiskClassThresholdMap.get(marginType).get(riskClass).get(key)[0][0];
					} catch (Exception e1) {
						concentrationThreshold = 1.0E12;
					}
				}
			}
		}

		double riskWeightAdjustment = this.getRiskWeightAdjustment(bucketIndex);
		netSensi = netSensi.mult(riskWeightAdjustment);
		RandomVariableInterface CR = (netSensi.abs().div(concentrationThreshold)).sqrt();
		CR = CR.barrier(CR.sub(1.0), CR, 1.0);
		return CR;
	}

	public double getRiskWeightAdjustment(int bucketIndex) {
		if (marginType == MarginType.VEGA && (riskClass == RiskClass.FX || riskClass == RiskClass.EQUITY) || riskClass == RiskClass.COMMODITY) {
			Double[][] deltaRiskWeights = getModality().getParameterSet().MapRiskClassRiskweightMap.get(MarginType.DELTA).get(riskClass).entrySet().iterator().next().getValue();
			double deltaRiskWeight = deltaRiskWeights[0][bucketIndex];
			double riskWeight = deltaRiskWeight;
			if (getModality().getParameterSet().MapHistoricalVolaRatio.containsKey(this.riskClass)) {
				double historicalVolaRatio = getModality().getParameterSet().MapHistoricalVolaRatio.get(this.riskClass);
				riskWeight = riskWeight * historicalVolaRatio;
			}
			return riskWeight;
		} else {
			return 1.0;
		}
	}

	public RandomVariableInterface getNetSensitivity(String riskFactorKey, String bucketKey, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {

		if (riskClass == RiskClass.FX) /* Sensitivities against Calculation CCY should be zero*/ {
			if (getModality().getCalculationCurrency().equals(riskFactorKey) && riskFactorKey.length() == 3) {
				return model.getRandomVariableForConstant(0.0);
			}
		}
		if (riskClass == RiskClass.CREDIT_Q || riskClass == RiskClass.CREDIT_NON_Q) {
			return Arrays.stream(getModality().getParameterSet().getCreditVertices()).map(vertex -> this.simmSensitivitivityProvider.getSIMMSensitivity(new Simm2Coordinate(vertex, riskFactorKey, bucketKey, riskClass, marginType, productClass), evaluationTime, model)).reduce(model.getRandomVariableForConstant(0.0),RandomVariableInterface::add);
		} else {

			if (marginType == MarginType.DELTA) {
				return simmSensitivitivityProvider.getSIMMSensitivity(new Simm2Coordinate(null, riskFactorKey, bucketKey, riskClass, marginType, productClass), evaluationTime, model);
			} else if (marginType == MarginType.VEGA && riskFactorKey.equals(SIMMParameter.inflationKey)) {
				return simmSensitivitivityProvider.getSIMMSensitivity(new Simm2Coordinate(null, riskFactorKey, bucketKey, riskClass, marginType, productClass), evaluationTime, model);
			} else {
				return Arrays.stream(getModality().getParameterSet().getIRVertices()).
						map(vertex -> simmSensitivitivityProvider.getSIMMSensitivity(new Simm2Coordinate(vertex, riskFactorKey, bucketKey, riskClass, marginType, productClass), evaluationTime, model)).
						reduce(model.getRandomVariableForConstant(0.0), RandomVariableInterface::add);
			}
		}
	}

	public SimmModality getModality() {
		return modality;
	}
}
