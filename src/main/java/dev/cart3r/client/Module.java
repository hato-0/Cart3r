package dev.cart3r.client;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
	public enum Category {
		MOVEMENT("Move"),
		RENDER("Render"),
		VISUAL("Visual"),
		PLAYER("Player"),
		UTILITY("Utility"),
		PERFORMANCE("Perf"),
		CLIENT("Client");

		public final String label;

		Category(String label) {
			this.label = label;
		}
	}

	private final String name;
	private final Category category;
	private final String description;
	private final List<Setting> settings = new ArrayList<>();
	private boolean enabled;
	private int key = 0;
	private boolean keyDown = false;

	public float guiAnim;
	public float guiHover;
	public float hudAnim;

	protected Module(String name, Category category, String description) {
		this.name = name;
		this.category = category;
		this.description = description;
	}

	protected Setting add(Setting s) {
		settings.add(s);
		return s;
	}

	protected Setting num(String settingName, double min, double max, double def, double step) {
		return add(Setting.number(settingName, min, max, def, step));
	}

	protected Setting toggle(String settingName, boolean def) {
		return add(Setting.toggle(settingName, def));
	}

	protected Setting options(String settingName, int def, String... labels) {
		return add(Setting.options(settingName, def, labels));
	}

	public List<Setting> getSettings() {
		return settings;
	}

	public Setting setting(String settingName) {
		for (Setting s : settings) {
			if (s.name().equals(settingName)) {
				return s;
			}
		}
		return null;
	}

	public boolean getBool(String settingName) {
		Setting s = setting(settingName);
		return s != null && s.bool();
	}

	public double getNum(String settingName) {
		Setting s = setting(settingName);
		return s == null ? 0 : s.num();
	}

	public String getName() {
		return name;
	}

	public Category getCategory() {
		return category;
	}

	public String getDescription() {
		return description;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean value) {
		this.enabled = value;
		this.guiAnim = value ? 1f : 0f;
	}

	public boolean persistent() {
		return true;
	}

	public int getKey() {
		return key;
	}

	public void setKey(int key) {
		this.key = key;
	}

	public boolean risingEdge(boolean down) {
		boolean r = down && !keyDown;
		keyDown = down;
		return r;
	}

	public void toggle(Minecraft mc) {
		enabled = !enabled;
		if (enabled) {
			onEnable(mc);
		} else {
			onDisable(mc);
		}
		Config.save();
		Notify.module(name, enabled);
	}

	public void onEnable(Minecraft mc) {
	}

	public void onTick(Minecraft mc) {
	}

	public void onDisable(Minecraft mc) {
	}

	public static String keyName(int key) {
		if (key <= 0) {
			return "none";
		}
		if (key >= 290 && key <= 314) {
			return "F" + (key - 289);
		}
		String n = GLFW.glfwGetKeyName(key, 0);
		if (n != null) {
			return n.toUpperCase();
		}
		return switch (key) {
			case 32 -> "Space";
			case 256 -> "Esc";
			case 257 -> "Enter";
			case 258 -> "Tab";
			case 259 -> "Backspace";
			case 260 -> "Insert";
			case 261 -> "Delete";
			case 262 -> "Right";
			case 263 -> "Left";
			case 264 -> "Down";
			case 265 -> "Up";
			case 266 -> "PgUp";
			case 267 -> "PgDn";
			case 268 -> "Home";
			case 269 -> "End";
			case 340 -> "LShift";
			case 341 -> "LCtrl";
			case 342 -> "LAlt";
			case 344 -> "RShift";
			case 345 -> "RCtrl";
			case 346 -> "RAlt";
			default -> "Key " + key;
		};
	}
}
