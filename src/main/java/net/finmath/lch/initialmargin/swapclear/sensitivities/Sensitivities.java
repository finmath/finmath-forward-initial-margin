package net.finmath.lch.initialmargin.swapclear.sensitivities;


import java.time.LocalDateTime;

import net.finmath.exception.CalculationException;

/**
 * Sensitivities of the common PAIRS curve on an explicit tenor grid.
 */
public interface Sensitivities {
	
	/**
	 * Calculates pathwise zero-rate deltas in currency per basis point, mapped to
	 * a fixed grid of remaining tenors. Projection and discount contributions add.
	 * @param evaluationDate The date of the evaluation
	 * @return Delta sensitivities on the diagonal of the matrix
	 * @throws CalculationException
	 */
	public SensitivityMatrix getDeltaSensitivities(LocalDateTime evaluationDate) throws CalculationException;

	
	/**
	 * Calculates the gamma sensitivities mapped to a fixed grid of standard tenors
	 * @param evaluationDate The date of the evaluation
	 * @return Gamma sensitivities in the provider's matrix convention
	 * @throws CalculationException
	 * @throws UnsupportedOperationException If the provider supplies delta only
	 */
	public SensitivityMatrix getGammaSensitivities(LocalDateTime evaluationDate) throws CalculationException;
			

}
