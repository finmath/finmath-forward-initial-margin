/**
 * Construction and selection of LCH scenarios from supplied zero-rate histories.
 *
 * <p>For each constant-tenor series the source must provide consecutive business-day
 * observations. Historical scalar values may be followed by simulated path vectors;
 * path ordering must be consistent across dates and tenors. Obtaining aligned daily
 * model rates and constructing the historical-to-model transition belongs to the
 * data source, not to this package.</p>
 *
 * <p>Returns are labelled by their end date. The evaluation-date cutoff is applied
 * before any transformation. EWMA uses the complete available return history and
 * the existing first-date seed convention; rolling windows are selected afterwards.
 * Moving to a later evaluation date updates the current volatility and therefore
 * also rescales retained ES scenarios. Fixed stress intervals refer to return end
 * dates, and overlapping rolling/stress scenarios are included only once.</p>
 *
 * @author Raphael Prandtl
 */
package net.finmath.lch.initialmargin.simulation.scenarios;
