package net.finmath.lch.initialmargin.swapclear.sensitivities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import net.finmath.exception.CalculationException;
import net.finmath.lch.initialmargin.simulation.modeldata.SimulatedZeroRateSource;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGrid;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.swapclear.products.components.LchSwapLeg;
import net.finmath.lch.initialmargin.valuation.CurveValuation;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAAD;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.FloatingpointDate;

/**
 * Diagnostic projection of local PAIRS value derivatives in an AAD LMM onto
 * rolling zero-rate quotes. This is not the full independent LCH bucket risk.
 * For row gradients, J = dz/dL and the reported decimal-rate gradient is gL J+.
 * A requested zero shock h is represented only as J J+ h. The Moore-Penrose
 * inverse uses unweighted Euclidean norms in decimal zero/forward-rate units.
 *
 * J is differentiated from the actual SimulatedZeroRateSource. Its tenor
 * interpolation and moving observation origin therefore already include the
 * grid adjustment; no approximate SIMM interpolation matrix is inserted.
 * Only current native nodes with fixing time >= t are coordinates. An old stub
 * fixing is an ancestor of current nodes and must not also be treated as an
 * independent coordinate. Consequently short-end shocks may be unrepresentable.
 * A reset at t remains a curve coordinate, while known product coupons are fixed.
 *
 * The valuation is the local common-curve PAIRS valuation, with no Monte Carlo
 * conditional-expectation or numeraire valuation. Gamma is not provided.
 * This optional diagnostic performs one full AAD reverse sweep per target quote
 * plus one for the product value. Use bounded simulation horizons; the production
 * AadZeroRateSensitivities provider uses only one small local reverse sweep.
 */
public final class LmmModelSensitivityProjection {

    private static final double TIME_TOLERANCE = 1E-10;
    private final CurveValuation valuation;
    private final ZeroRateModel original;
    private final SimulatedZeroRateSource source;
    private final LIBORModelMonteCarloSimulationModel simulation;
    private final List<Double> tenors;

    public LmmModelSensitivityProjection(List<LchSwapLeg> legs, ZeroRateModel original,
            SimulatedZeroRateSource source, TenorGrid tenorGrid) {
        this(CurveValuation.sum(legs), original, source, tenorGrid);
    }

    public LmmModelSensitivityProjection(CurveValuation valuation, ZeroRateModel original,
            SimulatedZeroRateSource source, TenorGrid tenorGrid) {
        this.valuation = Objects.requireNonNull(valuation, "valuation");
        this.original = Objects.requireNonNull(original, "original");
        this.source = Objects.requireNonNull(source, "source");
        if (!(source.getSimulation() instanceof LIBORModelMonteCarloSimulationModel)
                || !(source.getSimulation().getModel() instanceof LIBORMarketModel)) {
            throw new IllegalArgumentException("Native LIBOR projection requires a LIBOR market model (LMM).");
        }
        simulation = (LIBORModelMonteCarloSimulationModel) source.getSimulation();
        if (!original.getReferenceDate().toLocalDate().equals(source.getReferenceDate())
                || original.getNumberOfPaths() != source.getNumberOfPaths()) {
            throw new IllegalArgumentException("Pricing and simulation reference dates and path counts must agree.");
        }
        tenors = List.copyOf(Objects.requireNonNull(tenorGrid, "tenorGrid").getFixings());
        double previous = 0.0;
        for (double tenor : tenors) {
            if (!Double.isFinite(tenor) || tenor <= previous) {
                throw new IllegalArgumentException("Target tenors must be finite, positive and strictly increasing.");
            }
            previous = tenor;
        }
        if (tenors.isEmpty()) {
            throw new IllegalArgumentException("At least one target tenor is required.");
        }
    }

    public Result calculate(LocalDateTime evaluationDate) throws CalculationException {
        Objects.requireNonNull(evaluationDate, "evaluationDate");
        if (!evaluationDate.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            throw new IllegalArgumentException("Evaluation date must be at midnight.");
        }
        LocalDate date = evaluationDate.toLocalDate();
        // Validate coverage of the complete requested target grid before valuation.
        RandomVariable[] zeros = new RandomVariable[tenors.size()];
        for (int j = 0; j < zeros.length; j++) {
            zeros[j] = alignedZero(date, tenors.get(j));
        }
        double dateTime = FloatingpointDate.getFloatingPointDateFromDate(source.getReferenceDate(), date);
        int timeIndex = simulation.getTimeDiscretization().getTimeIndexNearestLessOrEqual(dateTime + TIME_TOLERANCE);
        if (timeIndex < 0 || Math.abs(simulation.getTime(timeIndex) - dateTime) > TIME_TOLERANCE) {
            throw new IllegalArgumentException("Evaluation date must be on the simulation time grid.");
        }
        double time = simulation.getTime(timeIndex);
        Map<Long, Integer> indices = new TreeMap<>();
        for (int i = 0; i < simulation.getNumberOfLibors(); i++) {
            if (simulation.getLiborPeriod(i) < time - TIME_TOLERANCE) {
                continue;
            }
            RandomVariable value = simulation.getLIBOR(timeIndex, i);
            if (!(value instanceof RandomVariableDifferentiableAAD)) {
                throw new IllegalArgumentException("Native LIBOR projection requires backward AAD random variables.");
            }
            RandomVariableDifferentiableAAD node = (RandomVariableDifferentiableAAD) value;
            if (node.getFactory() != null && node.getFactory().isGradientRetainsLeafNodesOnly()) {
                throw new IllegalArgumentException("AAD must retain intermediate nodes: isGradientRetainsLeafNodesOnly=false.");
            }
            if (indices.put(node.getID(), i) != null) {
                throw new IllegalArgumentException("Native LIBOR coordinates must have distinct AAD IDs.");
            }
        }
        List<Long> ids = new ArrayList<>(indices.keySet());
        RandomVariable[][] jacobian = new RandomVariable[tenors.size()][ids.size()];
        for (int j = 0; j < zeros.length; j++) {
            jacobian[j] = gradient(zeros[j], ids);
        }
        ZeroRateModel pricing = original.withPricingSource((observation, tenor) -> {
            if (observation.equals(date)) {
                return alignedZero(observation, tenor);
            }
            if (observation.isBefore(date)) {
                return original.getZeroRate(observation, tenor).getValues();
            }
            throw new IllegalArgumentException("Local valuation cannot request future observed curves.");
        });
        RandomVariable value = valuation.getValue(evaluationDate, pricing, null, null);
        RandomVariable[] nativeGradient = gradient(value, ids);
        return project(time, value.getValues(), indices, nativeGradient, jacobian);
    }

    private RandomVariable alignedZero(LocalDate date, double tenor) throws CalculationException {
        RandomVariable value = source.getZeroRate(date, tenor);
        if (value.sub(original.getZeroRate(date, tenor)).abs().getMax()
                > ZeroRateModel.DEFAULT_INITIAL_CURVE_TOLERANCE) {
            throw new CalculationException("Diagnostic simulation and pricing curve differ at " + date + ", tenor " + tenor + ".");
        }
        return value;
    }

    private RandomVariable[] gradient(RandomVariable value, List<Long> ids) {
        // finmath 6.0.19's getGradient(Set) removes the supplied IDs. Select
        // explicitly from getGradient(), in exactly the same order for J and gL.
        Map<Long, RandomVariable> derivatives = value instanceof RandomVariableDifferentiable
                ? ((RandomVariableDifferentiable) value).getGradient() : Collections.emptyMap();
        RandomVariable[] result = new RandomVariable[ids.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = derivatives.getOrDefault(ids.get(i), new Scalar(0.0));
            if (!result[i].isDeterministic() && result[i].size() != source.getNumberOfPaths()) {
                throw new IllegalArgumentException("AAD derivative has an incompatible path count.");
            }
        }
        return result;
    }

    private Result project(double time, RandomVariable value, Map<Long, Integer> indices,
            RandomVariable[] nativeGradient, RandomVariable[][] jacobian) {
        int paths = source.getNumberOfPaths();
        int targets = tenors.size();
        int natives = nativeGradient.length;
        double[][] deltas = new double[targets][paths];
        double[][] residuals = new double[natives][paths];
        double[][] shockResiduals = new double[targets][paths];
        double[] relativeResiduals = new double[paths];
        int[] ranks = new int[paths];
        for (int path = 0; path < paths; path++) {
            if (natives == 0) {
                for (int j = 0; j < targets; j++) {
                    shockResiduals[j][path] = 1.0;
                }
                continue;
            }
            double[][] entries = new double[targets][natives];
            double[] g = new double[natives];
            for (int i = 0; i < natives; i++) {
                g[i] = finite(nativeGradient[i].get(path));
                for (int j = 0; j < targets; j++) {
                    entries[j][i] = finite(jacobian[j][i].get(path));
                }
            }
            RealMatrix j = MatrixUtils.createRealMatrix(entries);
            SingularValueDecomposition svd = new SingularValueDecomposition(j);
            RealMatrix inverse = svd.getSolver().getInverse();
            ranks[path] = svd.getRank();
            double[] projected = inverse.preMultiply(g);
            double[] reconstructed = j.preMultiply(projected);
            double nativeNorm = 0.0;
            double residualNorm = 0.0;
            for (int i = 0; i < natives; i++) {
                residuals[i][path] = finite(g[i] - reconstructed[i]);
                nativeNorm = Math.hypot(nativeNorm, g[i]);
                residualNorm = Math.hypot(residualNorm, residuals[i][path]);
            }
            relativeResiduals[path] = nativeNorm == 0.0 ? 0.0 : residualNorm / nativeNorm;
            RealMatrix represented = j.multiply(inverse);
            for (int target = 0; target < targets; target++) {
                deltas[target][path] = finite(projected[target] * 1E-4);
                double norm = 0.0;
                for (int row = 0; row < targets; row++) {
                    norm = Math.hypot(norm, (row == target ? 1.0 : 0.0) - represented.getEntry(row, target));
                }
                shockResiduals[target][path] = finite(norm);
            }
        }
        Map<Long, RandomVariable> nativeValues = new LinkedHashMap<>();
        Map<Long, RandomVariable> nativeResiduals = new LinkedHashMap<>();
        int i = 0;
        for (long id : indices.keySet()) {
            nativeValues.put(id, nativeGradient[i].getValues());
            nativeResiduals.put(id, new RandomVariableFromDoubleArray(time, residuals[i++]));
        }
        return new Result(value, indices, nativeValues, nativeResiduals, keyed(time, deltas),
                keyed(time, shockResiduals), ranks, new RandomVariableFromDoubleArray(time, relativeResiduals));
    }

    private static double finite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Projection requires finite gradients and matrix entries.");
        }
        return value;
    }

    private Map<Double, RandomVariable> keyed(double time, double[][] values) {
        Map<Double, RandomVariable> result = new LinkedHashMap<>();
        for (int j = 0; j < tenors.size(); j++) {
            result.put(tenors.get(j), new RandomVariableFromDoubleArray(time, values[j]));
        }
        return result;
    }

    /** Immutable diagnostic output; it is deliberately not a Sensitivities provider. */
    public static final class Result {
        private final RandomVariable value;
        private final Map<Long, Integer> nativeLiborIndices;
        private final Map<Long, RandomVariable> nativeGradient;
        private final Map<Long, RandomVariable> nativeGradientResidual;
        private final Map<Double, RandomVariable> projectedDeltas;
        private final Map<Double, RandomVariable> unrepresentedShockNorms;
        private final int[] ranks;
        private final RandomVariable relativeNativeGradientResidual;

        private Result(RandomVariable value, Map<Long, Integer> indices, Map<Long, RandomVariable> nativeGradient,
                Map<Long, RandomVariable> nativeResidual, Map<Double, RandomVariable> deltas,
                Map<Double, RandomVariable> shockResiduals, int[] ranks, RandomVariable relativeResidual) {
            this.value = value;
            nativeLiborIndices = Collections.unmodifiableMap(new LinkedHashMap<>(indices));
            this.nativeGradient = Collections.unmodifiableMap(nativeGradient);
            nativeGradientResidual = Collections.unmodifiableMap(nativeResidual);
            projectedDeltas = Collections.unmodifiableMap(deltas);
            unrepresentedShockNorms = Collections.unmodifiableMap(shockResiduals);
            this.ranks = ranks.clone();
            relativeNativeGradientResidual = relativeResidual;
        }

        public RandomVariable getValue() { return value; }
        public Map<Long, Integer> getNativeLiborIndices() { return nativeLiborIndices; }
        /** dV/dL in decimal forward-rate units, ordered by AAD ID. */
        public Map<Long, RandomVariable> getNativeGradient() { return nativeGradient; }
        /** gL - (gL J+) J, in decimal forward-rate units. */
        public Map<Long, RandomVariable> getNativeGradientResidual() { return nativeGradientResidual; }
        /** Projected zero-rate deltas per basis point, keyed by remaining tenor. */
        public Map<Double, RandomVariable> getProjectedDeltas() { return projectedDeltas; }
        /**
         * Norm of (I - J J+) e_j. Zero means the requested target-quote shock is
         * representable. It does not guarantee the same interpolated LCH shock
         * at cashflow maturities between the target quotes.
         */
        public Map<Double, RandomVariable> getUnrepresentedShockNorms() { return unrepresentedShockNorms; }
        /** Numerical SVD rank for each simulation path. */
        public int[] getRanks() { return ranks.clone(); }
        /** ||gL - (gL J+) J|| / ||gL||; zero when gL is identically zero. */
        public RandomVariable getRelativeNativeGradientResidual() { return relativeNativeGradientResidual; }
    }
}
