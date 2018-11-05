package net.finmath.xva.initialmargin;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.google.gson.Gson;

import net.finmath.xva.coordinates.simm2.ProductClass;
import net.finmath.xva.coordinates.simm2.RiskClass;
import net.finmath.xva.coordinates.simm2.Vertex;

public class SIMMParameter {
	public SIMMParameter() {

	}

	public SIMMParameter(String jsonString) {
		Gson gson = new Gson();
		Map<String, String> map = gson.fromJson(jsonString, new HashMap<String, String>().getClass());
		CrossRiskClassCorrelationMatrix = gson.fromJson(map.get("CrossRiskClassCorrelationMatrix"), (new Double[0][0]).getClass());
		MapFXCategory = gson.fromJson(map.get("MapFXCategory"), (new HashMap<String, String>()).getClass());
		MapHistoricalVolaRatio = gson.fromJson(map.get("MapHistoricalVolaRatio"), (new HashMap<String, String>()).getClass());
		ProductClassKeys = gson.fromJson(map.get("ProductClassKeys"), String[].class);
		RiskClassKeys = gson.fromJson(map.get("RiskClassKeys"), String[].class);
		CreditMaturityBuckets = gson.fromJson(map.get("CreditMaturityBuckets"), String[].class);
		IRMaturityBuckets = gson.fromJson(map.get("IRMaturityBuckets"), String[].class);
		IRCurveIndexNames = gson.fromJson(map.get("IRCurveIndexNames"), String[].class);
		MapRiskClassThresholdMap = gson.fromJson(map.get("MapRiskClassThresholdMap"), new HashMap<String, Map<String, Double[][]>>().getClass());
		IRCorrelationCrossCurrency = gson.fromJson(map.get("IRCorrelationCrossCurrency"), Double.class);
		IRCurrencyMap = gson.fromJson(map.get("IRCurrencyMap"), (new HashMap<String, String>()).getClass());

		MapRiskClassCorrelationIntraBucketMap = gson.fromJson(map.get("MapRiskClassCorrelationIntraBucketMap"), (new HashMap<RiskClass, Double[][]>()).getClass());
		MapRiskClassCorrelationCrossBucketMap = gson.fromJson(map.get("MapRiskClassCorrelationCrossBucketMap"), (new HashMap<RiskClass, Double[][]>()).getClass());
		MapRiskClassThresholdMap = gson.fromJson(map.get("MapRiskClassThresholdMap"), (new HashMap<String, Map<String, Double[][]>>()).getClass());
		MapRiskClassRiskweightMap = gson.fromJson(map.get("MapRiskClassRiskweightMap"), (new HashMap<String, Map<String, Double[][]>>()).getClass());
	}

	final static public String inflationKey = "inflation";
	final static public String ccyBasisKey = "ccybasis";

	public enum RatesCurveNames {
		OIS,
		Libor1m,
		Libor3m,
		Libor6m,
		Libor12m,
	}

	public static String[] getProductClassKeys() {
		return Stream.of(ProductClass.values()).map(ProductClass::name).toArray(String[]::new);
	}

	public static String[] getRiskClassKeys() {
		return Stream.of(RiskClass.values()).map(RiskClass::name).toArray(String[]::new);
	}

	public static String[] getRateCurveKeys() {
		return Stream.of(RatesCurveNames.values()).map(RatesCurveNames::name).toArray(String[]::new);
	}

	public Double[][] CrossRiskClassCorrelationMatrix;

	public Map<String, String> MapFXCategory;

	public Map<String, Double> MapHistoricalVolaRatio;

	public String[] ProductClassKeys = {"RATES_FX", "CREDIT", "EQUITY", "COMMODITY"};
	public String[] RiskClassKeys = {"INTEREST_RATE", "CREDIT_Q", "CREDIT_NON_Q", "EQUITY", "COMMODITY", "FX"};
	public String[] CreditMaturityBuckets = {"1y", "2y", "3y", "5y", "10y"};
	public Vertex[] getCreditVertices() {
		return new Vertex[] { Vertex.Y1, Vertex.Y2, Vertex.Y3, Vertex.Y5, Vertex.Y10 };
	}
	public Vertex[] getIRVertices() {
		return Vertex.values();
	}
	public String[] IRMaturityBuckets = {"2w", "1m", "3m", "6m", "1y", "2y", "3y", "5y", "10y", "15y", "20y", "30y"};
	public String[] IRCurveIndexNames = {"OIS", "Libor1m", "Libor3m", "Libor6m", "Libor12m"};

	//       final public Double             IRCorrelationCrossCurveIndex = 0.982;
	public Double IRCorrelationCrossCurrency;// = .27;

	public Map<String, String> IRCurrencyMap;

	public Map<RiskClass, Double[][]> MapRiskClassCorrelationIntraBucketMap;
	public Map<RiskClass, Double[][]> MapRiskClassCorrelationCrossBucketMap;
	public Map<RiskClass, Map<String, Map<String, Double[][]>>> MapRiskClassThresholdMap;
	public Map<RiskClass, Map<String, Map<String, Double[][]>>> MapRiskClassRiskweightMap;

	public void setIRCurrencyMap(Map<String, String> IRCurrencyMap) {
		this.IRCurrencyMap = IRCurrencyMap;
	}

	public SIMMParameter setCrossRiskClassCorrelationMatrix(Double[][] crossRiskClassCorrelationMatrix) {
		CrossRiskClassCorrelationMatrix = crossRiskClassCorrelationMatrix;
		return this;
	}
}
