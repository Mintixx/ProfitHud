package dev.mintixx.profithud.config;

import java.util.ArrayList;
import java.util.List;

/** Server-specific colors and economy-message settings. */
final class ServerProfile {
	int preset;
	int bgColor;
	int bgColor2;
	int borderColor;
	int textColor;
	int labelColor;
	int accentColor;
	int positiveColor;
	int negativeColor;
	int estimateColor;
	String currencySymbol;
	boolean currencyBefore;
	List<String> currencySymbols = new ArrayList<>();
	List<String> incomingPatterns = new ArrayList<>();
	List<String> outgoingPatterns = new ArrayList<>();
	List<String> balancePatterns = new ArrayList<>();
	List<String> ignoredPlayers = new ArrayList<>();
	boolean parsePlayerChat;

	static ServerProfile from(ProfitConfig config) {
		ServerProfile profile = new ServerProfile();
		profile.preset = config.preset;
		profile.bgColor = config.bgColor;
		profile.bgColor2 = config.bgColor2;
		profile.borderColor = config.borderColor;
		profile.textColor = config.textColor;
		profile.labelColor = config.labelColor;
		profile.accentColor = config.accentColor;
		profile.positiveColor = config.positiveColor;
		profile.negativeColor = config.negativeColor;
		profile.estimateColor = config.estimateColor;
		profile.currencySymbol = config.currencySymbol;
		profile.currencyBefore = config.currencyBefore;
		profile.currencySymbols = new ArrayList<>(config.currencySymbols);
		profile.incomingPatterns = new ArrayList<>(config.incomingPatterns);
		profile.outgoingPatterns = new ArrayList<>(config.outgoingPatterns);
		profile.balancePatterns = new ArrayList<>(config.balancePatterns);
		profile.ignoredPlayers = new ArrayList<>(config.ignoredPlayers);
		profile.parsePlayerChat = config.parsePlayerChat;
		return profile;
	}

	void applyTo(ProfitConfig config) {
		config.preset = preset;
		config.bgColor = bgColor;
		config.bgColor2 = bgColor2;
		config.borderColor = borderColor;
		config.textColor = textColor;
		config.labelColor = labelColor;
		config.accentColor = accentColor;
		config.positiveColor = positiveColor;
		config.negativeColor = negativeColor;
		config.estimateColor = estimateColor;
		if (currencySymbol != null) config.currencySymbol = currencySymbol;
		config.currencyBefore = currencyBefore;
		config.currencySymbols = copyOrFallback(currencySymbols, config.currencySymbols);
		config.incomingPatterns = copyOrFallback(incomingPatterns, config.incomingPatterns);
		config.outgoingPatterns = copyOrFallback(outgoingPatterns, config.outgoingPatterns);
		config.balancePatterns = copyOrFallback(balancePatterns, config.balancePatterns);
		config.ignoredPlayers = copyOrFallback(ignoredPlayers, config.ignoredPlayers);
		config.parsePlayerChat = parsePlayerChat;
	}

	private static List<String> copyOrFallback(List<String> value, List<String> fallback) {
		return new ArrayList<>(value == null ? fallback : value);
	}
}
