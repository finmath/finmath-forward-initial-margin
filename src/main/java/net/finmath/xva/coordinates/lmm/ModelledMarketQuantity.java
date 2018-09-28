package net.finmath.xva.coordinates.lmm;

import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

public interface ModelledMarketQuantity {
	Simm2Coordinate getCoordinate();

	AbstractMonteCarloProduct getProduct();
}
