package dev.mintixx.profithud;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.mintixx.profithud.gui.ProfitScreen;

public final class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> new ProfitScreen(ProfitScreen.T_OVERVIEW, parent);
	}
}
