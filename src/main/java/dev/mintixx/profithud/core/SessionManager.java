package dev.mintixx.profithud.core;

import com.google.gson.reflect.TypeToken;
import dev.mintixx.profithud.config.ConfigManager;
import dev.mintixx.profithud.data.Session;
import dev.mintixx.profithud.data.Transaction;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Owns the current session and the list of previous sessions; persists them to sessions.json. */
public final class SessionManager {
	private static final class Store {
		Session current;
		List<Session> past = new ArrayList<>();
	}

	private static final Type STORE_TYPE = new TypeToken<Store>() { }.getType();

	private final ConfigManager cm;
	private Session current = Session.create();
	private final List<Session> past = new ArrayList<>();
	private boolean dirty = false;
	private long lastSaveMs = 0;
	private long lastTickNs = System.nanoTime();

	public SessionManager(ConfigManager cm) { this.cm = cm; }

	public Session current() { return current; }
	/** Newest first. */
	public List<Session> past() { return past; }
	public void markDirty() { dirty = true; }

	private Path file() { return ConfigManager.dir().resolve("sessions.json"); }

	public void load() {
		Path p = file();
		if (Files.exists(p)) {
			try {
				Store s = ConfigManager.GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), STORE_TYPE);
				if (s != null) {
					if (s.past != null) past.addAll(s.past);
					if (s.current != null) current = s.current;
					if (current.transactions == null) current.transactions = new ArrayList<>();
				}
			} catch (Exception e) {
				System.err.println("[PROFIT HUD] Could not read sessions.json: " + e);
			}
		}
		if (cm.get().autoNewSessionOnLaunch && current.hasActivity()) startNew();
		dirty = true;
		saveNow();
	}

	public void startNew() {
		if (current.hasActivity()) {
			current.endMillis = System.currentTimeMillis();
			past.add(0, current);
		}
		current = Session.create();
		dirty = true;
		saveNow();
	}

	/** Clears the CURRENT session statistics (without archiving). */
	public void resetCurrent() {
		current.clearStats();
		dirty = true;
		saveNow();
	}

	public void delete(long id) {
		past.removeIf(s -> s.id == id);
		dirty = true;
		saveNow();
	}

	/** Called every client tick: accumulates active time (only while in a world) and autosaves. */
	public void tick(Minecraft mc) {
		long now = System.nanoTime();
		long dtMs = (now - lastTickNs) / 1_000_000L;
		lastTickNs = now;
		if (mc.player != null && dtMs > 0 && dtMs < 2000) current.activeMillis += dtMs;
		long wall = System.currentTimeMillis();
		if (dirty && wall - lastSaveMs > 5000) saveNow();
	}

	public void saveNow() {
		Store s = new Store();
		s.current = current;
		s.past = past;
		ConfigManager.writeJson(file(), s);
		dirty = false;
		lastSaveMs = System.currentTimeMillis();
	}

	private static String csv(String s) {
		if (s == null) return "";
		String t = s.replace("\"", "\"\"");
		return (t.contains(",") || t.contains("\"") || t.contains("\n")) ? "\"" + t + "\"" : t;
	}

	/**
	 * Exports sessions + transactions. Only CONFIRMED chat transactions are exported;
	 * estimated inventory value is intentionally absent.
	 */
	public Path exportCsv() throws IOException {
		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
		SimpleDateFormat fileFmt = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT);
		List<Session> all = new ArrayList<>();
		all.add(current);
		all.addAll(past);
		StringBuilder sb = new StringBuilder();
		sb.append("# PROFIT HUD export - confirmed chat transactions only (estimated inventory value is never included)\n");
		sb.append("# SESSIONS\n");
		sb.append("session_id,started,active_minutes,earned,spent,net,highest_received,highest_sent,payments_received,payments_sent\n");
		for (Session s : all) {
			sb.append(s.id).append(',').append(csv(f.format(new Date(s.startMillis)))).append(',')
				.append(String.format(Locale.ROOT, "%.2f", s.activeMillis / 60000.0)).append(',')
				.append(s.earned).append(',').append(s.spent).append(',').append(s.net()).append(',')
				.append(s.highestIn).append(',').append(s.highestOut).append(',')
				.append(s.countIn).append(',').append(s.countOut).append('\n');
		}
		sb.append("# TRANSACTIONS\n");
		sb.append("session_id,time,direction,player,amount,raw_message\n");
		for (Session s : all) {
			for (Transaction t : s.transactions) {
				sb.append(s.id).append(',').append(csv(f.format(new Date(t.time)))).append(',')
					.append(t.incoming ? "received" : "sent").append(',').append(csv(t.player)).append(',')
					.append(t.amount).append(',').append(csv(t.raw)).append('\n');
			}
		}
		Path dir = ConfigManager.dir().resolve("exports");
		Files.createDirectories(dir);
		Path out = dir.resolve("profithud_" + fileFmt.format(new Date()) + ".csv");
		Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
		return out;
	}

	/** Exports only matching transactions from one selected session. */
	public Path exportFilteredCsv(Session session, boolean includeIncoming, boolean includeOutgoing, String query) throws IOException {
		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
		SimpleDateFormat fileFmt = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT);
		String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
		StringBuilder sb = new StringBuilder("# PROFIT HUD filtered confirmed transactions\n");
		sb.append("session_id,time,direction,player,amount,raw_message\n");
		for (Transaction t : session.transactions == null ? List.<Transaction>of() : session.transactions) {
			if ((t.incoming && !includeIncoming) || (!t.incoming && !includeOutgoing)) continue;
			String player = t.player == null ? "" : t.player;
			String raw = t.raw == null ? "" : t.raw;
			if (!needle.isEmpty() && !player.toLowerCase(Locale.ROOT).contains(needle)
				&& !raw.toLowerCase(Locale.ROOT).contains(needle)
				&& !String.valueOf(t.amount).contains(needle)) continue;
			sb.append(session.id).append(',').append(csv(f.format(new Date(t.time)))).append(',')
				.append(t.incoming ? "received" : "sent").append(',').append(csv(player)).append(',')
				.append(t.amount).append(',').append(csv(raw)).append('\n');
		}
		Path dir = ConfigManager.dir().resolve("exports");
		Files.createDirectories(dir);
		Path out = dir.resolve("profithud_filtered_" + fileFmt.format(new Date()) + ".csv");
		Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
		return out;
	}
}
