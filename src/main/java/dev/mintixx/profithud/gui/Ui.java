package dev.mintixx.profithud.gui;

import dev.mintixx.profithud.config.ProfitConfig;
import dev.mintixx.profithud.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Tiny custom widget kit used by the PROFIT HUD menu (instead of vanilla widgets). */
public final class Ui {
	private Ui() {}

	public static String clipboard() {
		try {
			String s = GLFW.glfwGetClipboardString(Minecraft.getInstance().getWindow().handle());
			return s == null ? "" : s;
		} catch (Throwable t) {
			return "";
		}
	}

	public static List<String> wrap(Font f, String s, int w) {
		List<String> out = new ArrayList<>();
		if (w < 20) { out.add(s); return out; }
		StringBuilder line = new StringBuilder();
		for (String word : s.split(" ")) {
			String t = line.length() == 0 ? word : line + " " + word;
			if (f.width(t) > w && line.length() > 0) {
				out.add(line.toString());
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(t);
			}
		}
		if (line.length() > 0) out.add(line.toString());
		return out;
	}

	/** Base control. ax/ay/aw are absolute screen coordinates assigned by the screen every frame. */
	public abstract static class Ctl {
		public int h = 20, ax, ay, aw;

		public abstract void render(GuiGraphicsExtractor g, Font f, int mx, int my);
		public boolean click(int mx, int my, int btn) { return false; }
		public void drag(int mx, int my) { }
		public void release() { }
		public boolean key(int key, int mods) { return false; }
		public boolean typed(String s) { return false; }
		public void blur() { }
		public boolean focused() { return false; }
		public boolean hit(int mx, int my) { return mx >= ax && mx < ax + aw && my >= ay && my < ay + h; }
	}

	// ------------------------------------------------------------------ static display

	public static final class Header extends Ctl {
		private final String text;
		public Header(String text) { this.text = text; this.h = 26; }
		@Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my) {
			Draw.textBold(g, f, text, ax, ay + 10, Draw.ACCENT, false);
			Draw.fill(g, ax, ay + h - 3, aw, 1, Draw.BORDER);
			Draw.fill(g, ax, ay + h - 3, 28, 1, Draw.ACCENT);
		}
	}

	public static final class Spacer extends Ctl {
		public Spacer(int h) { this.h = h; }
		@Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my) { }
	}

	public static final class Note extends Ctl {
		private final Supplier<String> text;
		private final int color;
		public Note(String text, int color) { this(() -> text, color); }
		public Note(Supplier<String> text, int color) { this.text = text; this.color = color; this.h = 14; }
		@Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my) {
			List<String> lines = wrap(f, text.get(), aw);
			h = lines.size() * 11 + 4;
			for (int i = 0; i < lines.size(); i++) Draw.text(g, f, lines.get(i), ax, ay + 2 + i * 11, color, false);
		}
	}

	public static final class StatRow extends Ctl {
		private final String label;
		private final Supplier<String> value;
		private final Supplier<Integer> color;
		public StatRow(String label, Supplier<String> value, Supplier<Integer> color) {
			this.label = label; this.value = value; this.color = color; this.h = 18;
		}
		@Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my) {
			Draw.fill(g, ax, ay, aw, h - 1, Draw.PANEL_2);
			Draw.fill(g, ax, ay, 2, h - 1, Draw.ACCENT_DIM);
			Draw.text(g, f, label, ax + 8, ay + 5, Draw.MUTED, false);
			Draw.textRight(g, f, value.get(), ax + aw - 6, ay + 5, color.get(), false);
		}
	}

	public interface Painter { void paint(GuiGraphicsExtractor g, Font f, int x, int y, int w, int h, int mx, int my); }

	public static final class Custom extends Ctl {
		private final Painter painter;
		public Custom(int h, Painter p) { this.h = h; this.painter = p; }
		@Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my) { painter.paint(g, f, ax, ay, aw, h, mx, my); }
	}

	// ------------------------------------------------------------------ buttons

	public static final class Btn extends Ctl {
		private final Supplier<String> label;
		private final Runnable action;
		private final boolean primary;
		public Btn(String label, Runnable action) { this(() -> label, action, false); }
		public Btn(Supplier<String> label, Runnable action, boolean primary) {
			this.label = label; this.action = action; this.primary = primary; this.h = 20;
		}
		@Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my) {
			drawButton(g, f, ax, ay, aw, h, label.get(), hit(mx, my), primary);
		}
		@Override public boolean click(int mx, int my, int btn) {
			if (btn == 0) action.run();
			return true;
		}
	}

	public static void drawButton(GuiGraphicsExtractor g, Font f, int x, int y, int w, int h, String text, boolean hover, boolean primary) {
		int bg = primary ? (hover ? Colors.lerp(Draw.ACCENT, 0xFFFFFFFF, 0.12f) : Draw.ACCENT) : (hover ? Draw.PANEL_3 : Draw.PANEL_2);
		int border = primary ? Draw.ACCENT : (hover ? Draw.ACCENT_DIM : Draw.BORDER);
		Draw.rrect(g, x, y, w, h, 2, bg);
		Draw.rrectBorder(g, x, y, w, h, 2, 1, border);
		int tc = primary ? 0xFF05110E : (hover ? Draw.ACCENT : Draw.TEXT);
		Draw.textCentered(g, f, Draw.fit(f, text, w - 6), x + w / 2, y + (h - 8) / 2, tc, false);
	}

	/** Lays several controls side by side. */
	public static final class Split extends Ctl {
		private final Ctl[] kids;
		private final int gap = 4;
		public Split(Ctl... kids) { this.kids = kids; }
		private void place() {
			int n = kids.length;
			int w = (aw - gap * (n - 1)) / n;
			int mh = 0;
			for (int i = 0; i < n; i++) {
				kids[i].ax = ax + i * (w + gap);
				kids[i].ay = ay;
				kids[i].aw = w;
				mh = Math.max(mh, kids[i].h);
			}
			h = mh;
		}
		@Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my) {
			place();
			for (Ctl k : kids) k.render(g, f, mx, my);
		}
		@Override public boolean click(int mx, int my, int btn) {
			place();
			for (Ctl k : kids) { if (k.hit(mx, my)) return k.click(mx, my, btn); }
			return false;
		}
		@Override public void blur() { for (Ctl k : kids) k.blur(); }
		@Override public boolean focused() { for (Ctl k : kids) if (k.focused()) return true; return false; }
		@Override public boolean key(int key, int mods) { for (Ctl k : kids) if (k.focused()) return k.key(key, mods); return false; }
		@Override public boolean typed(String s) { for (Ctl k : kids) if (k.focused()) return k.typed(s); return false; }
	}

	public static final class PresetGrid extends Ctl {
		private final ProfitConfig config;
		private final Runnable onChange;
		private static final int COLUMNS = 4, GAP = 4, ROW_H = 20, ROW_GAP = 3;

		public PresetGrid(ProfitConfig config, Runnable onChange) {
			this.config = config;
			this.onChange = onChange;
			this.h = ROW_H * 2 + ROW_GAP;
		}

		private int cellWidth() { return (aw - GAP * (COLUMNS - 1)) / COLUMNS; }

		@Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my) {
			int cellW = cellWidth();
			for (int i = 0; i < ProfitConfig.PRESET_NAMES.length; i++) {
				int bx = ax + (i % COLUMNS) * (cellW + GAP);
				int by = ay + (i / COLUMNS) * (ROW_H + ROW_GAP);
				boolean selected = config.preset == i;
				boolean hover = mx >= bx && mx < bx + cellW && my >= by && my < by + ROW_H;
				Draw.rrect(g, bx, by, cellW, ROW_H, 2, selected ? Draw.PANEL_3 : hover ? Draw.PANEL_2 : Draw.PANEL);
				Draw.rrectBorder(g, bx, by, cellW, ROW_H, 2, 1, selected ? ProfitConfig.presetAccent(i) : Draw.BORDER);
				Draw.fill(g, bx + 5, by + 5, 3, ROW_H - 10, ProfitConfig.presetAccent(i));
				Draw.textCentered(g, f, ProfitConfig.PRESET_NAMES[i], bx + cellW / 2 + 3, by + 6,
					selected ? ProfitConfig.presetAccent(i) : Draw.TEXT, false);
			}
		}

		@Override public boolean click(int mx, int my, int btn) {
			if (btn != 0) return true;
			int cellW = cellWidth();
			for (int i = 0; i < ProfitConfig.PRESET_NAMES.length; i++) {
				int bx = ax + (i % COLUMNS) * (cellW + GAP);
				int by = ay + (i / COLUMNS) * (ROW_H + ROW_GAP);
				if (mx >= bx && mx < bx + cellW && my >= by && my < by + ROW_H) {
					config.applyPreset(i);
					onChange.run();
					return true;
				}
			}
			return false;
		}
	}

	// ------------------------------------------------------------------ value controls

	public static final class Toggle extends Ctl {
		private final String label;
		private final BooleanSupplier get;
		private final Consumer<Boolean> set;
		private float anim = -1f;
		public Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
			this.label = label; this.get = get; this.set = set; this.h = 20;
		}
		@Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my) {
			boolean on = get.getAsBoolean();
			if (anim < 0) anim = on ? 1f : 0f;
			anim += ((on ? 1f : 0f) - anim) * 0.35f;
			boolean hover = hit(mx, my);
			Draw.fill(g, ax, ay, aw, h - 1, hover ? Draw.PANEL_3 : Draw.PANEL_2);
			Draw.text(g, f, label, ax + 8, ay + 6, Draw.TEXT, false);
			int tw = 28, th = 12;
			int tx = ax + aw - tw - 8, ty = ay + (h - th) / 2 - 1;
			Draw.rrect(g, tx, ty, tw, th, 5, Colors.lerp(Draw.PANEL_3, Draw.ACCENT_DIM, anim));
			Draw.rrectBorder(g, tx, ty, tw, th, 5, 1, Colors.lerp(Draw.BORDER, Draw.ACCENT, anim));
			int kx = tx + 2 + Math.round((tw - th) * anim);
			Draw.rrect(g, kx, ty + 2, th - 4, th - 4, 2, Colors.lerp(Draw.MUTED, Draw.ACCENT, anim));
		}
		@Override public boolean click(int mx, int my, int btn) {
			if (btn == 0) set.accept(!get.getAsBoolean());
			return true;
		}
	}

	public static final class Slider extends Ctl {
		private final String label;
		private final double min, max, step;
		private final DoubleSupplier get;
		private final DoubleConsumer set;
		private final DoubleFunction<String> fmt;
		private boolean dragging;
		public Slider(String label, double min, double max, double step, DoubleSupplier get, DoubleConsumer set, DoubleFunction<String> fmt) {
			this.label = label; this.min = min; this.max = max; this.step = step;
			this.get = get; this.set = set; this.fmt = fmt; this.h = 20;
		}
		private int lw() { return (int) (aw * 0.40); }
		private int trackX() { return ax + lw(); }
		private int trackW() { return Math.max(10, aw - lw() - 52); }
		@Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my) {
			Draw.fill(g, ax, ay, aw, h - 1, hit(mx, my) ? Draw.PANEL_3 : Draw.PANEL_2);
			Draw.text(g, f, label, ax + 8, ay + 6, Draw.TEXT, false);
			double v = get.getAsDouble();
			float t = (float) ((v - min) / (max - min));
			t = Math.max(0f, Math.min(1f, t));
			int tx = trackX(), tw = trackW(), cy = ay + h / 2 - 1;
			Draw.fill(g, tx, cy - 1, tw, 3, Draw.PANEL);
			Draw.fill(g, tx, cy - 1, Math.round(tw * t), 3, Draw.ACCENT);
			int kx = tx + Math.round(tw * t);
			Draw.fill(g, kx - 2, cy - 4, 5, 9, dragging ? 0xFFFFFFFF : Draw.ACCENT);
			Draw.textRight(g, f, fmt.apply(v), ax + aw - 6, ay + 6, Draw.MUTED, false);
		}
		private void apply(int mx) {
			double t = (mx - trackX()) / (double) trackW();
			t = Math.max(0, Math.min(1, t));
			double v = min + t * (max - min);
			if (step > 0) v = Math.round(v / step) * step;
			v = Math.max(min, Math.min(max, v));
			set.accept(v);
		}
		@Override public boolean click(int mx, int my, int btn) {
			if (btn != 0) return true;
			dragging = true;
			apply(mx);
			return true;
		}
		@Override public void drag(int mx, int my) { if (dragging) apply(mx); }
		@Override public void release() { dragging = false; }
	}

	public static final class Cycle extends Ctl {
		private final String label;
		private final List<String> options;
		private final IntSupplier get;
		private final IntConsumer set;
		public Cycle(String label, List<String> options, IntSupplier get, IntConsumer set) {
			this.label = label; this.options = options; this.get = get; this.set = set; this.h = 20;
		}
		@Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my) {
			boolean hover = hit(mx, my);
			Draw.fill(g, ax, ay, aw, h - 1, hover ? Draw.PANEL_3 : Draw.PANEL_2);
			Draw.text(g, f, label, ax + 8, ay + 6, Draw.TEXT, false);
			String v = options.get(Math.floorMod(get.getAsInt(), options.size()));
			Draw.textRight(g, f, "< " + v + " >", ax + aw - 8, ay + 6, Draw.ACCENT, false);
		}
		@Override public boolean click(int mx, int my, int btn) {
			int d = btn == 1 ? -1 : 1;
			set.accept(Math.floorMod(get.getAsInt() + d, options.size()));
			return true;
		}
	}

	public static final class ColorCtl extends Ctl {
		private static final int[] CHAN_COLORS = {0xFFE5534B, 0xFF4CC38A, 0xFF4C8DE5, 0xFFB0B8B6};
		private static final String[] CHAN = {"R", "G", "B", "A"};
		private final String label;
		private final IntSupplier get;
		private final IntConsumer set;
		private int dragChan = -1;
		public ColorCtl(String label, IntSupplier get, IntConsumer set) {
			this.label = label; this.get = get; this.set = set; this.h = 22;
		}
		private int lw() { return Math.max(60, (int) (aw * 0.24)); }
		private int sx() { return ax + lw() + 22; }
		private int sw() { return Math.max(20, (ax + aw - 6 - sx() - 3 * 4) / 4); }
		private int chanX(int i) { return sx() + i * (sw() + 4); }
		@Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my) {
			int c = get.getAsInt();
			Draw.fill(g, ax, ay, aw, h - 1, Draw.PANEL_2);
			Draw.text(g, f, Draw.fit(f, label, lw() - 8), ax + 8, ay + 7, Draw.TEXT, false);
			int swx = ax + lw();
			Draw.fill(g, swx, ay + 3, 16, 15, 0xFF000000);
			Draw.fill(g, swx + 1, ay + 4, 14, 13, 0xFF404040);
			Draw.fill(g, swx + 1, ay + 4, 14, 13, c);
			Draw.fill(g, swx + 1, ay + 4, 14, 13, Colors.withAlpha(c, 0xFF));
			Draw.outline(g, swx, ay + 3, 16, 15, Draw.BORDER);
			int[] v = {Colors.r(c), Colors.g(c), Colors.b(c), Colors.a(c)};
			for (int i = 0; i < 4; i++) {
				int x = chanX(i), w = sw();
				Draw.fill(g, x, ay + 4, w, 13, Draw.PANEL);
				Draw.fill(g, x, ay + 4, Math.round(w * (v[i] / 255f)), 13, Colors.withAlpha(CHAN_COLORS[i], 0xA0));
				Draw.text(g, f, w >= 38 ? CHAN[i] + " " + v[i] : String.valueOf(v[i]), x + 3, ay + 7, 0xFFFFFFFF, false);
			}
		}
		private int chanAt(int mx) {
			for (int i = 0; i < 4; i++) if (mx >= chanX(i) && mx < chanX(i) + sw() + 4) return i;
			return -1;
		}
		private void apply(int chan, int mx) {
			int v = Math.round(Math.max(0f, Math.min(1f, (mx - chanX(chan)) / (float) sw())) * 255f);
			int c = get.getAsInt();
			int a = Colors.a(c), r = Colors.r(c), gg = Colors.g(c), b = Colors.b(c);
			switch (chan) {
				case 0 -> r = v;
				case 1 -> gg = v;
				case 2 -> b = v;
				default -> a = v;
			}
			set.accept(Colors.argb(a, r, gg, b));
		}
		@Override public boolean click(int mx, int my, int btn) {
			int ch = chanAt(mx);
			if (ch < 0 || btn != 0) return true;
			dragChan = ch;
			apply(ch, mx);
			return true;
		}
		@Override public void drag(int mx, int my) { if (dragChan >= 0) apply(dragChan, mx); }
		@Override public void release() { dragChan = -1; }
	}

	public static final class TextCtl extends Ctl {
		private final String label;
		private final Supplier<String> get;
		private final Consumer<String> set;
		private final String hint;
		private final int maxLen;
		private final String allowed; // null = anything printable
		private boolean focused;
		private String buf = "";

		public TextCtl(String label, String hint, int maxLen, String allowed, Supplier<String> get, Consumer<String> set) {
			this.label = label; this.hint = hint; this.maxLen = maxLen; this.allowed = allowed;
			this.get = get; this.set = set; this.h = 20;
		}
		@Override public boolean focused() { return focused; }
		@Override public void blur() { focused = false; }
		public void focus() { focused = true; }
		private int fx() { return label == null ? ax : ax + (int) (aw * 0.38); }

		@Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my) {
			if (!focused) buf = get.get();
			if (label != null) {
				Draw.fill(g, ax, ay, aw, h - 1, Draw.PANEL_2);
				Draw.text(g, f, label, ax + 8, ay + 6, Draw.TEXT, false);
			}
			int x = fx() + (label == null ? 0 : 0), w = ax + aw - x - (label == null ? 0 : 4);
			Draw.fill(g, x, ay + 1, w, h - 3, focused ? Draw.PANEL : Draw.PANEL_3);
			Draw.outline(g, x, ay + 1, w, h - 3, focused ? Draw.ACCENT : Draw.BORDER);
			String show = buf;
			int maxW = w - 10;
			while (!show.isEmpty() && f.width(show) > maxW) show = show.substring(1);
			if (buf.isEmpty() && !focused) {
				Draw.text(g, f, Draw.fit(f, hint == null ? "" : hint, maxW), x + 4, ay + 6, Draw.MUTED, false);
			} else {
				boolean caret = focused && (System.currentTimeMillis() / 500) % 2 == 0;
				Draw.text(g, f, show + (caret ? "_" : ""), x + 4, ay + 6, Draw.TEXT, false);
			}
		}
		@Override public boolean click(int mx, int my, int btn) {
			focused = true;
			buf = get.get();
			return true;
		}
		@Override public boolean typed(String s) {
			if (!focused) return false;
			for (int i = 0; i < s.length(); i++) {
				char ch = s.charAt(i);
				if (ch < 32 || ch == 127 || ch == '\u00a7') continue;
				if (allowed != null && allowed.indexOf(ch) < 0) continue;
				if (buf.length() >= maxLen) break;
				buf += ch;
			}
			set.accept(buf);
			return true;
		}
		@Override public boolean key(int key, int mods) {
			if (!focused) return false;
			boolean ctrl = (mods & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;
			if (key == GLFW.GLFW_KEY_BACKSPACE) {
				buf = ctrl || buf.isEmpty() ? "" : buf.substring(0, buf.length() - 1);
				set.accept(buf);
				return true;
			}
			if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { focused = false; return true; }
			if (ctrl && key == GLFW.GLFW_KEY_V) { typed(clipboard().replace('\n', ' ').replace('\r', ' ')); return true; }
			return key != GLFW.GLFW_KEY_ESCAPE; // swallow other keys while typing; ESC is handled by the screen
		}
	}

	/** 3x3 anchor picker. */
	public static final class AnchorGrid extends Ctl {
		private final IntSupplier get;
		private final IntConsumer set;
		public AnchorGrid(IntSupplier get, IntConsumer set) { this.get = get; this.set = set; this.h = 54; }
		@Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my) {
			Draw.fill(g, ax, ay, aw, h - 1, Draw.PANEL_2);
			Draw.text(g, f, "Screen anchor", ax + 8, ay + 8, Draw.TEXT, false);
			Draw.text(g, f, dev.mintixx.profithud.config.ProfitConfig.ANCHOR_NAMES[get.getAsInt()], ax + 8, ay + 24, Draw.ACCENT, false);
			int gx = ax + aw - 3 * 20 - 8, gy = ay + 4;
			Draw.fill(g, gx - 2, gy - 2, 3 * 20 + 3, 3 * 15 + 3, Draw.PANEL);
			for (int i = 0; i < 9; i++) {
				int cx = gx + (i % 3) * 20, cy = gy + (i / 3) * 15;
				boolean sel = get.getAsInt() == i;
				boolean hover = mx >= cx && mx < cx + 18 && my >= cy && my < cy + 13;
				Draw.fill(g, cx, cy, 18, 13, sel ? Draw.ACCENT : (hover ? Draw.PANEL_3 : Draw.BORDER));
			}
		}
		@Override public boolean click(int mx, int my, int btn) {
			int gx = ax + aw - 3 * 20 - 8, gy = ay + 4;
			for (int i = 0; i < 9; i++) {
				int cx = gx + (i % 3) * 20, cy = gy + (i / 3) * 15;
				if (mx >= cx && mx < cx + 18 && my >= cy && my < cy + 13) { set.accept(i); break; }
			}
			return true;
		}
	}

	/** Editable list of strings (regex patterns, ignored players, currency symbols). */
	public static final class ListEditor extends Ctl {
		private final String title;
		private final List<String> list;
		private final Runnable onChange;
		private final String hint;
		private final List<TextCtl> fields = new ArrayList<>();

		public ListEditor(String title, String hint, List<String> list, Runnable onChange) {
			this.title = title; this.hint = hint; this.list = list; this.onChange = onChange;
		}
		private void rebuild() {
			fields.clear();
			for (int i = 0; i < list.size(); i++) {
				final int idx = i;
				fields.add(new TextCtl(null, hint, 200, null,
					() -> idx < list.size() ? list.get(idx) : "",
					v -> { if (idx < list.size()) { list.set(idx, v); onChange.run(); } }));
			}
		}
		private void place() {
			if (fields.size() != list.size()) rebuild();
			int y = ay + 20;
			for (TextCtl t : fields) { t.ax = ax; t.ay = y; t.aw = aw - 22; y += 22; }
			h = 20 + fields.size() * 22 + 24;
		}
		@Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my) {
			place();
			Draw.text(g, f, title, ax + 2, ay + 6, Draw.TEXT, false);
			for (int i = 0; i < fields.size(); i++) {
				TextCtl t = fields.get(i);
				t.render(g, f, mx, my);
				boolean hv = mx >= ax + aw - 20 && mx < ax + aw - 2 && my >= t.ay + 1 && my < t.ay + 19;
				drawButton(g, f, ax + aw - 20, t.ay + 1, 18, 18, "x", hv, false);
			}
			int by = ay + 20 + fields.size() * 22 + 2;
			drawButton(g, f, ax, by, 70, 18, "+ Add", mx >= ax && mx < ax + 70 && my >= by && my < by + 18, false);
		}
		@Override public boolean click(int mx, int my, int btn) {
			place();
			for (TextCtl t : fields) t.blur();
			int by = ay + 20 + fields.size() * 22 + 2;
			if (mx >= ax && mx < ax + 70 && my >= by && my < by + 18) {
				list.add("");
				onChange.run();
				rebuild();
				place();
				fields.get(fields.size() - 1).focus();
				return true;
			}
			for (int i = 0; i < fields.size(); i++) {
				TextCtl t = fields.get(i);
				if (mx >= ax + aw - 20 && mx < ax + aw - 2 && my >= t.ay + 1 && my < t.ay + 19) {
					list.remove(i);
					onChange.run();
					rebuild();
					return true;
				}
				if (t.hit(mx, my)) { t.click(mx, my, btn); return true; }
			}
			return true;
		}
		@Override public void blur() { for (TextCtl t : fields) t.blur(); }
		@Override public boolean focused() { for (TextCtl t : fields) if (t.focused()) return true; return false; }
		@Override public boolean key(int key, int mods) { for (TextCtl t : fields) if (t.focused()) return t.key(key, mods); return false; }
		@Override public boolean typed(String s) { for (TextCtl t : fields) if (t.focused()) return t.typed(s); return false; }
	}

	/** Read-only (optionally clickable) list with optional item icons. */
	public static final class RowData {
		public ItemStack icon;
		public String left = "";
		public String right = "";
		public int leftColor = Draw.TEXT;
		public int rightColor = Draw.MUTED;
		public Object tag;
		public boolean selected;
		public RowData(String left, String right) { this.left = left; this.right = right; }
		public RowData colors(int l, int r) { leftColor = l; rightColor = r; return this; }
		public RowData icon(ItemStack s) { icon = s; return this; }
		public RowData tag(Object t) { tag = t; return this; }
	}

	public static final class ListView extends Ctl {
		private final Supplier<List<RowData>> rows;
		private final Consumer<RowData> onClick;
		private final String empty;
		private List<RowData> cache = List.of();
		private final int rowH;
		public ListView(String empty, Supplier<List<RowData>> rows, Consumer<RowData> onClick) {
			this.empty = empty; this.rows = rows; this.onClick = onClick;
			this.rowH = 18;
		}
		@Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my) {
			cache = rows.get();
			h = Math.max(1, cache.size()) * rowH + 2;
			if (cache.isEmpty()) {
				Draw.fill(g, ax, ay, aw, rowH, Draw.PANEL_2);
				Draw.text(g, f, empty, ax + 8, ay + 5, Draw.MUTED, false);
				return;
			}
			for (int i = 0; i < cache.size(); i++) {
				RowData r = cache.get(i);
				int y = ay + i * rowH;
				boolean hv = onClick != null && mx >= ax && mx < ax + aw && my >= y && my < y + rowH;
				Draw.fill(g, ax, y, aw, rowH - 1, r.selected ? Draw.ACCENT_DIM : (hv ? Draw.PANEL_3 : (i % 2 == 0 ? Draw.PANEL_2 : Draw.PANEL)));
				int lx = ax + 6;
				if (r.icon != null) { g.item(r.icon, ax + 2, y + 1); lx = ax + 22; }
				int rw = f.width(r.right);
				Draw.text(g, f, Draw.fit(f, r.left, aw - rw - (lx - ax) - 14), lx, y + 5, r.leftColor, false);
				Draw.textRight(g, f, r.right, ax + aw - 6, y + 5, r.rightColor, false);
			}
		}
		@Override public boolean click(int mx, int my, int btn) {
			int i = (my - ay) / rowH;
			if (onClick != null && btn == 0 && i >= 0 && i < cache.size()) onClick.accept(cache.get(i));
			return true;
		}
	}
}
