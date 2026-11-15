package dev.mintixx.profithud.config;

import java.util.ArrayList;
import java.util.List;

/** Every user setting. Serialised as JSON by {@link ConfigManager}. Missing fields fall back to these defaults. */
public class ProfitConfig {
	public static final String[] ANCHOR_NAMES = {
		"Top Left", "Top Center", "Top Right",
		"Middle Left", "Center", "Middle Right",
		"Bottom Left", "Bottom Center", "Bottom Right"};

	public static final String[] PRESET_NAMES = {
		"Turquoise", "Emerald", "Crimson", "Amber", "Violet", "Ice", "Graphite", "Sunset"};

	/** bg, bg2, border, text, label, accent, positive, negative */
	private static final int[][] PRESETS = {
		{0xFF101213, 0xFF181C1D, 0xFF1F3B38, 0xFFE6ECEA, 0xFF8FA3A0, 0xFF1DE9B6, 0xFF3DFF9A, 0xFFFF4D5E},
		{0xFF0D130F, 0xFF152019, 0xFF1E3A28, 0xFFE4EFE7, 0xFF8AA595, 0xFF2ECC71, 0xFF5CFF9D, 0xFFFF5A5A},
		{0xFF140C0E, 0xFF21121A, 0xFF4A1F28, 0xFFF0E4E6, 0xFFA38A90, 0xFFFF3B5C, 0xFF47E08A, 0xFFFF6B81},
		{0xFF14110B, 0xFF211B0F, 0xFF4A3A1A, 0xFFF2EBDD, 0xFFA89A7E, 0xFFFFB300, 0xFF7CE06A, 0xFFFF5B4A},
		{0xFF110D16, 0xFF1B1426, 0xFF3A2A5C, 0xFFEAE4F2, 0xFF9A8FB0, 0xFFA855F7, 0xFF52E8A8, 0xFFFF5C8A},
		{0xFF0C1218, 0xFF121C26, 0xFF1F3A52, 0xFFE4EEF5, 0xFF8AA0B2, 0xFF38BDF8, 0xFF4DF0B4, 0xFFFF5D6C},
		{0xFF121212, 0xFF1C1C1C, 0xFF343434, 0xFFEDEDED, 0xFF9A9A9A, 0xFFE0E0E0, 0xFF6FE39A, 0xFFFF6B6B},
		{0xFF160F0D, 0xFF241512, 0xFF4F2A1E, 0xFFF5E8E2, 0xFFB09388, 0xFFFF7849, 0xFF8BE36B, 0xFFFF4D6D},
	};

	public int version = 5;

	// ---- Layout ----
	public boolean hudEnabled = true;
	/** 0..8, row-major: TL, TC, TR, ML, C, MR, BL, BC, BR. */
	public int anchor = 0;
	public int offsetX = 6;
	public int offsetY = 6;
	public float scale = 1.0f;
	/** Price-change popup position; -1 on X means horizontally centered. */
	public int priceToastX = -1;
	public int priceToastY = 12;
	public float priceToastScale = 1.0f;
	/** Minimum panel width in px (before scale). */
	public int width = 150;
	/** 0 = automatic height. */
	public int height = 0;
	public int padding = 6;
	public int hudLayoutPreset = 1;
	public boolean showBalance = true;
	public boolean showEarnings = true;
	public boolean showExpenses = true;
	public boolean showNet = true;
	public boolean showPerMinute = false;
	public boolean showPerHour = true;
	public boolean showDuration = true;
	public boolean showTransactionCount = false;
	public boolean showEstimatedInventory = true;

	// ---- Style ----
	public int preset = 0;
	public int cornerRadius = 3;
	public int borderThickness = 1;
	public boolean gradient = true;
	public boolean dropShadow = true;
	public boolean accentStripe = true;
	public float panelOpacity = 0.88f;

	// ---- Colours (ARGB) ----
	public int bgColor = PRESETS[0][0];
	public int bgColor2 = PRESETS[0][1];
	public int borderColor = PRESETS[0][2];
	public int textColor = PRESETS[0][3];
	public int labelColor = PRESETS[0][4];
	public int accentColor = PRESETS[0][5];
	public int positiveColor = PRESETS[0][6];
	public int negativeColor = PRESETS[0][7];
	public int estimateColor = 0xFFFFC857;

	// ---- Text ----
	public String title = "PROFIT HUD";
	public boolean showTitle = true;
	public String currencySymbol = "$";
	public boolean currencyBefore = true;
	public boolean shortNumbers = true;
	public int decimals = 1;
	public float textScale = 1.0f;
	public boolean textShadow = true;
	public boolean boldText = false;
	public boolean boldTitle = true;
	public int lineSpacing = 2;

	// ---- Effects ----
	public boolean flashOnPayment = true;
	public boolean counterAnimation = true;
	public boolean hudTransitions = true;
	public boolean profitAnimation = true;
	public boolean sounds = false;
	public float soundVolume = 0.6f;
	public int flashDurationMs = 700;
	public boolean priceAlertsEnabled = true;
	public boolean notifyNewPrices = true;
	public double priceAlertPercent = 0;

	// ---- Chat parsing / advanced ----
	/** Use {AMOUNT} for the money token; named groups: amount (required), player (optional). */
	static final List<String> MIGRATED_INCOMING_PATTERNS = List.of(
		"(?i)^[^A-Za-z0-9]*(?:you )?(?:earned|received) {AMOUNT} from (?:auction|selling|sell|sale)\\b.*",
		"(?i)^[^A-Za-z0-9]*(?:you )?(?:have )?sold .+? for {AMOUNT}.*",
		"(?i)^[^A-Za-z0-9]*(?:you )?(?:(?:have|just|successfully) )?sold .+? (?:for|worth) {AMOUNT}.*",
		"(?i)^[^A-Za-z0-9]*(?:you )?(?:earned|received|made) {AMOUNT}(?:\\s+(?:from|for|by|through|via)\\b.*)?",
		"(?i)^[^A-Za-z0-9]*(?:you )?(?:sold|sell) .+? (?:for|worth) {AMOUNT}.*",
		"(?i)^[^A-Za-z0-9]*(?:your )?(?:sale|auction sale) .+? (?:earned|paid|sold for) {AMOUNT}.*",
		"(?i)^[^A-Za-z0-9]*(?:you )?(?:sold|sold your|sale completed) .+? (?:for|and received|and earned) {AMOUNT}.*",
		"(?i)^[^A-Za-z0-9]*(?:you )?(?:received|earned|made) {AMOUNT} (?:for|from) (?:selling|a sale|an? auction|your item)\\b.*");
	static final List<String> MIGRATED_OUTGOING_PATTERNS = List.of(
		"(?i)^[^A-Za-z0-9]*(?:you )?bought .+? for {AMOUNT}.*",
		"(?i)^[^A-Za-z0-9]*(?:you )?purchased .+? for {AMOUNT}.*");
	static final List<String> MIGRATED_BALANCE_PATTERNS = List.of(
		"(?i)^[^A-Za-z0-9]*(?:your )?(?:current )?(?:balance|bal|purse|wallet|coins?|money|cash)(?: is)?[:\\s]+{AMOUNT}");
	public List<String> incomingPatterns = new ArrayList<>(List.of(
		"(?i)^[^A-Za-z0-9]*(?<player>[A-Za-z0-9_.]{1,20}) (?:has )?(?:paid|sent|gave) you {AMOUNT}",
		"(?i)^[^A-Za-z0-9]*(?:you )?received {AMOUNT}(?: from (?<player>[A-Za-z0-9_.]{1,20}))?",
		MIGRATED_INCOMING_PATTERNS.get(0), MIGRATED_INCOMING_PATTERNS.get(1),
		MIGRATED_INCOMING_PATTERNS.get(2), MIGRATED_INCOMING_PATTERNS.get(3),
		MIGRATED_INCOMING_PATTERNS.get(4), MIGRATED_INCOMING_PATTERNS.get(5),
		MIGRATED_INCOMING_PATTERNS.get(6), MIGRATED_INCOMING_PATTERNS.get(7)));
	public List<String> outgoingPatterns = new ArrayList<>(List.of(
		"(?i)^[^A-Za-z0-9]*you (?:paid|sent|gave) (?<player>[A-Za-z0-9_.]{1,20}) {AMOUNT}",
		"(?i)^[^A-Za-z0-9]*you (?:paid|sent) {AMOUNT} to (?<player>[A-Za-z0-9_.]{1,20})",
		MIGRATED_OUTGOING_PATTERNS.get(0), MIGRATED_OUTGOING_PATTERNS.get(1)));
	/** Used to read balance responses; balances are never counted as earnings. */
	public List<String> balancePatterns = new ArrayList<>(MIGRATED_BALANCE_PATTERNS);
	public List<String> ignoredPlayers = new ArrayList<>();
	public List<String> currencySymbols = new ArrayList<>(List.of("$", "€", "£"));
	/** Signed player chat can be spoofed by other players. Off by default. */
	public boolean parsePlayerChat = false;
	public boolean rawChatDebug = true;
	public int dedupeMs = 300;
	public boolean autoNewSessionOnLaunch = true;
	public boolean countArmorAndOffhand = true;
	public boolean includeShulkerContents = true;

	public void applyPreset(int i) {
		i = Math.floorMod(i, PRESETS.length);
		preset = i;
		int[] p = PRESETS[i];
		bgColor = p[0]; bgColor2 = p[1]; borderColor = p[2]; textColor = p[3];
		labelColor = p[4]; accentColor = p[5]; positiveColor = p[6]; negativeColor = p[7];
		estimateColor = p[5];
	}

	public void applyLayoutPreset(int i) {
		hudLayoutPreset = Math.floorMod(i, 3);
		switch (hudLayoutPreset) {
			case 0 -> { scale = 0.85f; width = 130; padding = 3; }
			case 1 -> { scale = 1.0f; width = 150; padding = 6; }
			case 2 -> { scale = 1.15f; width = 190; padding = 9; }
		}
	}

	public static int presetAccent(int i) {
		return PRESETS[Math.floorMod(i, PRESETS.length)][5];
	}

	// ---- Per-tab reset helpers (used by the "Reset tab" button) ----
	public void copyLayoutFrom(ProfitConfig d) {
		hudEnabled = d.hudEnabled; anchor = d.anchor; offsetX = d.offsetX; offsetY = d.offsetY;
		scale = d.scale; hudLayoutPreset = d.hudLayoutPreset; priceToastX = d.priceToastX; priceToastY = d.priceToastY;
		priceToastScale = d.priceToastScale; width = d.width; height = d.height; padding = d.padding;
		showBalance = d.showBalance; showEarnings = d.showEarnings; showExpenses = d.showExpenses;
		showNet = d.showNet; showPerMinute = d.showPerMinute; showPerHour = d.showPerHour;
		showDuration = d.showDuration; showTransactionCount = d.showTransactionCount;
		showEstimatedInventory = d.showEstimatedInventory;
	}

	public void copyStyleFrom(ProfitConfig d) {
		cornerRadius = d.cornerRadius; borderThickness = d.borderThickness; gradient = d.gradient;
		dropShadow = d.dropShadow; accentStripe = d.accentStripe; panelOpacity = d.panelOpacity;
	}

	public void copyColorsFrom(ProfitConfig d) {
		preset = d.preset; bgColor = d.bgColor; bgColor2 = d.bgColor2; borderColor = d.borderColor;
		textColor = d.textColor; labelColor = d.labelColor; accentColor = d.accentColor;
		positiveColor = d.positiveColor; negativeColor = d.negativeColor; estimateColor = d.estimateColor;
	}

	public void copyTextFrom(ProfitConfig d) {
		title = d.title; showTitle = d.showTitle; currencySymbol = d.currencySymbol;
		currencyBefore = d.currencyBefore; shortNumbers = d.shortNumbers; decimals = d.decimals;
		textScale = d.textScale; textShadow = d.textShadow; boldText = d.boldText;
		boldTitle = d.boldTitle; lineSpacing = d.lineSpacing;
	}

	public void copyEffectsFrom(ProfitConfig d) {
		flashOnPayment = d.flashOnPayment; counterAnimation = d.counterAnimation;
		hudTransitions = d.hudTransitions; profitAnimation = d.profitAnimation;
		sounds = d.sounds; soundVolume = d.soundVolume; flashDurationMs = d.flashDurationMs;
		priceAlertsEnabled = d.priceAlertsEnabled; notifyNewPrices = d.notifyNewPrices;
		priceAlertPercent = d.priceAlertPercent;
	}

	public void copyAdvancedFrom(ProfitConfig d) {
		incomingPatterns = new ArrayList<>(d.incomingPatterns);
		outgoingPatterns = new ArrayList<>(d.outgoingPatterns);
		balancePatterns = new ArrayList<>(d.balancePatterns);
		ignoredPlayers = new ArrayList<>(d.ignoredPlayers);
		currencySymbols = new ArrayList<>(d.currencySymbols);
		parsePlayerChat = d.parsePlayerChat; rawChatDebug = d.rawChatDebug; dedupeMs = d.dedupeMs;
		autoNewSessionOnLaunch = d.autoNewSessionOnLaunch; countArmorAndOffhand = d.countArmorAndOffhand;
		includeShulkerContents = d.includeShulkerContents;
	}

	public void copyAllFrom(ProfitConfig d) {
		copyLayoutFrom(d); copyStyleFrom(d); copyColorsFrom(d);
		copyTextFrom(d); copyEffectsFrom(d); copyAdvancedFrom(d);
	}
}
