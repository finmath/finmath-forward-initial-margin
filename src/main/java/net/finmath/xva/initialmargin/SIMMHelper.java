package net.finmath.xva.initialmargin;


import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SIMMHelper {

    Set<Simm2Coordinate> keySet;

    public  SIMMHelper(Set<Simm2Coordinate> keySet){
        this.keySet = keySet;
        //this.tradeSet = tradeSet.stream().map(trade->(SIMMTradeSpecification)trade).collect(Collectors.toSet());
    }



    public  static RandomVariableInterface getVarianceCovarianceAggregation(RandomVariableInterface[] contributions, Double[][] correlationMatrix){
        int i = 0;
        RandomVariableInterface value = null;
        for (RandomVariableInterface contribution1 : contributions) {
            if ( contribution1!=null) {
                value = value == null ? contribution1.squared() : value.add(contribution1.squared());
                int j = 0;
                for (RandomVariableInterface contribution2 : contributions) {
                    if (contribution2 != null && i != j) {
                        double correlation = correlationMatrix.length==1 ? correlationMatrix[0][0] : correlationMatrix[i][j];
                        RandomVariableInterface contribution = contribution1.mult(contribution2).mult(correlation);
                        value = value == null ? contribution : value.add(contribution);
                    }
                    j++;
                }
            }
            i++;
        }
        if ( value==null)
            return null;
        value = value.sqrt();
        return value;
    }


    /*public Set<SIMMTradeSpecification> getTadeSelection(String productClassKey, String riskClassKey, double evaluationTime){
        return this.keySet.stream()
                .filter(sensitivityKey -> sensitivityKey.getProductClass().equals(productClassKey) && sensitivityKey.getRiskClass().equals(riskClassKey)).collect(Collectors.toSet());

    }*/

    public Set<String>   getProductClassKeys(double evaluationTime)
    {
        Set<String> riskClasses =this.keySet.stream().filter(k->k!=null).map(k-> k.getProductClass().name())
                .distinct().collect(Collectors.toSet());
        return riskClasses;
    }

    public Set<String>   getRiskClassKeysForProductClass(String productClassKey, double evaluationTime)
    {
        Set<String> riskClasses = this.keySet.stream().filter(k->k!=null).filter(k->k.getProductClass().equals(productClassKey)).map(k-> k.getRiskClass().name()).distinct().collect(Collectors.toSet());

        return riskClasses;
    }

    public Set<String>   getRiskClassKeys(double evaluationTime)
    {
        Set<String> riskClasses = this.keySet.stream().filter(k->k!=null).map(k-> k.getRiskClass().name()).distinct().collect(Collectors.toSet());

        return riskClasses;
    }

    public Map<String,Set<String> > getRiskClassBucketKeyMap(String riskTypeString, double evaluationTime){

        Set<String> riskClassKeys = getRiskClassKeys(evaluationTime);
        Map<String,Set<String> >     mapRiskClassBucketKeys = new HashMap<>();
        riskClassKeys.stream().forEach(riskClass ->{
            Set<String> riskFactors = this.keySet.stream().filter(  k->  k!=null && k.getRiskClass().equals(riskClass) && k.getRiskType().equals(riskTypeString)).map(k->k.getBucketKey()).distinct().collect(Collectors.toSet());

            mapRiskClassBucketKeys.put(riskClass,riskFactors);
        });
        return mapRiskClassBucketKeys;

    }

    public Map<String,Set<String>  >     getRiskClassRiskFactorMap(String riskTypeString, String bucketKey, double evaluationTime){

        Set<String> riskClassKeys = getRiskClassKeys(evaluationTime);
        if (!riskTypeString.equals("vega") ) {
            Map<String, Set<String> > mapRiskClassRiskFactorKeys = new HashMap<>();
//        if ( riskTypeString.equals("delta")) {
            riskClassKeys.stream().forEach(riskClass -> {
                Set<String> riskFactors = this.keySet.stream().filter(k -> k!=null &&  k.getRiskClass().equals(riskClass) && k.getRiskType().equals(riskTypeString) && k.getBucketKey().equals(bucketKey)).map(k -> k.getRiskFactorKey()).distinct().collect(Collectors.toSet());
                mapRiskClassRiskFactorKeys.put(riskClass, riskFactors);
            });
            return mapRiskClassRiskFactorKeys;
//        }
        }
        else{
            Map<String,Set<String> >     mapRiskClassRiskFactorKeys = new HashMap<>();
            riskClassKeys.stream().forEach(riskClass -> {
                if ( riskClass.equals("InterestRate") ) {
                    Set<String> riskFactors = this.keySet.stream().filter(k -> k!=null && k.getRiskClass().equals(riskClass) && k.getRiskType().equals(riskTypeString) && k.getBucketKey().equals(bucketKey)).map(k -> k.getRiskFactorKey())
                            .distinct().collect(Collectors.toSet());
                    mapRiskClassRiskFactorKeys.put(riskClass, riskFactors);
                }
                else{
                    Set<String> riskFactors = this.keySet.stream().filter(k ->  k!=null && k.getRiskClass().equals(riskClass) && k.getRiskType().equals(riskTypeString) && k.getBucketKey().equals(bucketKey)).map(k -> k.getRiskFactorKey())
                            .distinct().collect(Collectors.toSet());
                    mapRiskClassRiskFactorKeys.put(riskClass, riskFactors);
                }
            });
            return mapRiskClassRiskFactorKeys;
        }

    }
}
