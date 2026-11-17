package dev.cart3r.client;

import net.minecraft.client.Minecraft;

public abstract class Module {
	public enum Category {
		MOVEMENT("Movement"),
		VISUAL("Visual"),
		UTILITY("Utility"),
		PERFORMANCE("Performance");

		public final String label;

		Category(String label) {
			this.label = label;
		}
	}

	private final String name;
	private final Category category;
	private final String description;
	private boolean enabled;

	protected Module(String name, Category category, String description) {
		this.name = name;
		this.category = category;
		this.description = description;
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
	}

	public void toggle(Minecraft mc) {
		enabled = !enabled;
		if (enabled) {
			onEnable(mc);
		} else {
			onDisable(mc);
		}
		Config.save();
	}

	public void onEnable(Minecraft mc) {
	}

	public void onTick(Minecraft mc) {
	}

	public void onDisable(Minecraft mc) {
	}
}
