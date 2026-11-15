package dev.mintixx.profithud.core;

import dev.mintixx.profithud.config.ConfigManager;
import dev.mintixx.profithud.config.ProfitConfig;
import dev.mintixx.profithud.util.NumberFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Turns raw chat lines into money events using user-editable regex patterns.
 * Nothing is assumed about a server's format: every pattern lives in the config.
 */
public final class ChatParser {
	public enum Kind { INCOMING, OUTGOING, BALANCE, NONE }

	public record Result(Kind kind, double amount, String player, String pattern) {
		public static final Result NONE = new Result(Kind.NONE, 0, "", "");
	}

	private final ConfigManager cm;
	private int compiledFor = -1;
	private final List<Pattern> incoming = new ArrayList<>();
	private final List<Pattern> outgoing = new ArrayList<>();
	private final List<Pattern> balance = new ArrayList<>();
	private final List<String> errors = new ArrayList<>();
	private Pattern standaloneSellReceipt;
	private Pattern fallbackAmount;

	public ChatParser(ConfigManager cm) { this.cm = cm; }

	public List<String> errors() { ensureCompiled(); return errors; }

	/** Builds the {AMOUNT} token with common abbreviated and written economy suffixes. */
	static String amountRegex(ProfitConfig c) {
		StringBuilder sym = new StringBuilder();
		for (String s : c.currencySymbols) {
			if (s == null || s.isBlank()) continue;
			if (sym.length() > 0) sym.append('|');
			sym.append(Pattern.quote(s.trim()));
		}
		String cur = sym.length() == 0 ? "" : "(?:" + sym + ")?";
		String suffix = "(?:\\s?(?:quadrillions?|quadr|qd|q|trillions?|tril|tn|t|billions?|bil|bn|b|millions?|mill|mil|m|thousands?|thou|thousand|k|crores?|cr|lakhs?|lacs?|lakh|hundred|h))?";
		return "(?<amount>" + cur + "\\s?\\d[\\d,]*(?:\\.\\d+)?\\s?" + suffix + "(?![A-Za-z0-9]))";
	}

	private void compileInto(List<String> src, List<Pattern> dst, String amount, String label) {
		dst.clear();
		for (String s : src) {
			if (s == null || s.isBlank()) continue;
			try {
				dst.add(Pattern.compile(s.replace("{AMOUNT}", amount)));
			} catch (PatternSyntaxException e) {
				errors.add(label + ": " + e.getDescription() + " in '" + s + "'");
			}
		}
	}

	private void ensureCompiled() {
		if (compiledFor == cm.changeCounter()) return;
		compiledFor = cm.changeCounter();
		ProfitConfig c = cm.get();
		errors.clear();
		String amount = amountRegex(c);
		compileInto(c.incomingPatterns, incoming, amount, "incoming");
		compileInto(c.outgoingPatterns, outgoing, amount, "outgoing");
		compileInto(c.balancePatterns, balance, amount, "balance");
		standaloneSellReceipt = Pattern.compile("^[^A-Za-z0-9]*" + amount + "[.!]*$");
		fallbackAmount = Pattern.compile(amount);
	}

	private static Result tryMatch(List<Pattern> list, String line, Kind kind) {
		for (Pattern p : list) {
			Matcher m = p.matcher(line);
			if (!m.find()) continue;
			double amt;
			try {
				amt = NumberFormatter.parseAmount(m.group("amount"));
			} catch (IllegalArgumentException noGroup) {
				continue; // user pattern has no (?<amount>...) group and no {AMOUNT}
			}
			if (Double.isNaN(amt) || amt < 0 || (amt == 0 && kind != Kind.BALANCE)) continue;
			String player = "";
			try {
				String g = m.group("player");
				if (g != null) player = g;
			} catch (IllegalArgumentException ignored) { }
			return new Result(kind, amt, player, p.pattern());
		}
		return null;
	}

	/** Pure function: no state is changed. Used by both the live tracker and the "Test message" tool. */
	public Result parse(String line) {
		ensureCompiled();
		if (line == null) return Result.NONE;
		String s = line.strip();
		if (s.isEmpty()) return Result.NONE;
		Result r = tryMatch(incoming, s, Kind.INCOMING);
		if (r != null) return r;
		r = tryMatch(outgoing, s, Kind.OUTGOING);
		if (r != null) return r;
		r = tryMatch(balance, s, Kind.BALANCE);
		if (r != null) return r;
		return inferTransaction(s);
	}

	private Result inferTransaction(String line) {
		String text = line.replaceAll("(?i)§[0-9A-FK-OR]", "").toLowerCase(java.util.Locale.ROOT);
		boolean incoming = Pattern.compile("\\b(earned|received|sold|sale complete|payout|credited|reward|got paid|(?:were|was) paid|paid you|sent you|gave you|transferred to you)\\b")
			.matcher(text).find();
		incoming |= Pattern.compile("\\bauction\\b.*\\b(?:sold|completed|ended)\\b").matcher(text).find();
		incoming |= Pattern.compile("\\b(?:paid|sent|gave|transferred)\\b.*\\bto you\\b").matcher(text).find();
		boolean outgoing = Pattern.compile("\\b(bought|purchased|purchase|spent|withdrawn|withdrawal|charged|deducted|debited|paid|sent|transferred)\\b")
			.matcher(text).find();
		if (!incoming && !outgoing) return Result.NONE;
		if (incoming && outgoing) {
			int incomingAt = lastIndexOfAny(text, "earned", "received", "sold", "sale complete", "payout", "credited", "reward", "got paid", "were paid", "was paid", "paid you", "sent you", "gave you", "transferred to you", "to you");
			int outgoingAt = lastIndexOfAny(text, "bought", "purchased", "purchase", "spent", "withdrawn", "withdrawal", "charged", "deducted", "debited", "paid", "sent", "transferred");
			if (incomingAt < outgoingAt && !text.contains("paid you") && !text.contains("sent you")) return Result.NONE;
			outgoing = incomingAt < outgoingAt;
			incoming = !outgoing;
		}
		Matcher matcher = fallbackAmount.matcher(line);
		double amount = Double.NaN;
		while (matcher.find()) amount = NumberFormatter.parseAmount(matcher.group("amount"));
		if (!Double.isFinite(amount) || amount <= 0) return Result.NONE;
		Kind kind = incoming ? Kind.INCOMING : Kind.OUTGOING;
		return new Result(kind, amount, "", "automatic transaction detection");
	}

	private static int lastIndexOfAny(String text, String... words) {
		int result = -1;
		for (String word : words) result = Math.max(result, text.lastIndexOf(word));
		return result;
	}

	/** Parses amount-only sale feedback; callers must first confirm the player ran /sell. */
	public Result parseSellReceipt(String line) {
		ensureCompiled();
		if (line == null) return Result.NONE;
		Matcher matcher = standaloneSellReceipt.matcher(line.strip());
		if (!matcher.matches()) return Result.NONE;
		double amount = NumberFormatter.parseAmount(matcher.group("amount"));
		return Double.isFinite(amount) && amount > 0
			? new Result(Kind.INCOMING, amount, "", standaloneSellReceipt.pattern())
			: Result.NONE;
	}
}
