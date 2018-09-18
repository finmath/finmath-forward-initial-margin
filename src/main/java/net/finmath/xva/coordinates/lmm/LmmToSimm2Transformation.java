package net.finmath.xva.coordinates.lmm;

import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

import java.util.Map;

public class LmmToSimm2Transformation {
	public Map<Simm2Coordinate, RandomVariableInterface> transform(Map<LmmCoordinate, RandomVariableInterface> modelSensitivities) {
		/*
			private RandomVariableInterface[][] getSensitivityWeightLIBOR(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

		if (riskWeightMapLibor.containsKey(evaluationTime)) {
			return riskWeightMapLibor.get(evaluationTime);
		}

		RandomVariableInterface[][] dLdS = null;
		double liborPeriodLength = model.getLiborPeriodDiscretization().getTimeStep(0);

		// Get index of first LIBOR starting >= evaluationTime
		int nextLiborIndex = model.getLiborPeriodDiscretization().getTimeIndexNearestGreaterOrEqual(evaluationTime);
		int numberOfRemainingLibors = model.getNumberOfLibors() - nextLiborIndex;
		dLdS = new RandomVariableInterface[numberOfRemainingLibors][numberOfRemainingLibors];

		// Calculate d\tilde{L}dS directly
		dLdS[0][0] = new RandomVariable(1.0);
		double discountTime = evaluationTime + liborPeriodLength;
		RandomVariableInterface sumDf = model.getForwardBondOIS(discountTime, evaluationTime);
		for (int liborIndex = 1; liborIndex < dLdS.length; liborIndex++) {
			discountTime += liborPeriodLength;
			RandomVariableInterface denominator = model.getForwardBondOIS(discountTime, evaluationTime);
			dLdS[liborIndex][liborIndex - 1] = sumDf.div(denominator).mult(-1.0);
			sumDf = sumDf.add(denominator);
			dLdS[liborIndex][liborIndex] = sumDf.div(denominator);
		}

		// Calculate dLdS = dLd\tilde{L} * d\tilde{L}dS
		RandomVariableInterface[][] dLdL = getLiborTimeGridAdjustment(evaluationTime, model);
		dLdS = multiply(dLdL, dLdS);

		riskWeightMapLibor.put(evaluationTime, dLdS);
		return dLdS;
	}
		 */
	}
}
