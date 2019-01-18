package net.finmath.sensitivities.transformation;

import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;

public interface Transformation<C> {
	TransformationOperator<C> getTransformationOperator(double time, LIBORModelMonteCarloSimulationModel simulation);
}
