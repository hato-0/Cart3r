package dev.cart3r.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class TourScreen extends Screen {
	private static final int PW = 330;
	private static final int PH = 160;

	private int step;
	private boolean leftWas;

	public TourScreen() {
		super(Component.literal("HATO Tour"));
		Theme.restart();
		leftWas = Input.left();
	}

	private static boolean inside(double x, double y, int rx, int ry, int rw, int rh) {
		return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
	}

	private String[][] steps() {
		String key = Module.keyName(ModuleManager.INTERFACE.getKey());
		return new String[][] {
			{"Welcome to HATO", "Press " + key + " at any time to open the menu. This short tour shows you the most important things."},
			{"Turn cheats on", "Left-click a cheat to switch it on or off. Right-click it, or the three dots, to open its settings and its keybind."},
			{"Find things fast", "Use the search box at the top of the menu. Shift + right-click on a cheat adds it to your Favs tab."},
			{"Make it yours", "Under Client > Interface you can change themes, colors, fonts and the HUD style. Edit HUD lets you drag and resize every HUD element."},
			{"Profiles", "Save your active cheats as a profile, give it a key, and share it with a code. Export all copies everything to your clipboard."},
			{"What is new in v17", "Bigger menu with colored tabs and counters. New: Target HUD, Inventory HUD, Block info, Stats HUD, AirJump, AquaSpeed, Freelook, SmoothZoom and many alerts."},
			{"Commands", "/hato opens the menu. /hato hud edits the HUD. /hato note add <text> adds a note. /hato tour shows this tour again."}
		};
	}

	private List<String> wrap(Font f, String text, int width) {
		List<String> out = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			String test = line.length() == 0 ? word : line + " " + word;
			if (Txt.width(f, test) > width && line.length() > 0) {
				out.add(line.toString());
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(test);
			}
		}
		if (line.length() > 0) {
			out.add(line.toString());
		}
		return out;
	}

	private void finish(Minecraft mc) {
		Tour.markDone();
		mc.setScreen(null);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		Minecraft mc = Minecraft.getInstance();
		Font f = this.font;
		float p = Theme.progress();
		int accent = Theme.accent();
		boolean l = Input.left();
		boolean click = l && !leftWas;
		leftWas = l;

		String[][] st = steps();
		int px = (this.width - PW) / 2;
		int py = (this.height - PH) / 2 + (int) ((1f - p) * 10f);
		Gfx.card(g, px, py, px + PW, py + PH, 5, Theme.alpha(Theme.dim(accent, 0.55), p), Theme.alpha(Theme.PANEL, 0.97 * p));
		Logo.draw(g, px + 12, py + 12, 26, p);
		Txt.draw(g, f, st[step][0], px + 48, py + 14, Theme.alpha(accent, p), false);
		Txt.draw(g, f, "Step " + (step + 1) + " of " + st.length, px + 48, py + 26, Theme.alpha(Theme.MUTED, p), false);
		g.fill(px + 10, py + 44, px + PW - 10, py + 45, Theme.alpha(Theme.LINE, p));

		int y = py + 54;
		for (String line : wrap(f, st[step][1], PW - 28)) {
			Txt.draw(g, f, line, px + 14, y, Theme.alpha(Theme.TEXT, p), false);
			y += 12;
		}

		int dotsW = st.length * 10;
		for (int i = 0; i < st.length; i++) {
			int dx = px + (PW - dotsW) / 2 + i * 10;
			Gfx.circle(g, dx + 3, py + PH - 38, 2, i == step ? Theme.alpha(accent, p) : Theme.alpha(Theme.LINE, p));
		}

		int by = py + PH - 26;
		boolean hs = inside(mouseX, mouseY, px + 12, by, 56, 16);
		boolean hb = step > 0 && inside(mouseX, mouseY, px + PW - 12 - 126, by, 60, 16);
		boolean hn = inside(mouseX, mouseY, px + PW - 12 - 60, by, 60, 16);
		Gfx.round(g, px + 12, by, px + 68, by + 16, 3, Theme.alpha(hs ? 0xFF2E2E3A : 0xFF20202A, p));
		Txt.draw(g, f, "Skip", px + 12 + (56 - Txt.width(f, "Skip")) / 2, by + 4, Theme.alpha(Theme.TEXT, p), false);
		if (step > 0) {
			Gfx.round(g, px + PW - 12 - 126, by, px + PW - 12 - 66, by + 16, 3, Theme.alpha(hb ? 0xFF2E2E3A : 0xFF20202A, p));
			Txt.draw(g, f, "Back", px + PW - 12 - 126 + (60 - Txt.width(f, "Back")) / 2, by + 4, Theme.alpha(Theme.TEXT, p), false);
		}
		boolean last = step == st.length - 1;
		String nt = last ? "Finish" : "Next";
		Gfx.round(g, px + PW - 12 - 60, by, px + PW - 12, by + 16, 3, Theme.alpha(hn ? Theme.dim(accent, 0.7) : Theme.dim(accent, 0.45), p));
		Txt.draw(g, f, nt, px + PW - 12 - 60 + (60 - Txt.width(f, nt)) / 2, by + 4, Theme.alpha(Theme.TEXT, p), false);

		if (click) {
			if (hs) {
				finish(mc);
			} else if (hb) {
				step--;
			} else if (hn) {
				if (last) {
					finish(mc);
				} else {
					step++;
				}
			}
		}
	}

	@Override
	public void onClose() {
		Tour.markDone();
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
