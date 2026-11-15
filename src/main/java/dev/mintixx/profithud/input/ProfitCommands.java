package dev.mintixx.profithud.input;

import dev.mintixx.profithud.ProfitHudMod;
import dev.mintixx.profithud.data.Session;
import dev.mintixx.profithud.util.NumberFormatter;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

/**
 * Client-side commands. They never send anything to the server.
 * /profit, /profit reset, /profit session, /profit prices, /profit hud, /profit export
 */
public final class ProfitCommands {
	private ProfitCommands() {}

	private static Component line(String s, ChatFormatting color) { return Component.literal(s).withStyle(color); }

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(ClientCommands.literal("profit")
				.executes(ctx -> { ProfitHudMod.get().openMenu(0); return 1; })
				.then(ClientCommands.literal("reset").executes(ctx -> {
					ProfitHudMod.get().sessions().resetCurrent();
					ctx.getSource().sendFeedback(line("[PROFIT HUD] Session statistics reset.", ChatFormatting.AQUA));
					return 1;
				}))
				.then(ClientCommands.literal("session").executes(ctx -> {
					ProfitHudMod m = ProfitHudMod.get();
					Session s = m.sessions().current();
					var c = m.config();
					var src = ctx.getSource();
					src.sendFeedback(line("[PROFIT HUD] Session (confirmed from chat messages)", ChatFormatting.AQUA));
					src.sendFeedback(line(" Earned: +" + NumberFormatter.money(s.earned, c, false)
						+ "   Spent: -" + NumberFormatter.money(s.spent, c, false), ChatFormatting.WHITE));
					src.sendFeedback(line(" Net profit: " + NumberFormatter.money(s.net(), c, true)
						+ "   Per hour: " + NumberFormatter.money(s.perHour(), c, true), ChatFormatting.WHITE));
					src.sendFeedback(line(" Duration: " + NumberFormatter.duration(s.activeMillis)
						+ "   Payments: " + (s.countIn + s.countOut), ChatFormatting.GRAY));
					double bal = m.tracker().balance();
					src.sendFeedback(line(" Balance: " + (Double.isNaN(bal) ? "not detected on this server"
						: NumberFormatter.money(bal, c, false)), ChatFormatting.GRAY));
					src.sendFeedback(line(" Estimated inventory (estimate only, not counted as profit): ~"
						+ NumberFormatter.money(m.scanner().totalEstimate(), c, false), ChatFormatting.YELLOW));
					return 1;
				}))
				.then(ClientCommands.literal("prices").executes(ctx -> { ProfitHudMod.get().openMenu(3); return 1; }))
				.then(ClientCommands.literal("hud").executes(ctx -> { ProfitHudMod.get().openMenu(6); return 1; }))
				.then(ClientCommands.literal("export").executes(ctx -> {
					try {
						Path p = ProfitHudMod.get().sessions().exportCsv();
						ctx.getSource().sendFeedback(line("[PROFIT HUD] Exported to " + p, ChatFormatting.AQUA));
					} catch (Exception e) {
						ctx.getSource().sendError(Component.literal("[PROFIT HUD] Export failed: " + e.getMessage()));
					}
					return 1;
				}))));
	}
}
