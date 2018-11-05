package net.finmath.xva.coordinates.lmm;

import java.util.Map;

import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

/**
 * Provides a method to perform the transformation of a random variable's gradient.
 */
public interface TransformationOperator {
	Map<Simm2Coordinate, RandomVariableInterface> apply(RandomVariableDifferentiableInterface x);
}
