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
	 * Calculates the full symmetric zero-rate Hessian in currency per squared
	 * basis point, on the same remaining-tenor grid as delta. Mixed derivatives
	 * are stored in both orientations; no Taylor factor 1/2 is included.
	 * @param evaluationDate The date of the evaluation
	 * @return Gamma sensitivities, including off-diagonal mixed derivatives
	 * @throws CalculationException
	 */
	public SensitivityMatrix getGammaSensitivities(LocalDateTime evaluationDate) throws CalculationException;
			

}
