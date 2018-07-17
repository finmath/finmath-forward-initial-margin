package net.finmath.xva.sensiproducts;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.MonteCarloSimulationInterface;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * A stub that only provides a time discretization and constants.
 */
class SimulationStub implements MonteCarloSimulationInterface {
	private TimeDiscretizationInterface timeDiscretization;

	SimulationStub(TimeDiscretizationInterface timeDiscretization) {
		this.timeDiscretization = timeDiscretization;
	}

	/**
	 * Returns the numberOfPaths.
	 *
	 * @return Returns the numberOfPaths.
	 */
	@Override
	public int getNumberOfPaths() {
		return 1;
	}

	/**
	 * Returns the timeDiscretization.
	 *
	 * @return Returns the timeDiscretization.
	 */
	@Override
	public TimeDiscretizationInterface getTimeDiscretization() {
		return timeDiscretization;
	}

	/**
	 * Returns the time for a given time index.
	 *
	 * @param timeIndex Time index
	 * @return Returns the time for a given time index.
	 */
	@Override
	public double getTime(int timeIndex) {
		return timeDiscretization.getTime(timeIndex);
	}

	/**
	 * Returns the time index for a given time.
	 *
	 * @param time The time.
	 * @return Returns the time index for a given time.
	 */
	@Override
	public int getTimeIndex(double time) {
		return timeDiscretization.getTimeIndex(time);
	}

	/**
	 * Returns a random variable which is initialized to a constant,
	 * but has exactly the same number of paths or discretization points as the ones used by this <code>MonteCarloSimulationInterface</code>.
	 *
	 * @param value The constant value to be used for initialized the random variable.
	 * @return A new random variable.
	 */
	@Override
	public RandomVariableInterface getRandomVariableForConstant(double value) {
		return new RandomVariable(value);
	}

	/**
	 * This method returns the weights of a weighted Monte Carlo method (the probability density).
	 *
	 * @param timeIndex Time index at which the process should be observed
	 * @return A vector of positive weights which sums up to one
	 * @throws CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariableInterface getMonteCarloWeights(int timeIndex) throws CalculationException {
		return null;
	}

	/**
	 * This method returns the weights of a weighted Monte Carlo method (the probability density).
	 *
	 * @param time Time at which the process should be observed
	 * @return A vector of positive weights which sums up to one
	 * @throws CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariableInterface getMonteCarloWeights(double time) throws CalculationException {
		return null;
	}

	/**
	 * Create a clone of this simulation modifying some of its properties (if any).
	 *
	 * @param dataModified The data which should be changed in the new model
	 * @return Returns a clone of this model, with some data modified (then it is no longer a clone :-)
	 * @throws CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public MonteCarloSimulationInterface getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {
		return null;
	}
}
