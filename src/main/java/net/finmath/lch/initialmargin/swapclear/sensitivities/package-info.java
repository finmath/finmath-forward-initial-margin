/**
 * Pathwise analytical PAIRS sensitivities of the common projection and discount curve.
 * Deltas are in currency per basis point, on the diagonal of
 * {@link net.finmath.lch.initialmargin.swapclear.sensitivities.SensitivityMatrix}.
 * Gamma is the full symmetric Hessian in currency per squared basis point,
 * including both orientations of every mixed derivative. Known fixings are
 * held fixed and simulation paths are not averaged.
 *
 * <p>{@link net.finmath.lch.initialmargin.swapclear.sensitivities.ForwardAndDiscountSensitivities}
 * differentiates the complete swap coupon
 * N alpha P(p) [(P(a)/P(b)-1)/Delta + spread], keeping accrual, forward interval
 * and payment dates distinct. Fixed/LIBOR coupons and their linear combinations
 * are supported; unsupported features are rejected explicitly.</p>
 *
 * <p>Forward and discount partial derivatives are computed separately, and
 * their mixed derivatives are added for common-curve gamma. Linear zero-shock
 * interpolation maps local deltas with W and Hessians with W-transpose H W;
 * endpoint extrapolation holds the zero-rate shock constant.</p>
 *
 * <p>For shocks h expressed in basis points, the Taylor P&amp;L is
 * delta-transpose h + 1/2 h-transpose gamma h. The factor 1/2 belongs to the
 * Taylor expansion, not to the stored Hessian. Payments on or before the
 * evaluation date have no remaining sensitivity.</p>
 * 
 * @author Raphael Prandtl
 */
package net.finmath.lch.initialmargin.swapclear.sensitivities;
