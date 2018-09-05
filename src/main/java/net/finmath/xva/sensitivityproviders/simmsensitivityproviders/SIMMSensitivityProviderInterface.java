package net.finmath.xva.sensitivityproviders.simmsensitivityproviders;


import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.initialmargin.SIMMParameter;
import net.finmath.xva.tradespecifications.SIMMSensitivityKey;

public interface SIMMSensitivityProviderInterface {



    public RandomVariableInterface getSIMMSensitivity(SIMMSensitivityKey key, double evaluationTime, LIBORModelMonteCarloSimulationInterface model);//throws SolverException, CloneNotSupportedException, CalculationException;


}

