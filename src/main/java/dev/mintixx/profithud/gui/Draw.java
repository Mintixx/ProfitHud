package dev.mintixx.profithud.gui;

import dev.mintixx.profithud.config.ProfitConfig;
import dev.mintixx.profithud.util.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Low-level drawing helpers (rects, rounded rects, scaled/bold text) and the menu colour theme. */
public final class Draw {
	private Draw() {}

	// ---- Live theme from the selected HUD palette ----
	public static int WINDOW = 0xFF0C0E0F;
	public static int PANEL = 0xFF121617;
	public static int PANEL_2 = 0xFF181D1E;
	public static int PANEL_3 = 0xFF1F2627;
	public static int BORDER = 0xFF2A3335;
	public static int ACCENT = 0xFF1DE9B6;
	public static int ACCENT_DIM = 0xFF0E5E4D;
	public static int TEXT = 0xFFE6ECEA;
	public static int MUTED = 0xFF7F908D;
	public static int POS = 0xFF3DFF9A;
	public static int NEG = 0xFFFF4D5E;
	public static int WARN = 0xFFFFC857;

	public static void applyTheme(ProfitConfig config) {
		WINDOW = Colors.lerp(config.bgColor, 0xFF000000, 0.55f);
		PANEL = config.bgColor;
		PANEL_2 = config.bgColor2;
		PANEL_3 = Colors.lerp(config.bgColor2, config.textColor, 0.08f);
		BORDER = config.borderColor;
		ACCENT = config.accentColor;
		ACCENT_DIM = Colors.lerp(config.accentColor, config.bgColor, 0.7f);
		TEXT = config.textColor;
		MUTED = config.labelColor;
		POS = config.positiveColor;
		NEG = config.negativeColor;
		WARN = config.estimateColor;
	}

	public static void fill(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
		if (w <= 0 || h <= 0 || (c >>> 24) == 0) return;
		g.fill(x, y, x + w, y + h, c);
	}

	public static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
		fill(g, x, y, w, 1, c);
		fill(g, x, y + h - 1, w, 1, c);
		fill(g, x, y + 1, 1, h - 2, c);
		fill(g, x + w - 1, y + 1, 1, h - 2, c);
	}

	/** Horizontal inset of row i for a corner radius r in a box of height h. */
	private static int inset(int r, int i, int h) {
		if (r <= 0) return 0;
		int k;
		if (i < r) k = i;
		else if (i >= h - r) k = h - 1 - i;
		else return 0;
		double dy = r - k - 0.5;
		double dx = Math.sqrt(Math.max(0, (double) r * r - dy * dy));
		return (int) Math.round(r - dx);
	}

	/** Rounded rectangle with optional vertical gradient (top == bottom for a flat colour). */
	public static void rrect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int top, int bottom) {
		if (w <= 0 || h <= 0) return;
		r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
		if (top == bottom) {
			if (r == 0) { fill(g, x, y, w, h, top); return; }
			for (int i = 0; i < r; i++) {
				int in = inset(r, i, h);
				fill(g, x + in, y + i, w - 2 * in, 1, top);
			}
			fill(g, x, y + r, w, h - 2 * r, top);
			for (int i = h - r; i < h; i++) {
				int in = inset(r, i, h);
				fill(g, x + in, y + i, w - 2 * in, 1, top);
			}
			return;
		}
		for (int i = 0; i < h; i++) {
			int in = inset(r, i, h);
			fill(g, x + in, y + i, w - 2 * in, 1, Colors.lerp(top, bottom, h <= 1 ? 0f : (float) i / (h - 1)));
		}
	}

	public static void rrect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
		rrect(g, x, y, w, h, r, color, color);
	}

	/** Border ring only (does not paint the interior, so translucent panels stay clean). */
	public static void rrectBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int t, int c) {
		if (t <= 0 || w <= 0 || h <= 0) return;
		r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
		int ir = Math.max(0, r - t);
		int ih = h - 2 * t;
		for (int i = 0; i < h; i++) {
			int o = inset(r, i, h);
			if (i < t || i >= h - t || ih <= 0) {
				fill(g, x + o, y + i, w - 2 * o, 1, c);
			} else {
				int in = Math.max(t + inset(ir, i - t, ih), o);
				fill(g, x + o, y + i, in - o, 1, c);
				fill(g, x + w - in, y + i, in - o, 1, c);
			}
		}
	}

	// ---- Text ----
	private static Component bold(String s) { return Component.literal(s).withStyle(ChatFormatting.BOLD); }

	public static int tw(Font f, String s, boolean bold) {
		return bold ? f.width(bold(s)) : f.width(s);
	}

	public static void text(GuiGraphicsExtractor g, Font f, String s, int x, int y, int color, boolean shadow) {
		g.text(f, s, x, y, color, shadow);
	}

	public static void textBold(GuiGraphicsExtractor g, Font f, String s, int x, int y, int color, boolean shadow) {
		g.text(f, bold(s), x, y, color, shadow);
	}

	/** Draws text scaled around its top-left corner. */
	public static void textScaled(GuiGraphicsExtractor g, Font f, String s, float x, float y, int color,
								  boolean shadow, boolean bold, float scale) {
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(scale, scale);
		if (bold) g.text(f, bold(s), 0, 0, color, shadow);
		else g.text(f, s, 0, 0, color, shadow);
		pose.popMatrix();
	}

	public static void textRight(GuiGraphicsExtractor g, Font f, String s, int rightX, int y, int color, boolean shadow) {
		g.text(f, s, rightX - f.width(s), y, color, shadow);
	}

	public static void textCentered(GuiGraphicsExtractor g, Font f, String s, int cx, int y, int color, boolean shadow) {
		g.text(f, s, cx - f.width(s) / 2, y, color, shadow);
	}

	/** Trims a string (with "...") so that it fits into maxW pixels. */
	public static String fit(Font f, String s, int maxW) {
		if (f.width(s) <= maxW) return s;
		String e = "...";
		int ew = f.width(e);
		int end = s.length();
		while (end > 0 && f.width(s.substring(0, end)) + ew > maxW) end--;
		return s.substring(0, Math.max(0, end)) + e;
	}
}
