package dev.mintixx.profithud.hud;

/**
 * Snapshot shown by the HUD. Payment statistics come from parsed chat; balance comes from the
 * scoreboard or a parsed balance response. Inventory value is estimated from observed item lore.
 */
public record HudData(double balance, double earned, double spent, double net, double perMinute,
					  double perHour, double estimatedInventory, long durationMs, int txCount) {
}
