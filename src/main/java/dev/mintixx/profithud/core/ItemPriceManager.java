package dev.mintixx.profithud.core;

import com.google.gson.reflect.TypeToken;
import dev.mintixx.profithud.config.ConfigManager;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Last observed item-lore prices. These are estimates and never enter transaction statistics.
 */
public final class ItemPriceManager {
	public static final class Entry {
		public double price;
		public boolean observed;
		public List<PricePoint> history = new ArrayList<>();
	}
	public record PricePoint(long time, double price) { }

	private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, Entry>>() { }.getType();
	private static final int MAX_HISTORY = 64;

	private Map<String, Entry> entries = new LinkedHashMap<>();
	private int version = 0;

	private Path file() { return ConfigManager.dir().resolve("prices.json"); }

	/** Increments on every change so the inventory scanner knows when to recompute. */
	public int version() { return version; }

	public void load() {
		Path p = file();
		if (!Files.exists(p)) return;
		try {
			Map<String, Entry> m = ConfigManager.GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), MAP_TYPE);
			if (m != null) {
				entries = new LinkedHashMap<>(m);
				for (Entry entry : entries.values()) {
					if (entry == null) continue;
					if (entry.history == null) entry.history = new ArrayList<>();
					if (entry.history.isEmpty() && entry.observed && entry.price > 0) {
						entry.history.add(new PricePoint(System.currentTimeMillis(), entry.price));
					}
					while (entry.history.size() > MAX_HISTORY) entry.history.remove(0);
				}
			}
		} catch (Exception e) {
			System.err.println("[PROFIT HUD] Could not read prices.json: " + e);
		}
		version++;
	}

	public void save() { ConfigManager.writeJson(file(), entries); }

	public boolean hasPrice(String id) { return price(id) > 0; }
	public double price(String id) {
		Entry e = entries.get(id);
		return e == null || !e.observed ? 0 : e.price;
	}

	public int configuredCount() {
		int count = 0;
		for (Entry e : entries.values()) if (e != null && e.observed && e.price > 0) count++;
		return count;
	}

	public List<PricePoint> history(String id) {
		Entry entry = entries.get(id);
		return entry == null || entry.history == null ? List.of() : List.copyOf(entry.history);
	}

	/** Records a price read from an item's lore and keeps the last value after the item is removed. */
	public boolean observe(String id, double price) {
		if (!Double.isFinite(price) || price <= 0) return false;
		Entry e = entries.computeIfAbsent(id, k -> new Entry());
		if (e.observed && Double.compare(e.price, price) == 0) return false;
		e.price = price;
		e.observed = true;
		if (e.history == null) e.history = new ArrayList<>();
		e.history.add(new PricePoint(System.currentTimeMillis(), price));
		while (e.history.size() > MAX_HISTORY) e.history.remove(0);
		version++;
		return true;
	}
}
