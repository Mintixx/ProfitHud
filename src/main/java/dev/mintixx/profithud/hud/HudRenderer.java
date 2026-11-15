package dev.mintixx.profithud.hud;

import dev.mintixx.profithud.config.ConfigManager;
import dev.mintixx.profithud.config.ProfitConfig;
import dev.mintixx.profithud.core.EconomyTracker;
import dev.mintixx.profithud.core.InventoryScanner;
import dev.mintixx.profithud.core.InventoryScanner.PriceNotice;
import dev.mintixx.profithud.core.SessionManager;
import dev.mintixx.profithud.data.Session;
import dev.mintixx.profithud.gui.Draw;
import dev.mintixx.profithud.util.Colors;
import dev.mintixx.profithud.util.NumberFormatter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Draws the overlay panel. The same code renders the in-game HUD, the menu preview and the drag screen. */
public final class HudRenderer {
	public record Row(String label, String value, int valueColor, int kind) { }
	public record Layout(int w, int h, int lh, int rowH, List<Row> rows, boolean title) { }
	public record PriceToastLayout(int x, int y, int width, int height, int baseWidth, int baseHeight, float scale) { }

	private static final int KIND_NORMAL = 0, KIND_EARN = 1, KIND_SPEND = 2, KIND_NET = 3, KIND_ESTIMATE = 4;

	private final ConfigManager cm;
	private final EconomyTracker tracker;
	private final SessionManager sessions;
	private final InventoryScanner scanner;
	private final Animations anim;
	private long testUntilMs = 0;

	public HudRenderer(ConfigManager cm, EconomyTracker tracker, SessionManager sessions,
					   InventoryScanner scanner, Animations anim) {
		this.cm = cm;
		this.tracker = tracker;
		this.sessions = sessions;
		this.scanner = scanner;
		this.anim = anim;
	}

	public void startTest(long ms) {
		testUntilMs = System.currentTimeMillis() + ms;
		anim.testFlash(true);
	}

	public boolean testing() { return System.currentTimeMillis() < testUntilMs; }

	public HudData live() {
		Session s = sessions.current();
		return new HudData(tracker.balance(), s.earned, s.spent, s.net(), s.perMinute(), s.perHour(),
			scanner.totalEstimate(), s.activeMillis, s.countIn + s.countOut);
	}

	/** Example numbers for previews and "Test HUD". Not recorded anywhere. */
	public static HudData sample() {
		return new HudData(125_500_000, 12_500_000, 3_200_000, 9_300_000, 70_000, 4_200_000, 15_000_000, 2_220_000, 14);
	}

	public HudData displayed(HudData d) {
		if (!cm.get().counterAnimation) return d;
		return new HudData(anim.dBalance, anim.dEarned, anim.dSpent, anim.dNet, anim.dPerMinute, anim.dPerHour,
			anim.dEst, d.durationMs(), d.txCount());
	}

	// ------------------------------------------------------------------ layout

	public Layout layout(Font font, HudData shown) {
		ProfitConfig c = cm.get();
		float ts = c.textScale;
		int lh = (int) Math.ceil(font.lineHeight * ts);
		int rowH = lh + c.lineSpacing;

		List<Row> rows = new ArrayList<>();
		if (c.showBalance) {
			boolean has = !Double.isNaN(shown.balance());
			rows.add(new Row("Balance", has ? NumberFormatter.money(shown.balance(), c, false) : "N/A",
				has ? c.textColor : c.labelColor, KIND_NORMAL));
		}
		if (c.showEarnings)
			rows.add(new Row("Earnings", "+" + NumberFormatter.money(Math.abs(shown.earned()), c, false), c.positiveColor, KIND_EARN));
		if (c.showExpenses)
			rows.add(new Row("Expenses", "-" + NumberFormatter.money(Math.abs(shown.spent()), c, false), c.negativeColor, KIND_SPEND));
		if (c.showNet)
			rows.add(new Row("Net Profit", NumberFormatter.money(shown.net(), c, true), signColor(c, shown.net()), KIND_NET));
		if (c.showPerMinute)
			rows.add(new Row("Profit/min", NumberFormatter.money(shown.perMinute(), c, true), signColor(c, shown.perMinute()), KIND_NORMAL));
		if (c.showPerHour)
			rows.add(new Row("Profit/hour", NumberFormatter.money(shown.perHour(), c, true), signColor(c, shown.perHour()), KIND_NORMAL));
		if (c.showDuration)
			rows.add(new Row("Session", NumberFormatter.duration(shown.durationMs()), c.textColor, KIND_NORMAL));
		if (c.showTransactionCount)
			rows.add(new Row("Payments", String.valueOf(shown.txCount()), c.textColor, KIND_NORMAL));
		if (c.showEstimatedInventory)
			rows.add(new Row("Inventory (est.)", "~" + NumberFormatter.money(shown.estimatedInventory(), c, false), c.estimateColor, KIND_ESTIMATE));

		int pad = c.padding;
		int stripe = c.accentStripe ? 2 : 0;
		int gap = 10;
		int inner = 0;
		boolean titleOn = c.showTitle && c.title != null && !c.title.isEmpty();
		if (titleOn) inner = Math.max(inner, (int) Math.ceil(Draw.tw(font, c.title, c.boldTitle) * ts));
		for (Row r : rows) {
			int w = (int) Math.ceil((Draw.tw(font, r.label(), c.boldText) + Draw.tw(font, r.value(), c.boldText)) * ts) + gap;
			inner = Math.max(inner, w);
		}
		int w = Math.max(c.width, inner + pad * 2 + stripe + c.borderThickness);
		int h = pad * 2;
		if (titleOn) h += lh + 6;
		if (!rows.isEmpty()) h += rows.size() * rowH - c.lineSpacing;
		for (Row r : rows) if (r.kind() == KIND_ESTIMATE && rows.size() > 1) h += 4;
		if (c.height > 0) h = Math.max(h, c.height);
		return new Layout(w, h, lh, rowH, rows, titleOn);
	}

	private static int signColor(ProfitConfig c, double v) {
		if (v > 0.0000001) return c.positiveColor;
		if (v < -0.0000001) return c.negativeColor;
		return c.textColor;
	}

	/** Top-left corner on screen for the configured anchor/offsets. */
	public float[] origin(int sw, int sh, int w, int h) {
		ProfitConfig c = cm.get();
		int col = c.anchor % 3, row = c.anchor / 3;
		float pw = w * c.scale, ph = h * c.scale;
		float x = col == 0 ? c.offsetX : col == 1 ? (sw - pw) / 2f + c.offsetX : sw - pw - c.offsetX;
		float y = row == 0 ? c.offsetY : row == 1 ? (sh - ph) / 2f + c.offsetY : sh - ph - c.offsetY;
		return new float[] {x, y};
	}

	/** Inverse of {@link #origin}: stores offsets so that the panel's top-left lands on (x, y). */
	public void setOriginFromScreen(int sw, int sh, int w, int h, float x, float y) {
		ProfitConfig c = cm.get();
		int col = c.anchor % 3, row = c.anchor / 3;
		float pw = w * c.scale, ph = h * c.scale;
		float ox = col == 0 ? x : col == 1 ? x - (sw - pw) / 2f : sw - pw - x;
		float oy = row == 0 ? y : row == 1 ? y - (sh - ph) / 2f : sh - ph - y;
		c.offsetX = Math.max(-2000, Math.min(2000, Math.round(ox)));
		c.offsetY = Math.max(-2000, Math.min(2000, Math.round(oy)));
	}

	// ------------------------------------------------------------------ rendering

	/** The in-game HUD element. */
	public void renderHud(GuiGraphicsExtractor g, Font font, int sw, int sh) {
		ProfitConfig c = cm.get();
		boolean test = testing();
		if (!c.hudEnabled && !test) return;
		HudData d = test ? sample() : live();
		anim.update(d);
		Layout lay = layout(font, displayed(d));
		float[] o = origin(sw, sh, lay.w(), lay.h());
		drawPanel(g, font, o[0], o[1], c.scale, lay, anim.appear);
	}

	/** Price-change notification is independent of whether the main HUD is enabled. */
	public void renderPriceToast(GuiGraphicsExtractor g, Font font, int sw, int sh) {
		PriceNotice notice = scanner.activePriceNotice();
		if (notice == null) return;
		drawPriceToast(g, font, sw, sh, notice);
	}

	public PriceToastLayout renderPriceToastPreview(GuiGraphicsExtractor g, Font font, int sw, int sh) {
		PriceNotice preview = new PriceNotice("Token of Undying", 37_500, 42_000, false);
		drawPriceToast(g, font, sw, sh, preview);
		return priceToastLayout(font, sw, sh, preview);
	}

	public PriceToastLayout priceToastLayout(Font font, int sw, int sh, PriceNotice notice) {
		ProfitConfig c = cm.get();
		int maxTextWidth = Math.max(40, Math.min(300, sw - 32));
		String title = notice.firstSeen() ? "ITEM PRICE FOUND" : "ITEM PRICE CHANGED";
		String name = Draw.fit(font, notice.name(), maxTextWidth);
		String price = notice.firstSeen()
			? "Now " + NumberFormatter.money(notice.current(), c, false)
			: NumberFormatter.money(notice.previous(), c, false) + " -> " + NumberFormatter.money(notice.current(), c, false);
		int baseWidth = Math.min(sw - 12, Math.max(font.width(title), Math.max(font.width(name), font.width(price))) + 20);
		int baseHeight = 42;
		float scale = Math.min(c.priceToastScale,
			Math.min((sw - 12) / (float) baseWidth, (sh - 8) / (float) baseHeight));
		int width = Math.round(baseWidth * scale);
		int height = Math.round(baseHeight * scale);
		int x = c.priceToastX < 0 ? (sw - width) / 2 : Math.max(0, Math.min(sw - width, c.priceToastX));
		int y = Math.max(0, Math.min(sh - height, c.priceToastY));
		return new PriceToastLayout(x, y, width, height, baseWidth, baseHeight, scale);
	}

	private void drawPriceToast(GuiGraphicsExtractor g, Font font, int sw, int sh, PriceNotice notice) {
		ProfitConfig c = cm.get();
		PriceToastLayout layout = priceToastLayout(font, sw, sh, notice);
		String title = notice.firstSeen() ? "ITEM PRICE FOUND" : "ITEM PRICE CHANGED";
		String name = Draw.fit(font, notice.name(), layout.baseWidth() - 20);
		String price = notice.firstSeen()
			? "Now " + NumberFormatter.money(notice.current(), c, false)
			: NumberFormatter.money(notice.previous(), c, false) + " -> " + NumberFormatter.money(notice.current(), c, false);
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(layout.x(), layout.y());
		pose.scale(layout.scale(), layout.scale());
		Draw.fill(g, 0, 0, layout.baseWidth(), layout.baseHeight(), 0xEE101516);
		Draw.outline(g, 0, 0, layout.baseWidth(), layout.baseHeight(), c.accentColor);
		Draw.text(g, font, title, 10, 5, c.accentColor, true);
		Draw.text(g, font, name, 10, 16, c.textColor, false);
		Draw.text(g, font, price, 10, 28, c.estimateColor, false);
		pose.popMatrix();
	}

	/** Draws the panel with its top-left at (x, y) using the given scale. */
	public void drawPanel(GuiGraphicsExtractor g, Font font, float x, float y, float scale, Layout lay, float appear) {
		ProfitConfig c = cm.get();
		int w = lay.w(), h = lay.h();
		int r = c.cornerRadius, t = c.borderThickness;
		float a = Math.max(0f, Math.min(1f, appear));
		float ts = c.textScale;
		float flash = c.flashOnPayment ? anim.flash() : 0f;
		int flashColor = anim.flashIncoming() ? c.positiveColor : c.negativeColor;

		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(x, y + (1f - a) * -5f);
		pose.scale(scale, scale);

		if (c.dropShadow) {
			Draw.rrect(g, 2, 3, w, h, r, Colors.mulAlpha(0x66000000, a));
			Draw.rrect(g, 1, 1, w + 2, h + 2, r + 1, Colors.mulAlpha(0x22000000, a));
		}
		float op = c.panelOpacity * a;
		int top = Colors.mulAlpha(c.bgColor, op);
		int bottom = c.gradient ? Colors.mulAlpha(c.bgColor2, op) : top;
		Draw.rrect(g, 0, 0, w, h, r, top, bottom);
		if (flash > 0.01f) Draw.rrect(g, 0, 0, w, h, r, Colors.withAlpha(flashColor, Math.round(70 * flash * a)));
		int border = Colors.lerp(c.borderColor, flashColor, flash * 0.9f);
		Draw.rrectBorder(g, 0, 0, w, h, r, t, Colors.mulAlpha(border, a));

		int pad = c.padding;
		int stripe = c.accentStripe ? 2 : 0;
		if (stripe > 0) {
			int sy = Math.max(t, r), sh2 = h - 2 * sy;
			Draw.fill(g, t, sy, stripe, sh2, Colors.mulAlpha(c.accentColor, a));
		}
		int left = t + stripe + pad;
		int right = w - t - pad;
		int ty = pad;
		boolean sh = c.textShadow;

		if (lay.title()) {
			Draw.textScaled(g, font, c.title, left, ty, Colors.mulAlpha(c.accentColor, a), sh, c.boldTitle, ts);
			ty += lay.lh() + 2;
			Draw.fill(g, left, ty, right - left, 1, Colors.mulAlpha(c.borderColor, a));
			ty += 4;
		}
		for (HudRenderer.Row row : lay.rows()) {
			if (row.kind() == KIND_ESTIMATE && lay.rows().size() > 1) {
				Draw.fill(g, left, ty, right - left, 1, Colors.mulAlpha(Colors.withAlpha(c.borderColor, 0x99), a));
				ty += 4;
			}
			int vc = row.valueColor();
			if (flash > 0.01f && ((row.kind() == KIND_EARN && anim.flashIncoming())
				|| (row.kind() == KIND_SPEND && !anim.flashIncoming())
				|| (row.kind() == KIND_NET && c.profitAnimation))) {
				vc = Colors.lerp(vc, 0xFFFFFFFF, flash * 0.75f);
			}
			int lc = Colors.mulAlpha(c.labelColor, a);
			Draw.textScaled(g, font, row.label(), left, ty, lc, sh, c.boldText, ts);
			int vw = (int) Math.ceil(Draw.tw(font, row.value(), c.boldText) * ts);
			Draw.textScaled(g, font, row.value(), right - vw, ty, Colors.mulAlpha(vc, a), sh, c.boldText, ts);
			ty += lay.rowH();
		}
		pose.popMatrix();
	}
}
