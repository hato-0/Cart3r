package dev.cart3r.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {
	private int drag = -1;
	private int lastX;
	private int lastY;
	private boolean leftWas;

	public HudEditorScreen() {
		super(Component.literal("HUD Editor"));
		Theme.restart();
		leftWas = Input.left();
	}

	private static boolean inside(double x, double y, int rx, int ry, int rw, int rh) {
		return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		Minecraft mc = Minecraft.getInstance();
		Font f = this.font;
		int accent = Theme.accent();
		float p = Theme.progress();
		g.fill(0, 0, this.width, this.height, Theme.alpha(0xFF000000, 0.35 * p));

		boolean l = Input.left();
		boolean click = l && !leftWas;
		leftWas = l;

		if (click) {
			drag = -1;
			for (int i = HudLayout.NAMES.length - 1; i >= 0; i--) {
				if (!HudLayout.visible(i)) {
					continue;
				}
				int[] r = HudLayout.rect(i);
				if (inside(mouseX, mouseY, r[0], r[1], r[2] - r[0], r[3] - r[1])) {
					drag = i;
					break;
				}
			}
			lastX = mouseX;
			lastY = mouseY;
		}
		if (!l && drag >= 0) {
			drag = -1;
			HudLayout.save();
		}
		if (l && drag >= 0) {
			HudLayout.move(drag, mouseX - lastX, mouseY - lastY, this.width, this.height);
			lastX = mouseX;
			lastY = mouseY;
		}

		for (int i = 0; i < HudLayout.NAMES.length; i++) {
			if (!HudLayout.visible(i)) {
				continue;
			}
			int[] r = HudLayout.rect(i);
			boolean hot = i == drag || (drag < 0 && inside(mouseX, mouseY, r[0], r[1], r[2] - r[0], r[3] - r[1]));
			int c = hot ? accent : Theme.alpha(0xFFFFFFFF, 0.55);
			g.fill(r[0] - 1, r[1] - 1, r[2] + 1, r[1], c);
			g.fill(r[0] - 1, r[3], r[2] + 1, r[3] + 1, c);
			g.fill(r[0] - 1, r[1], r[0], r[3], c);
			g.fill(r[2], r[1], r[2] + 1, r[3], c);
			if (hot) {
				g.fill(r[0], r[1], r[2], r[3], Theme.alpha(accent, 0.18));
			}
			String name = HudLayout.NAMES[i];
			int ty = r[1] > 12 ? r[1] - 11 : r[3] + 3;
			Txt.draw(g, f, name, r[0], ty, hot ? accent : Theme.TEXT, true);
		}

		int cx = this.width / 2;
		String title = "HUD EDITOR";
		String hint = "Drag elements to move them. Only enabled ones are shown.";
		int bw = Math.max(Txt.width(f, hint), 150) + 24;
		int top = 6;
		Gfx.card(g, cx - bw / 2, top, cx + bw / 2, top + 50, 4, Theme.dim(accent, 0.55), Theme.alpha(Theme.PANEL, 0.95));
		Txt.draw(g, f, title, cx - Txt.width(f, title) / 2, top + 7, accent, false);
		Txt.draw(g, f, hint, cx - Txt.width(f, hint) / 2, top + 19, Theme.MUTED, false);

		int by = top + 31;
		boolean hd = inside(mouseX, mouseY, cx - 64, by, 60, 14);
		boolean hr = inside(mouseX, mouseY, cx + 4, by, 60, 14);
		Gfx.round(g, cx - 64, by, cx - 4, by + 14, 3, hd ? Theme.dim(accent, 0.6) : Theme.dim(accent, 0.38));
		Gfx.round(g, cx + 4, by, cx + 64, by + 14, 3, hr ? 0xFF3A2026 : 0xFF241A1E);
		Txt.draw(g, f, "Done", cx - 34 - Txt.width(f, "Done") / 2, by + 3, Theme.TEXT, false);
		Txt.draw(g, f, "Reset all", cx + 34 - Txt.width(f, "Reset all") / 2, by + 3, Theme.TEXT, false);
		if (click && hd) {
			HudLayout.save();
			mc.setScreen(new MenuScreen());
		} else if (click && hr) {
			HudLayout.reset();
		}
	}

	@Override
	public void onClose() {
		HudLayout.save();
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
