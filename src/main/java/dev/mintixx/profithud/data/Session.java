package dev.mintixx.profithud.data;

import java.util.ArrayList;
import java.util.List;

/** Confirmed-transaction statistics for one session. Contains no estimated values by design. */
public class Session {
	public static final int MAX_TRANSACTIONS = 20000;

	public long id;
	public long startMillis;
	public long endMillis;
	public long activeMillis;
	public double earned;
	public double spent;
	public double highestIn;
	public double highestOut;
	public int countIn;
	public int countOut;
	public List<Transaction> transactions = new ArrayList<>();

	public Session() {}

	public static Session create() {
		Session s = new Session();
		s.startMillis = System.currentTimeMillis();
		s.id = s.startMillis;
		return s;
	}

	public double net() { return earned - spent; }
	public double avgIn() { return countIn == 0 ? 0 : earned / countIn; }
	public double avgOut() { return countOut == 0 ? 0 : spent / countOut; }
	public double avgAll() { int n = countIn + countOut; return n == 0 ? 0 : (earned + spent) / n; }
	public boolean hasActivity() { return countIn + countOut > 0 || activeMillis > 5000; }

	public double perHour() {
		if (activeMillis < 1000) return 0;
		return net() / (activeMillis / 3_600_000.0);
	}

	public double perMinute() { return perHour() / 60.0; }

	public void record(Transaction t) {
		if (t.incoming) {
			earned += t.amount;
			countIn++;
			highestIn = Math.max(highestIn, t.amount);
		} else {
			spent += t.amount;
			countOut++;
			highestOut = Math.max(highestOut, t.amount);
		}
		transactions.add(t);
		if (transactions.size() > MAX_TRANSACTIONS) transactions.remove(0);
	}

	public void clearStats() {
		earned = spent = highestIn = highestOut = 0;
		countIn = countOut = 0;
		activeMillis = 0;
		transactions.clear();
		startMillis = System.currentTimeMillis();
	}
}
