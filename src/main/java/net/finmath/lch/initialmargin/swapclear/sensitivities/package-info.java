/**
 * Local PAIRS risk of the common projection and discount curve.
 * {@link net.finmath.lch.initialmargin.swapclear.sensitivities.AadZeroRateSensitivities}
 * differentiates the existing curve valuation with respect to independent LCH
 * zero-rate bucket shocks. Deltas are RandomVariables in currency per basis point,
 * stored on the diagonal of SensitivityMatrix. Historical and simulated base
 * curves, including the initial historical closing curve, use the same algorithm.
 * Known coupons are held fixed. Simulation paths are preserved without averaging.
 *
 * <p>The ordinary model is sufficient for these local deltas: simulation ancestry
 * is detached and one reverse sweep covers the portfolio. This does not change
 * the valuation to a Monte Carlo cashflow/conditional-expectation estimator.</p>
 *
 * <p>LmmModelSensitivityProjection is a separate diagnostic. It requires an AAD
 * LMM with intermediate-node retention and computes the Jacobian from the same
 * simulated zero-rate source, including its time/tenor interpolation. Its SVD
 * projection reports ranks and residuals and must not replace full bucket risk.
 * Representing sampled quotes does not guarantee the same interpolated LCH shock
 * at every cashflow date. Compare its per-bp deltas with AadZeroRateSensitivities.
 * Use bounded simulation horizons for this optional full-model AAD calculation.</p>
 *
 * <p>The shared {@link net.finmath.lch.initialmargin.valuation.CurveValuation}
 * contract is used by AAD and full scenario revaluation. A single LchSwap can
 * be passed to either AadZeroRateSensitivities or ForwardAndDiscountSensitivities
 * and then to the same PairsInitialMargin calculation. The analytical reference
 * differentiates the complete coupon N alpha P(p) [(P(a)/P(b)-1)/Delta + spread],
 * keeping accrual, forward interval and payment dates distinct. Fixed/LIBOR
 * coupons and their linear combinations are supported; unsupported features
 * are rejected explicitly.</p>
 *
 * <p>Run {@link net.finmath.lch.initialmargin.swapclear.evaluation.SwapForwardImComparison}
 * from the repository root for an end-to-end equality and timing comparison.
 * It uses the same swap, paths, grids and PAIRS rules for both providers, warms
 * up both paths, alternates execution order and separates delta from total
 * forward-IM time. PAIRS ranking uses delta only, followed by full revaluation.
 * TODO: implement and validate gamma and cross-gamma before restoring a
 * second-order ranking approximation.</p>
 * 
 * @author Raphael Prandtl
 */
package net.finmath.lch.initialmargin.swapclear.sensitivities;
