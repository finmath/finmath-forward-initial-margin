package net.finmath.lch.initialmargin.valuation;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import net.finmath.exception.CalculationException;
import net.finmath.lch.initialmargin.simulation.modeldata.StochasticCurve;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * Value of a product or a netting set under the common PAIRS curve.
 *
 * <p>The result is the value conditional on information at the evaluation date,
 * in one agreed currency, with the model's path ordering. A null path selects
 * all paths without averaging; a non-null path returns just that realization
 * as a scalar or one-element random variable (read at index zero).
 * Shifts are additive continuously compounded zero rates in decimal units,
 * interpolated by {@link StochasticCurve}, and affect current projection and
 * discounting together. A null shift means the unshocked curve. Known fixings
 * are not shocked; payments on or before the evaluation date are excluded.</p>
 *
 * <p>Implementations used by local AAD must preserve RandomVariable operations
 * from curve shifts to value. This contract alone does not turn a Monte Carlo
 * future cashflow into a conditional value or define its forward derivative.
 * A future model-based implementation must explicitly supply consistent
 * conditioning and model reconstruction under the same curve shocks.</p>
 */
@FunctionalInterface
public interface CurveValuation {

    RandomVariable getValue(LocalDateTime evaluationDate, ZeroRateModel model,
            StochasticCurve shifts, Integer pathOrState) throws CalculationException;

    /**
     * Sum valuations sharing a currency, curve convention and path ordering.
     * Netting-set membership is supplied by the caller, not inferred here.
     */
    static CurveValuation sum(Collection<? extends CurveValuation> valuations) {
        List<CurveValuation> snapshot = List.copyOf(valuations);
        return (date, model, shifts, path) -> {
            RandomVariable value = new Scalar(0.0);
            for (CurveValuation valuation : snapshot) {
                value = value.add(valuation.getValue(date, model, shifts, path));
            }
            return value;
        };
    }
}
