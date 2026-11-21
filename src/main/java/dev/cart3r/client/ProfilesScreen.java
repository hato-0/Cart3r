package dev.cart3r.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ProfilesScreen extends Screen {
	private static final int PW = 360;
	private static final int ROW = 26;

	private EditBox box;
	private boolean leftWas;
	private boolean enterWas;
	private float scroll;
	private int px;
	private int py;
	private int ph;
	private float alpha = 1f;

	public ProfilesScreen() {
		super(Component.literal("Profiles"));
		Theme.restart();
		leftWas = Input.left();
		enterWas = Input.key(GLFW.GLFW_KEY_ENTER);
	}

	@Override
	protected void init() {
		ph = Math.min(300, this.height - 16);
		px = (this.width - PW) / 2;
		py = (this.height - ph) / 2;
		box = new EditBox(this.font, px + 12, py + 36, PW - 12 - 12 - 96, 16, Component.literal("Name"));
		box.setMaxLength(20);
		box.setHint(Component.literal("Profile name..."));
		this.addWidget(box);
		this.setInitialFocus(box);
	}

	private int a(int color, double k) {
		return Theme.alpha(color, alpha * k);
	}

	private static boolean inside(double x, double y, int rx, int ry, int rw, int rh) {
		return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
	}

	private float maxScroll() {
		return Math.max(0f, Profiles.LIST.size() * ROW - (ph - 36 - 26 - 34));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll = Math.max(0f, Math.min(maxScroll(), scroll - (float) scrollY * 26f));
		return true;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		Minecraft mc = Minecraft.getInstance();
		Font f = this.font;
		alpha = Theme.progress();
		int accent = Theme.accent();
		boolean l = Input.left();
		boolean click = l && !leftWas;
		leftWas = l;

		int slide = (int) ((1f - alpha) * 10f);
		int top = py + slide;

		if (Theme.glowOn()) {
			Gfx.glow(g, px, top, px + PW, top + ph, 5, accent, alpha);
		}
		Gfx.card(g, px, top, px + PW, top + ph, 5, a(Theme.dim(accent, 0.55), 1), a(Theme.PANEL, Theme.panelAlpha()));
		Txt.draw(g, f, "PROFILES", px + 12, top + 9, a(accent, 1), false);
		String sub = Profiles.active.isEmpty() ? "no profile active" : "active: " + Profiles.active;
		Txt.draw(g, f, sub, px + PW - 12 - Txt.width(f, sub), top + 9, a(Theme.MUTED, 1), false);
		g.fill(px + 10, top + 25, px + PW - 10, top + 26, a(Theme.LINE, 1));

		box.setY(top + 36);
		box.render(g, mouseX, mouseY, delta);

		int sx = px + PW - 12 - 90;
		boolean hs = inside(mouseX, mouseY, sx, top + 35, 90, 18);
		Gfx.round(g, sx, top + 35, sx + 90, top + 53, 3, a(hs ? Theme.dim(accent, 0.6) : Theme.dim(accent, 0.38), 1));
		String st = "Save current";
		Txt.draw(g, f, st, sx + (90 - Txt.width(f, st)) / 2, top + 40, a(Theme.TEXT, 1), false);
		boolean enter = Input.key(GLFW.GLFW_KEY_ENTER) || Input.key(GLFW.GLFW_KEY_KP_ENTER);
		boolean enterClick = enter && !enterWas && box.isFocused();
		enterWas = enter;
		if ((hs && click) || enterClick) {
			if (Profiles.saveCurrent(box.getValue())) {
				box.setValue("");
			}
		}

		int listTop = top + 62;
		int listBottom = top + ph - 30;
		List<Profiles.Profile> list = new ArrayList<>(Profiles.LIST);
		scroll = Math.max(0f, Math.min(maxScroll(), scroll));
		g.enableScissor(px + 6, listTop, px + PW - 6, listBottom);
		int y = listTop - Math.round(scroll);
		Profiles.Profile toLoad = null;
		String toDelete = null;
		for (Profiles.Profile p : list) {
			if (y + ROW > listTop - ROW && y < listBottom) {
				boolean rowHover = inside(mouseX, mouseY, px + 10, y, PW - 20, ROW - 3)
					&& mouseY >= listTop && mouseY < listBottom;
				boolean on = p.name().equalsIgnoreCase(Profiles.active);
				int pc = Theme.colorOf(p.accent());
				Gfx.round(g, px + 10, y, px + PW - 10, y + ROW - 3, 3, a(rowHover ? Theme.CARD_HOVER : Theme.CARD, 1));
				Gfx.round(g, px + 10, y + 3, px + 13, y + ROW - 6, 1, a(on ? pc : Theme.LINE, 1));
				Gfx.circle(g, px + 25, y + 10, 4, a(pc, 1));
				Txt.draw(g, f, p.name(), px + 36, y + 3, a(Theme.TEXT, 1), false);
				String info = p.mods().size() + " cheats";
				Txt.draw(g, f, info, px + 36, y + 13, a(Theme.MUTED, 1), false);

				int dx = px + PW - 10 - 20;
				int lx = dx - 6 - 48;
				boolean hl = inside(mouseX, mouseY, lx, y + 3, 48, 16) && mouseY >= listTop && mouseY < listBottom;
				boolean hd = inside(mouseX, mouseY, dx, y + 3, 16, 16) && mouseY >= listTop && mouseY < listBottom;
				Gfx.round(g, lx, y + 3, lx + 48, y + 19, 3, a(hl ? Theme.dim(pc, 0.7) : Theme.dim(pc, 0.4), 1));
				Txt.draw(g, f, on ? "Active" : "Load", lx + (48 - Txt.width(f, on ? "Active" : "Load")) / 2, y + 7, a(Theme.TEXT, 1), false);
				Gfx.round(g, dx, y + 3, dx + 16, y + 19, 3, a(hd ? 0xFFB02A3A : 0xFF2A1A1F, 1));
				Txt.draw(g, f, "x", dx + 6, y + 7, a(Theme.TEXT, 1), false);
				if (click && hl) {
					toLoad = p;
				}
				if (click && hd) {
					toDelete = p.name();
				}
			}
			y += ROW;
		}
		g.disableScissor();
		if (list.isEmpty()) {
			String e = "No profiles yet. Turn on some cheats, type a name, press Save.";
			Txt.draw(g, f, e, px + (PW - Txt.width(f, e)) / 2, listTop + 20, a(Theme.MUTED, 1), false);
		}

		int by = top + ph - 24;
		boolean hb = inside(mouseX, mouseY, px + 10, by, 60, 16);
		Gfx.round(g, px + 10, by, px + 70, by + 16, 3, a(hb ? Theme.dim(accent, 0.45) : 0xFF181821, 1));
		Txt.draw(g, f, "Back", px + 10 + (60 - Txt.width(f, "Back")) / 2, by + 4, a(Theme.TEXT, 1), false);
		String hint = "Saves your active cheats and the client color";
		Txt.draw(g, f, hint, px + PW - 10 - Txt.width(f, hint), by + 4, a(Theme.MUTED, 1), false);

		if (toLoad != null) {
			Profiles.apply(mc, toLoad);
		}
		if (toDelete != null) {
			Profiles.delete(toDelete);
		}
		if (hb && click) {
			mc.setScreen(new MenuScreen());
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
