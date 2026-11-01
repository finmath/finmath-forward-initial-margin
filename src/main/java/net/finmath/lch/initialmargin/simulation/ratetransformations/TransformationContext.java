package net.finmath.lch.initialmargin.simulation.ratetransformations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

import net.finmath.stochastic.RandomVariable;

/**
 * Stores chronological snapshots of a transformation pipeline.
 * Scalars broadcast to simulated paths; all nondeterministic observations in a
 * series must share a path count. RandomVariable objects themselves are retained,
 * preserving their pathwise values and any differentiation graph.
 *
 * @author Raphael Prandtl
 */
public class TransformationContext {

    private NavigableMap<LocalDateTime, RandomVariable> primaryData;
    private final Map<String, NavigableMap<LocalDateTime, RandomVariable>> transformationResults = new HashMap<>();
    private int numberOfPaths = -1;

    public TransformationContext(NavigableMap<LocalDateTime, RandomVariable> primaryData) {
        setPrimaryData(primaryData);
    }

    public NavigableMap<LocalDateTime, RandomVariable> getPrimaryData() {
        return primaryData;
    }

    /** Validates both input data and subsequent transformation results pathwise. */
    public void setPrimaryData(NavigableMap<LocalDateTime, RandomVariable> data) {
        primaryData = validatedCopy(data);
    }

    private NavigableMap<LocalDateTime, RandomVariable> validatedCopy(NavigableMap<LocalDateTime, RandomVariable> data) {
        NavigableMap<LocalDateTime, RandomVariable> snapshot = copySeries(data);
        int seriesPaths = numberOfPaths;
        for (Map.Entry<LocalDateTime, RandomVariable> entry : snapshot.entrySet()) {
            RandomVariable value = Objects.requireNonNull(entry.getValue(), "Missing value at " + entry.getKey());
            if (value.size() < 1) {
                throw new IllegalArgumentException("Empty path vector at " + entry.getKey() + ".");
            }
            if (!value.isDeterministic()) {
                if (seriesPaths >= 0 && seriesPaths != value.size()) {
                    throw new IllegalArgumentException("Inconsistent path count at " + entry.getKey() + ": expected " + seriesPaths + ", received " + value.size() + ".");
                }
                seriesPaths = value.size();
            }
            for (int path = 0; path < value.size(); path++) {
                if (!Double.isFinite(value.get(path))) {
                    throw new IllegalArgumentException("Nonfinite value at " + entry.getKey() + ", path " + path + ".");
                }
            }
        }
        numberOfPaths = seriesPaths;
        return snapshot;
    }

    public void addTransformationResult(String transformationId, NavigableMap<LocalDateTime, RandomVariable> data) {
        transformationResults.put(Objects.requireNonNull(transformationId, "transformationId"),
                data == primaryData ? primaryData : validatedCopy(data));
    }

    public NavigableMap<LocalDateTime, RandomVariable> getTransformationResult(String transformationId) {
        return transformationResults.get(transformationId);
    }

    public Map<String, NavigableMap<LocalDateTime, RandomVariable>> getTransformationResultMap() {
        return new HashMap<>(transformationResults);
    }

    private static NavigableMap<LocalDateTime, RandomVariable> copySeries(NavigableMap<LocalDateTime, RandomVariable> data) {
        Objects.requireNonNull(data, "data");
        NavigableMap<LocalDateTime, RandomVariable> snapshot = new TreeMap<>();
        snapshot.putAll(data);
        return Collections.unmodifiableNavigableMap(snapshot);
    }
}
