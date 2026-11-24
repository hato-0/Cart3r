package dev.cart3r.client;

import org.lwjgl.glfw.GLFW;

public final class InterfaceModule extends Module {
	public InterfaceModule() {
		super("Interface", Category.CLIENT, "Menu look, animations and the menu key");
		add(Theme.PRESET);
		add(Theme.STYLE);
		add(Theme.ACCENT);
		add(Theme.ACC_R);
		add(Theme.ACC_G);
		add(Theme.ACC_B);
		add(Theme.FONT_STYLE);
		add(Theme.FONT_CASE);
		add(Theme.TEXT_SHADOW);
		add(Theme.ANIMATIONS);
		add(Theme.ANIM_MS);
		add(Theme.OPACITY);
		add(Theme.KEY_HINTS);
		add(Theme.GLOW);
		add(Theme.NOTIFY);
		add(Theme.HUD_OPACITY);
		add(Theme.SOUNDS);
		add(Theme.INTRO);
		add(Theme.HUD_STYLE);
		add(Theme.MENU_SCALE);
		setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
		setEnabled(true);
	}

	@Override
	public boolean persistent() {
		return false;
	}
}
