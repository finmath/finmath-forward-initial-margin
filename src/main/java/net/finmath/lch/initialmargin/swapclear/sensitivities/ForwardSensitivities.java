package net.finmath.lch.initialmargin.swapclear.sensitivities;

import net.finmath.lch.initialmargin.simulation.modeldata.TenorGrid;
import net.finmath.lch.initialmargin.swapclear.sensitivities.DiscountFactor.DiscountDate;
import net.finmath.stochastic.RandomVariable;

/** Projection derivatives of the local coupon, holding its payment discount factor fixed. */
public class ForwardSensitivities extends AbstractSensitvities {

    public ForwardSensitivities(SensitivityComponentsForSwapLeg components, TenorGrid tenorGrid) {
        super(components, tenorGrid);
    }

    @Override
    protected void calculateDeltaSensitivity(SensitivityMatrix matrix, SensitivityComponent component) {
        DiscountFactor start = component.getDiscountFactor(DiscountDate.PERIOD_START);
        if (start == null) {
            return;
        }
        DiscountFactor end = component.getDiscountFactor(DiscountDate.PERIOD_END);
        DiscountFactor payment = component.getDiscountFactor(DiscountDate.PAYMENT);
        double length = end.getTimeToMaturity() - start.getTimeToMaturity();
        // dF/dz(a) = -a P(a)/(Delta P(b)); dF/dz(b) = b P(a)/(Delta P(b)).
        RandomVariable factor = getConstantCoefficient(component.getNotional(), component.getDayCountFraction(), component.isPayer())
                .mult(payment.getDiscountFactor()).mult(start.getDiscountFactor())
                .div(end.getDiscountFactor()).mult(ZERO_RATE_SHIFT / length);
        mapDeltaToMatrix(matrix, factor.mult(-start.getTimeToMaturity()), start.getTimeToMaturity());
        mapDeltaToMatrix(matrix, factor.mult(end.getTimeToMaturity()), end.getTimeToMaturity());
    }

    @Override
    protected void calculateGammaSensitivity(SensitivityMatrix matrix, SensitivityComponent component) {
        DiscountFactor start = component.getDiscountFactor(DiscountDate.PERIOD_START);
        if (start == null) {
            return;
        }
        DiscountFactor end = component.getDiscountFactor(DiscountDate.PERIOD_END);
        DiscountFactor payment = component.getDiscountFactor(DiscountDate.PAYMENT);
        double a = start.getTimeToMaturity();
        double b = end.getTimeToMaturity();
        RandomVariable factor = getConstantCoefficient(component.getNotional(), component.getDayCountFraction(), component.isPayer())
                .mult(payment.getDiscountFactor()).mult(start.getDiscountFactor())
                .div(end.getDiscountFactor()).mult(ZERO_RATE_SHIFT * ZERO_RATE_SHIFT / (b - a));
        // The forward ratio has exponent -a*z(a) + b*z(b).
        mapGammaToMatrix(matrix, factor.mult(a * a), a, a);
        mapGammaToMatrix(matrix, factor.mult(b * b), b, b);
        RandomVariable mixed = factor.mult(-a * b);
        mapGammaToMatrix(matrix, mixed, a, b);
        mapGammaToMatrix(matrix, mixed, b, a);
    }
}
