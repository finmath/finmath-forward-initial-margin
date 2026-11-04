package net.finmath.lch.initialmargin.swapclear.sensitivities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.finmath.exception.CalculationException;
import net.finmath.lch.initialmargin.simulation.modeldata.StochasticCurve;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGrid;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.swapclear.products.components.LchSwapLeg;
import net.finmath.lch.initialmargin.valuation.CurveValuation;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.FloatingpointDate;

/**
 * Local PAIRS deltas of the existing common-curve valuation, in currency per bp.
 * Each LCH bucket is an independent additive, continuously compounded zero-rate
 * shock. The pricing code interpolates these shocks and applies the same leaves
 * to projection and discounting. This is the full bucket risk, not a projection
 * onto the native simulation's risk factors.
 *
 * Base curves are detached from any simulation AAD graph. All paths are valued
 * together and one reverse sweep covers the portfolio and all buckets. The graph
 * lives for one call, so daily simulation history is not retained by this provider.
 * The underlying valuation is already measurable at the evaluation date; no
 * conditional-expectation regression or Monte-Carlo cashflow valuation is added.
 */
public final class AadZeroRateSensitivities implements Sensitivities {

    private static final double BASIS_POINT = 1E-4;
    private final CurveValuation valuation;
    private final List<Double> tenors;
    private final ZeroRateModel valuationModel;
    private final RandomVariableDifferentiableAADFactory factory = new RandomVariableDifferentiableAADFactory();

    public AadZeroRateSensitivities(List<LchSwapLeg> swapLegs, ZeroRateModel zeroRateModel, TenorGrid tenorGrid) {
        this(CurveValuation.sum(swapLegs), zeroRateModel, tenorGrid);
    }

    public AadZeroRateSensitivities(CurveValuation valuation, ZeroRateModel zeroRateModel, TenorGrid tenorGrid) {
        this.valuation = Objects.requireNonNull(valuation, "valuation");
        Objects.requireNonNull(zeroRateModel, "zeroRateModel");
        tenors = List.copyOf(Objects.requireNonNull(tenorGrid, "tenorGrid").getFixings());
        double previous = 0.0;
        for (double tenor : tenors) {
            if (!Double.isFinite(tenor) || tenor <= previous) {
                throw new IllegalArgumentException("Tenors must be finite, positive and strictly increasing.");
            }
            previous = tenor;
        }
        if (tenors.isEmpty()) {
            throw new IllegalArgumentException("The sensitivity grid must not be empty.");
        }
        valuationModel = zeroRateModel.withPricingSource(
                (date, tenor) -> zeroRateModel.getZeroRate(date, tenor).getValues());
    }

    @Override
    public SensitivityMatrix getDeltaSensitivities(LocalDateTime evaluationDate) throws CalculationException {
        Objects.requireNonNull(evaluationDate, "evaluationDate");
        if (!evaluationDate.equals(evaluationDate.toLocalDate().atStartOfDay())) {
            throw new IllegalArgumentException("Daily sensitivities require an evaluation date at midnight.");
        }
        double time = FloatingpointDate.getFloatingPointDateFromDate(valuationModel.getReferenceDate(), evaluationDate);
        StochasticCurve shifts = new StochasticCurve();
        List<Long> ids = new ArrayList<>(tenors.size());
        for (double tenor : tenors) {
            RandomVariableDifferentiable shift = (RandomVariableDifferentiable) factory.createRandomVariable(time, 0.0);
            ids.add(shift.getID());
            shifts.addRate(tenor, shift);
        }
        RandomVariable value = valuation.getValue(evaluationDate, valuationModel, shifts, null);
        // Empty/expired portfolios have no graph and zero risk. Do not average paths.
        Map<Long, RandomVariable> gradient = value instanceof RandomVariableDifferentiable
                ? ((RandomVariableDifferentiable) value).getGradient() : Map.of();
        SensitivityMatrix delta = new SensitivityMatrix();
        for (int index = 0; index < tenors.size(); index++) {
            RandomVariable derivative = gradient.getOrDefault(ids.get(index), new Scalar(0.0));
            delta.addValue(tenors.get(index), tenors.get(index), derivative.mult(BASIS_POINT));
        }
        return delta;
    }

    @Override
    public SensitivityMatrix getGammaSensitivities(LocalDateTime evaluationDate) {
        // TODO: Implement and validate common-curve gamma, including cross terms.
        throw new UnsupportedOperationException("AAD gamma is not implemented; this provider supplies delta only.");
    }
}
