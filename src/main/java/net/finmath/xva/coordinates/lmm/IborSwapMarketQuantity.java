package net.finmath.xva.coordinates.lmm;

import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.xva.coordinates.simm2.MarginType;
import net.finmath.xva.coordinates.simm2.ProductClass;
import net.finmath.xva.coordinates.simm2.RiskClass;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;
import net.finmath.xva.coordinates.simm2.Vertex;

/**
 * The SIMM required swap rate market quantities for IBOR rates.
 */
public class IborSwapMarketQuantity implements ModelledMarketQuantity {
	private Vertex tenor;
	private String currency;
	private ProductClass productClass;
	private String curveName;
	//TODO these are actually functions of tenor + currency (swap conventions)
	//How not to pass these in addition?
	private double iborPeriod;
	private double fixPeriod;

	public IborSwapMarketQuantity(Vertex tenor, String currency, ProductClass productClass, String curveName, double iborPeriod, double fixPeriod) {
		this.tenor = tenor;
		this.currency = currency;
		this.productClass = productClass;
		this.curveName = curveName;
		this.iborPeriod = iborPeriod;
		this.fixPeriod = fixPeriod;
	}

	@Override
	public Simm2Coordinate getCoordinate() {
		return new Simm2Coordinate(tenor, currency, curveName, RiskClass.INTEREST_RATE, MarginType.DELTA, productClass);
	}

	@Override
	public AbstractMonteCarloProduct getProduct(double evaluationTime) {
		return SwapRateBuilder.startingAt(evaluationTime).
				withTenor(tenor.getIdealizedYcf()).
				floatPaysEvery(iborPeriod).
				fixPaysEvery(fixPeriod).
				build();
	}
}
