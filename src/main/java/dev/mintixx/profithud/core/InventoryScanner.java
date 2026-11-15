package dev.mintixx.profithud.core;

import dev.mintixx.profithud.config.ConfigManager;
import dev.mintixx.profithud.util.NumberFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads item prices from inventory tooltips and values the player's own inventory. */
public final class InventoryScanner {
	public record Row(String id, ItemStack sample, int count, double unitPrice, double total) { }
	public record PriceNotice(String name, double previous, double current, boolean firstSeen) { }
	private record ObservedPrice(double price, String name) { }
	private record StackValue(String id, Item item, int count, double loreTotal) { }
	private record ParsedTooltip(double stackTotal, String text) { }
	private static final class Aggregate {
		private final ItemStack sample;
		private int count;
		private double total;

		private Aggregate(Item item) { sample = new ItemStack(item); }
	}

	private static final Pattern LORE_PRICE = Pattern.compile(
		"(?i)(?:[$€£]\\s*|\\b(?:worth|price|sell|value)\\s*[:=]?\\s*(?:[$€£]\\s*)?)(\\d[\\d,]*(?:\\.\\d+)?\\s*(?:quadrillions?|quadr|qd|q|trillions?|tril|tn|t|billions?|bil|bn|b|millions?|mill|mil|m|thousands?|thou|thousand|k|crores?|cr|lakhs?|lacs?|lakh|hundred|h)?)");

	private final ItemPriceManager prices;
	private final ConfigManager cm;
	private volatile List<Row> rows = List.of();
	private volatile double total = 0;
	private final ArrayDeque<PriceNotice> pendingNotices = new ArrayDeque<>();
	private PriceNotice activeNotice;
	private long activeNoticeUntil;
	private int lastHash = Integer.MIN_VALUE;
	private int tickCounter = 0;

	public InventoryScanner(ItemPriceManager prices, ConfigManager cm) {
		this.prices = prices;
		this.cm = cm;
	}

	public List<Row> rows() { return rows; }
	public double totalEstimate() { return total; }

	public PriceNotice activePriceNotice() {
		long now = System.currentTimeMillis();
		if (activeNotice == null || now >= activeNoticeUntil) {
			activeNotice = pendingNotices.pollFirst();
			activeNoticeUntil = activeNotice == null ? 0 : now + 3500;
		}
		return activeNotice;
	}

	/** Checks every five ticks and includes per-stack lore in the value calculation. */
	public void tick(Minecraft mc) {
		if (++tickCounter % 5 != 0) return;
		if (mc.player == null || mc.level == null) {
			if (!rows.isEmpty()) { rows = List.of(); total = 0; lastHash = Integer.MIN_VALUE; }
			return;
		}

		Inventory inventory = mc.player.getInventory();
		int limit = cm.get().countArmorAndOffhand ? inventory.getContainerSize() : Math.min(36, inventory.getContainerSize());
		int hash = 31 * limit + (cm.get().includeShulkerContents ? 1 : 0);
		List<StackValue> stacks = new ArrayList<>();
		Map<String, ObservedPrice> discovered = new LinkedHashMap<>();
		for (int i = 0; i < limit; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty()) continue;
			hash = collectStack(stack, mc, stacks, discovered, hash, 0);
		}

		boolean priceChanged = false;
		for (Map.Entry<String, ObservedPrice> e : discovered.entrySet()) {
			double previous = prices.price(e.getKey());
			ObservedPrice observed = e.getValue();
			if (!prices.observe(e.getKey(), observed.price())) continue;
			priceChanged = true;
			boolean firstSeen = previous == 0;
			double changePercent = firstSeen ? 100 : Math.abs(observed.price() - previous) / previous * 100;
			if (cm.get().priceAlertsEnabled
				&& (firstSeen ? cm.get().notifyNewPrices : changePercent >= cm.get().priceAlertPercent)) {
				queueNotice(new PriceNotice(observed.name(), previous, observed.price(), firstSeen));
			}
		}
		if (priceChanged) prices.save();
		if (hash == lastHash && !priceChanged) return;
		lastHash = hash;

		Map<String, Aggregate> totals = new LinkedHashMap<>();
		for (StackValue stack : stacks) {
			Aggregate aggregate = totals.computeIfAbsent(stack.id(), id -> new Aggregate(stack.item()));
			double unitPrice = stack.loreTotal() > 0
				? stack.loreTotal() / stack.count()
				: prices.price(stack.id());
			aggregate.count += stack.count();
			aggregate.total += stack.loreTotal() > 0 ? stack.loreTotal() : unitPrice * stack.count();
		}
		List<Row> updatedRows = new ArrayList<>();
		double inventoryTotal = 0;
		for (Map.Entry<String, Aggregate> e : totals.entrySet()) {
			Aggregate aggregate = e.getValue();
			inventoryTotal += aggregate.total;
			updatedRows.add(new Row(e.getKey(), aggregate.sample, aggregate.count,
				aggregate.count == 0 ? 0 : aggregate.total / aggregate.count, aggregate.total));
		}
		updatedRows.sort(Comparator.comparingDouble(Row::total).reversed()
			.thenComparing(Comparator.comparingInt(Row::count).reversed()));
		rows = updatedRows;
		total = inventoryTotal;
	}

	private int collectStack(ItemStack stack, Minecraft mc, List<StackValue> stacks,
							 Map<String, ObservedPrice> discovered, int hash, int depth) {
		Item item = stack.getItem();
		String id = BuiltInRegistries.ITEM.getKey(item).toString();
		hash = 31 * hash + id.hashCode();
		hash = 31 * hash + stack.getCount();

		ParsedTooltip tooltip = readTooltipPrice(stack, mc);
		hash = 31 * hash + tooltip.text().hashCode();
		if (tooltip.stackTotal() > 0) {
			discovered.put(id, new ObservedPrice(tooltip.stackTotal() / stack.getCount(), stack.getHoverName().getString()));
		}
		stacks.add(new StackValue(id, item, stack.getCount(), tooltip.stackTotal()));

		if (depth == 0 && cm.get().includeShulkerContents
			&& item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
			ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
			if (contents != null) {
				for (ItemStack inner : contents.nonEmptyItemCopyStream().toList()) {
					hash = collectStack(inner, mc, stacks, discovered, hash, depth + 1);
				}
			}
		}
		return hash;
	}

	private static ParsedTooltip readTooltipPrice(ItemStack stack, Minecraft mc) {
		StringBuilder text = new StringBuilder();
		ItemLore lore = stack.get(net.minecraft.core.component.DataComponents.LORE);
		double price = lore == null ? 0 : parsePrice(lore.lines(), text);
		if (price == 0 && lore != null) {
			List<Component> tooltip = stack.getTooltipLines(
				net.minecraft.world.item.Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL);
			price = parsePrice(tooltip, text);
		}
		return new ParsedTooltip(price, text.toString());
	}

	private static double parsePrice(List<Component> lines, StringBuilder text) {
		for (Component line : lines) {
			String lineText = line.getString();
			if (!text.isEmpty()) text.append(' ');
			text.append(lineText);
			Matcher matcher = LORE_PRICE.matcher(lineText);
			while (matcher.find()) {
				double price = NumberFormatter.parseAmount(matcher.group(1));
				if (Double.isFinite(price) && price > 0) return price;
			}
		}
		return 0;
	}

	private void queueNotice(PriceNotice notice) {
		if (pendingNotices.size() == 8) pendingNotices.removeFirst();
		pendingNotices.addLast(notice);
	}
}
