package net.finmath.lch.initialmargin.simulation.ratetransformations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

import net.finmath.exception.CalculationException;
import net.finmath.stochastic.RandomVariable;



/**
 * Calculates pathwise absolute or relative returns over a lag of observations.
 * Each return is labeled by its end date, when both observations are available.
 * A lag of five represents five business days only if the input contains every
 * relevant business-day observation in chronological order.
 * 
 * @author Raphael Prandtl
 */
public class RateTransformationReturns extends AbstractRateTransformation {
	
    public enum ReturnType {
        ABSOLUTE,
        RELATIVE
    }
	
	private final int 					marginPeriodOfRisk;
	private final ReturnType 			returnType;
	
	
	/**
	 * Performs a return transformation.
	 * The length (number of entries) of the initial data set is reduced by the return lag.
	 * 
	 * @param marginPeriodOfRisk Positive lag in observations, not calendar days.
	 * @param returnType Enum to calculate either absolute or relative returns w.r.t the lag.
	 */
	public RateTransformationReturns(final int marginPeriodOfRisk, final ReturnType returnType) {
		super("Returns"); 		// unique TransformationMethod ID
		if (marginPeriodOfRisk <= 0) {
			throw new IllegalArgumentException("marginPeriodOfRisk must be strictly positive.");
		}
		this.marginPeriodOfRisk = marginPeriodOfRisk;
		this.returnType = Objects.requireNonNull(returnType, "returnType must not be null.");
	}
	
	@Override
	public void applyTransformation(TransformationContext context) throws CalculationException {
		NavigableMap<LocalDateTime, RandomVariable> primaryData = context.getPrimaryData();
		if (primaryData.size() <= marginPeriodOfRisk) {
			throw new CalculationException("Not enough data points to apply return transformation. Reduce lag or increase data series.");
		}
		NavigableMap<LocalDateTime, RandomVariable> returnData = new TreeMap<>();
		List<LocalDateTime> dates = new ArrayList<>(primaryData.keySet());
		for (int i = marginPeriodOfRisk; i < dates.size(); i++) {
			LocalDateTime startDate = dates.get(i - marginPeriodOfRisk);
			LocalDateTime endDate = dates.get(i);
			RandomVariable startRate = primaryData.get(startDate);
			RandomVariable endRate = primaryData.get(endDate);
			
			switch (returnType) {
			case ABSOLUTE:
				RandomVariable resultAbsolute = endRate.sub(startRate);
				returnData.put(endDate, resultAbsolute);
				break;
			case RELATIVE:
				if (startRate.abs().getMin() == 0.0) {
					throw new CalculationException("Relative return ending at " + endDate + " has a zero starting rate on at least one path (" + startDate + ").");
				}
				RandomVariable resultRelative = endRate.div(startRate).sub(1.0);
				returnData.put(endDate, resultRelative);
				break;
			}
		}
	    context.setPrimaryData(returnData);
	    context.addTransformationResult(this.getId(), context.getPrimaryData());
	}	
	
}
