package net.finmath.lch.initialmargin.swapclear.sensitivities;

import net.finmath.lch.initialmargin.simulation.modeldata.TenorGrid;
import net.finmath.lch.initialmargin.swapclear.sensitivities.DiscountFactor.DiscountDate;
import net.finmath.stochastic.RandomVariable;

/** Discount contribution to the local coupon delta, holding its coupon rate fixed. */
public class DiscountSensitivities extends AbstractSensitvities {

    public DiscountSensitivities(SensitivityComponentsForSwapLeg components, TenorGrid tenorGrid) {
        super(components, tenorGrid);
    }

    @Override
    protected void calculateDeltaSensitivity(SensitivityMatrix matrix, SensitivityComponent component) {
        DiscountFactor payment = component.getDiscountFactor(DiscountDate.PAYMENT);
        RandomVariable sensitivity = getConstantCoefficient(component.getNotional(), component.getDayCountFraction(), component.isPayer())
                .mult(component.getRate()).mult(payment.getDerivative(1)).mult(ZERO_RATE_SHIFT);
        mapDeltaToMatrix(matrix, sensitivity, payment.getTimeToMaturity());
    }
}
