package net.finmath.lch.initialmargin.simulation.ratetransformations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import net.finmath.exception.CalculationException;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * A transformation method to calculate the dispersion of a rate time series based on an exponential weighted moving average.
 * The EWMA is an iterative formula and requires a ‘seed volatility’ (classic standard deviation) to begin the series. 
 * The sample variance of the first seed-period observations is calculated
 * separately on each path and assigned to the first observation. Recursion then
 * starts at the second observation. This historical initialization convention is
 * retained; it is not a causal estimate for the first seed-period dates.
 * 
 * @author Raphael Prandtl
 */
public class RateTransformationEWMA extends AbstractRateTransformation {
	
	private final int 				volatilitySeedPeriod;
	private final double 			decayFactor;
	
	
	/**
     * Performs an EWMA dispersion transformation.
     * 
     * @param volatilitySeedPeriod The number of entries to include in the initial standard deviation calculation
	 * @param decayFactor Finite EWMA decay factor in [0, 1).
	 * A high 𝜆 implies a relatively high weighting of historic data points, and low weighting of new data points.
	 */
	public RateTransformationEWMA(final int volatilitySeedPeriod, final double decayFactor) {
		super("EWMA");    // unique TransformationMethod ID
		if (volatilitySeedPeriod < 2) {
			throw new IllegalArgumentException("volatilitySeedPeriod must be at least 2.");
		}
		if (!Double.isFinite(decayFactor) || decayFactor < 0.0 || decayFactor >= 1.0) {
			throw new IllegalArgumentException("decayFactor must be finite and in [0, 1).");
		}
		this.volatilitySeedPeriod = volatilitySeedPeriod;
		this.decayFactor = decayFactor;
	}
	
	
	@Override
	public void applyTransformation(TransformationContext context) throws CalculationException{
		NavigableMap<LocalDateTime, RandomVariable> primaryData = context.getPrimaryData();
		if (primaryData.size() < volatilitySeedPeriod) {
			throw new CalculationException("volatilitySeedPeriod can't be greater than number of available rates.");
		}
		
		// "Plant" volatility seed at the beginning of the data set
		NavigableMap<LocalDateTime, RandomVariable> ewmaData = new TreeMap<>();
		List<LocalDateTime> dates = new ArrayList<>(primaryData.keySet());
		RandomVariable firstObservation = primaryData.get(dates.get(0));
		RandomVariable shiftedSum = new Scalar(0.0);

		for (int i = 0; i < volatilitySeedPeriod; i++) {
	        shiftedSum = shiftedSum.add(primaryData.get(dates.get(i)).sub(firstObservation));
	    }
		
		// Two-pass temporal variance avoids subtracting nearly equal second moments.
		// Shifting first keeps a constant series exactly zero on every path.
		RandomVariable shiftedMean = shiftedSum.div(volatilitySeedPeriod);
		RandomVariable squaredDeviations = new Scalar(0.0);
		for (int i = 0; i < volatilitySeedPeriod; i++) {
			RandomVariable deviation = primaryData.get(dates.get(i)).sub(firstObservation).sub(shiftedMean);
			squaredDeviations = squaredDeviations.add(deviation.squared());
		}
	    RandomVariable volatilitySeed = squaredDeviations.div(volatilitySeedPeriod - 1);

	    ewmaData.put(dates.get(0), volatilitySeed.sqrt());
	    
	    // EWMA Transformation based on volatility seed
	    double oneMinusDecayFactor = 1.0 - decayFactor; 
	    RandomVariable previousDispersionSquared = volatilitySeed;

	    for (int i = 1; i < primaryData.size(); i++) {
	    	LocalDateTime currentDate = dates.get(i);
	        RandomVariable currentReturnSquared = primaryData.get(currentDate).squared();
	        RandomVariable currentDispersionSquared = previousDispersionSquared.mult(decayFactor).add(currentReturnSquared.mult(oneMinusDecayFactor));
	        ewmaData.put(currentDate, currentDispersionSquared.sqrt());
	        previousDispersionSquared = currentDispersionSquared;
	    }
	    context.setPrimaryData(ewmaData);
	    context.addTransformationResult(this.getId(), context.getPrimaryData());
	}
	

}
