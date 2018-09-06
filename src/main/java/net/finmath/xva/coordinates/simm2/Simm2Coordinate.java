package net.finmath.xva.coordinates.simm2;

public class Simm2Coordinate {
	private Vertex vertex;
	private String riskFactorKey;
	private String bucketKey;
	private RiskClass riskClass;
	private MarginType marginType;
	private ProductClass productClass;

	@Deprecated
	public Simm2Coordinate(String maturityBucket, String riskFactorID, String bucketID, String riskClass, String riskType, String productClass) {
		this(Vertex.parseCrifTenor(maturityBucket), riskFactorID, bucketID, RiskClass.valueOf(riskClass), MarginType.valueOf(riskType), ProductClass.valueOf(productClass));
	}

	public Simm2Coordinate(Vertex vertex, String riskFactorKey, String bucketKey, RiskClass riskClass, MarginType marginType, ProductClass productClass) {
		this.vertex = vertex;
		this.riskFactorKey = riskFactorKey;
		this.bucketKey = bucketKey;
		this.riskClass = riskClass;
		this.marginType = marginType;
		this.productClass = productClass;
	}

	public Vertex getVertex() {
		return vertex;
	}

	public String getRiskFactorKey() {
		return riskFactorKey;
	}

	public String getBucketKey() {
		return bucketKey;
	}

	public RiskClass getRiskClass() {
		return riskClass;
	}

	public MarginType getRiskType() {
		return marginType;
	}

	public ProductClass getProductClass() {
		return productClass;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Simm2Coordinate key = (Simm2Coordinate) o;

		if (vertex != null ? !vertex.equals(key.vertex) : key.vertex != null)
			return false;
		if (riskFactorKey != null ? !riskFactorKey.equals(key.riskFactorKey) : key.riskFactorKey != null)
			return false;
		if (bucketKey != null ? !bucketKey.equals(key.bucketKey) : key.bucketKey != null) return false;
		if (riskClass != null ? !riskClass.equals(key.riskClass) : key.riskClass != null) return false;
		if (marginType != null ? !marginType.equals(key.marginType) : key.marginType != null) return false;
		return productClass != null ? productClass.equals(key.productClass) : key.productClass == null;
	}

	@Override
	public int hashCode() {
		int result = vertex != null ? vertex.hashCode() : 0;
		result = 31 * result + (riskFactorKey != null ? riskFactorKey.hashCode() : 0);
		result = 31 * result + (bucketKey != null ? bucketKey.hashCode() : 0);
		result = 31 * result + (riskClass != null ? riskClass.hashCode() : 0);
		result = 31 * result + (marginType != null ? marginType.hashCode() : 0);
		result = 31 * result + (productClass != null ? productClass.hashCode() : 0);
		return result;
	}
}
