package dev.mintixx.profithud.gui;

import dev.mintixx.profithud.ProfitHudMod;
import dev.mintixx.profithud.config.ConfigManager;
import dev.mintixx.profithud.config.ProfitConfig;
import dev.mintixx.profithud.core.ChatParser;
import dev.mintixx.profithud.core.InventoryScanner;
import dev.mintixx.profithud.core.TransactionHistoryManager;
import dev.mintixx.profithud.data.Session;
import dev.mintixx.profithud.data.Transaction;
import dev.mintixx.profithud.util.NumberFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Builds the control list for every menu tab. */
final class Tabs {
	private Tabs() {}

	private static final SimpleDateFormat DATE = new SimpleDateFormat("MM-dd HH:mm", Locale.ROOT);

	private static long selectedSession = -1;
	private static long resetConfirm = 0;
	private static String sessionMsg = "";
	private static final boolean[] transactionDirections = {true, true};
	private static final String[] transactionSearch = {""};

	static List<Ui.Ctl> build(int tab, ProfitScreen s) {
		ProfitHudMod m = s.mod;
		ProfitConfig c = m.config();
		List<Ui.Ctl> l = new ArrayList<>();
		switch (tab) {
			case ProfitScreen.T_OVERVIEW -> overview(l, m, c, s);
			case ProfitScreen.T_LIVE -> live(l, m, c);
			case ProfitScreen.T_INV -> inventory(l, m, c, s);
			case ProfitScreen.T_SESSIONS -> sessions(l, m, c);
			case ProfitScreen.T_STATS -> stats(l, m, c);
			case ProfitScreen.T_LAYOUT -> layout(l, m, c, s);
			case ProfitScreen.T_STYLE -> style(l, m, c);
			case ProfitScreen.T_COLORS -> colors(l, m, c);
			case ProfitScreen.T_TEXT -> text(l, c);
			case ProfitScreen.T_EFFECTS -> effects(l, m, c);
			case ProfitScreen.T_ADVANCED -> advanced(l, m, c, s);
			default -> { }
		}
		return l;
	}

	private static String money(ProfitConfig c, double v, boolean signed) { return NumberFormatter.money(v, c, signed); }
	private static int sign(ProfitConfig c, double v) { return v > 0 ? Draw.POS : v < 0 ? Draw.NEG : Draw.TEXT; }
	private static String i0(double v) { return String.valueOf((int) Math.round(v)); }
	private static String f2(double v) { return String.format(Locale.ROOT, "%.2f", v); }

	// ------------------------------------------------------------------ Overview
	private static void overview(List<Ui.Ctl> l, ProfitHudMod m, ProfitConfig c, ProfitScreen s) {
		l.add(new Ui.Header("QUICK SETUP"));
		l.add(new Ui.Note("Start here: show the bar, change its size, or open more position and color options.", Draw.MUTED));
		l.add(new Ui.Toggle("Show profit bar in-game", () -> c.hudEnabled, v -> c.hudEnabled = v));
		l.add(new Ui.Slider("HUD size", 0.5, 3, 0.05, () -> c.scale, v -> c.scale = (float) v, Tabs::f2));
		l.add(new Ui.Split(
			new Ui.Btn("Choose colors", () -> s.selectTab(ProfitScreen.T_COLORS)),
			new Ui.Btn("Set position", () -> s.selectTab(ProfitScreen.T_LAYOUT))));
		l.add(new Ui.Header("CONFIRMED MONEY  (scoreboard and chat)"));
		l.add(new Ui.StatRow("Balance", () -> Double.isNaN(m.tracker().balance()) ? "Waiting for scoreboard or balance response" : money(c, m.tracker().balance(), false),
			() -> Double.isNaN(m.tracker().balance()) ? Draw.MUTED : Draw.TEXT));
		l.add(new Ui.StatRow("Total earned", () -> "+" + money(c, m.sessions().current().earned, false), () -> Draw.POS));
		l.add(new Ui.StatRow("Total spent", () -> "-" + money(c, m.sessions().current().spent, false), () -> Draw.NEG));
		l.add(new Ui.StatRow("Net profit", () -> money(c, m.sessions().current().net(), true), () -> sign(c, m.sessions().current().net())));
		l.add(new Ui.StatRow("Profit / minute", () -> money(c, m.sessions().current().perMinute(), true), () -> sign(c, m.sessions().current().perMinute())));
		l.add(new Ui.StatRow("Profit / hour", () -> money(c, m.sessions().current().perHour(), true), () -> sign(c, m.sessions().current().perHour())));
		l.add(new Ui.StatRow("Session duration", () -> NumberFormatter.duration(m.sessions().current().activeMillis), () -> Draw.TEXT));
		l.add(new Ui.Header("ESTIMATED INVENTORY  (estimate only)"));
		l.add(new Ui.StatRow("Inventory wealth (est.)", () -> "~" + money(c, m.scanner().totalEstimate(), false), () -> c.estimateColor));
		l.add(new Ui.Note("Inventory estimates use prices detected in item lore and never affect payment statistics.", Draw.MUTED));
		l.add(new Ui.Btn("Test payment flash", () -> m.anim().testFlash(true)));
	}

	// ------------------------------------------------------------------ Live tracker
	private static void live(List<Ui.Ctl> l, ProfitHudMod m, ProfitConfig c) {
		final String[] test = {""};
		final String[] result = {"Type or paste a chat line, then press Test. Nothing is recorded."};
		l.add(new Ui.Header("CHAT TRACKING"));
		l.add(new Ui.Note("Detected buys, sales, and payments refresh balance with /balance (at most once every 2 seconds).", Draw.MUTED));
		l.add(new Ui.Toggle("Raw chat debug capture", () -> c.rawChatDebug, v -> c.rawChatDebug = v));
		l.add(new Ui.Note(() -> {
			List<String> e = m.parser().errors();
			return e.isEmpty() ? "All chat patterns compiled OK." : "Pattern problems: " + String.join(" | ", e);
		}, Draw.MUTED));
		l.add(new Ui.Header("TEST A MESSAGE"));
		l.add(new Ui.TextCtl(null, "e.g. Steve paid you $500K", 160, null, () -> test[0], v -> test[0] = v));
		l.add(new Ui.Btn("Test parse", () -> {
			ChatParser.Result r = m.parser().parse(test[0]);
			result[0] = switch (r.kind()) {
				case NONE -> "No pattern matched. Edit patterns in the Advanced tab.";
				case BALANCE -> "BALANCE detected: " + money(c, r.amount(), false);
				default -> r.kind() + ": " + money(c, r.amount(), false) + (r.player().isEmpty() ? "" : "  player=" + r.player());
			};
		}));
		l.add(new Ui.Note(() -> result[0], Draw.ACCENT));
		l.add(new Ui.Header("RECENT CONFIRMED TRANSACTIONS"));
		l.add(new Ui.ListView("No payments detected yet.", () -> {
			List<Ui.RowData> rows = new ArrayList<>();
			for (Transaction t : m.history().recent(15)) {
				String who = t.player.isEmpty() ? "(unknown)" : t.player;
				rows.add(new Ui.RowData((t.incoming ? "+ " : "- ") + who, money(c, t.amount, false) + "  " + DATE.format(new Date(t.time)))
					.colors(t.incoming ? Draw.POS : Draw.NEG, Draw.MUTED));
			}
			return rows;
		}, null));
		l.add(new Ui.Header("RAW CHAT (last 20)"));
		l.add(new Ui.ListView("Nothing captured yet.", () -> {
			List<Ui.RowData> rows = new ArrayList<>();
			for (TransactionHistoryManager.RawLine r : m.history().rawLines(20)) {
				int col = switch (r.kind()) { case INCOMING -> Draw.POS; case OUTGOING -> Draw.NEG; case BALANCE -> Draw.ACCENT; default -> Draw.MUTED; };
				rows.add(new Ui.RowData(r.text(), r.kind() == ChatParser.Kind.NONE ? "" : r.kind().name()).colors(col, col));
			}
			return rows;
		}, null));
		l.add(new Ui.Btn("Clear raw log", () -> m.history().clearRaw()));
	}

	// ------------------------------------------------------------------ Inventory
	private static void inventory(List<Ui.Ctl> l, ProfitHudMod m, ProfitConfig c, ProfitScreen s) {
		l.add(new Ui.Header("ESTIMATED INVENTORY WEALTH  (ESTIMATE)"));
		l.add(new Ui.StatRow("Estimated total", () -> "~" + money(c, m.scanner().totalEstimate(), false), () -> c.estimateColor));
		l.add(new Ui.Toggle("Include armor and offhand", () -> c.countArmorAndOffhand, v -> c.countArmorAndOffhand = v));
		l.add(new Ui.Toggle("Count items stored in carried shulkers", () -> c.includeShulkerContents, v -> c.includeShulkerContents = v));
		l.add(new Ui.Note("Estimates use item lore and remembered prices. Shulker contents are counted only when enabled.", Draw.MUTED));
		l.add(new Ui.ListView("Inventory is empty (or you are not in a world).", () -> {
			List<Ui.RowData> rows = new ArrayList<>();
			for (InventoryScanner.Row r : m.scanner().rows()) {
				String right = r.unitPrice() > 0
					? money(c, r.unitPrice(), false) + " each = ~" + money(c, r.total(), false)
					: "no lore price seen";
				rows.add(new Ui.RowData(ItemsPanel.displayName(r.id()) + " x" + r.count(), right)
					.icon(r.sample()).tag(r.id()).colors(Draw.TEXT, r.unitPrice() > 0 ? c.estimateColor : Draw.MUTED));
			}
			return rows;
		}, row -> s.openPrices((String) row.tag)));
	}

	// ------------------------------------------------------------------ Sessions
	private static void sessions(List<Ui.Ctl> l, ProfitHudMod m, ProfitConfig c) {
		l.add(new Ui.Header("SESSION CONTROL"));
		l.add(new Ui.Btn(() -> System.currentTimeMillis() - resetConfirm < 3000 ? "Click again to confirm" : "Reset current stats", () -> {
			long now = System.currentTimeMillis();
			if (now - resetConfirm < 3000) { m.sessions().resetCurrent(); resetConfirm = 0; sessionMsg = "Current session cleared."; }
			else resetConfirm = now;
		}, false));
		l.add(new Ui.Split(
			new Ui.Btn("Export all sessions", () -> {
				try { sessionMsg = "Exported: " + m.sessions().exportCsv(); } catch (Exception e) { sessionMsg = "Export failed: " + e.getMessage(); }
			}),
			new Ui.Btn("Export filtered", () -> {
				try {
					sessionMsg = "Exported: " + m.sessions().exportFilteredCsv(getSelectedSession(m),
						transactionDirections[0], transactionDirections[1], transactionSearch[0]);
				} catch (Exception e) { sessionMsg = "Export failed: " + e.getMessage(); }
			}),
			new Ui.Btn("Delete selected", () -> {
				if (selectedSession > 0) { m.sessions().delete(selectedSession); selectedSession = -1; sessionMsg = "Session deleted."; }
				else sessionMsg = "Select a previous session first.";
			})));
		l.add(new Ui.Note(() -> sessionMsg.isEmpty() ? "CSV files contain confirmed transactions only." : sessionMsg, Draw.ACCENT));
		l.add(new Ui.Header("FILTER TRANSACTIONS"));
		l.add(new Ui.Split(
			new Ui.Toggle("Received", () -> transactionDirections[0], v -> transactionDirections[0] = v),
			new Ui.Toggle("Sent", () -> transactionDirections[1], v -> transactionDirections[1] = v)));
		l.add(new Ui.TextCtl("Find player or message", "type a name or word...", 32, null,
			() -> transactionSearch[0], v -> transactionSearch[0] = v));
		l.add(new Ui.Note("Filters affect the list below and the filtered CSV export.", Draw.MUTED));
		l.add(new Ui.Header("SESSIONS (click to inspect)"));
		l.add(new Ui.ListView("No sessions yet.", () -> {
			List<Ui.RowData> rows = new ArrayList<>();
			Session cur = m.sessions().current();
			rows.add(sessionRow(c, cur, "Current", selectedSession == -1));
			int n = m.sessions().past().size();
			for (Session p : m.sessions().past()) rows.add(sessionRow(c, p, "#" + n--, selectedSession == p.id));
			return rows;
		}, row -> selectedSession = row.tag == null ? -1 : (Long) row.tag));
		l.add(new Ui.Header("SELECTED SESSION - TRANSACTIONS"));
		l.add(new Ui.ListView("No transactions in this session.", () -> {
			Session sel = getSelectedSession(m);
			List<Ui.RowData> rows = new ArrayList<>();
			List<Transaction> tx = sel.transactions == null ? List.of() : sel.transactions;
			String needle = transactionSearch[0].strip().toLowerCase(Locale.ROOT);
			for (int i = tx.size() - 1; i >= 0 && rows.size() < 40; i--) {
				Transaction t = tx.get(i);
				if ((t.incoming && !transactionDirections[0]) || (!t.incoming && !transactionDirections[1])) continue;
				String player = t.player == null ? "" : t.player;
				String raw = t.raw == null ? "" : t.raw;
				if (!needle.isEmpty() && !player.toLowerCase(Locale.ROOT).contains(needle)
					&& !raw.toLowerCase(Locale.ROOT).contains(needle)
					&& !String.valueOf(t.amount).contains(needle)) continue;
				rows.add(new Ui.RowData((t.incoming ? "+ " : "- ") + (player.isEmpty() ? "(unknown)" : player),
					money(c, t.amount, false) + "  " + DATE.format(new Date(t.time))).colors(t.incoming ? Draw.POS : Draw.NEG, Draw.MUTED));
			}
			return rows;
		}, null));
	}

	private static Session getSelectedSession(ProfitHudMod mod) {
		if (selectedSession == -1) return mod.sessions().current();
		for (Session session : mod.sessions().past()) if (session.id == selectedSession) return session;
		return mod.sessions().current();
	}

	private static Ui.RowData sessionRow(ProfitConfig c, Session s, String name, boolean selected) {
		Ui.RowData r = new Ui.RowData(name + "  " + DATE.format(new Date(s.startMillis)) + "  " + NumberFormatter.duration(s.activeMillis),
			"+" + money(c, s.earned, false) + "  -" + money(c, s.spent, false) + "  net " + money(c, s.net(), true));
		r.selected = selected;
		r.tag = name.equals("Current") ? null : (Object) s.id;
		r.rightColor = sign(c, s.net());
		return r;
	}

	// ------------------------------------------------------------------ Statistics
	private static void stats(List<Ui.Ctl> l, ProfitHudMod m, ProfitConfig c) {
		l.add(new Ui.Header("CURRENT SESSION  (confirmed)"));
		l.add(new Ui.StatRow("Total money earned", () -> money(c, m.sessions().current().earned, false), () -> Draw.POS));
		l.add(new Ui.StatRow("Total money spent", () -> money(c, m.sessions().current().spent, false), () -> Draw.NEG));
		l.add(new Ui.StatRow("Confirmed net profit", () -> money(c, m.sessions().current().net(), true), () -> sign(c, m.sessions().current().net())));
		l.add(new Ui.StatRow("Highest payment received", () -> money(c, m.sessions().current().highestIn, false), () -> Draw.TEXT));
		l.add(new Ui.StatRow("Highest payment sent", () -> money(c, m.sessions().current().highestOut, false), () -> Draw.TEXT));
		l.add(new Ui.StatRow("Average payment (all)", () -> money(c, m.sessions().current().avgAll(), false), () -> Draw.TEXT));
		l.add(new Ui.StatRow("Average received", () -> money(c, m.sessions().current().avgIn(), false), () -> Draw.TEXT));
		l.add(new Ui.StatRow("Average sent", () -> money(c, m.sessions().current().avgOut(), false), () -> Draw.TEXT));
		l.add(new Ui.StatRow("Payments received / sent", () -> m.sessions().current().countIn + " / " + m.sessions().current().countOut, () -> Draw.TEXT));
		l.add(new Ui.StatRow("Session duration", () -> NumberFormatter.duration(m.sessions().current().activeMillis), () -> Draw.TEXT));
		l.add(new Ui.Header("ALL SESSIONS  (confirmed)"));
		l.add(new Ui.StatRow("Lifetime earned", () -> money(c, total(m, true), false), () -> Draw.POS));
		l.add(new Ui.StatRow("Lifetime spent", () -> money(c, total(m, false), false), () -> Draw.NEG));
		l.add(new Ui.StatRow("Lifetime net", () -> money(c, total(m, true) - total(m, false), true), () -> sign(c, total(m, true) - total(m, false))));
		l.add(new Ui.Header("NET PROFIT OVER TIME  (current session)"));
		l.add(new Ui.Custom(70, (g, f, x, y, w, h, mx, my) -> {
			Draw.fill(g, x, y, w, h, Draw.PANEL_2);
			List<Transaction> tx = m.sessions().current().transactions;
			if (tx.size() < 2) { Draw.text(g, f, "Not enough payments yet.", x + 8, y + 8, Draw.MUTED, false); return; }
			double[] cum = new double[tx.size()];
			double run = 0, mn = 0, mxv = 0;
			for (int i = 0; i < tx.size(); i++) {
				run += tx.get(i).incoming ? tx.get(i).amount : -tx.get(i).amount;
				cum[i] = run; mn = Math.min(mn, run); mxv = Math.max(mxv, run);
			}
			double span = Math.max(1, mxv - mn);
			int zero = y + 4 + (int) ((mxv) / span * (h - 8));
			Draw.fill(g, x, zero, w, 1, Draw.BORDER);
			int prev = -1;
			for (int px = 0; px < w - 8; px++) {
				int idx = (int) ((long) px * (tx.size() - 1) / Math.max(1, w - 9));
				int py = y + 4 + (int) ((mxv - cum[idx]) / span * (h - 8));
				int a = prev < 0 ? py : Math.min(prev, py), b = prev < 0 ? py : Math.max(prev, py);
				Draw.fill(g, x + 4 + px, a, 1, b - a + 1, cum[idx] >= 0 ? Draw.POS : Draw.NEG);
				prev = py;
			}
		}));
		l.add(new Ui.Header("ESTIMATED  (separate from the numbers above)"));
		l.add(new Ui.StatRow("Estimated inventory value", () -> "~" + money(c, m.scanner().totalEstimate(), false), () -> c.estimateColor));
	}

	private static double total(ProfitHudMod m, boolean earned) {
		double t = earned ? m.sessions().current().earned : m.sessions().current().spent;
		for (Session p : m.sessions().past()) t += earned ? p.earned : p.spent;
		return t;
	}

	// ------------------------------------------------------------------ Layout
	private static void layout(List<Ui.Ctl> l, ProfitHudMod m, ProfitConfig c, ProfitScreen s) {
		l.add(new Ui.Header("QUICK LAYOUTS"));
		l.add(new Ui.Note("Choose a starting size. You can fine-tune every setting below.", Draw.MUTED));
		l.add(new Ui.Split(
			new Ui.Btn(() -> "Compact", () -> applyLayoutPreset(m, c, s, 0), c.hudLayoutPreset == 0),
			new Ui.Btn(() -> "Standard", () -> applyLayoutPreset(m, c, s, 1), c.hudLayoutPreset == 1),
			new Ui.Btn(() -> "Spacious", () -> applyLayoutPreset(m, c, s, 2), c.hudLayoutPreset == 2)));
		l.add(new Ui.Header("POSITION"));
		l.add(new Ui.Toggle("Show HUD in game", () -> c.hudEnabled, v -> c.hudEnabled = v));
		l.add(new Ui.AnchorGrid(() -> c.anchor, v -> c.anchor = v));
		l.add(new Ui.Slider("X offset", -600, 600, 1, () -> c.offsetX, v -> c.offsetX = (int) Math.round(v), Tabs::i0));
		l.add(new Ui.Slider("Y offset", -600, 600, 1, () -> c.offsetY, v -> c.offsetY = (int) Math.round(v), Tabs::i0));
		l.add(new Ui.Header("SIZE"));
		l.add(new Ui.Slider("HUD scale", 0.5, 3, 0.05, () -> c.scale, v -> c.scale = (float) v, Tabs::f2));
		l.add(new Ui.Slider("Min width", 80, 300, 1, () -> c.width, v -> c.width = (int) Math.round(v), Tabs::i0));
		l.add(new Ui.Slider("Height (0 = auto)", 0, 300, 1, () -> c.height, v -> c.height = (int) Math.round(v), Tabs::i0));
		l.add(new Ui.Slider("Padding", 0, 16, 1, () -> c.padding, v -> c.padding = (int) Math.round(v), Tabs::i0));
		l.add(new Ui.Header("VISIBLE ROWS"));
		l.add(new Ui.Toggle("Title", () -> c.showTitle, v -> c.showTitle = v));
		l.add(new Ui.Toggle("Balance (only if detected)", () -> c.showBalance, v -> c.showBalance = v));
		l.add(new Ui.Toggle("Earnings", () -> c.showEarnings, v -> c.showEarnings = v));
		l.add(new Ui.Toggle("Expenses", () -> c.showExpenses, v -> c.showExpenses = v));
		l.add(new Ui.Toggle("Net profit", () -> c.showNet, v -> c.showNet = v));
		l.add(new Ui.Toggle("Profit per minute", () -> c.showPerMinute, v -> c.showPerMinute = v));
		l.add(new Ui.Toggle("Profit per hour", () -> c.showPerHour, v -> c.showPerHour = v));
		l.add(new Ui.Toggle("Session duration", () -> c.showDuration, v -> c.showDuration = v));
		l.add(new Ui.Toggle("Payment count", () -> c.showTransactionCount, v -> c.showTransactionCount = v));
		l.add(new Ui.Toggle("Estimated inventory (separate line)", () -> c.showEstimatedInventory, v -> c.showEstimatedInventory = v));
	}

	// ------------------------------------------------------------------ Style
	private static void style(List<Ui.Ctl> l, ProfitHudMod m, ProfitConfig c) {
		l.add(new Ui.Header("PANEL"));
		l.add(new Ui.Slider("Corner radius", 0, 8, 1, () -> c.cornerRadius, v -> c.cornerRadius = (int) Math.round(v), Tabs::i0));
		l.add(new Ui.Slider("Border thickness", 0, 4, 1, () -> c.borderThickness, v -> c.borderThickness = (int) Math.round(v), Tabs::i0));
		l.add(new Ui.Slider("Panel opacity", 0, 1, 0.01, () -> c.panelOpacity, v -> c.panelOpacity = (float) v, Tabs::f2));
		l.add(new Ui.Toggle("Gradient background", () -> c.gradient, v -> c.gradient = v));
		l.add(new Ui.Toggle("Drop shadow", () -> c.dropShadow, v -> c.dropShadow = v));
		l.add(new Ui.Toggle("Accent stripe", () -> c.accentStripe, v -> c.accentStripe = v));
	}

	private static void applyLayoutPreset(ProfitHudMod m, ProfitConfig c, ProfitScreen s, int index) {
		c.applyLayoutPreset(index);
		m.configManager().save();
		s.build();
	}

	// ------------------------------------------------------------------ Colors
	private static void colors(List<Ui.Ctl> l, ProfitHudMod m, ProfitConfig c) {
		l.add(new Ui.Header("HUD COLOR PRESETS"));
		l.add(new Ui.PresetGrid(c, m.configManager()::save));
		l.add(new Ui.Note("Presets change the HUD panel, title, labels, values, and price popup immediately.", Draw.MUTED));
		l.add(new Ui.Header("CUSTOM RGBA COLORS"));
		l.add(new Ui.ColorCtl("Background", () -> c.bgColor, v -> c.bgColor = v));
		l.add(new Ui.ColorCtl("Gradient end", () -> c.bgColor2, v -> c.bgColor2 = v));
		l.add(new Ui.ColorCtl("Border", () -> c.borderColor, v -> c.borderColor = v));
		l.add(new Ui.ColorCtl("Text", () -> c.textColor, v -> c.textColor = v));
		l.add(new Ui.ColorCtl("Labels", () -> c.labelColor, v -> c.labelColor = v));
		l.add(new Ui.ColorCtl("Accent", () -> c.accentColor, v -> c.accentColor = v));
		l.add(new Ui.ColorCtl("Profit (+)", () -> c.positiveColor, v -> c.positiveColor = v));
		l.add(new Ui.ColorCtl("Loss (-)", () -> c.negativeColor, v -> c.negativeColor = v));
		l.add(new Ui.ColorCtl("Estimate line", () -> c.estimateColor, v -> c.estimateColor = v));
		l.add(new Ui.Note("Tip: panel opacity (Style tab) multiplies the background alpha.", Draw.MUTED));
	}

	// ------------------------------------------------------------------ Text
	private static void text(List<Ui.Ctl> l, ProfitConfig c) {
		l.add(new Ui.Header("TITLE AND CURRENCY"));
		l.add(new Ui.TextCtl("Custom title", "PROFIT HUD", 24, null, () -> c.title, v -> c.title = v));
		l.add(new Ui.Toggle("Show title", () -> c.showTitle, v -> c.showTitle = v));
		l.add(new Ui.TextCtl("Currency symbol", "$", 3, null, () -> c.currencySymbol, v -> c.currencySymbol = v));
		l.add(new Ui.Toggle("Symbol before number", () -> c.currencyBefore, v -> c.currencyBefore = v));
		l.add(new Ui.Header("NUMBERS"));
		l.add(new Ui.Toggle("Short numbers (K, M, B, T)", () -> c.shortNumbers, v -> c.shortNumbers = v));
		l.add(new Ui.Slider("Decimals", 0, 4, 1, () -> c.decimals, v -> c.decimals = (int) Math.round(v), Tabs::i0));
		l.add(new Ui.Header("FONT"));
		l.add(new Ui.Slider("Text scale", 0.5, 2, 0.05, () -> c.textScale, v -> c.textScale = (float) v, Tabs::f2));
		l.add(new Ui.Slider("Line spacing", 0, 8, 1, () -> c.lineSpacing, v -> c.lineSpacing = (int) Math.round(v), Tabs::i0));
		l.add(new Ui.Toggle("Text shadow", () -> c.textShadow, v -> c.textShadow = v));
		l.add(new Ui.Toggle("Bold text", () -> c.boldText, v -> c.boldText = v));
		l.add(new Ui.Toggle("Bold title", () -> c.boldTitle, v -> c.boldTitle = v));
	}

	// ------------------------------------------------------------------ Effects
	private static void effects(List<Ui.Ctl> l, ProfitHudMod m, ProfitConfig c) {
		l.add(new Ui.Header("PRICE ALERTS"));
		l.add(new Ui.Toggle("Show popups when prices change", () -> c.priceAlertsEnabled, v -> c.priceAlertsEnabled = v));
		l.add(new Ui.Toggle("Alert when a price is first found", () -> c.notifyNewPrices, v -> c.notifyNewPrices = v));
		l.add(new Ui.Note("0% shows every price change; increase it to ignore small changes.", Draw.MUTED));
		l.add(new Ui.Slider("Minimum price change (%)", 0, 100, 1, () -> c.priceAlertPercent,
			v -> c.priceAlertPercent = v, Tabs::i0));
		l.add(new Ui.Header("ANIMATIONS"));
		l.add(new Ui.Toggle("New payment flash", () -> c.flashOnPayment, v -> c.flashOnPayment = v));
		l.add(new Ui.Slider("Flash duration (ms)", 200, 2000, 50, () -> c.flashDurationMs, v -> c.flashDurationMs = (int) Math.round(v), Tabs::i0));
		l.add(new Ui.Toggle("Profit counter animation", () -> c.counterAnimation, v -> c.counterAnimation = v));
		l.add(new Ui.Toggle("HUD fade/slide transition", () -> c.hudTransitions, v -> c.hudTransitions = v));
		l.add(new Ui.Toggle("Positive/negative profit highlight", () -> c.profitAnimation, v -> c.profitAnimation = v));
		l.add(new Ui.Header("SOUND"));
		l.add(new Ui.Toggle("Notification sounds", () -> c.sounds, v -> c.sounds = v));
		l.add(new Ui.Slider("Volume", 0, 1, 0.05, () -> c.soundVolume, v -> c.soundVolume = (float) v, Tabs::f2));
		l.add(new Ui.Split(new Ui.Btn("Preview received", () -> m.anim().onPayment(true)),
			new Ui.Btn("Preview sent", () -> m.anim().onPayment(false))));
	}

	// ------------------------------------------------------------------ Advanced
	private static void advanced(List<Ui.Ctl> l, ProfitHudMod m, ProfitConfig c, ProfitScreen s) {
		ConfigManager cm = m.configManager();
		Runnable changed = cm::touch;
		l.add(new Ui.Note("Current server profile: " + cm.activeServerProfile()
			+ ". HUD colors and chat rules are saved separately for each server.", Draw.MUTED));
		l.add(new Ui.Header("CHAT PATTERNS (regex)"));
		l.add(new Ui.Note("Use {AMOUNT} for the money token. Named groups: amount (required), player (optional). "
			+ "Every server differs: edit these, then check them in the Live Tracker tab.", Draw.MUTED));
		l.add(new Ui.ListEditor("Incoming payments (you received)", "regex...", c.incomingPatterns, changed));
		l.add(new Ui.ListEditor("Outgoing payments (you paid)", "regex...", c.outgoingPatterns, changed));
		l.add(new Ui.ListEditor("Balance response messages", "regex...", c.balancePatterns, changed));
		l.add(new Ui.Header("PLAYERS AND CURRENCY"));
		l.add(new Ui.ListEditor("Ignored players", "player name", c.ignoredPlayers, changed));
		l.add(new Ui.ListEditor("Currency symbols recognised in chat", "$", c.currencySymbols, changed));
		l.add(new Ui.Header("BEHAVIOUR"));
		l.add(new Ui.Toggle("Also parse signed player chat (spoofable)", () -> c.parsePlayerChat, v -> c.parsePlayerChat = v));
		l.add(new Ui.Toggle("Start a new session on every launch", () -> c.autoNewSessionOnLaunch, v -> c.autoNewSessionOnLaunch = v));
		l.add(new Ui.Slider("Duplicate-line window (ms)", 0, 1000, 50, () -> c.dedupeMs, v -> c.dedupeMs = (int) Math.round(v), Tabs::i0));
		l.add(new Ui.Btn("Restore default patterns", () -> {
			ProfitConfig d = new ProfitConfig();
			c.incomingPatterns = new ArrayList<>(d.incomingPatterns);
			c.outgoingPatterns = new ArrayList<>(d.outgoingPatterns);
			c.balancePatterns = new ArrayList<>(d.balancePatterns);
			cm.touch();
			s.build();
			s.setStatus("Default patterns restored.");
		}));
		l.add(new Ui.Note("Config folder: config/profithud/  (config.json, prices.json, sessions.json, exports/)", Draw.MUTED));
	}
}
