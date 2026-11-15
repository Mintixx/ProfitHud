package dev.mintixx.profithud.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Loads/saves JSON files inside .minecraft/config/profithud/. */
public final class ConfigManager {
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type PROFILES_TYPE = new TypeToken<LinkedHashMap<String, ServerProfile>>() { }.getType();

	private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("profithud");
	private ProfitConfig config = new ProfitConfig();
	private ProfitConfig defaultConfig = new ProfitConfig();
	private Map<String, ServerProfile> serverProfiles = new LinkedHashMap<>();
	private String activeServer = "";
	private int changeCounter = 0;

	public static Path dir() {
		try { Files.createDirectories(DIR); } catch (IOException ignored) { }
		return DIR;
	}

	public ProfitConfig get() { return config; }
	public String activeServerProfile() { return activeServer.isEmpty() ? "Default" : activeServer; }

	/** Bumped whenever settings are saved/reset; lets caches (regex etc.) know to rebuild. */
	public int changeCounter() { return changeCounter; }
	public void touch() { changeCounter++; }

	public void load() {
		Path p = dir().resolve("config.json");
		if (Files.exists(p)) {
			try {
				ProfitConfig c = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), ProfitConfig.class);
				if (c != null) config = c;
			} catch (Exception e) {
				System.err.println("[PROFIT HUD] Could not read config.json, using defaults: " + e);
				try { Files.copy(p, dir().resolve("config.json.broken"), StandardCopyOption.REPLACE_EXISTING); } catch (IOException ignored) { }
			}
		}
		sanitize();
		defaultConfig = copyConfig(config);
		loadServerProfiles();
		save();
	}

	private void loadServerProfiles() {
		Path p = dir().resolve("server-profiles.json");
		if (!Files.exists(p)) return;
		try {
			Map<String, ServerProfile> loaded = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), PROFILES_TYPE);
			if (loaded != null) serverProfiles = new LinkedHashMap<>(loaded);
		} catch (Exception e) {
			System.err.println("[PROFIT HUD] Could not read server-profiles.json: " + e);
		}
	}

	/** Switches only colors and economy parsing settings; layout and display preferences remain global. */
	public void switchServerProfile(String serverId) {
		String next = serverId == null || serverId.isBlank() ? "" : serverId.strip().toLowerCase(Locale.ROOT);
		if (next.equals(activeServer)) return;
		if (!activeServer.isEmpty()) {
			serverProfiles.put(activeServer, ServerProfile.from(config));
			saveServerProfiles();
		}
		activeServer = next;
		if (activeServer.isEmpty()) {
			config.copyAllFrom(defaultConfig);
		} else {
			config.copyAllFrom(defaultConfig);
			ServerProfile profile = serverProfiles.get(activeServer);
			if (profile == null) {
				profile = ServerProfile.from(defaultConfig);
				serverProfiles.put(activeServer, profile);
				saveServerProfiles();
			}
			profile.applyTo(config);
		}
		touch();
		save();
	}

	private void saveServerProfiles() {
		writeJson(dir().resolve("server-profiles.json"), serverProfiles);
	}

	private static ProfitConfig copyConfig(ProfitConfig source) {
		ProfitConfig copy = new ProfitConfig();
		copy.copyAllFrom(source);
		copy.version = source.version;
		return copy;
	}

	private static void preserveGlobalProfileSettings(ProfitConfig global, ProfitConfig current) {
		ServerProfile defaults = ServerProfile.from(global);
		global.copyAllFrom(current);
		defaults.applyTo(global);
		global.version = current.version;
	}

	public void sanitize() {
		ProfitConfig d = new ProfitConfig();
		if (config.incomingPatterns == null) config.incomingPatterns = d.incomingPatterns;
		if (config.outgoingPatterns == null) config.outgoingPatterns = d.outgoingPatterns;
		if (config.balancePatterns == null) config.balancePatterns = d.balancePatterns;
		if (config.ignoredPlayers == null) config.ignoredPlayers = d.ignoredPlayers;
		if (config.currencySymbols == null) config.currencySymbols = d.currencySymbols;
		if (config.version < 2) {
			appendMissing(config.incomingPatterns, ProfitConfig.MIGRATED_INCOMING_PATTERNS);
			appendMissing(config.outgoingPatterns, ProfitConfig.MIGRATED_OUTGOING_PATTERNS);
			appendMissing(config.balancePatterns, ProfitConfig.MIGRATED_BALANCE_PATTERNS);
			config.version = 2;
		}
		if (config.version < 3) {
			appendMissing(config.incomingPatterns, ProfitConfig.MIGRATED_INCOMING_PATTERNS);
			config.version = 3;
		}
		if (config.version < 4) {
			appendMissing(config.incomingPatterns, ProfitConfig.MIGRATED_INCOMING_PATTERNS);
			config.version = 4;
		}
		if (config.version < 5) {
			appendMissing(config.incomingPatterns, ProfitConfig.MIGRATED_INCOMING_PATTERNS);
			config.version = 5;
		}
		if (config.title == null) config.title = d.title;
		if (config.currencySymbol == null) config.currencySymbol = d.currencySymbol;
		config.anchor = Math.floorMod(config.anchor, 9);
		config.hudLayoutPreset = Math.floorMod(config.hudLayoutPreset, 3);
		config.scale = Math.max(0.5f, Math.min(3f, config.scale));
		config.priceToastX = Math.max(-1, Math.min(4000, config.priceToastX));
		config.priceToastY = Math.max(0, Math.min(4000, config.priceToastY));
		config.priceToastScale = Math.max(0.6f, Math.min(2f, config.priceToastScale));
		config.textScale = Math.max(0.5f, Math.min(2f, config.textScale));
		config.decimals = Math.max(0, Math.min(4, config.decimals));
		config.padding = Math.max(0, Math.min(24, config.padding));
		config.cornerRadius = Math.max(0, Math.min(8, config.cornerRadius));
		config.borderThickness = Math.max(0, Math.min(4, config.borderThickness));
		config.panelOpacity = Math.max(0f, Math.min(1f, config.panelOpacity));
		config.width = Math.max(60, Math.min(400, config.width));
		config.height = Math.max(0, Math.min(400, config.height));
		config.dedupeMs = Math.max(0, Math.min(2000, config.dedupeMs));
		config.priceAlertPercent = Math.max(0, Math.min(1000, config.priceAlertPercent));
	}

	private static void appendMissing(java.util.List<String> target, java.util.List<String> defaults) {
		for (String pattern : defaults) if (!target.contains(pattern)) target.add(pattern);
	}

	public void save() {
		sanitize();
		touch();
		if (activeServer.isEmpty()) {
			defaultConfig = copyConfig(config);
		} else {
			preserveGlobalProfileSettings(defaultConfig, config);
			serverProfiles.put(activeServer, ServerProfile.from(config));
			saveServerProfiles();
		}
		writeJson(dir().resolve("config.json"), defaultConfig);
	}

	public void resetAll() {
		config = new ProfitConfig();
		save();
	}

	/** Atomic JSON write (temp file then move) so a crash can never leave half a file. */
	public static void writeJson(Path target, Object data) {
		try {
			Files.createDirectories(target.getParent());
			Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
			Files.writeString(tmp, GSON.toJson(data), StandardCharsets.UTF_8);
			try {
				Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			System.err.println("[PROFIT HUD] Failed to write " + target + ": " + e);
		}
	}
}
