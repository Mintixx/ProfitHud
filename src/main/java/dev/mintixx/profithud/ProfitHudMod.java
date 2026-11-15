package dev.mintixx.profithud;

import dev.mintixx.profithud.config.ConfigManager;
import dev.mintixx.profithud.config.ProfitConfig;
import dev.mintixx.profithud.core.ChatParser;
import dev.mintixx.profithud.core.EconomyTracker;
import dev.mintixx.profithud.core.InventoryScanner;
import dev.mintixx.profithud.core.ItemPriceManager;
import dev.mintixx.profithud.core.ScoreboardBalanceReader;
import dev.mintixx.profithud.core.SessionManager;
import dev.mintixx.profithud.core.TransactionHistoryManager;
import dev.mintixx.profithud.gui.ProfitScreen;
import dev.mintixx.profithud.hud.Animations;
import dev.mintixx.profithud.hud.HudRenderer;
import dev.mintixx.profithud.input.Keybinds;
import dev.mintixx.profithud.input.ProfitCommands;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

/** Client entrypoint and service locator. Only explicit balance refreshes send a server command. */
public final class ProfitHudMod implements ClientModInitializer {
	public static final String MOD_ID = "profithud";
	public static final String VERSION = "1.0.0";
	private static ProfitHudMod instance;

	private ConfigManager configManager;
	private ItemPriceManager prices;
	private SessionManager sessions;
	private ChatParser parser;
	private TransactionHistoryManager history;
	private Animations anim;
	private EconomyTracker tracker;
	private InventoryScanner scanner;
	private HudRenderer hud;

	private Supplier<Screen> pending;
	private int pendingTicks;
	private int scoreboardTicks;
	private long lastBalanceQueryMs;
	private long sellReceiptUntilMs;
	/** True while the drag-position screen draws its own copy of the HUD. */
	public boolean suppressHud;

	public static ProfitHudMod get() { return instance; }

	public ConfigManager configManager() { return configManager; }
	public ProfitConfig config() { return configManager.get(); }
	public ItemPriceManager prices() { return prices; }
	public SessionManager sessions() { return sessions; }
	public ChatParser parser() { return parser; }
	public TransactionHistoryManager history() { return history; }
	public Animations anim() { return anim; }
	public EconomyTracker tracker() { return tracker; }
	public InventoryScanner scanner() { return scanner; }
	public HudRenderer hud() { return hud; }

	@Override
	public void onInitializeClient() {
		instance = this;
		configManager = new ConfigManager();
		configManager.load();
		prices = new ItemPriceManager();
		prices.load();
		sessions = new SessionManager(configManager);
		sessions.load();
		parser = new ChatParser(configManager);
		history = new TransactionHistoryManager(sessions);
		anim = new Animations(configManager);
		tracker = new EconomyTracker(configManager, parser, sessions, history, anim);
		scanner = new InventoryScanner(prices, configManager);
		hud = new HudRenderer(configManager, tracker, sessions, scanner, anim);

		Keybinds.register();
		ProfitCommands.register();

		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath(MOD_ID, "profit_hud"), (graphics, deltaTracker) -> {
				if (suppressHud) return;
				Minecraft mc = Minecraft.getInstance();
				int screenWidth = mc.getWindow().getGuiScaledWidth();
				int screenHeight = mc.getWindow().getGuiScaledHeight();
				hud.renderHud(graphics, mc.font, screenWidth, screenHeight);
				hud.renderPriceToast(graphics, mc.font, screenWidth, screenHeight);
			});

		ClientTickEvents.END_CLIENT_TICK.register(this::tick);

		// System/server messages (this is where almost every server prints payment lines).
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			String line = message.getString();
			ChatParser.Kind kind = parser.parse(line).kind();
			if (!overlay || kind == ChatParser.Kind.INCOMING || kind == ChatParser.Kind.OUTGOING
				|| System.currentTimeMillis() <= sellReceiptUntilMs) onChatLine(line);
		});
		// Player-style chat. Profile-less ("disguised") lines are accepted; real signed player chat only if enabled.
		ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) -> {
			if (signed == null || config().parsePlayerChat) onChatLine(message.getString());
		});
		ClientSendMessageEvents.COMMAND.register(command -> {
			if (command.strip().matches("(?i)sell(?:\\s+.*)?")) sellReceiptUntilMs = System.currentTimeMillis() + 5000;
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			var server = client.getCurrentServer();
			configManager.switchServerProfile(server == null ? "" : server.ip);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			configManager.switchServerProfile("");
			anim.resetAppear();
			tracker.clearBalance(); // a balance only means something on the server that reported it
			lastBalanceQueryMs = 0;
			scoreboardTicks = 0;
			sessions.saveNow();
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> saveAll());
		Runtime.getRuntime().addShutdownHook(new Thread(this::saveAll, "profithud-save"));
	}

	private void saveAll() {
		try {
			configManager.save();
			sessions.saveNow();
			prices.save();
		} catch (Throwable ignored) { }
	}

	private void tick(Minecraft mc) {
		sessions.tick(mc);
		scanner.tick(mc);
		if (++scoreboardTicks >= 10) {
			scoreboardTicks = 0;
			tracker.setBalance(ScoreboardBalanceReader.read(mc));
		}
		while (Keybinds.OPEN.consumeClick()) openMenu(0);
		if (pending != null) {
			pendingTicks++;
			// Commands run while the chat screen is still open; wait until it closed.
			if (mc.gui.screen() == null || pendingTicks > 20) {
				Screen s = pending.get();
				pending = null;
				mc.gui.setScreen(s);
			}
		}
	}

	private void onChatLine(String text) {
		if (tracker.onChat(text)) {
			sellReceiptUntilMs = 0;
			refreshBalanceAfterTransaction();
			return;
		}
		if (System.currentTimeMillis() <= sellReceiptUntilMs && tracker.onSellReceipt(text)) {
			sellReceiptUntilMs = 0;
			refreshBalanceAfterTransaction();
		}
	}

	private void refreshBalanceAfterTransaction() {
		Minecraft mc = Minecraft.getInstance();
		long now = System.currentTimeMillis();
		if (mc.player == null || mc.getConnection() == null || now - lastBalanceQueryMs < 2000) return;
		mc.getConnection().sendCommand("balance");
		lastBalanceQueryMs = now;
	}

	public void openMenu(int tab) { openScreenSoon(() -> new ProfitScreen(tab)); }

	public void openScreenSoon(Supplier<Screen> s) {
		pending = s;
		pendingTicks = 0;
	}
}
