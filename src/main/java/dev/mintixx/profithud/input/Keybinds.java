package dev.mintixx.profithud.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Registers the menu keybind (Right Shift by default, rebindable in Options > Controls > Key Binds). */
public final class Keybinds {
	public static KeyMapping OPEN;

	private Keybinds() {}

	public static void register() {
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("profithud", "main"));
		OPEN = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.profithud.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
	}
}
