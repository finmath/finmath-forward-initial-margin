package net.finmath.xva.initialmargin;

import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.Scalar;
import net.finmath.xva.coordinates.simm2.MarginType;
import net.finmath.xva.coordinates.simm2.ProductClass;
import net.finmath.xva.coordinates.simm2.RiskClass;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SIMMHelper {
	Set<Simm2Coordinate> coordinates;

	public SIMMHelper(Set<Simm2Coordinate> coordinates) {
		this.coordinates = coordinates;
		//this.tradeSet = tradeSet.stream().map(trade->(SIMMTradeSpecification)trade).collect(Collectors.toSet());
	}

	public Set<Simm2Coordinate> getCoordinates(MarginType margin, RiskClass rc) {
		return coordinates.stream().filter(k -> k.getRiskType() == margin && k.getRiskClass() == rc).collect(Collectors.toSet());
	}

	public static RandomVariableInterface getVarianceCovarianceAggregation(RandomVariableInterface[] contributions, Double[][] correlationMatrix) {
		int i = 0;
		RandomVariableInterface value = null;
		for (RandomVariableInterface contribution1 : contributions) {
			if (contribution1 != null) {
				value = value == null ? contribution1.squared() : value.add(contribution1.squared());
				int j = 0;
				for (RandomVariableInterface contribution2 : contributions) {
					if (contribution2 != null && i != j) {
						double correlation = correlationMatrix.length == 1 ? correlationMatrix[0][0] : correlationMatrix[i][j];
						RandomVariableInterface contribution = contribution1.mult(contribution2).mult(correlation);
						value = value == null ? contribution : value.add(contribution);
					}
					j++;
				}
			}
			i++;
		}
		if (value == null) {
			return null;
		}
		value = value.sqrt();
		return value;
	}

	public static RandomVariableInterface doAgg(RandomVariableInterface[] contributions, ToDoubleBiFunction<Integer, Integer> correlator) {
		return IntStream.range(0, contributions.length).
				mapToObj(i -> IntStream.range(0, contributions.length).
							mapToObj(j -> contributions[i].mult(contributions[j]).mult(correlator.applyAsDouble(i, j)))).
				flatMap(Function.identity()).
				reduce(new Scalar(0.0), RandomVariableInterface::add).sqrt();
	}

	public Set<ProductClass> getAvailableProductClasses(double evaluationTime) {
		return this.coordinates.stream().
				filter(Objects::nonNull).
				map(Simm2Coordinate::getProductClass).
				collect(Collectors.toSet());
	}

	public Set<RiskClass> getRiskClassesForProductClass(ProductClass productClass, double evaluationTime) {
		return coordinates.stream().
				filter(Objects::nonNull).
				filter(k -> k.getProductClass() == productClass).
				map(Simm2Coordinate::getRiskClass).
				collect(Collectors.toSet());
	}

	public Set<RiskClass> getAvailableRiskClasses(double evaluationTime) {
		return this.coordinates.stream().
				filter(Objects::nonNull).
				map(Simm2Coordinate::getRiskClass).
				collect(Collectors.toSet());
	}

	public Map<RiskClass, Set<String>> getBucketsByRiskClass(MarginType marginType, double evaluationTime) {
		return coordinates.stream().
				filter(Objects::nonNull).
				filter(k -> k.getRiskType() == marginType).
				collect(Collectors.groupingBy(Simm2Coordinate::getRiskClass,
						Collectors.mapping(Simm2Coordinate::getBucketKey, Collectors.toSet())));
	}

	public Map<RiskClass, Set<String>> getRiskFactorKeysByRiskClass(MarginType riskTypeString, String bucketKey, double evaluationTime) {
		return coordinates.stream().
				filter(Objects::nonNull).
				filter(k -> k.getRiskType() == riskTypeString && k.getBucketKey().equals(bucketKey)).
				collect(Collectors.groupingBy(Simm2Coordinate::getRiskClass,
						Collectors.mapping(Simm2Coordinate::getQualifier, Collectors.toSet())));
	}
}
