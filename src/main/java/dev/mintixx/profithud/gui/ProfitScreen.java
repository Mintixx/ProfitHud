package dev.mintixx.profithud.gui;

import dev.mintixx.profithud.ProfitHudMod;
import dev.mintixx.profithud.config.ProfitConfig;
import dev.mintixx.profithud.hud.HudData;
import dev.mintixx.profithud.hud.HudRenderer;
import dev.mintixx.profithud.input.Keybinds;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** The large custom PROFIT HUD menu: top bar, sidebar, scrollable content, bottom action bar. */
public final class ProfitScreen extends Screen {
	public static final String[] TAB_NAMES = {
		"Overview", "Tracker", "Inventory", "Item Prices", "Sessions", "Statistics",
		"Position", "Panel Style", "Colors", "Text & Font", "Animations", "More Settings"};
	public static final int T_OVERVIEW = 0, T_LIVE = 1, T_INV = 2, T_PRICES = 3, T_SESSIONS = 4, T_STATS = 5,
		T_LAYOUT = 6, T_STYLE = 7, T_COLORS = 8, T_TEXT = 9, T_EFFECTS = 10, T_ADVANCED = 11;

	private static final int TOP_H = 30, BOT_H = 34, ROW_H = 24;

	public final ProfitHudMod mod = ProfitHudMod.get();
	private int tab;
	private int winX, winY, winW, winH, sideW;
	private int cx, cy, cw, ch;       // content rectangle
	private int sy, sh;               // scrollable region
	private int previewH;
	private List<Ui.Ctl> controls = List.of();
	private ItemsPanel items;
	private int scroll, contentH;
	private int sidebarScroll;
	private Ui.Ctl dragging;
	private float indicatorY = -1f;
	private final long openMs = System.currentTimeMillis();
	private final Screen parent;
	private String status = "";
	private long statusMs;
	private long confirmResetMs;
	private long lastAutoSave = System.currentTimeMillis();

	public ProfitScreen(int tab) {
		this(tab, null);
	}

	public ProfitScreen(int tab, Screen parent) {
		super(Component.literal("PROFIT HUD"));
		this.tab = Math.floorMod(tab, TAB_NAMES.length);
		this.parent = parent;
	}

	public void setStatus(String s) { status = s; statusMs = System.currentTimeMillis(); }

	@Override public boolean isPauseScreen() { return false; }

	@Override
	protected void init() {
		Draw.applyTheme(mod.config());
		geometry();
		ensureSidebarTabVisible();
		build();
	}

	/** Called by tabs when their content must be rebuilt (e.g. after a reset). */
	public void build() {
		scroll = 0;
		dragging = null;
		items = null;
		controls = List.of();
		if (tab == T_PRICES) items = new ItemsPanel();
		else controls = Tabs.build(tab, this);
	}

	public void selectTab(int t) {
		if (t == tab) return;
		tab = t;
		ensureSidebarTabVisible();
		build();
	}

	/** Opens the Item Prices tab with a specific item selected. */
	public void openPrices(String itemId) {
		tab = T_PRICES;
		build();
		if (items != null) items.select(itemId);
	}

	private void geometry() {
		winW = Math.min(width - 4, Math.max(420, (int) (width * 0.80)));
		winH = Math.min(height - 4, Math.max(260, (int) (height * 0.80)));
		winX = (width - winW) / 2;
		winY = (height - winH) / 2;
		sideW = Math.max(96, Math.min(140, winW / 5));
		cx = winX + sideW + 1;
		cy = winY + TOP_H + 1;
		cw = winW - sideW - 1;
		ch = winH - TOP_H - BOT_H - 2;
		boolean preview = hasPreview();
		previewH = preview ? Math.min(108, Math.max(84, (int) (ch * 0.24))) : 0;
		sy = cy + previewH + (preview ? 4 : 0);
		sh = cy + ch - sy;
	}

	private int sidebarTop() { return winY + TOP_H + 1; }

	private int sidebarHeight() { return Math.max(0, winY + winH - 1 - sidebarTop()); }

	private int maxSidebarScroll() {
		return Math.max(0, 6 + TAB_NAMES.length * ROW_H - sidebarHeight());
	}

	private void ensureSidebarTabVisible() {
		int top = sidebarTop();
		int rowY = top + 6 + tab * ROW_H - sidebarScroll;
		int bottom = top + sidebarHeight();
		if (rowY + ROW_H - 2 > bottom) sidebarScroll = rowY + ROW_H - 2 - bottom;
		else if (rowY < top) sidebarScroll -= top - rowY;
		sidebarScroll = Math.max(0, Math.min(sidebarScroll, maxSidebarScroll()));
	}

	private boolean hasPreview() {
		return tab == T_OVERVIEW || (tab >= T_LAYOUT && tab <= T_EFFECTS);
	}

	// ------------------------------------------------------------------ rendering

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
		super.extractRenderState(g, mx, my, delta);
		Draw.applyTheme(mod.config());
		Font f = this.font;
		geometry();
		long nowMs = System.currentTimeMillis();
		if (nowMs - lastAutoSave > 3000) { mod.configManager().save(); lastAutoSave = nowMs; }

		// window
		Draw.fill(g, winX - 2, winY - 2, winW + 4, winH + 4, 0x66000000);
		Draw.fill(g, winX, winY, winW, winH, Draw.WINDOW);
		Draw.outline(g, winX, winY, winW, winH, Draw.BORDER);

		renderTopBar(g, f, mx, my);
		renderSidebar(g, f, mx, my);

		Draw.fill(g, cx, cy, cw, ch, Draw.PANEL);
		if (previewH > 0) renderPreview(g, f);

		if (items != null) {
			items.layout(cx + 6, sy + 4, cw - 12, sh - 8);
			items.render(g, f, mx, my);
		} else {
			renderControls(g, f, mx, my);
		}
		renderBottomBar(g, f, mx, my);

		// subtle open fade
		long t = System.currentTimeMillis() - openMs;
		if (t < 180) Draw.fill(g, winX, winY, winW, winH, ((int) (170 * (1f - t / 180f))) << 24);
	}

	private void renderTopBar(GuiGraphicsExtractor g, Font f, int mx, int my) {
		Draw.fill(g, winX + 1, winY + 1, winW - 2, TOP_H - 1, Draw.PANEL);
		Draw.fill(g, winX + 1, winY + TOP_H, winW - 2, 1, Draw.BORDER);
		Draw.fill(g, winX + 1, winY + TOP_H, 56, 1, Draw.ACCENT);
		int right = winX + winW - 6;
		Ui.drawButton(g, f, right - 22, winY + 6, 22, 18, "X", over(mx, my, right - 22, winY + 6, 22, 18), false);
		boolean compact = winW < 500;
		int reservedRight = compact ? 34 : 106;
		if (!compact) {
			int resetX = right - 100;
			Ui.drawButton(g, f, resetX, winY + 6, 66, 18, "Reset tab", over(mx, my, resetX, winY + 6, 66, 18), false);
			String hint = "[RSHIFT] close";
			int hx = resetX - 10 - f.width(hint);
			if (hx > winX + 200) Draw.text(g, f, hint, hx, winY + 12, Draw.MUTED, false);
		}
		float titleScale = compact ? 1.1f : 1.35f;
		int titleX = winX + 10;
		Draw.textScaled(g, f, "PROFIT HUD", titleX, winY + 9, Draw.ACCENT, true, true, titleScale);
		int creditX = titleX + (int) (Draw.tw(f, "PROFIT HUD", true) * titleScale) + 6;
		int creditMaxW = right - reservedRight - 6 - creditX;
		String credit = compact ? "by Mintixx_" : "v" + ProfitHudMod.VERSION + "  by Mintixx_";
		if (creditMaxW > 0) Draw.text(g, f, Draw.fit(f, credit, creditMaxW), creditX, winY + 12, Draw.MUTED, false);
	}

	private void renderSidebar(GuiGraphicsExtractor g, Font f, int mx, int my) {
		int sx = winX + 1, top = sidebarTop(), height = sidebarHeight();
		Draw.fill(g, sx, top, sideW, height, 0xFF0F1213);
		Draw.fill(g, sx + sideW - 1, top, 1, height, Draw.BORDER);
		sidebarScroll = Math.max(0, Math.min(sidebarScroll, maxSidebarScroll()));
		float target = top + 6 + tab * ROW_H - sidebarScroll;
		if (indicatorY < 0) indicatorY = target;
		indicatorY += (target - indicatorY) * 0.3f;
		g.enableScissor(sx, top, sx + sideW - 1, top + height);
		for (int i = 0; i < TAB_NAMES.length; i++) {
			int y = top + 6 + i * ROW_H - sidebarScroll;
			if (y + ROW_H - 2 <= top || y >= top + height) continue;
			boolean sel = i == tab;
			boolean hv = over(mx, my, sx, y, sideW - 1, ROW_H - 2);
			if (sel) Draw.fill(g, sx, y, sideW - 1, ROW_H - 2, 0xFF14201E);
			else if (hv) Draw.fill(g, sx, y, sideW - 1, ROW_H - 2, Draw.PANEL_2);
			float labelScale = 1.0f;
			int labelX = sx + 10;
			String label = Draw.fit(f, TAB_NAMES[i], Math.max(1, (int) ((sideW - 20) / labelScale) - 6));
			Draw.textScaled(g, f, label, labelX, y + 7, sel ? Draw.ACCENT : (hv ? Draw.TEXT : Draw.MUTED),
				false, false, labelScale);
			if (i == T_STATS) Draw.fill(g, sx + 8, y + ROW_H - 1, sideW - 18, 1, Draw.BORDER); // separator before customization tabs
		}
		Draw.fill(g, sx, Math.round(indicatorY), 2, ROW_H - 2, Draw.ACCENT);
		g.disableScissor();
	}

	private void renderPreview(GuiGraphicsExtractor g, Font f) {
		int x = cx + 6, y = cy + 6, w = cw - 12, h = previewH - 6;
		Draw.fill(g, x, y, w, h, 0xFF0A0C0D);
		// subtle grid so translucency is visible
		for (int gx = x; gx < x + w; gx += 12) Draw.fill(g, gx, y, 1, h, 0xFF101415);
		for (int gy = y; gy < y + h; gy += 12) Draw.fill(g, x, gy, w, 1, 0xFF101415);
		Draw.outline(g, x, y, w, h, Draw.BORDER);
		Draw.text(g, f, "LIVE PREVIEW", x + 5, y + 4, Draw.MUTED, false);

		HudRenderer hud = mod.hud();
		ProfitConfig c = mod.config();
		HudData d = tab == T_OVERVIEW ? hud.live() : HudRenderer.sample();
		HudRenderer.Layout lay = hud.layout(f, d);
		float fit = Math.min(2.2f, Math.min((w - 20) / (float) lay.w(), (h - 22) / (float) lay.h()));
		fit = Math.max(0.3f, fit);
		float px = x + (w - lay.w() * fit) / 2f;
		float py = y + 14 + (h - 14 - lay.h() * fit) / 2f;
		hud.drawPanel(g, f, px, py, fit, lay, 1f);
		if (tab != T_OVERVIEW) Draw.textRight(g, f, "sample values", x + w - 5, y + 4, Draw.MUTED, false);
	}

	private void renderControls(GuiGraphicsExtractor g, Font f, int mx, int my) {
		int x = cx + 8, w = cw - 16 - 6;
		int y = sy - scroll + 2;
		for (Ui.Ctl c : controls) {
			c.ax = x; c.ay = y; c.aw = w;
			y += c.h + 2;
		}
		contentH = y - (sy - scroll) + 4;
		scroll = Math.max(0, Math.min(scroll, Math.max(0, contentH - sh)));

		g.enableScissor(cx, sy, cx + cw, sy + sh);
		int cmx = my >= sy && my < sy + sh ? mx : -1000;
		int cmy = my >= sy && my < sy + sh ? my : -1000;
		for (Ui.Ctl c : controls) {
			if (c.ay + c.h < sy || c.ay > sy + sh) continue;
			c.render(g, f, cmx, cmy);
		}
		g.disableScissor();

		if (contentH > sh) {
			int trackX = cx + cw - 5;
			Draw.fill(g, trackX, sy, 3, sh, Draw.PANEL_2);
			int th = Math.max(16, sh * sh / contentH);
			int ty = sy + (int) ((sh - th) * (scroll / (float) (contentH - sh)));
			Draw.fill(g, trackX, ty, 3, th, Draw.ACCENT_DIM);
		}
	}

	private void renderBottomBar(GuiGraphicsExtractor g, Font f, int mx, int my) {
		int by = winY + winH - BOT_H;
		Draw.fill(g, winX + sideW + 1, by - 1, winW - sideW - 2, 1, Draw.BORDER);
		Draw.fill(g, winX + sideW + 1, by, winW - sideW - 2, BOT_H - 1, Draw.PANEL);
		int bw = bottomButtonWidth();
		int x = winX + winW - 6 - bw;
		int y = by + 7;
		String[] labels = bw < 75
			? new String[] {resetLabelShort(), "New", "Save", "Test", "Drag"}
			: new String[] {resetLabel(), "New session", "Save settings", "Test HUD", "Drag position"};
		for (int i = 4; i >= 0; i--) {
			boolean primary = i == 2;
			Ui.drawButton(g, f, x, y, bw, 20, labels[i], over(mx, my, x, y, bw, 20), primary);
			x -= bw + 4;
		}
		int statusWidth = x + bw - (cx + 8);
		if (!status.isEmpty() && statusWidth >= f.width("...") && System.currentTimeMillis() - statusMs < 4000) {
			Draw.text(g, f, Draw.fit(f, status, statusWidth), cx + 8, y + 6, Draw.ACCENT, false);
		}
	}

	private String resetLabel() {
		return System.currentTimeMillis() - confirmResetMs < 3000 ? "Confirm reset?" : "Reset settings";
	}

	private String resetLabelShort() {
		return System.currentTimeMillis() - confirmResetMs < 3000 ? "Confirm?" : "Reset";
	}

	private int bottomButtonWidth() {
		return Math.max(50, Math.min(96, (cw - 16 - 16) / 5));
	}

	private static boolean over(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(MouseButtonEvent ev, boolean doubleClick) {
		int mx = (int) ev.x(), my = (int) ev.y(), btn = ev.button();
		// top bar
		int right = winX + winW - 6;
		if (over(mx, my, right - 22, winY + 6, 22, 18)) { onClose(); return true; }
		if (winW >= 500 && over(mx, my, right - 100, winY + 6, 66, 18)) { resetTab(); return true; }
		// sidebar
		int top = sidebarTop();
		if (mx >= winX + 1 && mx < winX + sideW && my >= top && my < top + sidebarHeight()) {
			for (int i = 0; i < TAB_NAMES.length; i++) {
				if (over(mx, my, winX + 1, top + 6 + i * ROW_H - sidebarScroll, sideW - 1, ROW_H - 2)) { selectTab(i); return true; }
			}
		}
		// bottom bar
		int by = winY + winH - BOT_H;
		int bw = bottomButtonWidth();
		int x = winX + winW - 6 - bw;
		for (int i = 4; i >= 0; i--) {
			if (over(mx, my, x, by + 7, bw, 20)) { bottomAction(i); return true; }
			x -= bw + 4;
		}
		// content
		if (items != null) {
			items.click(mx, my, btn);
			return true;
		}
		if (my >= sy && my < sy + sh && mx >= cx && mx < cx + cw) {
			for (Ui.Ctl c : controls) {
				if (c.hit(mx, my)) {
					for (Ui.Ctl o : controls) if (o != c) o.blur();
					if (c.click(mx, my, btn)) dragging = c;
					return true;
				}
			}
		}
		for (Ui.Ctl c : controls) c.blur();
		return super.mouseClicked(ev, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent ev, double dx, double dy) {
		if (dragging != null) { dragging.drag((int) ev.x(), (int) ev.y()); return true; }
		return super.mouseDragged(ev, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent ev) {
		if (dragging != null) { dragging.release(); dragging = null; }
		return super.mouseReleased(ev);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy2) {
		int sidebarTop = sidebarTop();
		if (mx >= winX + 1 && mx < winX + sideW && my >= sidebarTop && my < sidebarTop + sidebarHeight()) {
			sidebarScroll = Math.max(0, Math.min(maxSidebarScroll(), sidebarScroll - (int) (sy2 * ROW_H)));
			return true;
		}
		if (items != null && items.scroll((int) mx, (int) my, sy2)) return true;
		if (mx >= cx && mx < cx + cw && my >= sy && my < sy + sh) {
			scroll = Math.max(0, Math.min(Math.max(0, contentH - sh), scroll - (int) (sy2 * 20)));
			return true;
		}
		return super.mouseScrolled(mx, my, sx, sy2);
	}

	private boolean anyFocus() {
		if (items != null) return items.focused();
		for (Ui.Ctl c : controls) if (c.focused()) return true;
		return false;
	}

	private void blurAll() {
		if (items != null) items.blur();
		for (Ui.Ctl c : controls) c.blur();
	}

	@Override
	public boolean keyPressed(KeyEvent ev) {
		int key = ev.key();
		if (anyFocus()) {
			if (key == GLFW.GLFW_KEY_ESCAPE) { blurAll(); return true; }
			if (items != null && items.key(key, ev.modifiers())) return true;
			for (Ui.Ctl c : controls) if (c.focused() && c.key(key, ev.modifiers())) return true;
			return true; // swallow while typing so letters never trigger anything else
		}
		if (key == GLFW.GLFW_KEY_RIGHT_SHIFT || (Keybinds.OPEN != null && Keybinds.OPEN.matches(ev))) {
			onClose();
			return true;
		}
		return super.keyPressed(ev);
	}

	@Override
	public boolean charTyped(CharacterEvent ev) {
		String s = ev.codepointAsString();
		if (items != null && items.typed(s)) return true;
		for (Ui.Ctl c : controls) if (c.focused() && c.typed(s)) return true;
		return super.charTyped(ev);
	}

	@Override
	public void onClose() {
		mod.configManager().save();
		if (parent != null) minecraft.gui.setScreen(parent);
		else super.onClose();
	}

	// ------------------------------------------------------------------ actions

	private void bottomAction(int i) {
		ProfitConfig c = mod.config();
		switch (i) {
			case 0 -> {
				long now = System.currentTimeMillis();
				if (now - confirmResetMs < 3000) {
					c.copyAllFrom(new ProfitConfig());
					mod.configManager().save();
					confirmResetMs = 0;
					build();
					setStatus("All settings reset to defaults (prices and history untouched).");
				} else {
					confirmResetMs = now;
				}
			}
			case 1 -> {
				mod.sessions().startNew();
				setStatus("New session started.");
			}
			case 2 -> {
				mod.configManager().save();
				mod.prices().save();
				mod.sessions().saveNow();
				setStatus("Settings saved.");
			}
			case 3 -> {
				mod.hud().startTest(6000);
				onClose();
			}
			case 4 -> minecraft.gui.setScreen(new HudDragScreen(new ProfitScreen(tab)));
			default -> { }
		}
	}

	private void resetTab() {
		ProfitConfig c = mod.config();
		ProfitConfig d = new ProfitConfig();
		switch (tab) {
			case T_LAYOUT -> c.copyLayoutFrom(d);
			case T_STYLE -> c.copyStyleFrom(d);
			case T_COLORS -> c.copyColorsFrom(d);
			case T_TEXT -> c.copyTextFrom(d);
			case T_EFFECTS -> c.copyEffectsFrom(d);
			case T_ADVANCED -> c.copyAdvancedFrom(d);
			default -> { setStatus("Nothing to reset on this tab."); return; }
		}
		mod.configManager().save();
		build();
		setStatus(TAB_NAMES[tab] + " reset to defaults.");
	}
}
