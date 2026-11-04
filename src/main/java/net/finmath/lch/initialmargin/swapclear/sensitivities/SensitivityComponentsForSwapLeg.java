package net.finmath.lch.initialmargin.swapclear.sensitivities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.finmath.exception.CalculationException;
import net.finmath.lch.initialmargin.simulation.modeldata.ZeroRateModel;
import net.finmath.lch.initialmargin.swapclear.products.components.LchNotionalFromConstant;
import net.finmath.lch.initialmargin.swapclear.products.components.LchPeriod;
import net.finmath.lch.initialmargin.swapclear.products.components.LchSwapLeg;
import net.finmath.lch.initialmargin.swapclear.products.indices.LchAbstractIndex;
import net.finmath.lch.initialmargin.swapclear.products.indices.LchFixedCoupon;
import net.finmath.lch.initialmargin.swapclear.products.indices.LchLIBORIndex;
import net.finmath.lch.initialmargin.swapclear.products.indices.LchLinearCombinationIndex;
import net.finmath.lch.initialmargin.swapclear.sensitivities.DiscountFactor.DiscountDate;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.FloatingpointDate;

/**
 * Analytical reference inputs for the local PAIRS coupon valuation. A linear
 * combination is decomposed into fixed/LIBOR terms, with its scaling carried by
 * the notional. Accrual fraction, forward interval and payment maturity remain
 * distinct. Known coupons, including fixings at the evaluation date, have only
 * discount risk. Notional exchanges and unsupported product types are rejected.
 */
public class SensitivityComponentsForSwapLeg {

    private final List<LchSwapLeg> swapLegs;
    private final ZeroRateModel forwardCurveModel;
    private final ZeroRateModel discountCurveModel;
    private List<SensitivityComponent> sensitivityComponents;
    private LocalDateTime lastUsedEvaluationDate;

    public SensitivityComponentsForSwapLeg(List<LchSwapLeg> swapLegs, ZeroRateModel forwardCurveModel,
            ZeroRateModel discountCurveModel) {
        this.swapLegs = List.copyOf(Objects.requireNonNull(swapLegs, "swapLegs"));
        this.forwardCurveModel = Objects.requireNonNull(forwardCurveModel, "forwardCurveModel");
        this.discountCurveModel = Objects.requireNonNull(discountCurveModel, "discountCurveModel");
        for (LchSwapLeg leg : this.swapLegs) {
            for (AbstractProductComponent component : leg.getComponents().getProducts()) {
                if (!(component instanceof LchPeriod)) {
                    throw new IllegalArgumentException("Analytical swap deltas require LchPeriod components.");
                }
                LchPeriod period = (LchPeriod) component;
                if (period.isNotionalFlow() || period.isExcludeAccruedInterest()) {
                    throw new IllegalArgumentException("Analytical local deltas do not support notional exchanges or accrued-interest exclusion.");
                }
                if (!(period.getNotional() instanceof LchNotionalFromConstant)) {
                    throw new IllegalArgumentException("Analytical local deltas require constant notionals.");
                }
                if (period.isCouponFlow()) {
                    validateIndex(period.getLchIndex());
                }
            }
        }
    }

    private static void validateIndex(LchAbstractIndex index) {
        Objects.requireNonNull(index, "index");
        if (index.getClass() == LchLinearCombinationIndex.class) {
            LchLinearCombinationIndex combination = (LchLinearCombinationIndex) index;
            if (!Double.isFinite(combination.getScaling1()) || !Double.isFinite(combination.getScaling2())) {
                throw new IllegalArgumentException("Index scalings must be finite.");
            }
            validateIndex(combination.getIndex1());
            validateIndex(combination.getIndex2());
        } else if (index.getClass() != LchLIBORIndex.class && index.getClass() != LchFixedCoupon.class) {
            throw new IllegalArgumentException("Unsupported analytical coupon index: " + index.getClass().getName());
        }
    }

    /** Returns the coupon terms for all simulation paths, without path averaging. */
    public synchronized List<SensitivityComponent> getSensitivityComponents(LocalDateTime evaluationDate)
            throws CalculationException {
        Objects.requireNonNull(evaluationDate, "evaluationDate");
        if (!evaluationDate.equals(evaluationDate.toLocalDate().atStartOfDay())) {
            throw new IllegalArgumentException("Daily sensitivities require an evaluation date at midnight.");
        }
        if (evaluationDate.equals(lastUsedEvaluationDate)) {
            return sensitivityComponents;
        }
        List<SensitivityComponent> result = new ArrayList<>();
        for (LchSwapLeg leg : swapLegs) {
            double scheduleOffset = FloatingpointDate.getFloatingPointDateFromDate(leg.getReferenceDate(), evaluationDate);
            for (AbstractProductComponent component : leg.getComponents().getProducts()) {
                LchPeriod period = (LchPeriod) component;
                LocalDateTime paymentDate = FloatingpointDate.getDateFromFloatingPointDate(
                        leg.getReferenceDate(), period.getPaymentDate());
                if (!period.isCouponFlow() || !paymentDate.isAfter(evaluationDate)) {
                    continue;
                }
                LocalDateTime fixingDate = FloatingpointDate.getDateFromFloatingPointDate(
                        leg.getReferenceDate(), period.getFixingDate());
                DiscountFactor payment = getDiscountFactor(evaluationDate,
                        period.getPaymentDate() - scheduleOffset, discountCurveModel);
                addIndexComponents(result, period, period.getLchIndex(), 1.0, evaluationDate, fixingDate, payment);
            }
        }
        sensitivityComponents = List.copyOf(result);
        lastUsedEvaluationDate = evaluationDate;
        return sensitivityComponents;
    }

    private void addIndexComponents(List<SensitivityComponent> result, LchPeriod period,
            LchAbstractIndex index, double scaling, LocalDateTime evaluationDate,
            LocalDateTime fixingDate, DiscountFactor payment) throws CalculationException {
        if (index instanceof LchLinearCombinationIndex) {
            LchLinearCombinationIndex combination = (LchLinearCombinationIndex) index;
            addIndexComponents(result, period, combination.getIndex1(), scaling * combination.getScaling1(),
                    evaluationDate, fixingDate, payment);
            addIndexComponents(result, period, combination.getIndex2(), scaling * combination.getScaling2(),
                    evaluationDate, fixingDate, payment);
            return;
        }
        Map<DiscountDate, DiscountFactor> factors = new EnumMap<>(DiscountDate.class);
        factors.put(DiscountDate.PAYMENT, payment);
        if (index instanceof LchLIBORIndex && fixingDate.isAfter(evaluationDate)) {
            LchLIBORIndex libor = (LchLIBORIndex) index;
            double length = libor.getPeriodLength(forwardCurveModel, fixingDate);
            double start = FloatingpointDate.getFloatingPointDateFromDate(evaluationDate, fixingDate)
                    + libor.getPeriodStartOffset();
            if (!Double.isFinite(length) || length <= 0.0) {
                throw new IllegalArgumentException("LIBOR period length must be finite and positive.");
            }
            factors.put(DiscountDate.PERIOD_START, getDiscountFactor(evaluationDate, start, forwardCurveModel));
            factors.put(DiscountDate.PERIOD_END, getDiscountFactor(evaluationDate, start + length, forwardCurveModel));
        }
        RandomVariable rate = index.getValue(evaluationDate, fixingDate, forwardCurveModel, null, null).getValues();
        result.add(new SensitivityComponent(factors, rate, period.getDaycountFraction(),
                period.getNotional().getConstantNotional().getValues().mult(scaling), period.isPayer()));
    }

    private static DiscountFactor getDiscountFactor(LocalDateTime evaluationDate, double maturity,
            ZeroRateModel model) throws CalculationException {
        return new DiscountFactor(model.getDiscountFactor(evaluationDate, maturity, null, null).getValues(), maturity);
    }
}
