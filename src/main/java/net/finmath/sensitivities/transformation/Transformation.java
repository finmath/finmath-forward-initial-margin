package net.finmath.sensitivities.transformation;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;

public interface Transformation<C> {
	TransformationOperator<C> getTransformationOperator(double time, LIBORModelMonteCarloSimulationInterface simulation);
}
