package dev.cart3r.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class Sfx {
	private Sfx() {
	}

	public static void tick() {
		if (!Theme.SOUNDS.bool() || Notify.mute) {
			return;
		}
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.8f));
	}

	public static void click(boolean on) {
		if (!Theme.SOUNDS.bool() || Notify.mute) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, on ? 1.35f : 0.8f));
	}
}
