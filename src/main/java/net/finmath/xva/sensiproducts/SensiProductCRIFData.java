package net.finmath.xva.sensiproducts;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.initialmargin.SIMMParameter;
import net.finmath.xva.tradespecifications.SIMMTradeSpecification;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class SensiProductCRIFData extends AbstractLIBORMonteCarloProduct {

    //Map<SIMMSensitivityKey, Double>     sensitivityMap;

    @Override
    public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
        return null;
    }

}
