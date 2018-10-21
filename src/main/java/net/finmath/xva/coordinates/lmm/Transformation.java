package net.finmath.xva.coordinates.lmm;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;

public interface Transformation {
	TransformationOperator getTransformationOperator(double time, LIBORModelMonteCarloSimulationInterface simulation);
}
