package net.finmath.lch.initialmargin.swapclear.sensitivities;

import java.util.TreeMap;

import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * A RandomVariable matrix to store the delta and gamma sensitivities.
 * The sensitivities are sampled only on fixed tenor grid points, which represent the keys of the two maps.
 * Delta sensitivities are only stored on the diagonal of the matrix, where both keys are equal.
 * Gamma is the full symmetric product-value Hessian in currency per squared
 * basis point, including both orientations of mixed derivatives. Its entries
 * do not include the Taylor factor 1/2. Delta is in currency per basis point.
 * The class is solely used for storing calculated sensitivities and returning them for the Taylor Expansion -> no path dependence or interpolation method needed
 * 
 * @author Raphael Prandtl
 *
 */
public class SensitivityMatrix {

	private TreeMap<Double, TreeMap<Double, RandomVariable>> matrix = new TreeMap<>();

	
	public SensitivityMatrix() {
	}	
	
	
	/**
	 * @param maturityFirstOrder The maturity of the first derivative
	 * @param maturitySecondOrder The maturity of the second derivative
	 * @param value The value / rate that will be added for the pair of maturities
	 */
	public void addValue(double maturityFirstOrder, double maturitySecondOrder, RandomVariable value) {
		TreeMap<Double, RandomVariable> secondOrderTerms = matrix.computeIfAbsent(maturityFirstOrder, k -> new TreeMap<Double, RandomVariable>());
		RandomVariable currentSensitivity = secondOrderTerms.getOrDefault(maturitySecondOrder, new Scalar(0.0));
		secondOrderTerms.put(maturitySecondOrder, currentSensitivity.add(value));
		matrix.put(maturityFirstOrder, secondOrderTerms);
	}

	
	public TreeMap<Double, TreeMap<Double, RandomVariable>> getSensitivityMatrix() {
		return matrix;
	}

}
