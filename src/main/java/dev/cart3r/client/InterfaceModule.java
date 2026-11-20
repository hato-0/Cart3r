package dev.cart3r.client;

import org.lwjgl.glfw.GLFW;

public final class InterfaceModule extends Module {
	public InterfaceModule() {
		super("Interface", Category.CLIENT, "Menu look, animations and the menu key");
		add(Theme.ACCENT);
		add(Theme.ANIMATIONS);
		add(Theme.ANIM_MS);
		add(Theme.OPACITY);
		add(Theme.KEY_HINTS);
		add(Theme.GLOW);
		add(Theme.NOTIFY);
		add(Theme.HUD_OPACITY);
		setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
		setEnabled(true);
	}

	@Override
	public boolean persistent() {
		return false;
	}
}
