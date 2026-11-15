package dev.mintixx.profithud.data;

/** One CONFIRMED money transaction detected from a chat message. Never derived from inventory estimates. */
public class Transaction {
	public long time;
	public boolean incoming;
	public double amount;
	public String player;
	public String raw;

	public Transaction() {}

	public Transaction(long time, boolean incoming, double amount, String player, String raw) {
		this.time = time;
		this.incoming = incoming;
		this.amount = amount;
		this.player = player == null ? "" : player;
		this.raw = raw == null ? "" : raw;
	}
}
