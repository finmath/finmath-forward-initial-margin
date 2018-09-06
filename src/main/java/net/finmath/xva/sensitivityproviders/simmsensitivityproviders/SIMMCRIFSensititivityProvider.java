package net.finmath.xva.sensitivityproviders.simmsensitivityproviders;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

import java.util.Map;

public class SIMMCRIFSensititivityProvider implements SIMMSensitivityProviderInterface {

	Map<Simm2Coordinate, Double> SensitivitiyMap;

	public SIMMCRIFSensititivityProvider(Map<Simm2Coordinate, Double> SensitivityMap) {
		this.SensitivitiyMap = SensitivityMap;
	}

	public Map<Simm2Coordinate, Double> getSensitivitiyMap() {
		return SensitivitiyMap;
	}

	public RandomVariableInterface getSIMMSensitivity(Simm2Coordinate key, double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		if (SensitivitiyMap.containsKey(key))
			return new RandomVariable(evaluationTime, model.getNumberOfPaths(), SensitivitiyMap.get(key));
		else
			return new RandomVariable(evaluationTime, model.getNumberOfPaths(), 0.0);
	}
}
