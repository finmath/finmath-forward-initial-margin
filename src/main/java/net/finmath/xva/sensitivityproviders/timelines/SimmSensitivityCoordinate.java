package net.finmath.xva.sensitivityproviders.timelines;

/**
 * Holds the parameters for identifying the requested ISDA SIMM sensitivity.
 */
public class SimmSensitivityCoordinate {
	private final String productClass;
	private final String riskClass;
	private final String riskType;
	private final String bucketKey;// currency for IR otherwise bucket number
	private final String maturityBucket;// only for IR and Credit risk class, null otherwise
	private final String curveIndexName;// null if riskClass is not IR

	public SimmSensitivityCoordinate(String productClass, String riskClass, String riskType, String bucketKey, String maturityBucket, String curveIndexName) {
		this.productClass = productClass;
		this.riskClass = riskClass;
		this.riskType = riskType;
		this.bucketKey = bucketKey;
		this.maturityBucket = maturityBucket;
		this.curveIndexName = curveIndexName;
	}

	public String getProductClass() {
		return productClass;
	}

	public String getRiskClass() {
		return riskClass;
	}

	public String getRiskType() {
		return riskType;
	}

	public String getBucketKey() {
		return bucketKey;
	}

	public String getMaturityBucket() {
		return maturityBucket;
	}

	public String getCurveIndexName() {
		return curveIndexName;
	}
}
