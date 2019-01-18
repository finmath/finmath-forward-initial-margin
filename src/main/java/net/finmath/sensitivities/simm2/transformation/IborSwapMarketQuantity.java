package net.finmath.sensitivities.simm2.transformation;

import net.finmath.montecarlo.MonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.SwapRateBuilder;
import net.finmath.sensitivities.simm2.MarginType;
import net.finmath.sensitivities.simm2.ProductClass;
import net.finmath.sensitivities.simm2.RiskClass;
import net.finmath.sensitivities.simm2.SimmCoordinate;
import net.finmath.sensitivities.simm2.SubCurve;
import net.finmath.sensitivities.simm2.Vertex;
import net.finmath.sensitivities.transformation.TargetQuantity;

/**
 * The SIMM required swap rate market quantities for IBOR rates.
 */
public class IborSwapMarketQuantity implements TargetQuantity<SimmCoordinate> {
	private Vertex tenor;
	private String currency;
	private ProductClass productClass;
	private SubCurve subCurve;
	//TODO these are actually functions of tenor + currency (swap conventions)
	//How not to pass these in addition?
	private double iborPeriod;
	private double fixPeriod;

	public IborSwapMarketQuantity(Vertex tenor, String currency, ProductClass productClass, SubCurve subCurve, double iborPeriod, double fixPeriod) {
		this.tenor = tenor;
		this.currency = currency;
		this.productClass = productClass;
		this.subCurve = subCurve;
		this.iborPeriod = iborPeriod;
		this.fixPeriod = fixPeriod;
	}

	@Override
	public SimmCoordinate getCoordinate() {
		return new SimmCoordinate(tenor, subCurve, currency, RiskClass.INTEREST_RATE, MarginType.DELTA, productClass);
	}

	@Override
	public MonteCarloProduct getProduct(double evaluationTime) {
		return SwapRateBuilder.startingAt(evaluationTime).
				withTenor(tenor.getIdealizedYcf()).
				floatPaysEvery(iborPeriod).
				fixPaysEvery(fixPeriod).
				build();
	}
}
