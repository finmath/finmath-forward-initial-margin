package net.finmath.xva.coordinates.lmm;

import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.xva.coordinates.simm2.MarginType;
import net.finmath.xva.coordinates.simm2.ProductClass;
import net.finmath.xva.coordinates.simm2.RiskClass;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;
import net.finmath.xva.coordinates.simm2.Vertex;

/**
 * The SIMM
 */
public class IborSwapMarketQuantity implements ModelledMarketQuantity {
	private Vertex tenor;
	private String currency;
	private ProductClass productClass;

	@Override
	public Simm2Coordinate getCoordinate() {
		return new Simm2Coordinate(tenor, currency, "Curvename?", RiskClass.INTEREST_RATE, MarginType.DELTA, productClass);
	}

	@Override
	public AbstractMonteCarloProduct getProduct() {
		//TODO create tenor schedules in dependence of vertex.
		return new SwapMarketRateProduct(null, null);
	}
}
