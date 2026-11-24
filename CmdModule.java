package dev.cart3r.client;

import net.minecraft.client.Minecraft;

// A module that runs a normal Minecraft command for you (works in your own world with cheats on).
// With only an "on" command it is a one-shot button; with an "off" command it is a switch.
public class CmdModule extends Module {
	private final String on;
	private final String off;

	public CmdModule(String name, Category category, String description, String on, String off) {
		super(name, category, description);
		this.on = on;
		this.off = off;
	}

	public static void send(Minecraft mc, String command) {
		if (mc.player != null && mc.player.connection != null) {
			mc.player.connection.sendCommand(command);
		}
	}

	@Override
	public boolean persistent() {
		return false;
	}

	@Override
	public void onEnable(Minecraft mc) {
		send(mc, on);
		if (off == null) {
			setEnabled(false);
		}
	}

	@Override
	public void onDisable(Minecraft mc) {
		if (off != null) {
			send(mc, off);
		}
	}
}
