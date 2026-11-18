package dev.cart3r.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

public class Cart3rClient implements ClientModInitializer {
	private static boolean openRequested = false;
	private static boolean keyWasDown = false;
	private static boolean joined = false;

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
			(graphics, tickCounter) -> Hud.render(graphics, tickCounter.getGameTimeDeltaPartialTick(false)));

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			int menuKey = ModuleManager.INTERFACE.getKey();
			boolean down = menuKey > 0 && InputConstants.isKeyDown(mc.getWindow(), menuKey);
			if (down && !keyWasDown && mc.screen == null && mc.player != null) {
				openRequested = true;
			}
			keyWasDown = down;

			if (openRequested && mc.player != null) {
				openRequested = false;
				mc.setScreen(new MenuScreen());
			}

			if (mc.player == null || mc.level == null) {
				if (joined) {
					joined = false;
					ModuleManager.onLeave();
				}
				return;
			}
			if (!joined) {
				joined = true;
				ModuleManager.onJoin(mc);
			}

			for (Module m : ModuleManager.MODULES) {
				int k = m.getKey();
				if (m == ModuleManager.INTERFACE || k <= 0) {
					continue;
				}
				boolean d = InputConstants.isKeyDown(mc.getWindow(), k);
				if (m.risingEdge(d) && mc.screen == null) {
					m.toggle(mc);
				}
			}

			ModuleManager.tick(mc);
			Hud.tick(mc);
		});
	}
}
