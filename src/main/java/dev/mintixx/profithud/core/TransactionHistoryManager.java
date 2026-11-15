package dev.mintixx.profithud.core;

import dev.mintixx.profithud.data.Transaction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Recent confirmed transactions (view over the current session) plus the raw-chat debug buffer. */
public final class TransactionHistoryManager {
	public record RawLine(long time, String text, ChatParser.Kind kind, double amount, String player) { }

	private static final int RAW_MAX = 120;
	private final Deque<RawLine> raw = new ArrayDeque<>();
	private final SessionManager sessions;

	public TransactionHistoryManager(SessionManager sessions) { this.sessions = sessions; }

	public synchronized void addRaw(RawLine line) {
		raw.addFirst(line);
		while (raw.size() > RAW_MAX) raw.removeLast();
	}

	public synchronized void clearRaw() { raw.clear(); }

	/** Newest first. */
	public synchronized List<RawLine> rawLines(int max) {
		List<RawLine> out = new ArrayList<>();
		for (RawLine r : raw) { if (out.size() >= max) break; out.add(r); }
		return out;
	}

	/** Newest first. */
	public List<Transaction> recent(int max) {
		List<Transaction> all = sessions.current().transactions;
		List<Transaction> out = new ArrayList<>();
		for (int i = all.size() - 1; i >= 0 && out.size() < max; i--) out.add(all.get(i));
		return out;
	}
}
