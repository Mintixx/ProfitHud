package dev.mintixx.profithud.gui;

import dev.mintixx.profithud.ProfitHudMod;
import dev.mintixx.profithud.config.ProfitConfig;
import dev.mintixx.profithud.hud.HudRenderer;
import dev.mintixx.profithud.hud.HudRenderer.PriceToastLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Drag-to-position mode: drag the HUD anywhere; the nearest of the 9 anchors is picked automatically. */
public final class HudDragScreen extends Screen {
	private final Screen parent;
	private final ProfitHudMod mod = ProfitHudMod.get();
	private boolean dragging;
	private boolean draggingToast;
	private float grabX, grabY;
	private float toastGrabX, toastGrabY;
	private int doneX, doneY, doneW = 70, doneH = 20;

	public HudDragScreen(Screen parent) {
		super(Component.literal("Drag HUD"));
		this.parent = parent;
	}

	@Override
	protected void init() { mod.suppressHud = true; }

	@Override
	public void removed() { mod.suppressHud = false; }

	@Override public boolean isPauseScreen() { return false; }

	private HudRenderer.Layout layout() {
		return mod.hud().layout(this.font, HudRenderer.sample());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
		super.extractRenderState(g, mx, my, delta);
		ProfitConfig c = mod.config();
		Draw.applyTheme(c);
		HudRenderer.Layout lay = layout();
		float[] o = mod.hud().origin(width, height, lay.w(), lay.h());

		// 3x3 anchor guide
		for (int i = 1; i < 3; i++) {
			Draw.fill(g, width * i / 3, 0, 1, height, 0x22FFFFFF);
			Draw.fill(g, 0, height * i / 3, width, 1, 0x22FFFFFF);
		}
		int pw = Math.round(lay.w() * c.scale), ph = Math.round(lay.h() * c.scale);
		Draw.outline(g, Math.round(o[0]) - 2, Math.round(o[1]) - 2, pw + 4, ph + 4, dragging ? Draw.ACCENT : Draw.ACCENT_DIM);
		mod.hud().drawPanel(g, this.font, o[0], o[1], c.scale, lay, 1f);
		PriceToastLayout toast = mod.hud().renderPriceToastPreview(g, this.font, width, height);
		boolean toastHover = mx >= toast.x() && mx < toast.x() + toast.width()
			&& my >= toast.y() && my < toast.y() + toast.height();
		if (draggingToast || toastHover)
			Draw.outline(g, toast.x() - 2, toast.y() - 2, toast.width() + 4, toast.height() + 4, Draw.ACCENT);

		Draw.textCentered(g, this.font, "Drag the HUD or popup to move it; scroll over either to resize",
			width / 2, height - 48, Draw.ACCENT, true);
		Draw.textCentered(g, this.font, "Arrow keys nudge the HUD  |  HUD scale: " + String.format(java.util.Locale.ROOT, "%.2f", c.scale)
			+ "  Popup scale: " + String.format(java.util.Locale.ROOT, "%.2f", c.priceToastScale),
			width / 2, height - 36, Draw.MUTED, true);
		doneX = width / 2 - doneW - 2;
		doneY = height - doneH - 10;
		Ui.drawButton(g, this.font, doneX, doneY, doneW, doneH, "Done", mx >= doneX && mx < doneX + doneW && my >= doneY && my < doneY + doneH, true);
		int rx = width / 2 + 2;
		Ui.drawButton(g, this.font, rx, doneY, doneW, doneH, "Reset pos", mx >= rx && mx < rx + doneW && my >= doneY && my < doneY + doneH, false);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent ev, boolean doubleClick) {
		int mx = (int) ev.x(), my = (int) ev.y();
		if (my >= doneY && my < doneY + doneH) {
			if (mx >= doneX && mx < doneX + doneW) { onClose(); return true; }
			int rx = width / 2 + 2;
			if (mx >= rx && mx < rx + doneW) {
				ProfitConfig d = new ProfitConfig();
				ProfitConfig c = mod.config();
				c.anchor = d.anchor; c.offsetX = d.offsetX; c.offsetY = d.offsetY;
				c.priceToastX = d.priceToastX; c.priceToastY = d.priceToastY; c.priceToastScale = d.priceToastScale;
				return true;
			}
		}
		ProfitConfig c = mod.config();
		PriceToastLayout toast = mod.hud().priceToastLayout(this.font, width, height,
			new dev.mintixx.profithud.core.InventoryScanner.PriceNotice("Token of Undying", 37_500, 42_000, false));
		if (mx >= toast.x() && mx <= toast.x() + toast.width() && my >= toast.y() && my <= toast.y() + toast.height()) {
			draggingToast = true;
			c.priceToastX = toast.x();
			c.priceToastY = toast.y();
			toastGrabX = mx - toast.x();
			toastGrabY = my - toast.y();
			return true;
		}
		HudRenderer.Layout lay = layout();
		float[] o = mod.hud().origin(width, height, lay.w(), lay.h());
		if (mx >= o[0] && mx <= o[0] + lay.w() * c.scale && my >= o[1] && my <= o[1] + lay.h() * c.scale) {
			dragging = true;
			grabX = mx - o[0];
			grabY = my - o[1];
			return true;
		}
		return super.mouseClicked(ev, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent ev, double dx, double dy) {
		if (draggingToast) {
			ProfitConfig c = mod.config();
			c.priceToastX = Math.round((float) ev.x() - toastGrabX);
			c.priceToastY = Math.round((float) ev.y() - toastGrabY);
			return true;
		}
		if (!dragging) return super.mouseDragged(ev, dx, dy);
		HudRenderer.Layout lay = layout();
		mod.hud().setOriginFromScreen(width, height, lay.w(), lay.h(), (float) ev.x() - grabX, (float) ev.y() - grabY);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent ev) {
		if (draggingToast) {
			draggingToast = false;
			return true;
		}
		if (dragging) {
			dragging = false;
			snapAnchor();
			return true;
		}
		return super.mouseReleased(ev);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy2) {
		ProfitConfig c = mod.config();
		PriceToastLayout toast = mod.hud().priceToastLayout(this.font, width, height,
			new dev.mintixx.profithud.core.InventoryScanner.PriceNotice("Token of Undying", 37_500, 42_000, false));
		if (mx >= toast.x() && mx <= toast.x() + toast.width() && my >= toast.y() && my <= toast.y() + toast.height()) {
			float centerX = toast.x() + toast.width() / 2f;
			float centerY = toast.y() + toast.height() / 2f;
			c.priceToastScale = Math.max(0.6f, Math.min(2f, c.priceToastScale + (sy2 > 0 ? 0.1f : -0.1f)));
			c.priceToastX = Math.round(centerX - toast.baseWidth() * c.priceToastScale / 2f);
			c.priceToastY = Math.round(centerY - toast.baseHeight() * c.priceToastScale / 2f);
			return true;
		}
		HudRenderer.Layout lay = layout();
		float[] origin = mod.hud().origin(width, height, lay.w(), lay.h());
		if (mx < origin[0] || mx > origin[0] + lay.w() * c.scale
			|| my < origin[1] || my > origin[1] + lay.h() * c.scale) return super.mouseScrolled(mx, my, sx, sy2);

		float centerX = origin[0] + lay.w() * c.scale / 2f;
		float centerY = origin[1] + lay.h() * c.scale / 2f;
		c.scale = Math.max(0.5f, Math.min(3f, c.scale + (sy2 > 0 ? 0.1f : -0.1f)));
		mod.hud().setOriginFromScreen(width, height, lay.w(), lay.h(),
			centerX - lay.w() * c.scale / 2f, centerY - lay.h() * c.scale / 2f);
		return true;
	}

	/** Picks the anchor whose third of the screen contains the panel centre, keeping the panel where it is. */
	private void snapAnchor() {
		ProfitConfig c = mod.config();
		HudRenderer.Layout lay = layout();
		float[] o = mod.hud().origin(width, height, lay.w(), lay.h());
		float cxp = o[0] + lay.w() * c.scale / 2f, cyp = o[1] + lay.h() * c.scale / 2f;
		int col = cxp < width / 3f ? 0 : cxp < width * 2 / 3f ? 1 : 2;
		int row = cyp < height / 3f ? 0 : cyp < height * 2 / 3f ? 1 : 2;
		c.anchor = row * 3 + col;
		mod.hud().setOriginFromScreen(width, height, lay.w(), lay.h(), o[0], o[1]);
	}

	@Override
	public boolean keyPressed(KeyEvent ev) {
		int step = (ev.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? 5 : 1;
		ProfitConfig c = mod.config();
		int col = c.anchor % 3, row = c.anchor / 3;
		// offsets grow towards the screen centre for right/bottom anchors
		int sx = col == 2 ? -1 : 1, sy = row == 2 ? -1 : 1;
		switch (ev.key()) {
			case GLFW.GLFW_KEY_LEFT -> c.offsetX += -step * sx;
			case GLFW.GLFW_KEY_RIGHT -> c.offsetX += step * sx;
			case GLFW.GLFW_KEY_UP -> c.offsetY += -step * sy;
			case GLFW.GLFW_KEY_DOWN -> c.offsetY += step * sy;
			default -> { return super.keyPressed(ev); }
		}
		return true;
	}

	@Override
	public void onClose() {
		mod.configManager().save();
		mod.suppressHud = false;
		minecraft.gui.setScreen(parent);
	}
}
