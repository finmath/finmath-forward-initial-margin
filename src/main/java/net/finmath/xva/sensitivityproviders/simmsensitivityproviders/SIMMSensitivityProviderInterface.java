package net.finmath.xva.sensitivityproviders.simmsensitivityproviders;


import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

public interface SIMMSensitivityProviderInterface {


    public    RandomVariableInterface getSIMMSensitivity(String productClass,
                                                                     String riskClass,
                                                                     String riskType,
                                                                     String bucketKey,      // currency for IR otherwise bucket number
                                                                     String maturityBucket, // only for IR and Credit risk class, null otherwise
                                                                     String curveIndexName, // null if riskClass is not IR
                                                                     double evaluationTime, LIBORModelMonteCarloSimulationInterface model) ;//throws SolverException, CloneNotSupportedException, CalculationException;


}
