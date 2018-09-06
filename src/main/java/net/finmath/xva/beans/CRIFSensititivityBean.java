package net.finmath.xva.beans;

import com.google.gson.annotations.SerializedName;
import net.finmath.xva.coordinates.simm2.MarginType;
import net.finmath.xva.coordinates.simm2.ProductClass;
import net.finmath.xva.coordinates.simm2.RiskClass;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

public class CRIFSensititivityBean {
	public static class CRIFSensiBean {

		@SerializedName("Counterparty")
		String counterparty;
		@SerializedName("TradeID")
		String tradeId;

		@SerializedName("ProductClass")
		String productClass;

		@SerializedName("RiskType")
		String riskType;

		@SerializedName("qualifier")
		String qualifier;

		@SerializedName("Bucket")
		String bucket;

		@SerializedName("Label1")
		String label1;

		@SerializedName("Label2")
		String label2;

		@SerializedName("Amount")
		Double amount;

		@SerializedName("AmountCCY")
		String amountCcy;

		@SerializedName("AmountUSD")
		Double amountUsd;

		public String getCounterparty() {
			return counterparty;
		}

		public Simm2Coordinate getSensitivityKey() {
			Simm2Coordinate key = null;
			if (riskType.contains("IR") && !riskType.contains("Vol"))
				key = new Simm2Coordinate(label1, label2, qualifier, RiskClass.INTEREST_RATE, MarginType.DELTA, getParsedProductClass());
			else if (riskType.contains("IR") && riskType.contains("Vol"))
				key = new Simm2Coordinate(label1, label2, qualifier, RiskClass.INTEREST_RATE, MarginType.VEGA, getParsedProductClass());
			else if (riskType.contains("Equity") && !riskType.contains("Vol"))
				key = new Simm2Coordinate("None", qualifier, bucket, RiskClass.EQUITY, MarginType.DELTA, getParsedProductClass());
			else if (riskType.contains("Equity") && riskType.contains("Vol"))
				key = new Simm2Coordinate(label1, qualifier, bucket, RiskClass.EQUITY, MarginType.VEGA, getParsedProductClass());
			else if (riskType.contains("Commodity") && !riskType.contains("Vol"))
				key = new Simm2Coordinate("None", qualifier, bucket, RiskClass.COMMODITY, MarginType.DELTA, getParsedProductClass());
			else if (riskType.contains("Commodity") && riskType.contains("Vol"))
				key = new Simm2Coordinate(label1, qualifier, bucket, RiskClass.COMMODITY, MarginType.VEGA, getParsedProductClass());
			else if (riskType.contains("FX") && !riskType.contains("Vol"))
				key = new Simm2Coordinate("None", qualifier, "0", RiskClass.FX, MarginType.DELTA, getParsedProductClass());
			else if (riskType.contains("FX") && riskType.contains("Vol")) {
				if (qualifier.length() == 6) {
					String ccy1 = qualifier.substring(0, 3);
					String ccy2 = qualifier.substring(3, 6);
					if (ccy2.compareTo(ccy1) > 0) {
						qualifier = ccy2 + ccy1;
					}
				}
				key = new Simm2Coordinate(label1, qualifier, "0", RiskClass.FX, MarginType.VEGA, getParsedProductClass());
			} else if (riskType.contains("CreditQ"))
				key = new Simm2Coordinate(label1, qualifier, bucket, RiskClass.CREDIT_Q, MarginType.DELTA, getParsedProductClass());
			else if (riskType.contains("CreditNonQ"))
				key = new Simm2Coordinate(label1, qualifier, bucket, RiskClass.CREDIT_NON_Q, MarginType.DELTA, getParsedProductClass());
			else if (riskType.contains("Credit") && riskType.contains("Vol"))
				key = new Simm2Coordinate(label1, qualifier, bucket, RiskClass.CREDIT_Q, MarginType.VEGA, getParsedProductClass());
			else if (riskType.contains("Inflation") && !riskType.contains("Vol"))
				key = new Simm2Coordinate("None", "inflation", qualifier, RiskClass.INTEREST_RATE, MarginType.DELTA, getParsedProductClass());
			else if (riskType.contains("Inflation") && riskType.contains("Vol"))
				key = new Simm2Coordinate("None", "inflation", qualifier, RiskClass.INTEREST_RATE, MarginType.VEGA, getParsedProductClass());
			else if (riskType.contains("XCcy"))
				key = new Simm2Coordinate("None", "ccybasis", qualifier, RiskClass.INTEREST_RATE, MarginType.DELTA, getParsedProductClass());
			return key;

		}

		public CRIFSensiBean(String counterparty, String tradeId, String productClass, String riskType, String qualifier, String bucket, String label1, String label2, Double amount, String amountCcy, Double amountUsd) {
			this.counterparty = counterparty;
			this.tradeId = tradeId;
			this.productClass = productClass;
			this.riskType = riskType;
			this.qualifier = qualifier;
			this.bucket = bucket;
			this.label1 = label1;
			this.label2 = label2;
			this.amount = amount;
			this.amountCcy = amountCcy;
			this.amountUsd = amountUsd;
		}

		public String getTradeId() {
			return tradeId;
		}

		public void setTradeId(String tradeId) {
			this.tradeId = tradeId;
		}

		public String getProductClass() {
			return productClass;
		}

		public ProductClass getParsedProductClass() {
			return ProductClass.parseCrifProductClass(productClass);
		}

		public void setProductClass(String productClass) {
			this.productClass = productClass;
		}

		public String getRiskType() {
			return riskType;
		}

		public void setRiskType(String riskType) {
			this.riskType = riskType;
		}

		public String getQualifier() {
			return qualifier;
		}

		public void setQualifier(String qualifier) {
			this.qualifier = qualifier;
		}

		public String getBucket() {
			return bucket;
		}

		public void setBucket(String bucket) {
			this.bucket = bucket;
		}

		public String getLabel1() {
			return label1;
		}

		public void setLabel1(String label1) {
			this.label1 = label1;
		}

		public String getLabel2() {
			return label2;
		}

		public void setLabel2(String label2) {
			this.label2 = label2;
		}

		public Double getAmount() {
			return amount;
		}

		public void setAmount(Double amount) {
			this.amount = amount;
		}

		public String getAmountCcy() {
			return amountCcy;
		}

		public void setAmountCcy(String amountCcy) {
			this.amountCcy = amountCcy;
		}

		public Double getAmountUsd() {
			return amountUsd;
		}

		public void setAmountUsd(Double amountUsd) {
			this.amountUsd = amountUsd;
		}
	}
}
