package dev.cart3r.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class Cart3rClient implements ClientModInitializer {
	private static boolean openRequested = false;
	private static boolean editorRequested = false;
	private static boolean keyWasDown = false;
	private static boolean joined = false;
	private static boolean tourRequested = false;

	@Override
	public void onInitializeClient() {
		Config.load();
		Waypoints.load();
		Profiles.load();
		HudLayout.load();
		Notes.load();

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			var node = dispatcher.register(ClientCommandManager.literal("cart3r")
				.executes(ctx -> {
					openRequested = true;
					return 1;
				})
				.then(ClientCommandManager.literal("hud")
					.executes(ctx -> {
						editorRequested = true;
						return 1;
					}))
				.then(ClientCommandManager.literal("profile")
					.then(ClientCommandManager.literal("save")
						.then(ClientCommandManager.argument("name", StringArgumentType.word())
							.executes(ctx -> {
								boolean ok = Profiles.saveCurrent(StringArgumentType.getString(ctx, "name"));
								ctx.getSource().sendFeedback(Component.literal(ok ? "Profile saved" : "Invalid name"));
								return 1;
							})))
					.then(ClientCommandManager.literal("load")
						.then(ClientCommandManager.argument("name", StringArgumentType.word())
							.executes(ctx -> {
								Profiles.Profile p = Profiles.find(StringArgumentType.getString(ctx, "name"));
								if (p == null) {
									ctx.getSource().sendFeedback(Component.literal("Profile not found"));
								} else {
									Profiles.apply(Minecraft.getInstance(), p);
								}
								return 1;
							})))
					.then(ClientCommandManager.literal("del")
						.then(ClientCommandManager.argument("name", StringArgumentType.word())
							.executes(ctx -> {
								boolean ok = Profiles.delete(StringArgumentType.getString(ctx, "name"));
								ctx.getSource().sendFeedback(Component.literal(ok ? "Profile deleted" : "Profile not found"));
								return 1;
							})))
					.then(ClientCommandManager.literal("list")
						.executes(ctx -> {
							for (Profiles.Profile p : Profiles.LIST) {
								ctx.getSource().sendFeedback(Component.literal(p.name() + " (" + p.mods().size() + " cheats)"));
							}
							return 1;
						})))
				.then(ClientCommandManager.literal("tour")
					.executes(ctx -> {
						tourRequested = true;
						return 1;
					}))
				.then(ClientCommandManager.literal("note")
					.then(ClientCommandManager.literal("add")
						.then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
							.executes(ctx -> {
								boolean ok = Notes.add(StringArgumentType.getString(ctx, "text"));
								ctx.getSource().sendFeedback(Component.literal(ok ? "Note added" : "Note not added (max 12)"));
								return 1;
							})))
					.then(ClientCommandManager.literal("del")
						.then(ClientCommandManager.argument("n", IntegerArgumentType.integer(1))
							.executes(ctx -> {
								boolean ok = Notes.remove(IntegerArgumentType.getInteger(ctx, "n"));
								ctx.getSource().sendFeedback(Component.literal(ok ? "Note removed" : "No such note"));
								return 1;
							})))
					.then(ClientCommandManager.literal("clear")
						.executes(ctx -> {
							Notes.clear();
							ctx.getSource().sendFeedback(Component.literal("Notes cleared"));
							return 1;
						})))
				.then(ClientCommandManager.literal("wp")
					.then(ClientCommandManager.literal("add")
						.then(ClientCommandManager.argument("name", StringArgumentType.word())
							.executes(ctx -> {
								String n = StringArgumentType.getString(ctx, "name");
								Waypoints.add(Minecraft.getInstance(), n);
								ctx.getSource().sendFeedback(Component.literal("Waypoint saved: " + n));
								return 1;
							})))
					.then(ClientCommandManager.literal("del")
						.then(ClientCommandManager.argument("name", StringArgumentType.word())
							.executes(ctx -> {
								String n = StringArgumentType.getString(ctx, "name");
								boolean ok = Waypoints.remove(n);
								ctx.getSource().sendFeedback(Component.literal(ok ? "Removed: " + n : "Not found: " + n));
								return 1;
							})))
					.then(ClientCommandManager.literal("list")
						.executes(ctx -> {
							if (Waypoints.LIST.isEmpty()) {
								ctx.getSource().sendFeedback(Component.literal("No waypoints"));
							}
							for (Waypoints.Point p : Waypoints.LIST) {
								ctx.getSource().sendFeedback(Component.literal(
									p.name() + "  " + (int) p.x() + " " + (int) p.y() + " " + (int) p.z()));
							}
							return 1;
						}))
					.then(ClientCommandManager.literal("clear")
						.executes(ctx -> {
							Waypoints.clear();
							ctx.getSource().sendFeedback(Component.literal("All waypoints removed"));
							return 1;
						}))));
			dispatcher.register(ClientCommandManager.literal("hato").executes(ctx -> {
				openRequested = true;
				return 1;
			}).redirect(node));
		});

		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, tickCounter) -> {
			if (!ModuleManager.CROSS.isEnabled()) {
				original.render(graphics, tickCounter);
			}
		});

		HudElementRegistry.attachElementAfter(
			VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath("cart3rclient", "hud"),
			(graphics, tickCounter) -> Hud.render(graphics, tickCounter.getGameTimeDeltaPartialTick(false)));

		ClientTickEvents.START_CLIENT_TICK.register(Freecam::preTick);

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			int menuKey = ModuleManager.INTERFACE.getKey();
			boolean down = menuKey > 0 && InputConstants.isKeyDown(mc.getWindow(), menuKey);
			if (down && !keyWasDown && mc.screen == null && mc.player != null) {
				openRequested = true;
			}
			keyWasDown = down;

			if (editorRequested && mc.player != null) {
				editorRequested = false;
				mc.setScreen(new HudEditorScreen());
			}
			if (tourRequested && mc.player != null) {
				tourRequested = false;
				mc.setScreen(new TourScreen());
			}
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
				if (Theme.INTRO.bool()) {
					Intro.trigger();
				} else {
					Notify.push("HATO v16 ready - press " + Module.keyName(ModuleManager.INTERFACE.getKey()), Theme.accent());
				}
				ModuleManager.onJoin(mc);
			}

			if (Tour.needed() && !Tour.shown && mc.player.tickCount > 80 && mc.screen == null) {
				Tour.shown = true;
				mc.setScreen(new TourScreen());
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
			Profiles.tick(mc);
		});
	}
}
