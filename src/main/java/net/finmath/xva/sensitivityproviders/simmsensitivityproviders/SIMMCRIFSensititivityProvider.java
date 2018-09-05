package net.finmath.xva.sensitivityproviders.simmsensitivityproviders;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.sensiproducts.SensiProductSimpleSwapBPV;
import net.finmath.xva.tradespecifications.SIMMSensitivityKey;

import java.util.Map;

public class SIMMCRIFSensititivityProvider implements SIMMSensitivityProviderInterface {

    Map<SIMMSensitivityKey,Double>  SensitivitiyMap;


    public SIMMCRIFSensititivityProvider(Map<SIMMSensitivityKey,Double> SensitivityMap){
        this.SensitivitiyMap = SensitivityMap;
    }

    public Map<SIMMSensitivityKey,Double> getSensitivitiyMap(){
        return SensitivitiyMap;
    }

    public RandomVariableInterface getSIMMSensitivity(SIMMSensitivityKey key, double evaluationTime, LIBORModelMonteCarloSimulationInterface model){
        if ( SensitivitiyMap.containsKey(key))
            return new RandomVariable(evaluationTime,model.getNumberOfPaths(),SensitivitiyMap.get(key));
        else
            return new RandomVariable(evaluationTime,model.getNumberOfPaths(),0.0);

    }


}
