package dev.mintixx.profithud.core;

import dev.mintixx.profithud.util.NumberFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads an explicitly labelled balance from the visible scoreboard, without sending commands. */
public final class ScoreboardBalanceReader {
	private static final Pattern BALANCE_LABEL = Pattern.compile("(?i)\\b(?:balance|bal|wallet|purse|coins?|money|cash)\\b");
	private static final Pattern LABELED_AMOUNT = Pattern.compile(
		"(?i)\\b(?:balance|bal|wallet|purse|coins?|money|cash)\\b[^0-9]{0,24}((?:[$€£]\\s*)?\\d[\\d,]*(?:\\.\\d+)?\\s*[KMBT]?)");
	private static final Pattern CURRENCY_AMOUNT = Pattern.compile(
		"(?i)[$€£]\\s*\\d[\\d,]*(?:\\.\\d+)?\\s*[KMBT]?");
	private static final Pattern AMOUNT = Pattern.compile(
		"(?i)(?:[$€£]\\s*)?\\d[\\d,]*(?:\\.\\d+)?\\s*[KMBT]?");

	private ScoreboardBalanceReader() { }

	public static double read(Minecraft mc) {
		if (mc.level == null) return Double.NaN;
		Scoreboard scoreboard = mc.level.getScoreboard();
		Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		if (objective == null) return Double.NaN;

		String title = objective.getDisplayName().getString();
		double amount = labeledAmount(title);
		if (Double.isFinite(amount)) return amount;

		boolean balanceObjective = BALANCE_LABEL.matcher(title).find();
		for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
			PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
			String owner = team == null
				? entry.ownerName().getString()
				: team.getFormattedName(Component.literal(entry.owner())).getString();
			String display = entry.display() == null ? "" : entry.display().getString();
			String score = entry.formatValue(objective.numberFormatOrDefault(entry.numberFormatOverride())).getString();
			String row = owner + " " + display + " " + score + " " + entry.value();

			amount = labeledAmount(row);
			if (Double.isFinite(amount)) return amount;
			amount = currencyAmount(row);
			if (Double.isFinite(amount)) return amount;
			if (balanceObjective) {
				amount = anyAmount(score);
				if (Double.isFinite(amount)) return amount;
				amount = entry.value();
				if (amount >= 0) return amount;
			}
		}
		return Double.NaN;
	}

	private static double labeledAmount(String text) {
		Matcher matcher = LABELED_AMOUNT.matcher(text);
		return matcher.find() ? NumberFormatter.parseAmount(matcher.group(1)) : Double.NaN;
	}

	private static double anyAmount(String text) {
		Matcher matcher = AMOUNT.matcher(text);
		return matcher.find() ? NumberFormatter.parseAmount(matcher.group()) : Double.NaN;
	}

	private static double currencyAmount(String text) {
		Matcher matcher = CURRENCY_AMOUNT.matcher(text);
		return matcher.find() ? NumberFormatter.parseAmount(matcher.group()) : Double.NaN;
	}
}
