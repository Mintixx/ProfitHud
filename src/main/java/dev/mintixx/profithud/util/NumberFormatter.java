package dev.mintixx.profithud.util;

import dev.mintixx.profithud.config.ProfitConfig;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Money formatting (K/M/B/T) and amount parsing. */
public final class NumberFormatter {
	private static final Pattern AMOUNT_TOKEN = Pattern.compile(
		"(?i)(\\d+(?:\\.\\d+)?|\\.\\d+)\\s*(quadrillions?|quadr|qd|q|trillions?|tril|tn|t|billions?|bil|bn|b|millions?|mill|mil|m|thousands?|thou|thousand|k|crores?|cr|lakhs?|lacs?|lakh|hundred|h)?\\b");

	private NumberFormatter() {}

	private static String trim(String s) {
		if (s.indexOf('.') < 0) return s;
		int end = s.length();
		while (end > 0 && s.charAt(end - 1) == '0') end--;
		if (end > 0 && s.charAt(end - 1) == '.') end--;
		return s.substring(0, end);
	}

	/** Compact form without sign or currency: 1.5M, 320K, 12. */
	public static String compact(double v, int decimals) {
		double a = Math.abs(v);
		String suffix = "";
		double d = a;
		if (a >= 1e12) { d = a / 1e12; suffix = "T"; }
		else if (a >= 1e9) { d = a / 1e9; suffix = "B"; }
		else if (a >= 1e6) { d = a / 1e6; suffix = "M"; }
		else if (a >= 1e3) { d = a / 1e3; suffix = "K"; }
		int dec = Math.max(0, Math.min(4, decimals));
		return trim(String.format(Locale.ROOT, "%." + dec + "f", d)) + suffix;
	}

	/** Full form with thousands separators: 1,250,000. */
	public static String full(double v, int decimals) {
		int dec = Math.max(0, Math.min(4, decimals));
		StringBuilder p = new StringBuilder("#,##0");
		if (dec > 0) { p.append('.'); p.append("0".repeat(dec)); }
		DecimalFormat df = new DecimalFormat(p.toString(), DecimalFormatSymbols.getInstance(Locale.ROOT));
		return trim(df.format(Math.abs(v)));
	}

	public static String money(double v, ProfitConfig c, boolean signed) {
		if (Double.isNaN(v)) return "N/A";
		String body = c.shortNumbers ? compact(v, c.decimals) : full(v, c.decimals);
		String sign = "";
		if (signed) sign = v > 0.0000001 ? "+" : (v < -0.0000001 ? "-" : "");
		else if (v < 0) sign = "-";
		String cur = c.currencySymbol == null ? "" : c.currencySymbol;
		return c.currencyBefore ? sign + cur + body : sign + body + cur;
	}

	public static String duration(long ms) {
		long s = Math.max(0, ms / 1000);
		long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
		return h > 0 ? String.format(Locale.ROOT, "%dh %02dm %02ds", h, m, sec)
			: String.format(Locale.ROOT, "%dm %02ds", m, sec);
	}

	/**
	 * Parses amounts such as "$500K", "1.5M", "1,234.56", "2b". Returns NaN when it cannot parse.
	 * Thousands separators are assumed to be commas.
	 */
	public static double parseAmount(String raw) {
		if (raw == null) return Double.NaN;
		Matcher matcher = AMOUNT_TOKEN.matcher(raw.trim().replace(",", "").replace("_", ""));
		if (!matcher.find()) return Double.NaN;
		try {
			double value = Double.parseDouble(matcher.group(1));
			String suffix = matcher.group(2);
			double multiplier = 1;
			if (suffix != null) {
				String normalized = suffix.toLowerCase(Locale.ROOT);
				if (normalized.startsWith("q")) multiplier = 1e15;
				else if (normalized.startsWith("thou")) multiplier = 1e3;
				else if (normalized.startsWith("t")) multiplier = 1e12;
				else if (normalized.startsWith("b")) multiplier = 1e9;
				else if (normalized.startsWith("m")) multiplier = 1e6;
				else if (normalized.startsWith("c")) multiplier = 1e7;
				else if (normalized.startsWith("l")) multiplier = 1e5;
				else if (normalized.startsWith("h")) multiplier = 1e2;
				else if (normalized.startsWith("k")) multiplier = 1e3;
			}
			return value * multiplier;
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}
}
