package net.finmath.initialmargin.isdasimm;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.xva.beans.CrifSensitivityBean;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;
import net.finmath.xva.initialmargin.SIMMParameter;
import net.finmath.xva.initialmargin.SimmModality;
import net.finmath.xva.initialmargin.SimmProduct;
import net.finmath.xva.sensitivityproviders.timelines.SimmConstantTimeline;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import static java.lang.System.exit;

public class SIMMUnitTests {

	@Test
	public void testDataRetrievableISDASIMMParameterSet() {

		try {

			Gson gson = new Gson();
			JsonReader reader = new JsonReader(new FileReader("crif.json"));
			Type empTypeList = new TypeToken<Collection<CrifSensitivityBean>>() {
			}.getType();
			List<CrifSensitivityBean> test = gson.fromJson(reader, new TypeToken<List<CrifSensitivityBean>>() {
			}.getType());

			List<String> counterpartyList = test.stream().map(entry -> entry.getCounterparty()).distinct().collect(Collectors.toList());

			String content = new Scanner(new File("simm.json")).next();
			SIMMParameter parameter = new SIMMParameter(content);
			counterpartyList.stream().forEach(cp -> {

				try {

					Map<Simm2Coordinate, Double> simmSensitivityKeyDoubleMap = test.stream().filter(entry -> entry.getCounterparty().equals(cp)).collect(Collectors.toMap(entry -> entry.getSensitivityKey(), entry -> entry.getAmount()));

					SimmConstantTimeline provider = SimmConstantTimeline.fromDouble(simmSensitivityKeyDoubleMap);

					SimmProduct simmProduct = new SimmProduct(0.0, provider, new SimmModality("EUR", 0.0));

					RandomVariableInterface result = simmProduct.getValue(0.0, getDummySimulation());
				} catch (Exception e) {
					System.out.println(e);
				}
			});

			exit(0);
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public LIBORModelMonteCarloSimulationInterface getDummySimulation() throws Exception {
		double lastTime = 30.0;
		double dt = 0.1;
		TimeDiscretization timeDiscretization = new TimeDiscretization(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create the libor tenor structure and the initial values
		 */
		final int numberOfPaths = 1;
		final int numberOfFactors = 1;

		double liborPeriodLength = 0.5;
		double liborRateTimeHorzion = 30.0;
		TimeDiscretization liborPeriodDiscretization = new TimeDiscretization(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);
		final BrownianMotionInterface brownianMotion = new net.finmath.montecarlo.BrownianMotion(timeDiscretization, numberOfFactors, numberOfPaths, 31415 /* seed */);
		LIBORModelInterface liborMarketModelCalibrated = new LIBORMarketModel(
				liborPeriodDiscretization,
				null,
				null, null,
				new RandomVariableFactory(), // No AAD here
				null,
				null,
				null);
		ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion);
		LIBORModelMonteCarloSimulationInterface simulationCalibrated = new LIBORModelMonteCarloSimulation(liborMarketModelCalibrated, process);
		return simulationCalibrated;
	}
}
