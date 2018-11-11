# SIMM/CRIF specifics and their implementation in ${project.name}

## CRIF and SIMM mapping

The correspondence of CRIF columns and SIMM quantities has some edge cases.

Note that for IR the buckets in the SIMM formulae are the currencies, but in CRIF `Bucket` is an integer indicating the currency volatility.

_-_ indicates unused fields. _?_ indicates that the official documentation misses this case; it appears to be unused.

| For |RiskType|Qualifier|Bucket|Label1|Label2|
| ------------- | ------- | ---- | ---- | ---- | --- |
| IR Delta | `Risk_IRCurve` | Currency | `1` (regular volatility), `2` (low volatility) or `3` (high volatility) | Tenor | Sub-curve name |
| IR Vega  | `Risk_IRVol`   | Currency | - | Tenor | - |
| FX Delta | `Risk_FX` | Currency | ? | - | - |
| FX Vega  | `Risk_FXVol` | Currency pair | ? | Tenor | - |
| Equity Delta | `Risk_Equity` | ISIN or index | `1`–`4` (large cap EM by sector), `5`–`8` (large cap DM by sector), `9`–`10` (small cap EM and DM), `11` (indexes, funds and ETFs) or `Residual` (`12` for volatility indexes is referenced in SIMM but not for CRIF) | - | - |
| Equity Vega | `Risk_EquityVol` | ISIN or index | `1`-`11` (`12`) or `Residual` | Tenor | - |
| Commodity Delta | `Risk_Commodity` | Name | `1`-`16` | - | - |
| Commodity Vega | `Risk_CommodityVol` | Name | `1`-`16` | Tenor | - |

### How we map this to `Simm2Coordinate`

Since _Label1_ always refers to a vertex, we mapped it to `vertex`.

We map _Qualifier_ to `qualifier`, which is of a special type that facilitates currency and currency pair parsing.

_Bucket_ is mapped to `bucketKey` and the IR schemes will not use it for bucketing.

## Currency groupings

Currencies are often grouped when distinguishing cases.

### IR

- low volatility: JPY
- regular volatility
    - well traded: USD, EUR und GBP
    - less well traded: AUD, CAD, CHF, DKK, HKD, KRW, NOK, NZD, SEK, SGD und TWD
- high volatility: all other currencies

### FX

- Category 1: USD, EUR, JPY, GBP, AUD, CHF und CAD
- Category 2: BRL, CNY, HKD, INR, KRW, MXN, NOK, NZD, RUB, SEK, SGD, TRY und ZAR
- Category 3: all other currencies