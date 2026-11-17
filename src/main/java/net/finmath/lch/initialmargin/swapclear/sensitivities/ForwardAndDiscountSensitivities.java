package net.finmath.lch.initialmargin.swapclear.sensitivities;

import java.util.List;
import java.util.Objects;

import net.finmath.lch.initialmargin.simulation.modeldata.TenorGrid;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.swapclear.products.LchSwap;
import net.finmath.lch.initialmargin.swapclear.sensitivities.DiscountFactor.DiscountDate;
import net.finmath.stochastic.RandomVariable;


/**
 * Analytical local PAIRS delta and gamma on the common zero-rate curve.
 * Gamma includes projection, discount and both mixed derivative orientations.
 */
public class ForwardAndDiscountSensitivities extends AbstractSensitvities {

    private final ForwardSensitivities forwardSensitivities;
    private final DiscountSensitivities discountSensitivities;

    public ForwardAndDiscountSensitivities(LchSwap swap, ZeroRateModel model, TenorGrid tenorGrid) {
        this(new SensitivityComponentsForSwapLeg(
                Objects.requireNonNull(swap, "swap").getLegPayer() == null
                        ? List.of(swap.getLegReceiver()) : List.of(swap.getLegReceiver(), swap.getLegPayer()),
                model, model), tenorGrid);
    }
    
    public ForwardAndDiscountSensitivities(SensitivityComponentsForSwapLeg sensitivityComponentsForSwapLeg, TenorGrid tenorGrid) {
        super(sensitivityComponentsForSwapLeg, tenorGrid);
        forwardSensitivities = new ForwardSensitivities(sensitivityComponentsForSwapLeg, tenorGrid);
        discountSensitivities = new DiscountSensitivities(sensitivityComponentsForSwapLeg, tenorGrid);
    }

    
    @Override
    protected void calculateDeltaSensitivity(SensitivityMatrix sensitivityMatrix, SensitivityComponent component) {
        forwardSensitivities.calculateDeltaSensitivity(sensitivityMatrix, component);
        discountSensitivities.calculateDeltaSensitivity(sensitivityMatrix, component);
    }

    @Override
    protected void calculateGammaSensitivity(SensitivityMatrix matrix, SensitivityComponent component) {
        forwardSensitivities.calculateGammaSensitivity(matrix, component);
        discountSensitivities.calculateGammaSensitivity(matrix, component);
        DiscountFactor start = component.getDiscountFactor(DiscountDate.PERIOD_START);
        if (start == null) {
            return;
        }
        DiscountFactor end = component.getDiscountFactor(DiscountDate.PERIOD_END);
        DiscountFactor payment = component.getDiscountFactor(DiscountDate.PAYMENT);
        double a = start.getTimeToMaturity();
        double b = end.getTimeToMaturity();
        double p = payment.getTimeToMaturity();
        RandomVariable factor = getConstantCoefficient(component.getNotional(), component.getDayCountFraction(), component.isPayer())
                .mult(payment.getDiscountFactor()).mult(start.getDiscountFactor())
                .div(end.getDiscountFactor()).mult(ZERO_RATE_SHIFT * ZERO_RATE_SHIFT * p / (b - a));
        // Product rule for P(p)*F: dP/dz(p)=-p*P and dF/dz(a), dF/dz(b).
        RandomVariable startMixed = factor.mult(a);
        RandomVariable endMixed = factor.mult(-b);
        mapGammaToMatrix(matrix, startMixed, a, p);
        mapGammaToMatrix(matrix, startMixed, p, a);
        mapGammaToMatrix(matrix, endMixed, b, p);
        mapGammaToMatrix(matrix, endMixed, p, b);
    }
}
