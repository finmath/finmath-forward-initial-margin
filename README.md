# finmath-forward-initial-margin

The goal of this project is to use finmath's automatic differentiation capabalities to calculate forward sensitivities which can be used to simulate quantities that rely on sensitivities, for example regulatory figures like SA-CCR or SIMM.

## LCH PAIRS for swaps

The LCH calculation uses pathwise analytical swap sensitivities. Its scenario
sources and model setup are based on commit `1638ae99`; the corrected floating
coupon derivatives and index scaling from `e38642fe` are retained. The LCH AAD
providers and generic curve-valuation experiment have been removed. The separate
ISDA-SIMM implementation is unchanged.

`ForwardAndDiscountSensitivities` supplies delta in currency/bp and the full
symmetric gamma matrix in currency/bp² on the 37-point LCH risk grid. Gamma
includes the mixed projection/discount derivatives. Both providers use the
current pathwise curve, hold known fixings fixed and exclude payments on or
before the evaluation date. Coupon accrual, forward period and payment maturity
remain distinct.

`PairsInitialMargin` ranks scenarios with `delta * s + 0.5 * s^T * gamma * s`,
where `s` is the zero-rate shock vector in basis points. It fully revalues the
20 selected scenarios on the complete shock grid, reranks their P&Ls, and
calculates base ES6 and floor VaR (13th worst P&L). PAIRS is their pathwise
maximum; gains alone do not generate positive margin. Historical and simulated
observations support both frozen and moving scenario windows. `LocalPortfolio`,
`MarginValuationAdjustment` and `InitialMarginExperiments` use this shared logic.

The implemented scope is the single-curve PAIRS core for constant-notional
swaps with fixed/LIBOR coupons and linear combinations of these indices.
Notional exchanges, accrued-interest exclusion and LCH add-ons are outside
this scope. The common EUR 3M curve remains the current simplification.

The analytical derivatives are checked against independent bucket and mixed
curve bumps, including off-grid maturities, fixing boundaries and stochastic
paths. Run the focused regression tests with:

```text
mvn -Dtest=AnalyticZeroRateSensitivitiesTest,AnalyticInitialMarginIntegrationTest,LchLinearCombinationIndexTest test
```
