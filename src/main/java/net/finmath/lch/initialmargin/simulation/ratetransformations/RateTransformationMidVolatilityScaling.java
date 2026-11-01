package net.finmath.lch.initialmargin.simulation.ratetransformations;


import java.time.LocalDateTime;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import net.finmath.exception.CalculationException;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;



/**
 * Transformation method to perform a mid volatility scaling r^*_t = r_t/2 * (1 + vol_N/vol_t).
 * Used by LCH in order to apply a historical scenario to today's market environment.
 * Returns are scaled to reflect halfway (i.e. the mid-point) between the current volatility and historic. 
 * Returns and dispersions must have identical observation dates. A zero return
 * with zero dispersion remains zero on that path; a nonzero return with zero
 * dispersion is undefined and rejected without an implicit volatility floor.
 * 
 * @author Raphael Prandtl
 */
public class RateTransformationMidVolatilityScaling extends AbstractRateTransformation {
	
    private final String 						transformationId; 
	
    
    /**
     * Performs a mid volatility scaling transformation.
     * The primary data set is assumed to be the dispersion data and the one the must be provided the return series.
     * 
     * @param transformationId The transformation ID of the return data set.
     */
	public RateTransformationMidVolatilityScaling(final String transformationId) {
		super("MidVolatilityScaling");    // unique TransformationMethod ID
		this.transformationId = transformationId;
	}
	
	
	@Override
	public void applyTransformation(TransformationContext context) throws CalculationException {
		NavigableMap<LocalDateTime, RandomVariable> primaryData = context.getPrimaryData();
		NavigableMap<LocalDateTime, RandomVariable> additionalData = context.getTransformationResult(transformationId);
	    if (additionalData == null) {
	        throw new CalculationException("Additional data set for transformation not found.");
	    }
		if (primaryData.isEmpty()) {
			throw new CalculationException("Mid-volatility scaling requires a nonempty dispersion series.");
		}
		if (!primaryData.keySet().equals(additionalData.keySet())) {
			throw new CalculationException("Return and dispersion series must have identical observation dates.");
		}
			
		NavigableMap<LocalDateTime, RandomVariable> midVolatilityData = new TreeMap<>();
	    RandomVariable latestDispersion = primaryData.lastEntry().getValue();
	    for (Map.Entry<LocalDateTime, RandomVariable> entry : primaryData.entrySet()) {
	    	LocalDateTime currentDate = entry.getKey();
	    	RandomVariable currentReturn = additionalData.get(currentDate);
	    	RandomVariable currentDispersion = entry.getValue();
			if (currentDispersion.getMin() < 0.0) {
				throw new CalculationException("Negative dispersion at " + currentDate + " on at least one path.");
			}
			RandomVariable zeroDispersion = currentDispersion.mult(-1.0).choose(new Scalar(1.0), new Scalar(0.0));
			if (currentReturn.mult(zeroDispersion).abs().getMax() > 0.0) {
				throw new CalculationException("Nonzero return with zero dispersion at " + currentDate + " on at least one path; mid-volatility scaling is undefined.");
			}
			// Replace only zero denominators, whose returns have just been verified as zero.
			RandomVariable safeDispersion = currentDispersion.add(zeroDispersion);
			RandomVariable midVolatilityScaling = currentReturn.mult(0.5).mult(latestDispersion.div(safeDispersion).add(1.0));
			midVolatilityData.put(currentDate, midVolatilityScaling);
		}
	    context.setPrimaryData(midVolatilityData);
	    context.addTransformationResult(this.getId(), context.getPrimaryData());
	}


}


