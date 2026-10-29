package com.fima.timerewind.client;

import com.fima.timerewind.network.ClientNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class TimeRewindClient implements ClientModInitializer {
	public static KeyBinding openMenuKey;

	@Override
	public void onInitializeClient() {
		ClientNetwork.register();

		openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.timerewind.open_menu",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_R,
			"category.timerewind"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenuKey.wasPressed()) {
				if (client.player != null && client.currentScreen == null) {
					client.setScreen(new TimeMenuScreen());
				}
			}
		});
	}
}
