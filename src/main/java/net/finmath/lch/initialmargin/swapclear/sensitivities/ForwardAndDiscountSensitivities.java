package net.finmath.lch.initialmargin.swapclear.sensitivities;

import java.util.List;
import java.util.Objects;

import net.finmath.lch.initialmargin.simulation.modeldata.TenorGrid;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.swapclear.products.LchSwap;


/**
 * Analytical local PAIRS delta: projection and discount contributions mapped to
 * the same zero-rate buckets. Gamma is deliberately unsupported.
 */
public class ForwardAndDiscountSensitivities extends AbstractSensitvities {

    private ForwardSensitivities forwardSensitivities;
    private DiscountSensitivities discountSensitivities;

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
}
