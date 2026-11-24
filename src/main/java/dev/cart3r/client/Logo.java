package dev.cart3r.client;

import net.minecraft.client.gui.GuiGraphics;

public final class Logo {
	private Logo() {
	}

	public static void draw(GuiGraphics g, int x, int y, int s, double a) {
		int accent = Theme.accent();
		int r = Math.max(2, s / 5);
		Gfx.round(g, x, y, x + s, y + s, r, Theme.alpha(Theme.dim(accent, 0.4), a));
		g.fillGradient(x + r / 2, y + r / 2, x + s - r / 2, y + s - r / 2, Theme.alpha(accent, 0.6 * a), 0x00000000);
		int u = Math.max(1, s / 8);
		int white = Theme.alpha(0xFFFFFFFF, a);
		g.fill(x + 2 * u, y + 2 * u, x + 3 * u, y + s - 2 * u, white);
		g.fill(x + s - 3 * u, y + 2 * u, x + s - 2 * u, y + s - 2 * u, white);
		g.fill(x + 2 * u, y + s / 2 - u / 2, x + s - 2 * u, y + s / 2 - u / 2 + u, white);
	}
}
