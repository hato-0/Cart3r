package dev.cart3r.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class Gfx {
	private Gfx() {
	}

	public static void round(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int c) {
		if (r <= 0 || x2 - x1 < 2 * r || y2 - y1 < 2 * r) {
			g.fill(x1, y1, x2, y2, c);
			return;
		}
		for (int i = 0; i < r; i++) {
			int inset = r - i;
			g.fill(x1 + inset, y1 + i, x2 - inset, y1 + i + 1, c);
			g.fill(x1 + inset, y2 - i - 1, x2 - inset, y2 - i, c);
		}
		g.fill(x1, y1 + r, x2, y2 - r, c);
	}

	public static void card(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int border, int fill) {
		round(g, x1, y1, x2, y2, r, border);
		round(g, x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(1, r - 1), fill);
	}

	public static void glow(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int color, double a) {
		for (int i = 4; i >= 1; i--) {
			double k = a * (0.02 + (4 - i) * 0.02);
			round(g, x1 - i * 2, y1 - i * 2, x2 + i * 2, y2 + i * 2, r + i, Theme.alpha(color, k));
		}
	}

	public static void circle(GuiGraphics g, int cx, int cy, int r, int color) {
		for (int dy = -r; dy <= r; dy++) {
			int half = (int) Math.sqrt((double) r * r - (double) dy * dy);
			g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
		}
	}

	public static void line(GuiGraphics g, double x1, double y1, double x2, double y2, float thickness, int color) {
		double dx = x2 - x1;
		double dy = y2 - y1;
		double len = Math.hypot(dx, dy);
		if (len < 0.5) {
			return;
		}
		var p = g.pose();
		p.pushMatrix();
		p.translate((float) x1, (float) y1);
		p.rotate((float) Math.atan2(dy, dx));
		p.translate(0f, -thickness / 2f);
		p.scale(1f, thickness);
		g.fill(0, 0, (int) Math.ceil(len), 1, color);
		p.popMatrix();
	}

	public static void pushScale(GuiGraphics g, float cx, float cy, float s) {
		var p = g.pose();
		p.pushMatrix();
		p.translate(cx, cy);
		p.scale(s, s);
		p.translate(-cx, -cy);
	}

	public static void pop(GuiGraphics g) {
		g.pose().popMatrix();
	}

	public static int textWidth(String s) {
		return Minecraft.getInstance().font.width(s);
	}
}
