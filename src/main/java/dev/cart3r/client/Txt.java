package dev.cart3r.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

public final class Txt {
	private Txt() {
	}

	private static boolean plain() {
		return Theme.FONT_STYLE.num() < 0.5 && Theme.FONT_CASE.num() < 0.5;
	}

	private static String cased(String s) {
		int c = (int) Theme.FONT_CASE.num();
		if (c == 1) {
			return s.toUpperCase(Locale.ROOT);
		}
		if (c == 2) {
			return s.toLowerCase(Locale.ROOT);
		}
		return s;
	}

	private static Component comp(String s) {
		MutableComponent c = Component.literal(cased(s));
		switch ((int) Theme.FONT_STYLE.num()) {
			case 1 -> c = c.withStyle(ChatFormatting.BOLD);
			case 2 -> c = c.withStyle(ChatFormatting.ITALIC);
			case 3 -> c = c.withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC);
			case 4 -> c = c.withStyle(ChatFormatting.UNDERLINE);
			default -> {
			}
		}
		return c;
	}

	public static void draw(GuiGraphics g, Font f, String s, int x, int y, int color, boolean shadow) {
		boolean sh = shadow || Theme.TEXT_SHADOW.bool();
		if (plain()) {
			g.drawString(f, s, x, y, color, sh);
			return;
		}
		g.drawString(f, comp(s), x, y, color, sh);
	}

	public static int width(Font f, String s) {
		return plain() ? f.width(s) : f.width(comp(s));
	}
}
