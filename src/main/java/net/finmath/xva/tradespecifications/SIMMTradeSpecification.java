package net.finmath.xva.tradespecifications;

import net.finmath.xva.coordinates.simm2.ProductClass;
import net.finmath.xva.coordinates.simm2.RiskClass;
import net.finmath.xva.coordinates.simm2.Simm2Coordinate;

import java.util.Set;
import java.util.stream.Collectors;

public class SIMMTradeSpecification {
	private double notional;
	private double maturity;
	private IRCurveSpec irCurve;
	private Set<Simm2Coordinate> sensitivityKeySet;

	public SIMMTradeSpecification(double notional, double maturity, IRCurveSpec irCurve) {
		this.notional = notional;
		this.maturity = maturity;
		this.irCurve = irCurve;
	}

	public double getMaxTimeToMaturity() {
		return 0.0;
	}

	public double getNotional() {
		return 0.0;
	}

	public ProductClass getProductClass() {
		return sensitivityKeySet.stream().map(key -> key.getProductClass()).distinct().findAny().get();
	}

	public Set<RiskClass> getRiskClasses() {
		return sensitivityKeySet.stream().map(key -> key.getRiskClass()).collect(Collectors.toSet());
	}

	public Set<String> getRiskfactors() {
		return this.sensitivityKeySet.stream().map(key -> key.getRiskFactorKey()).collect(Collectors.toSet());
	}

	public String getTradeID() {
		return "";
	}

	public Set<Simm2Coordinate> getSensitivityKeySet(double evaluationTime) {
		return sensitivityKeySet;
	}

	public IRCurveSpec getIRCurve() {
		return irCurve;
	}
}
