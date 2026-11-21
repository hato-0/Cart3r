package dev.cart3r.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public final class Notify {
	private record Item(String text, int color, long born) {
	}

	private static final List<Item> ITEMS = new ArrayList<>();
	private static final long LIFE = 2400L;
	public static boolean mute;

	private Notify() {
	}

	public static void push(String text, int color) {
		if (mute || !Theme.NOTIFY.bool()) {
			return;
		}
		ITEMS.add(new Item(text, color, System.currentTimeMillis()));
		while (ITEMS.size() > 6) {
			ITEMS.remove(0);
		}
	}

	public static void module(String name, boolean on) {
		push(name + (on ? "  ON" : "  OFF"), on ? Theme.accent() : Theme.MUTED);
	}

	public static void draw(GuiGraphics g, Font f) {
		int mid = g.guiHeight() / 2 - 10;
		HudLayout.report(HudLayout.NOTIFY, 8, mid, 150, mid + 18);
		if (ITEMS.isEmpty()) {
			return;
		}
		HudLayout.begin(g, HudLayout.NOTIFY);
		long now = System.currentTimeMillis();
		ITEMS.removeIf(i -> now - i.born() > LIFE);
		int y = g.guiHeight() / 2 - 10;
		for (Item it : ITEMS) {
			long age = now - it.born();
			double in = Math.min(1.0, age / 260.0);
			double ease = 1.0 - Math.pow(1.0 - in, 3);
			double out = age > LIFE - 300 ? (LIFE - age) / 300.0 : 1.0;
			int w = Txt.width(f, it.text());
			int x = (int) (8 - (w + 24) * (1.0 - ease));
			Gfx.round(g, x, y, x + w + 16, y + 15, 3, Theme.alpha(Theme.hudBg(), out));
			g.fill(x, y + 2, x + 2, y + 13, Theme.alpha(it.color(), out));
			Txt.draw(g, f, it.text(), x + 8, y + 4, Theme.alpha(Theme.TEXT, out), false);
			y += 18;
		}
		HudLayout.end(g);
	}
}
