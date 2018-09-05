package net.finmath.xva.legacy;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.sensitivityproviders.modelsensitivityproviders.ModelSensitivityProviderInterface;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

import java.util.Map;
import java.util.Optional;

@Deprecated
public class SIMMSensitivityProviderSIMMPrecalculated {


    ModelSensitivityProviderInterface forwardSensitivityProvider;

    Map<Simm2Coordinate, Double> sensitivityMap;

    protected RandomVariableInterface getSIMMSensitivity(String productClass,
                                                      String riskClass,
                                                      String riskType,
                                                      String bucketKey,      // currency for IR otherwise bucket number
                                                      String maturityBucket, // only for IR and Credit risk class, null otherwise
                                                      String curveIndexName, // null if riskClass is not IR
                                                      double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {

        Optional<Simm2Coordinate> optional = sensitivityMap.keySet().stream().filter(key -> key.getRiskClass().equals(riskClass) && key.getProductClass().equals(productClass) && key.getRiskType().equals(riskType) && key.getBucketKey().equals(bucketKey)).findAny();
        if (optional.isPresent()) {

            double externalProvidedSensitivity = sensitivityMap.get(optional.get());
            Map<String,RandomVariableInterface> sensitivityMap = null;
            /*if (SIMMParameter.MarginType.valueOf(riskType).equals(SIMMParameter.MarginType.DELTA))
                sensitivityMap = forwardSensitivityProvider.getDeltaSensitivity(evaluationTime,model);
            else if (SIMMParameter.MarginType.valueOf(riskType).equals(SIMMParameter.MarginType.VEGA))
                sensitivityMap = forwardSensitivityProvider.getVegaSensitivities(evaluationTime,model);*/



            return model.getRandomVariableForConstant(0.0);
        } else
            return model.getRandomVariableForConstant(0.0);

    }
}
