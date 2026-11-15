package dev.mintixx.profithud.core;

import dev.mintixx.profithud.config.ConfigManager;
import dev.mintixx.profithud.config.ProfitConfig;
import dev.mintixx.profithud.data.Transaction;
import dev.mintixx.profithud.hud.Animations;

import java.util.Locale;

/**
 * Applies parsed chat events to the current session.
 * ONLY chat-confirmed payments reach the session statistics. Inventory estimates have no path in here.
 */
public final class EconomyTracker {
	private final ConfigManager cm;
	private final ChatParser parser;
	private final SessionManager sessions;
	private final TransactionHistoryManager history;
	private final Animations anim;

	private double balance = Double.NaN;
	private String lastRaw = "";
	private long lastRawMs = 0;

	public EconomyTracker(ConfigManager cm, ChatParser parser, SessionManager sessions,
						  TransactionHistoryManager history, Animations anim) {
		this.cm = cm;
		this.parser = parser;
		this.sessions = sessions;
		this.history = history;
		this.anim = anim;
	}

	/** NaN when the server never exposed a balance message that matched. Never guessed. */
	public double balance() { return balance; }
	public void clearBalance() { balance = Double.NaN; }
	public void setBalance(double value) {
		if (Double.isFinite(value) && value >= 0) balance = value;
	}

	/** Entry point for every received chat/system line. */
	public boolean onChat(String text) {
		if (text == null) return false;
		String line = text.strip();
		if (line.isEmpty()) return false;
		ProfitConfig c = cm.get();
		long now = System.currentTimeMillis();
		if (line.equals(lastRaw) && now - lastRawMs < c.dedupeMs) return false; // same line delivered twice
		lastRaw = line;
		lastRawMs = now;

		ChatParser.Result r = parser.parse(line);
		if (c.rawChatDebug) history.addRaw(new TransactionHistoryManager.RawLine(now, line, r.kind(), r.amount(), r.player()));

		switch (r.kind()) {
			case BALANCE -> setBalance(r.amount());
			case INCOMING, OUTGOING -> {
				if (isIgnored(r.player(), c)) return false;
				boolean in = r.kind() == ChatParser.Kind.INCOMING;
				recordTransaction(now, in, r.amount(), r.player(), line);
				return true;
			}
			default -> { }
		}
		return false;
	}

	public boolean onSellReceipt(String text) {
		ChatParser.Result result = parser.parseSellReceipt(text);
		if (result.kind() != ChatParser.Kind.INCOMING) return false;
		recordTransaction(System.currentTimeMillis(), true, result.amount(), "", text.strip());
		return true;
	}

	private void recordTransaction(long time, boolean incoming, double amount, String player, String raw) {
		sessions.current().record(new Transaction(time, incoming, amount, player, raw));
		sessions.markDirty();
		if (!Double.isNaN(balance)) balance = Math.max(0, balance + (incoming ? amount : -amount));
		anim.onPayment(incoming);
	}

	private static boolean isIgnored(String player, ProfitConfig c) {
		if (player == null || player.isEmpty()) return false;
		String p = player.toLowerCase(Locale.ROOT);
		for (String s : c.ignoredPlayers) {
			if (s != null && s.strip().toLowerCase(Locale.ROOT).equals(p)) return true;
		}
		return false;
	}
}
