package dev.cart3r.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {
	private static final int SNAP = 6;
	private static final int MARGIN = 4;
	private static final int GRID = 8;

	private int drag = -1;
	private int startMx;
	private int startMy;
	private int startOffX;
	private int startOffY;
	private final int[] startRect = new int[4];
	private int mouseNowX;
	private int mouseNowY;
	private int guideX = -1;
	private int guideY = -1;
	private boolean grid;
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

	private int hover(int mx, int my) {
		for (int i = HudLayout.NAMES.length - 1; i >= 0; i--) {
			if (!HudLayout.visible(i)) {
				continue;
			}
			int[] r = HudLayout.rect(i);
			if (inside(mx, my, r[0], r[1], r[2] - r[0], r[3] - r[1])) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int h = drag >= 0 ? drag : hover(mouseNowX, mouseNowY);
		if (h >= 0) {
			HudLayout.addScale(h, scrollY > 0 ? 0.1f : -0.1f);
			HudLayout.save();
			return true;
		}
		return false;
	}

	private int bestSnap(int[] values, int[] targets) {
		int best = 0;
		int bestAbs = SNAP + 1;
		for (int v : values) {
			for (int t : targets) {
				int d = t - v;
				if (Math.abs(d) < bestAbs) {
					bestAbs = Math.abs(d);
					best = d;
				}
			}
		}
		return bestAbs <= SNAP ? best : 0;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		Minecraft mc = Minecraft.getInstance();
		Font f = this.font;
		int accent = Theme.accent();
		float p = Theme.progress();
		mouseNowX = mouseX;
		mouseNowY = mouseY;
		g.fill(0, 0, this.width, this.height, Theme.alpha(0xFF000000, 0.35 * p));

		if (grid) {
			for (int x = 0; x < this.width; x += GRID * 4) {
				g.fill(x, 0, x + 1, this.height, Theme.alpha(0xFFFFFFFF, 0.06));
			}
			for (int y = 0; y < this.height; y += GRID * 4) {
				g.fill(0, y, this.width, y + 1, Theme.alpha(0xFFFFFFFF, 0.06));
			}
		}
		g.fill(this.width / 2, 0, this.width / 2 + 1, this.height, Theme.alpha(0xFFFFFFFF, 0.08));
		g.fill(0, this.height / 2, this.width, this.height / 2 + 1, Theme.alpha(0xFFFFFFFF, 0.08));

		boolean l = Input.left();
		boolean click = l && !leftWas;
		leftWas = l;

		if (click) {
			drag = hover(mouseX, mouseY);
			if (drag >= 0) {
				startMx = mouseX;
				startMy = mouseY;
				startOffX = HudLayout.offX(drag);
				startOffY = HudLayout.offY(drag);
				int[] r = HudLayout.rect(drag);
				System.arraycopy(r, 0, startRect, 0, 4);
			}
		}
		if (!l && drag >= 0) {
			drag = -1;
			guideX = -1;
			guideY = -1;
			HudLayout.save();
		}
		if (l && drag >= 0) {
			int dx = mouseX - startMx;
			int dy = mouseY - startMy;
			int x1 = startRect[0] + dx;
			int x2 = startRect[2] + dx;
			int y1 = startRect[1] + dy;
			int y2 = startRect[3] + dy;
			int sx = bestSnap(new int[] {x1, x2, (x1 + x2) / 2}, new int[] {MARGIN, this.width - MARGIN, this.width / 2});
			int sy = bestSnap(new int[] {y1, y2, (y1 + y2) / 2}, new int[] {MARGIN, this.height - MARGIN, this.height / 2});
			guideX = -1;
			guideY = -1;
			if (sx != 0) {
				guideX = sx;
			} else if (grid) {
				sx = Math.round(x1 / (float) GRID) * GRID - x1;
			}
			if (sy != 0) {
				guideY = sy;
			} else if (grid) {
				sy = Math.round(y1 / (float) GRID) * GRID - y1;
			}
			int tx = startOffX + dx + sx;
			int ty = startOffY + dy + sy;
			HudLayout.move(drag, tx - HudLayout.offX(drag), ty - HudLayout.offY(drag), this.width, this.height);
		}

		int hot = drag >= 0 ? drag : hover(mouseX, mouseY);
		for (int i = 0; i < HudLayout.NAMES.length; i++) {
			if (!HudLayout.visible(i)) {
				continue;
			}
			int[] r = HudLayout.rect(i);
			boolean isHot = i == hot;
			int c = isHot ? accent : Theme.alpha(0xFFFFFFFF, 0.55);
			g.fill(r[0] - 1, r[1] - 1, r[2] + 1, r[1], c);
			g.fill(r[0] - 1, r[3], r[2] + 1, r[3] + 1, c);
			g.fill(r[0] - 1, r[1], r[0], r[3], c);
			g.fill(r[2], r[1], r[2] + 1, r[3], c);
			if (isHot) {
				g.fill(r[0], r[1], r[2], r[3], Theme.alpha(accent, 0.18));
			}
			String name = HudLayout.NAMES[i] + (HudLayout.scale(i) != 1f ? "  x" + String.format("%.2f", HudLayout.scale(i)) : "");
			int ty = r[1] > 12 ? r[1] - 11 : r[3] + 3;
			Txt.draw(g, f, name, r[0], ty, isHot ? accent : Theme.TEXT, true);
		}

		int cx = this.width / 2;
		String title = "HUD EDITOR";
		String hint = "Drag to move  -  Mouse wheel to resize  -  Edges snap automatically";
		int bw = Math.max(Txt.width(f, hint), 260) + 24;
		int top = 6;
		Gfx.card(g, cx - bw / 2, top, cx + bw / 2, top + 50, 4, Theme.dim(accent, 0.55), Theme.alpha(Theme.PANEL, 0.95));
		Txt.draw(g, f, title, cx - Txt.width(f, title) / 2, top + 7, accent, false);
		Txt.draw(g, f, hint, cx - Txt.width(f, hint) / 2, top + 19, Theme.MUTED, false);

		int by = top + 31;
		boolean hd = inside(mouseX, mouseY, cx - 100, by, 60, 14);
		boolean hg = inside(mouseX, mouseY, cx - 30, by, 60, 14);
		boolean hr = inside(mouseX, mouseY, cx + 40, by, 60, 14);
		Gfx.round(g, cx - 100, by, cx - 40, by + 14, 3, hd ? Theme.dim(accent, 0.6) : Theme.dim(accent, 0.38));
		Gfx.round(g, cx - 30, by, cx + 30, by + 14, 3, grid ? Theme.dim(accent, 0.6) : (hg ? 0xFF2A2A34 : 0xFF1E1E26));
		Gfx.round(g, cx + 40, by, cx + 100, by + 14, 3, hr ? 0xFF3A2026 : 0xFF241A1E);
		Txt.draw(g, f, "Done", cx - 70 - Txt.width(f, "Done") / 2, by + 3, Theme.TEXT, false);
		String gl = grid ? "Grid on" : "Grid off";
		Txt.draw(g, f, gl, cx - Txt.width(f, gl) / 2, by + 3, Theme.TEXT, false);
		Txt.draw(g, f, "Reset all", cx + 70 - Txt.width(f, "Reset all") / 2, by + 3, Theme.TEXT, false);
		if (click && hd) {
			HudLayout.save();
			mc.setScreen(new MenuScreen());
		} else if (click && hg) {
			grid = !grid;
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
