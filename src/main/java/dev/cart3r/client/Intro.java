package dev.cart3r.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class Intro {
	private static long start = -1L;
	private static final long LIFE = 3200L;

	private Intro() {
	}

	public static void trigger() {
		start = System.currentTimeMillis();
	}

	public static void draw(GuiGraphics g, Minecraft mc) {
		if (start < 0 || !Theme.INTRO.bool()) {
			return;
		}
		long t = System.currentTimeMillis() - start;
		if (t > LIFE) {
			start = -1L;
			return;
		}
		double in = Math.min(1.0, t / 500.0);
		double ease = 1.0 - Math.pow(1.0 - in, 3);
		double out = t > LIFE - 600 ? (LIFE - t) / 600.0 : 1.0;
		double a = ease * out;
		Font f = mc.font;
		int w = 190;
		int h = 56;
		int x = (g.guiWidth() - w) / 2;
		int y = g.guiHeight() / 4 - (int) ((1.0 - ease) * 14);
		int accent = Theme.accent();
		Gfx.card(g, x, y, x + w, y + h, 5, Theme.alpha(Theme.dim(accent, 0.6), a), Theme.alpha(Theme.PANEL, 0.95 * a));
		Logo.draw(g, x + 10, y + 10, 36, a);
		var p = g.pose();
		p.pushMatrix();
		p.translate((float) (x + 56), (float) (y + 11));
		p.scale(2f, 2f);
		Txt.draw(g, f, "HATO", 0, 0, Theme.alpha(accent, a), false);
		p.popMatrix();
		Txt.draw(g, f, "CLIENT  v17", x + 57, y + 31, Theme.alpha(Theme.MUTED, a), false);
		String hint = "Press " + Module.keyName(ModuleManager.INTERFACE.getKey()) + " to open the menu";
		Txt.draw(g, f, hint, x + 57, y + 42, Theme.alpha(Theme.TEXT, a * 0.8), false);
		int bar = (int) ((w - 20) * Math.min(1.0, t / (double) LIFE));
		g.fill(x + 10, y + h - 5, x + 10 + bar, y + h - 4, Theme.alpha(accent, a));
	}
}
