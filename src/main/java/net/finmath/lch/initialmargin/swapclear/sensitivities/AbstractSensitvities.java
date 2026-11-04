package net.finmath.lch.initialmargin.swapclear.sensitivities;

import java.time.LocalDateTime;
import java.util.Objects;

import net.finmath.exception.CalculationException;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGrid;
import net.finmath.stochastic.RandomVariable;

/** Shared aggregation and zero-rate bucket mapping for analytical swap deltas. */
public abstract class AbstractSensitvities implements Sensitivities {

    protected final SensitivityComponentsForSwapLeg sensitivityComponentsForSwapLeg;
    protected final TenorGrid tenorGrid;
    protected static final double ZERO_RATE_SHIFT = 0.0001; // One basis point.

    public AbstractSensitvities(SensitivityComponentsForSwapLeg components, TenorGrid tenorGrid) {
        sensitivityComponentsForSwapLeg = Objects.requireNonNull(components, "sensitivityComponentsForSwapLeg");
        this.tenorGrid = Objects.requireNonNull(tenorGrid, "tenorGrid");
        double previous = 0.0;
        for (double tenor : tenorGrid.getFixings()) {
            if (!Double.isFinite(tenor) || tenor <= previous) {
                throw new IllegalArgumentException("Tenors must be finite, positive and strictly increasing.");
            }
            previous = tenor;
        }
        if (tenorGrid.getFixings().isEmpty()) {
            throw new IllegalArgumentException("The sensitivity grid must not be empty.");
        }
    }

    @Override
    public SensitivityMatrix getDeltaSensitivities(LocalDateTime evaluationDate) throws CalculationException {
        SensitivityMatrix matrix = new SensitivityMatrix();
        for (SensitivityComponent component : sensitivityComponentsForSwapLeg.getSensitivityComponents(evaluationDate)) {
            calculateDeltaSensitivity(matrix, component);
        }
        return matrix;
    }

    @Override
    public SensitivityMatrix getGammaSensitivities(LocalDateTime evaluationDate) throws CalculationException {
        // TODO: General coupon gamma must include projection/discount cross terms.
        throw new UnsupportedOperationException("Analytical common-curve gamma is not implemented; this reference supplies delta only.");
    }

    protected abstract void calculateDeltaSensitivity(SensitivityMatrix matrix, SensitivityComponent component);

    protected RandomVariable getConstantCoefficient(RandomVariable notional, double dayCountFraction, boolean isPayer) {
        return notional.mult(isPayer ? -dayCountFraction : dayCountFraction);
    }

    /** Matches linear zero-shock interpolation and constant endpoint extrapolation. */
    protected void mapDeltaToMatrix(SensitivityMatrix matrix, RandomVariable sensitivity, double maturity) {
        double lower = tenorGrid.getNearestSmallerFixing(maturity);
        double upper = tenorGrid.getNearestGreaterFixing(maturity);
        if (lower == upper) {
            matrix.addValue(lower, lower, sensitivity);
        } else {
            double upperWeight = (maturity - lower) / (upper - lower);
            matrix.addValue(lower, lower, sensitivity.mult(1.0 - upperWeight));
            matrix.addValue(upper, upper, sensitivity.mult(upperWeight));
        }
    }
}
