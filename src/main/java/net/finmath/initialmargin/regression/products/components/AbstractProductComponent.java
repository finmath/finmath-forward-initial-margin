/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 03.09.2006
 */
package net.finmath.initialmargin.regression.products.components;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.finmath.exception.CalculationException;
import net.finmath.initialmargin.regression.products.AbstractLIBORMonteCarloRegressionProduct;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * Base class for product components.
 * <p>
 * Product components are small functions mapping a vector of
 * random variables to a random variable.
 * <p>
 * Components are numeraire adjusted and can be valued on its own.
 *
 * @author Christian Fries
 */
public abstract class AbstractProductComponent extends AbstractLIBORMonteCarloRegressionProduct implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -916286619811716575L;

	protected static ThreadPoolExecutor executor = new ThreadPoolExecutor(
			10 + Runtime.getRuntime().availableProcessors(),
			100 + 2 * Runtime.getRuntime().availableProcessors(),
			10L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {
				@Override
				public Thread newThread(Runnable runnable) {
					Thread thread = Executors.defaultThreadFactory().newThread(runnable);
					thread.setDaemon(true);
					return thread;
				}
			});

	public AbstractProductComponent(String currency) {
		super(currency);
	}

	public AbstractProductComponent() {
		this(null);
	}

	/**
	 * Returns a set of underlying names referenced by this product component (i.e., required for valuation) or null if none.
	 *
	 * @return A set of underlying names referenced by this product component (i.e., required for valuation) or null if none.
	 */
	public abstract Set<String> queryUnderlyings();

	@Override
	public Map<String, Object> getValues(double evaluationTime, TermStructureMonteCarloSimulationModel model) throws CalculationException {
		RandomVariable value = this.getValue(evaluationTime, model);
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("value", value);
		return result;
	}

	// INSERTED
	public abstract RandomVariable getValue(double evaluationTime, double fixingDate, LIBORModelMonteCarloSimulationModel model) throws CalculationException;
}
