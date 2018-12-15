package net.finmath.sensitivities.simm2;

import java.util.Objects;

public final class SimmCoordinate {
	private Vertex vertex; //~~Label1 in CRIF
	private SubCurve subCurve; //~~Label2 in CRIF
	private Qualifier qualifier;
	private String bucketKey;//~~CRIF bucket, not necessarily SIMM bucket!
	private RiskClass riskClass;
	private MarginType marginType;
	private ProductClass productClass;

	public SimmCoordinate(Vertex vertex, String qualifier, RiskClass riskClass, MarginType marginType, ProductClass productClass) {
		this(vertex, null, new Qualifier(qualifier), null, riskClass, marginType, productClass);
	}

	public SimmCoordinate(Vertex vertex, String qualifier, String bucketKey, RiskClass riskClass, MarginType marginType, ProductClass productClass) {
		this(vertex, null, new Qualifier(qualifier), bucketKey, riskClass, marginType, productClass);
	}

	public SimmCoordinate(Vertex vertex, SubCurve subCurve, String qualifier, RiskClass riskClass, MarginType marginType, ProductClass productClass) {
		this(vertex, subCurve, new Qualifier(qualifier), null, riskClass, marginType, productClass);
	}

	public SimmCoordinate(Vertex vertex, SubCurve subCurve, Qualifier qualifier, String bucketKey, RiskClass riskClass, MarginType marginType, ProductClass productClass) {
		this.vertex = vertex;
		this.subCurve = subCurve;
		this.qualifier = qualifier;
		this.bucketKey = bucketKey;
		this.riskClass = riskClass;
		this.marginType = marginType;
		this.productClass = productClass;
	}

	public Vertex getVertex() {
		return vertex;
	}

	/**
	 * @return Returns the same coordinate without a vertex, which can be used to group different vertices together.
	 */
	public SimmCoordinate stripVertex() {
		return new SimmCoordinate(null, subCurve, qualifier, bucketKey, riskClass, marginType, productClass);
	}

	public SimmCoordinate withMarginType(MarginType newMarginType) {
		return new SimmCoordinate(vertex, subCurve, qualifier, bucketKey, riskClass, newMarginType, productClass);
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
		return qualifier;
	}

	/**
	 * @return Returns the bucket that is used in the CRIF breakdown. This differs from the SIMM bucket for interest rate risk.
	 * @see SimmCoordinate#getSimmBucket()
	 */
	public String getCrifBucket() {
		if (riskClass == RiskClass.INTEREST_RATE && bucketKey == null) {
			return "1"; //TODO: somehow differentiate between low-vol, reg-vol and high-vol currencies, maybe take Simm2Parameters into account?
		}

		return bucketKey;
	}

	/**
	 * @return Returns the bucket that is used in the SIMM aggregation. This differs from the CRIF bucket for interest rate risk.
	 * @see SimmCoordinate#getCrifBucket()
	 */
	public String getSimmBucket() {
		if (riskClass == RiskClass.INTEREST_RATE) {
			return qualifier.getCurrency();
		}

		return bucketKey;
	}

	public RiskClass getRiskClass() {
		return riskClass;
	}

	public MarginType getMarginType() {
		return marginType;
	}

	public ProductClass getProductClass() {
		return productClass;
	}

	/**
	 * @return Returns the sub-curve's name for interest rate risk.
	 * @implNote Might be null for non-suitable risk classes.
	 */
	public SubCurve getSubCurve() {
		return subCurve;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SimmCoordinate that = (SimmCoordinate) o;
		return vertex == that.vertex &&
				Objects.equals(subCurve, that.subCurve) &&
				Objects.equals(qualifier, that.qualifier) &&
				Objects.equals(bucketKey, that.bucketKey) &&
				riskClass == that.riskClass &&
				marginType == that.marginType &&
				productClass == that.productClass;
	}

	@Override
	public int hashCode() {
		return Objects.hash(vertex, subCurve, qualifier, bucketKey, riskClass, marginType, productClass);
	}

	@Override
	public String toString() {
		return "SimmCoordinate{" +
				"vertex=" + vertex +
				", subCurve='" + subCurve + '\'' +
				", qualifier=" + qualifier +
				", bucketKey='" + bucketKey + '\'' +
				", riskClass=" + riskClass +
				", marginType=" + marginType +
				", productClass=" + productClass +
				'}';
	}
}
