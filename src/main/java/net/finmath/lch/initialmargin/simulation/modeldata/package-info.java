/**
 * Daily zero-rate observations for the common PAIRS risk curve.
 * HistoricalZeroRateSource snapshots deterministic input curves;
 * SimulatedZeroRateSource derives the same kind of curve from a generic term
 * structure simulation. Neither source is tied to a three-month index.
 * ZeroRateModel composes the sources, checks their initial alignment, and supplies
 * complete constant-tenor histories to the scenario transformations.
 *
 * <p>The reference date belongs to the historical source when one is supplied.
 * Every later observation comes from the simulation, even if the input file
 * contains later historical dates. Model observations must lie on the simulation
 * grid; no interpolation across future states is performed. The observation
 * calendar is explicit (the bundled EUR history uses all Monday-Friday dates).
 * Sources must share units, tenor conventions and curve identity. Simulated
 * paths retain their original ordering and must also be used for valuation.</p>
 *
 * <p>The legacy ZeroRateModel pricing methods use this single curve for both
 * forwards and discounting. OIS and tenor-basis add-ons are not implemented here.
 * Pure simulations require sufficient observations before a scenario window can
 * be formed; they do not synthesize historical stress periods.</p>
 * 
 * @author Raphael Prandtl
 */
package net.finmath.lch.initialmargin.simulation.modeldata;
