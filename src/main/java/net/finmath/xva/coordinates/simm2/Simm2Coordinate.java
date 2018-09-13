package net.finmath.xva.coordinates.simm2;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;

public final class Simm2Coordinate {
	private Vertex vertex;
	private String riskFactorKey;//~qualifier
	private String bucketKey;//~label2
	private RiskClass riskClass;
	private MarginType marginType;
	private ProductClass productClass;

	public static class Qualifier {
		private String text;

		Qualifier(String text) {
			this.text = text;
		}

		public Pair<String, String> getCurrencyPair() {
			return Pair.of(text.substring(0, 3).toUpperCase(), text.substring(3, 6).toUpperCase());
		}

		public String getCurrency() {
			return text.toUpperCase();
		}

		public String getText() {
			return text;
		}
	}

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

	/**
	 * Gets the additional identifier needed to determine the risk factor. This shall correspond to the CRIF qualifier.
	 * For interest rate/inflation risk factors this will be the currency.
	 * For the FX delta this will be the currency.
	 * For the FX vega this will be a currency pair, represented by the concatenated currencies, e. g. <tt>USDEUR</tt>.
	 * For equities this will be the ISIN of the security or a pre-defined identifier for indices.
	 * For commodities this will be a pre-defined human-readable identifier, e. g. <tt>Freight</tt>.
	 * @return A {@link Qualifier} object wrapping the string and offering methods for handling the different formats.
	 */
	public Qualifier getQualifier() {
		return new Qualifier(riskFactorKey);
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
		return Objects.hash(vertex, riskFactorKey, bucketKey, riskClass,marginType, productClass);
	}
}
