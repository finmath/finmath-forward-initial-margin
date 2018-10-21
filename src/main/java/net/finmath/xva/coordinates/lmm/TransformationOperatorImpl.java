package net.finmath.xva.coordinates.lmm;

import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

import java.util.Map;

public class TransformationOperatorImpl implements TransformationOperator {


	@Override
	public Map<Simm2Coordinate, RandomVariableInterface> apply(double evaluationTime, AbstractLIBORMonteCarloProduct x) {
		return null;
	}
}
