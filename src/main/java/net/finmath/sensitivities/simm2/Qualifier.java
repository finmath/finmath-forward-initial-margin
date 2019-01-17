package net.finmath.sensitivities.simm2;

import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Represents the additional of a SIMM coordinate which corresponds to the CRIF qualifier.
 */
public class Qualifier {
	private String text;

	public Qualifier(String text) {
		this.text = text;
	}

	/**
	 * In case the qualifier represents a currency pair return a pair of ISO currency codes.
	 * @return A pair of uppercase three-letter currency codes.
	 */
	public Pair<String, String> getCurrencyPair() {
		return Pair.of(text.substring(0, 3).toUpperCase(), text.substring(3, 6).toUpperCase());
	}

	/**
	 * In case the qualifier represents a currency return a standard ISO currency code.
	 * @return An uppercase three-letter currency code.
	 */
	public String getCurrency() {
		return text.toUpperCase();
	}

	/**
	 * Returns the raw textual CRIF qualifier.
	 * @return A string whose meaning is context-dependent.
	 */
	public String getText() {
		return text;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Qualifier qualifier = (Qualifier) o;
		return Objects.equals(text, qualifier.text);
	}

	@Override
	public int hashCode() {
		return Objects.hash(text);
	}

	@Override
	public String toString() {
		return text;
	}
}
