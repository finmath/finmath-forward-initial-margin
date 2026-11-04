package net.finmath.lch.initialmargin.swapclear.evaluation;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGrid;
import net.finmath.lch.initialmargin.simulation.modeldata.TenorGridFactory;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.PairsInitialMargin;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.Currency;
import net.finmath.lch.initialmargin.swapclear.aggregationscheme.SchemeStorage.CurveName;
import net.finmath.lch.initialmargin.swapclear.libormodels.ModelFactory;
import net.finmath.lch.initialmargin.swapclear.products.LchSwap;
import net.finmath.lch.initialmargin.swapclear.sensitivities.AadZeroRateSensitivities;
import net.finmath.lch.initialmargin.swapclear.sensitivities.ForwardAndDiscountSensitivities;
import net.finmath.lch.initialmargin.swapclear.sensitivities.Sensitivities;
import net.finmath.lch.initialmargin.swapclear.sensitivities.SensitivityMatrix;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * Reproducible end-to-end comparison for one and the same 10Y payer swap.
 * Run from the repository root (IntelliJ main class); optional arguments are
 * measured repetitions, warmup repetitions, and output CSV path.
 *
 * <p>Both providers use the same curve valuation, simulation paths, scenario
 * rules and full revaluation. A pass computes pathwise PAIRS = max(base, floor)
 * at three dates, including scenario construction, delta ranking and the 20
 * full revaluations per path. Simulation/input loading and provider construction
 * are outside the timing.
 * Warmups initialize lazy simulation/curve data. Providers are fresh per pass;
 * measured execution order alternates to reduce ordering effects.</p>
 *
 * <p>These are application timings, not a JVM microbenchmark or a performance
 * assertion. Separate delta timings distinguish derivative cost from the
 * shared scenario and revaluation work. No gamma or model-CE pricing is used.</p>
 */
public final class SwapForwardImComparison {

    private SwapForwardImComparison() { }

    public static void main(String[] args) throws Exception {
        int repetitions = args.length > 0 ? Integer.parseInt(args[0]) : 3;
        int warmups = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        if (repetitions < 1 || warmups < 1 || args.length > 3) {
            throw new IllegalArgumentException("Use: measuredRepetitions>=1 warmupRepetitions>=1 [output.csv]");
        }
        Path output = Path.of(args.length > 2 ? args[2] : "target/swap-forward-im-comparison.csv");
        if (output.toAbsolutePath().getParent() != null) {
            Files.createDirectories(output.toAbsolutePath().getParent());
        }
        LocalDateTime reference = ModelFactory.REFERENCE_DATE;
        // All three dates are weekdays in the historical/model calendar.
        List<LocalDateTime> dates = List.of(reference, reference.plusDays(30), reference.plusDays(91));
        TenorGrid grid = TenorGridFactory.getTenorGrid(TenorGridFactory.GridType.INITIAL_MARGIN_RISK_GRID);
        var simulation = ModelFactory.getModel(CurveName.EUR_EURIBOR_3M,
                dates.get(dates.size() - 1).toLocalDate(), 60.0);
        ZeroRateModel model = new ZeroRateModel(Currency.EUR, CurveName.EUR_EURIBOR_3M,
                simulation, "spreadsheets/EUR_EURIBOR_3M_Input_Data.xlsx");
        LchSwap swap = PortfolioFactory.create10YPayerSwap();
        System.out.printf(Locale.ROOT, "10Y payer, EUR10000, paths=%d, buckets=%d, dates=%s, warmups=%d, repetitions=%d%n",
                model.getNumberOfPaths(), grid.getFixings().size(), dates, warmups, repetitions);

        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(output))) {
            csv.println("moving_window,repetition,method,total_ms,delta_ms,mean_im_day0,mean_im_day30,mean_im_day91,max_delta_difference_eur_per_bp,max_im_difference_eur");
            for (boolean moving : new boolean[] {false, true}) {
                for (int warmup = 0; warmup < warmups; warmup++) {
                    compare(run(true, swap, model, grid, dates, moving),
                            run(false, swap, model, grid, dates, moving), grid, dates, model.getNumberOfPaths());
                }
                double[][] totalMillis = new double[2][repetitions];
                double[][] deltaMillis = new double[2][repetitions];
                for (int repetition = 0; repetition < repetitions; repetition++) {
                    boolean aadFirst = repetition % 2 == 0;
                    Run first = run(aadFirst, swap, model, grid, dates, moving);
                    Run second = run(!aadFirst, swap, model, grid, dates, moving);
                    Run aad = aadFirst ? first : second;
                    Run analytic = aadFirst ? second : first;
                    double[] differences = compare(aad, analytic, grid, dates, model.getNumberOfPaths());
                    Run[] results = {aad, analytic};
                    for (int method = 0; method < results.length; method++) {
                        Run result = results[method];
                        totalMillis[method][repetition] = result.totalNanos / 1E6;
                        deltaMillis[method][repetition] = result.deltaNanos / 1E6;
                        csv.printf(Locale.ROOT, "%s,%d,%s,%.6f,%.6f,%.12g,%.12g,%.12g,%.12g,%.12g%n",
                                moving, repetition + 1, method == 0 ? "AAD" : "ANALYTIC",
                                totalMillis[method][repetition], deltaMillis[method][repetition],
                                result.margins.get(dates.get(0)).getAverage(), result.margins.get(dates.get(1)).getAverage(),
                                result.margins.get(dates.get(2)).getAverage(), differences[0], differences[1]);
                    }
                    csv.flush();
                    System.out.printf(Locale.ROOT,
                            "moving=%s run=%d: total_ms AAD=%.3f analytic=%.3f; max_delta_error=%.3g max_IM_error=%.3g%n",
                            moving, repetition + 1, totalMillis[0][repetition], totalMillis[1][repetition],
                            differences[0], differences[1]);
                }
                System.out.printf(Locale.ROOT,
                        "moving=%s median per three-date pass: total_ms AAD=%.3f analytic=%.3f; delta_ms AAD=%.3f analytic=%.3f%n",
                        moving, median(totalMillis[0]), median(totalMillis[1]), median(deltaMillis[0]), median(deltaMillis[1]));
            }
            if (csv.checkError()) {
                throw new java.io.IOException("Could not write comparison results to " + output);
            }
        }
        System.out.println("Results: " + output.toAbsolutePath());
    }

    private static Run run(boolean aad, LchSwap swap, ZeroRateModel model, TenorGrid grid,
            List<LocalDateTime> dates, boolean moving) throws CalculationException {
        Sensitivities provider = aad ? new AadZeroRateSensitivities(swap, model, grid)
                : new ForwardAndDiscountSensitivities(swap, model, grid);
        TimedSensitivities measured = new TimedSensitivities(provider);
        Map<LocalDateTime, RandomVariable> margins = new LinkedHashMap<>();
        long start = System.nanoTime();
        for (LocalDateTime date : dates) {
            margins.put(date, PairsInitialMargin.getPairsInitialMargin(date, model, swap,
                    true, moving, measured, grid));
        }
        return new Run(System.nanoTime() - start, measured.nanos, measured.deltas, margins);
    }

    private static double[] compare(Run aad, Run analytic, TenorGrid grid,
            List<LocalDateTime> dates, int paths) {
        double deltaError = 0.0;
        double marginError = 0.0;
        for (LocalDateTime date : dates) {
            for (int path = 0; path < paths; path++) {
                marginError = Math.max(marginError, Math.abs(aad.margins.get(date).get(path) - analytic.margins.get(date).get(path)));
                for (double tenor : grid.getFixings()) {
                    deltaError = Math.max(deltaError, Math.abs(delta(aad.deltas.get(date), tenor).get(path)
                            - delta(analytic.deltas.get(date), tenor).get(path)));
                }
            }
        }
        if (!Double.isFinite(deltaError) || !Double.isFinite(marginError) || deltaError > 1E-8 || marginError > 1E-6) {
            throw new IllegalStateException("Comparison failed: delta error=" + deltaError + ", IM error=" + marginError);
        }
        return new double[] {deltaError, marginError};
    }

    private static RandomVariable delta(SensitivityMatrix matrix, double tenor) {
        Map<Double, RandomVariable> row = matrix.getSensitivityMatrix().get(tenor);
        return row == null ? new Scalar(0.0) : row.getOrDefault(tenor, new Scalar(0.0));
    }

    private static double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return (sorted[(sorted.length - 1) / 2] + sorted[sorted.length / 2]) / 2.0;
    }

    private static final class TimedSensitivities implements Sensitivities {
        private final Sensitivities provider;
        private final Map<LocalDateTime, SensitivityMatrix> deltas = new LinkedHashMap<>();
        private long nanos;

        private TimedSensitivities(Sensitivities provider) { this.provider = provider; }

        @Override
        public SensitivityMatrix getDeltaSensitivities(LocalDateTime date) throws CalculationException {
            long start = System.nanoTime();
            SensitivityMatrix result = provider.getDeltaSensitivities(date);
            nanos += System.nanoTime() - start;
            deltas.put(date, result);
            return result;
        }

        @Override
        public SensitivityMatrix getGammaSensitivities(LocalDateTime date) {
            throw new UnsupportedOperationException("This comparison measures delta-only PAIRS.");
        }
    }

    private static final class Run {
        private final long totalNanos;
        private final long deltaNanos;
        private final Map<LocalDateTime, SensitivityMatrix> deltas;
        private final Map<LocalDateTime, RandomVariable> margins;

        private Run(long totalNanos, long deltaNanos, Map<LocalDateTime, SensitivityMatrix> deltas,
                Map<LocalDateTime, RandomVariable> margins) {
            this.totalNanos = totalNanos;
            this.deltaNanos = deltaNanos;
            this.deltas = deltas;
            this.margins = margins;
        }
    }
}
