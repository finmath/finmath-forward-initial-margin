package net.finmath.initialmargin.isdasimm;

import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.interestrate.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.LIBORModel;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.time.TimeDiscretizationFromArray;

public class SIMMUnitTests {

	public LIBORModelMonteCarloSimulationModel getDummySimulation() throws Exception {
		double lastTime = 30.0;
		double dt = 0.1;
		TimeDiscretizationFromArray timeDiscretizationFromArray = new TimeDiscretizationFromArray(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create the libor tenor structure and the initial values
		 */
		final int numberOfPaths = 1;
		final int numberOfFactors = 1;

		double liborPeriodLength = 0.5;
		double liborRateTimeHorzion = 30.0;
		TimeDiscretizationFromArray liborPeriodDiscretization = new TimeDiscretizationFromArray(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);
		final BrownianMotion brownianMotion = new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray, numberOfFactors, numberOfPaths, 31415 /* seed */);
		LIBORModel liborMarketModelCalibrated = new LIBORMarketModelFromCovarianceModel(
				liborPeriodDiscretization,
				null,
				null, null,
				new RandomVariableFactory(), // No AAD here
				null,
				null,
				null);
		EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(brownianMotion);
		LIBORModelMonteCarloSimulationModel simulationCalibrated = new LIBORMonteCarloSimulationFromLIBORModel(liborMarketModelCalibrated, process);
		return simulationCalibrated;
	}
}
