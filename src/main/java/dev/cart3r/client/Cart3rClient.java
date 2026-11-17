package dev.cart3r.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class Cart3rClient implements ClientModInitializer {
	private static boolean openRequested = false;
	private static boolean keyWasDown = false;

	@Override
	public void onInitializeClient() {
		Config.load();

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(ClientCommandManager.literal("cart3r").executes(ctx -> {
				openRequested = true;
				return 1;
			})));

		HudElementRegistry.attachElementAfter(
			VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath("cart3rclient", "hud"),
			(graphics, tickCounter) -> Hud.render(graphics));

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			boolean down = InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
			if (down && !keyWasDown && mc.screen == null && mc.player != null) {
				openRequested = true;
			}
			keyWasDown = down;

			if (openRequested && mc.player != null) {
				openRequested = false;
				mc.setScreen(new MenuScreen());
			}

			ModuleManager.tick(mc);
		});
	}
}
