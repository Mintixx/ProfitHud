package dev.mintixx.profithud.gui;

import dev.mintixx.profithud.ProfitHudMod;
import dev.mintixx.profithud.config.ProfitConfig;
import dev.mintixx.profithud.core.InventoryScanner;
import dev.mintixx.profithud.core.ItemPriceManager;
import dev.mintixx.profithud.util.NumberFormatter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Searchable, paged cards for item prices automatically discovered in inventory lore. */
final class ItemsPanel {
	record Entry(String id, String name, String lower, ItemStack stack) { }

	private static List<Entry> ALL;
	private static final Map<String, String> NAMES = new HashMap<>();
	private static final int COLUMNS = 5, ROWS = 4, CELL_GAP = 6;

	private static synchronized List<Entry> all() {
		if (ALL == null) {
			List<Entry> out = new ArrayList<>();
			for (Item item : BuiltInRegistries.ITEM) {
				String id = BuiltInRegistries.ITEM.getKey(item).toString();
				if (id.equals("minecraft:air")) continue;
				ItemStack stack = new ItemStack(item);
				String name = stack.getHoverName().getString();
				NAMES.put(id, name);
				out.add(new Entry(id, name, (name + " " + id).toLowerCase(Locale.ROOT), stack));
			}
			out.sort(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
			ALL = out;
		}
		return ALL;
	}

	static String vanillaName(String id) {
		all();
		return NAMES.getOrDefault(id, id);
	}

	static String displayName(String id) { return vanillaName(id); }

	private final ProfitHudMod mod = ProfitHudMod.get();
	private final String[] query = {""};
	private final Ui.TextCtl search;
	private List<Entry> filtered = List.of();
	private String lastQuery;
	private int lastVersion = -1;
	private int page;
	private String selected;
	private int x, y, w, h, gridY, gridH;

	ItemsPanel() {
		search = new Ui.TextCtl(null, "Search observed item prices...", 40, null, () -> query[0], v -> query[0] = v);
	}

	void select(String id) {
		query[0] = "";
		selected = id;
		lastQuery = null;
		lastVersion = -1;
		refilter();
		int index = indexOf(id);
		if (index >= 0) page = index / pageSize();
	}

	private void refilter() {
		int version = mod.prices().version();
		if (query[0].equals(lastQuery) && version == lastVersion) return;
		boolean queryChanged = !query[0].equals(lastQuery);
		lastQuery = query[0];
		lastVersion = version;
		String[] tokens = query[0].toLowerCase(Locale.ROOT).strip().split("\\s+");
		List<Entry> out = new ArrayList<>();
		for (Entry entry : all()) {
			if (!mod.prices().hasPrice(entry.id()) && !entry.id().equals(selected)) continue;
			boolean matches = true;
			for (String token : tokens) {
				if (!token.isEmpty() && !entry.lower().contains(token)) { matches = false; break; }
			}
			if (matches) out.add(entry);
		}
		filtered = out;
		if (queryChanged) page = 0;
		page = Math.max(0, Math.min(page, maxPage()));
	}

	void layout(int x, int y, int w, int h) {
		this.x = x; this.y = y; this.w = w; this.h = h;
		gridY = y + 50;
		gridH = Math.max(0, h - 50);
	}

	private int columns() { return COLUMNS; }
	private int cellHeight() {
		int reservedHistoryHeight = selected == null ? 0 : 50;
		return Math.max(32, Math.min(90, (gridH - CELL_GAP * (ROWS - 1) - reservedHistoryHeight) / ROWS));
	}
	private int pageSize() { return COLUMNS * ROWS; }
	private int maxPage() { return Math.max(0, (filtered.size() - 1) / pageSize()); }
	private int indexOf(String id) {
		for (int i = 0; i < filtered.size(); i++) if (filtered.get(i).id().equals(id)) return i;
		return -1;
	}

	void render(GuiGraphicsExtractor g, Font f, int mx, int my) {
		refilter();
		search.ax = x; search.ay = y; search.aw = w; search.render(g, f, mx, my);

		int pageCount = Math.max(1, maxPage() + 1);
		Draw.text(g, f, "20 per page | " + filtered.size() + " prices found", x, y + 32, Draw.MUTED, false);
		int prevX = x + w - 104;
		int nextX = x + w - 52;
		Ui.drawButton(g, f, prevX, y + 24, 48, 18, "<", over(mx, my, prevX, y + 24, 48, 18), false);
		Ui.drawButton(g, f, nextX, y + 24, 48, 18, ">", over(mx, my, nextX, y + 24, 48, 18), false);
		Draw.textCentered(g, f, (page + 1) + " / " + pageCount, x + w - 130, y + 29, Draw.TEXT, false);

		if (filtered.isEmpty()) {
			int cardW = Math.min(w, 340), cardH = 68;
			int cardX = x + (w - cardW) / 2, cardY = gridY + Math.max(0, (gridH - cardH) / 2);
			Draw.rrect(g, cardX, cardY, cardW, cardH, 4, Draw.PANEL_2);
			Draw.rrectBorder(g, cardX, cardY, cardW, cardH, 4, 1, Draw.BORDER);
			Draw.textCentered(g, f, "No prices discovered yet", cardX + cardW / 2, cardY + 13, Draw.TEXT, false);
			Draw.textCentered(g, f, "Keep priced items in your inventory to discover them.",
				cardX + cardW / 2, cardY + 31, Draw.MUTED, false);
			return;
		}

		int cols = columns();
		int cellW = (w - CELL_GAP * (cols - 1)) / cols;
		int cellH = cellHeight();
		int first = page * pageSize();
		int end = Math.min(filtered.size(), first + pageSize());
		ProfitConfig config = mod.config();
		for (int i = first; i < end; i++) {
			Entry entry = filtered.get(i);
			int local = i - first;
			int cellX = x + (local % cols) * (cellW + CELL_GAP);
			int cellY = gridY + (local / cols) * (cellH + CELL_GAP);
			boolean isSelected = entry.id().equals(selected);
			boolean hover = over(mx, my, cellX, cellY, cellW, cellH);
			int fill = isSelected ? Draw.PANEL_3 : hover ? Draw.PANEL_2 : 0xFF101415;
			int border = isSelected ? config.accentColor : hover ? Draw.ACCENT_DIM : Draw.BORDER;
			Draw.rrect(g, cellX, cellY, cellW, cellH, 2, fill);
			Draw.rrectBorder(g, cellX, cellY, cellW, cellH, 2, 1, border);
			int iconY = cellY + (cellH >= 48 ? Math.max(4, (cellH - 44) / 3) : Math.max(0, (cellH - 30) / 2));
			g.item(entry.stack(), cellX + (cellW - 16) / 2, iconY);
			if (cellH >= 48) {
				int nameY = cellY + cellH - 27;
				Draw.textCentered(g, f, Draw.fit(f, entry.name(), cellW - 6),
					cellX + cellW / 2, nameY, Draw.TEXT, false);
			}
			double price = mod.prices().price(entry.id());
			Draw.textCentered(g, f, price > 0 ? NumberFormatter.money(price, config, false) : "No price",
				cellX + cellW / 2, cellY + cellH - (cellH >= 48 ? 14 : 11), price > 0 ? config.estimateColor : Draw.MUTED, false);
		}
		renderHistory(g, f, config, cellH);
	}

	private void renderHistory(GuiGraphicsExtractor g, Font f, ProfitConfig config, int cellH) {
		if (selected == null) return;
		int historyY = gridY + ROWS * (cellH + CELL_GAP) - CELL_GAP + 4;
		int historyH = gridY + gridH - historyY;
		if (historyH < 42) return;
		String name = displayName(selected);
		List<ItemPriceManager.PricePoint> points = mod.prices().history(selected);
		Draw.rrect(g, x, historyY, w, historyH, 3, Draw.PANEL_2);
		Draw.rrectBorder(g, x, historyY, w, historyH, 3, 1, Draw.BORDER);
		Draw.text(g, f, Draw.fit(f, "PRICE HISTORY  " + name, w - 12), x + 6, historyY + 4, Draw.TEXT, false);
		if (points.size() < 2) {
			Draw.text(g, f, "Waiting for another observed price.", x + 6, historyY + 20, Draw.MUTED, false);
			return;
		}
		int graphX = x + 6, graphY = historyY + 19;
		int graphW = w - 12, graphH = historyH - 25;
		double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
		for (ItemPriceManager.PricePoint point : points) {
			min = Math.min(min, point.price());
			max = Math.max(max, point.price());
		}
		double span = Math.max(1, max - min);
		int prevY = graphY + graphH - 1;
		for (int px = 0; px < graphW; px++) {
			int index = (int) ((long) px * (points.size() - 1) / Math.max(1, graphW - 1));
			int py = graphY + graphH - 1 - (int) ((points.get(index).price() - min) / span * (graphH - 1));
			Draw.fill(g, graphX + px, Math.min(prevY, py), 1, Math.max(1, Math.abs(prevY - py) + 1), config.accentColor);
			prevY = py;
		}
	}

	private static boolean over(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	void click(int mx, int my, int btn) {
		if (search.hit(mx, my)) { search.click(mx, my, btn); return; }
		if (over(mx, my, x + w - 104, y + 24, 48, 18)) { page = Math.max(0, page - 1); return; }
		if (over(mx, my, x + w - 52, y + 24, 48, 18)) { page = Math.min(maxPage(), page + 1); return; }
		int cols = columns();
		int cellW = (w - CELL_GAP * (cols - 1)) / cols;
		int cellH = cellHeight();
		for (int i = page * pageSize(); i < Math.min(filtered.size(), (page + 1) * pageSize()); i++) {
			int local = i - page * pageSize();
			int cellX = x + (local % cols) * (cellW + CELL_GAP);
			int cellY = gridY + (local / cols) * (cellH + CELL_GAP);
			if (over(mx, my, cellX, cellY, cellW, cellH)) { selected = filtered.get(i).id(); return; }
		}
		search.blur();
	}

	boolean scroll(int mx, int my, double dy) {
		return over(mx, my, x, gridY, w, gridH);
	}

	boolean focused() { return search.focused(); }
	void blur() { search.blur(); }
	boolean key(int key, int mods) { return search.focused() && search.key(key, mods); }
	boolean typed(String text) { return search.focused() && search.typed(text); }
}
