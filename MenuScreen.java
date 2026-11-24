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

public class MenuScreen extends Screen {
	private static final int PW = 520;
	private static final int SB = 96;
	private static final int ROW = 22;
	private static final int[] KEYS = buildKeys();

	private static Module.Category selected = Module.Category.HOME;
	private static Module picked = null;

	private float scroll;
	private float scrollAnim;
	private float tabX = -1f;
	private float sbY = -1f;
	private final float[] catHov = new float[16];
	private Module lastPicked;
	private long paneStart;
	private long catStart = System.currentTimeMillis();
	private boolean leftWas;
	private boolean rightWas;
	private boolean upWas;
	private boolean downWas;
	private boolean closeRequested;
	private Module listening;
	private Setting dragging;
	private final boolean[] held = new boolean[400];
	private int px;
	private int py;
	private int ph;
	private int listTop;
	private int listBottom;
	private double mx;
	private double my;
	private float alpha = 1f;
	private String hoverDesc = "";
	private EditBox search;
	private String lastSearch = "";

	public MenuScreen() {
		super(Component.literal("HATO CLIENT"));
		Theme.restart();
		leftWas = Input.left();
		rightWas = Input.right();
	}

	private static int[] buildKeys() {
		List<Integer> l = new ArrayList<>();
		l.add(32);
		l.add(39);
		for (int k = 44; k <= 57; k++) {
			l.add(k);
		}
		l.add(59);
		l.add(61);
		for (int k = 65; k <= 93; k++) {
			l.add(k);
		}
		l.add(96);
		for (int k = 256; k <= 269; k++) {
			l.add(k);
		}
		for (int k = 280; k <= 284; k++) {
			l.add(k);
		}
		for (int k = 290; k <= 314; k++) {
			l.add(k);
		}
		for (int k = 320; k <= 336; k++) {
			l.add(k);
		}
		for (int k = 340; k <= 348; k++) {
			l.add(k);
		}
		int[] out = new int[l.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = l.get(i);
		}
		return out;
	}

	private int a(int color, double k) {
		return Theme.alpha(color, alpha * k);
	}

	private static boolean inside(double x, double y, int rx, int ry, int rw, int rh) {
		return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
	}

	private String query() {
		return search == null ? "" : search.getValue().trim().toLowerCase();
	}

	@Override
	protected void init() {
		search = new EditBox(this.font, px0() + PW - 142, 0, 104, 12, Component.literal("Search"));
		search.setBordered(false);
		search.setMaxLength(16);
		search.setHint(Component.literal("Search..."));
		this.addWidget(search);
	}

	private int px0() {
		return (this.width - PW) / 2;
	}

	private List<Module> mods() {
		List<Module> out = new ArrayList<>();
		String q = query();
		for (Module m : ModuleManager.MODULES) {
			if (!q.isEmpty() ? m.getName().toLowerCase().contains(q)
				: (selected == Module.Category.FAVS ? m.favorite : selected == Module.Category.ACTIVE ? (m.isEnabled() && m != ModuleManager.INTERFACE) : m.getCategory() == selected)) {
				out.add(m);
			}
		}
		return out;
	}

	private float maxScroll() {
		return Math.max(0f, mods().size() * ROW - (listBottom - listTop));
	}

	private void select(Module.Category c) {
		selected = c;
		Sfx.tick();
		scroll = 0f;
		scrollAnim = 0f;
		catStart = System.currentTimeMillis();
		List<Module> l = mods();
		if (!l.isEmpty() && (picked == null || picked.getCategory() != c)) {
			picked = l.get(0);
		}
	}

	private static String keyHint(Module m) {
		if (!Theme.KEY_HINTS.bool() || m.getKey() <= 0) {
			return "";
		}
		return "[" + Module.keyName(m.getKey()) + "]";
	}

	private void startListening(Module m) {
		for (int k : KEYS) {
			held[k] = Input.key(k);
		}
		listening = m;
	}

	private void pollKey() {
		for (int k : KEYS) {
			boolean d = Input.key(k);
			if (d && !held[k]) {
				held[k] = true;
				if (k == 256) {
					listening = null;
				} else if (k == 259 || k == 261) {
					listening.setKey(0);
					listening = null;
					Config.save();
				} else {
					listening.setKey(k);
					listening = null;
					Config.save();
				}
				return;
			}
			held[k] = d;
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll = Math.max(0f, Math.min(maxScroll(), scroll - (float) scrollY * 24f));
		return true;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		Minecraft mc = Minecraft.getInstance();
		Font f = this.font;
		ph = Math.min(340, this.height - 16);
		px = (this.width - PW) / 2;

		float p = Theme.progress();
		alpha = p;
		py = (this.height - ph) / 2 + (int) ((1f - p) * 10f);
		listTop = py + 44;
		listBottom = py + ph - 28;
		float fit = Math.min(1f, (this.width - 8f) / PW);
		float s = (0.94f + 0.06f * p) * (float) Theme.MENU_SCALE.num() * fit;
		float cx = this.width / 2f;
		float cy = this.height / 2f;
		mx = (mouseX - cx) / s + cx;
		my = (mouseY - cy) / s + cy;

		boolean l = Input.left();
		boolean r = Input.right();
		boolean lClick = l && !leftWas;
		boolean rClick = r && !rightWas;

		if (picked == null) {
			List<Module> first = mods();
			if (!first.isEmpty()) {
				picked = first.get(0);
			}
		}
		if (listening != null) {
			pollKey();
		}
		boolean up = Input.key(GLFW.GLFW_KEY_UP);
		boolean down = Input.key(GLFW.GLFW_KEY_DOWN);
		if (up && !upWas) {
			scroll = Math.max(0f, scroll - ROW);
		}
		if (down && !downWas) {
			scroll = Math.min(maxScroll(), scroll + ROW);
		}
		upWas = up;
		downWas = down;

		hoverDesc = "";
		if (lClick) {
			rippleX = (float) mx;
			rippleY = (float) my;
			rippleAt = System.currentTimeMillis();
		}
		g.fillGradient(0, 0, this.width, this.height, Theme.alpha(0xFF000000, 0.25 * p), Theme.alpha(0xFF000000, 0.55 * p));
		Gfx.pushScale(g, cx, cy, s);
		drawFrame(g, f);
		drawSearch(g, f, mouseX, mouseY, delta);
		drawHelp(g, f, mc, lClick);
		drawSidebar(g, f, lClick);
		if (selected == Module.Category.HOME && query().isEmpty()) {
			drawHome(g, f, mc, lClick);
		} else {
			drawList(g, f, mc, lClick, rClick);
			drawPane(g, f, mc, lClick, rClick, l);
		}
		drawFooter(g, f, mc, lClick);
		float rt = (System.currentTimeMillis() - rippleAt) / 420f;
		if (rt >= 0f && rt < 1f) {
			Gfx.circle(g, Math.round(rippleX), Math.round(rippleY), 3 + (int) (rt * 14f), a(Theme.accent(), (1f - rt) * 0.16));
		}
		Gfx.pop(g);

		if (!l && dragging != null) {
			dragging = null;
			Config.save();
		}
		leftWas = l;
		rightWas = r;
		if (closeRequested) {
			closeRequested = false;
			this.onClose();
		}
	}

	private void drawSearch(GuiGraphics g, Font f, int mouseX, int mouseY, float delta) {
		int accent = Theme.accent();
		int sx = px + PW - 142;
		boolean focus = search.isFocused();
		Gfx.round(g, sx, py + 10, sx + 112, py + 25, 3, a(focus ? Theme.dim(accent, 0.5) : 0xFF181821, 1));
		search.setX(sx + 6);
		search.setY(py + 14);
		search.render(g, mouseX, mouseY, delta);
		String q = query();
		if (!q.equals(lastSearch)) {
			lastSearch = q;
			scroll = 0f;
			scrollAnim = 0f;
			catStart = System.currentTimeMillis();
			List<Module> l = mods();
			if (!l.isEmpty() && (picked == null || !l.contains(picked))) {
				picked = l.get(0);
			}
		}
	}

	private void drawFrame(GuiGraphics g, Font f) {
		int accent = Theme.accent();
		if (Theme.glowOn()) {
			Gfx.glow(g, px, py, px + PW, py + ph, 5, accent, alpha);
		}
		Gfx.card(g, px, py, px + PW, py + ph, 5, a(Theme.dim(accent, 0.35), 1), a(Theme.PANEL, Theme.panelAlpha()));
		Gfx.round(g, px + 5, py + 5, px + SB - 3, py + ph - 5, 4, a(0xFF0C0C12, 1));
		g.fill(px + SB, py + 10, px + SB + 1, py + ph - 10, a(Theme.LINE, 1));
		Logo.draw(g, px + 13, py + 11, 20, alpha);
		Txt.draw(g, f, "HATO", px + 38, py + 12, a(Theme.TEXT, 1), false);
		Txt.draw(g, f, "CLIENT", px + 38, py + 22, a(Theme.MUTED, 1), false);
		Txt.draw(g, f, "v21.0", px + 14, py + ph - 17, a(Theme.MUTED, 1), false);

		String title = query().isEmpty() ? selected.label : "Search";
		Txt.draw(g, f, title, px + SB + 10, py + 14, a(Theme.TEXT, 1), false);
		int active = 0;
		for (Module am : ModuleManager.MODULES) {
			if (am != ModuleManager.INTERFACE && am.isEnabled()) {
				active++;
			}
		}
		String act = active + " active";
		Txt.draw(g, f, act, px + PW - 142 - 10 - Txt.width(f, act), py + 14, a(accent, 0.9), false);

		int lx1 = px + SB + 10;
		int lx2 = px + PW - 10;
		g.fill(lx1, py + 34, lx2, py + 35, a(Theme.LINE, 1));
		float sh = (System.currentTimeMillis() % 3200L) / 3200f;
		int sx0 = lx1 + (int) ((lx2 - lx1 - 60) * sh);
		g.fill(sx0, py + 34, sx0 + 60, py + 35, a(accent, 0.25));
		g.fill(sx0 + 15, py + 34, sx0 + 45, py + 35, a(accent, 0.8));
	}

	private void drawHelp(GuiGraphics g, Font f, Minecraft mc, boolean lClick) {
		int hx = px + PW - 24;
		int hy = py + 10;
		boolean hov = mx >= hx && mx < hx + 14 && my >= hy && my < hy + 14;
		Gfx.round(g, hx, hy, hx + 14, hy + 14, 3, a(hov ? 0xFF2E2E3A : 0xFF181821, 1));
		Txt.draw(g, f, "?", hx + (14 - Txt.width(f, "?")) / 2, hy + 3, a(Theme.TEXT, 1), false);
		if (hov && lClick) {
			mc.setScreen(new TourScreen());
		}
	}

	private static final String[][] GLYPHS = {
		{"...#...", "..###..", ".#####.", "#######", ".##.##.", ".##.##.", ".##.##."},
		{"...#...", "...#...", "#######", ".#####.", "..###..", ".##.##.", ".#...#."},
		{"..###..", ".#.#.#.", "#..#..#", "#.....#", "#.....#", ".#...#.", "..###.."},
		{".......", "...#...", "....#..", "#######", "....#..", "...#...", "......."},
		{".......", ".#####.", "#..#..#", "#.###.#", "#..#..#", ".#####.", "......."},
		{".#####.", "#.#.#.#", "#######", "#.#.#.#", "#######", "#.#.#.#", ".#####."},
		{"...#...", "...#...", "..###..", "#######", "..###..", "...#...", "...#..."},
		{"..###..", "..###..", "..###..", ".#####.", "#.###.#", "..#.#..", "..#.#.."},
		{"#..#..#", ".#####.", "..#.#..", "###.###", "..#.#..", ".#####.", "#..#..#"},
		{"#######", "#.....#", "#######", "#.#.#.#", "#.....#", "#.#.#.#", "#######"},
		{"...###.", "..###..", ".###...", "#######", "...###.", "..##...", ".#....."},
		{"#######", "...#...", "#######", ".#.....", "#######", "...#...", "#######"}
	};

	private static final String[] TIPS = {
		"Right-click a cheat to open its settings and set a key.",
		"Shift + right-click adds a cheat to your Favs tab.",
		"Save your setup as a profile and switch with one key.",
		"Edit HUD lets you drag and resize every HUD element.",
		"Freelook and SmoothZoom make screenshots look great.",
		"Use Buffs, Creative and SetDay in your own world.",
		"Type /hato note add <text> to keep a quick note on screen."
	};

	private float rippleX;
	private float rippleY;
	private long rippleAt;

	private void drawGlyph(GuiGraphics g, int idx, int x, int y, int color, double k) {
		String[] rows = GLYPHS[Math.min(idx, GLYPHS.length - 1)];
		int c = a(color, k);
		for (int r = 0; r < rows.length; r++) {
			for (int cc = 0; cc < rows[r].length(); cc++) {
				if (rows[r].charAt(cc) == '#') {
					g.fill(x + cc, y + r, x + cc + 1, y + r + 1, c);
				}
			}
		}
	}

	private Module findMod(String name) {
		for (Module m : ModuleManager.MODULES) {
			if (m.getName().equals(name)) {
				return m;
			}
		}
		return null;
	}

	private void drawHome(GuiGraphics g, Font f, Minecraft mc, boolean lClick) {
		int accent = Theme.accent();
		int x0 = px + SB + 10;
		int x1 = px + PW - 10;
		int w = x1 - x0;
		int y0 = listTop;
		int y1 = listBottom;
		long now = System.currentTimeMillis();
		float k = Theme.clamp01((now - catStart) / 260f);
		k = 1f - (1f - k) * (1f - k);
		int slide = (int) ((1f - k) * 8f);

		String name = mc.player == null ? "" : mc.player.getName().getString();
		Txt.draw(g, f, "Welcome back, " + name, x0, y0 + 2 + slide, a(accent, k), false);
		Txt.draw(g, f, "Press " + Module.keyName(ModuleManager.INTERFACE.getKey()) + " to close this menu", x0, y0 + 13 + slide, a(Theme.MUTED, k), false);

		int active = 0;
		for (Module m : ModuleManager.MODULES) {
			if (m != ModuleManager.INTERFACE && m.isEnabled()) {
				active++;
			}
		}
		String[] lab = {"ACTIVE", "FPS", "CHEATS"};
		String[] val = {String.valueOf(active), String.valueOf(mc.getFps()), String.valueOf(ModuleManager.MODULES.size() - 1)};
		int cw = (w - 12) / 3;
		for (int i = 0; i < 3; i++) {
			int cx = x0 + i * (cw + 6);
			int cy = y0 + 28 + slide;
			Gfx.round(g, cx, cy, cx + cw, cy + 34, 4, a(Theme.CARD, k));
			Gfx.round(g, cx, cy + 6, cx + 2, cy + 28, 1, a(accent, k));
			Txt.draw(g, f, lab[i], cx + 10, cy + 6, a(Theme.MUTED, k), false);
			var p = g.pose();
			p.pushMatrix();
			p.translate((float) (cx + 10), (float) (cy + 17));
			p.scale(1.6f, 1.6f);
			Txt.draw(g, f, val[i], 0, 0, a(Theme.TEXT, k), false);
			p.popMatrix();
		}

		int ty = y0 + 72 + slide;
		Txt.draw(g, f, "QUICK TOGGLES", x0, ty, a(Theme.MUTED, k), false);
		List<Module> quick = new ArrayList<>();
		for (Module m : ModuleManager.MODULES) {
			if (m.favorite && m != ModuleManager.INTERFACE && quick.size() < 8) {
				quick.add(m);
			}
		}
		String[] defs = {"Sprint", "ESP", "Fullbright", "SmoothZoom", "StorageESP", "Freelook", "Tracers", "Speed"};
		for (String d : defs) {
			Module m = findMod(d);
			if (m != null && !quick.contains(m) && quick.size() < 8) {
				quick.add(m);
			}
		}
		int tw = (w - 18) / 4;
		for (int i = 0; i < quick.size(); i++) {
			Module m = quick.get(i);
			int tx = x0 + (i % 4) * (tw + 6);
			int tyy = ty + 12 + (i / 4) * 34 + slide;
			boolean hov = inside(mx, my, tx, tyy, tw, 30);
			m.guiHover = Theme.approach(m.guiHover, hov ? 1f : 0f, 18f);
			m.guiAnim = Theme.approach(m.guiAnim, m.isEnabled() ? 1f : 0f, 14f);
			int bg = Theme.mix(Theme.CARD, Theme.CARD_HOVER, m.guiHover);
			bg = Theme.mix(bg, Theme.dim(accent, 0.3), m.guiAnim * 0.6f);
			float kk = Theme.clamp01((now - catStart - i * 25L) / 220f);
			Gfx.round(g, tx, tyy, tx + tw, tyy + 30, 4, a(bg, kk));
			float fl = Theme.clamp01(1f - (now - m.toggledAt) / 450f);
			if (fl > 0f) {
				Gfx.round(g, tx, tyy, tx + tw, tyy + 30, 4, a(accent, kk * fl * 0.35));
			}
			Txt.draw(g, f, m.getName(), tx + 8, tyy + 6, a(Theme.mix(0xFFB4B4C0, Theme.TEXT, m.guiAnim), kk), false);
			Txt.draw(g, f, m.isEnabled() ? "ON" : "OFF", tx + 8, tyy + 17, a(m.isEnabled() ? accent : Theme.MUTED, kk), false);
			if (hov) {
				hoverDesc = m.getDescription();
				if (lClick) {
					m.toggle(mc);
				}
			}
		}

		int ry = ty + 12 + ((quick.size() + 3) / 4) * 34 + 6;
		int tipTop = y1 - 30;
		Txt.draw(g, f, "RECENT", x0, ry, a(Theme.MUTED, k), false);
		List<Module> recent = new ArrayList<>();
		for (Module m : ModuleManager.MODULES) {
			if (m.toggledAt > 0L) {
				recent.add(m);
			}
		}
		recent.sort((m1, m2) -> Long.compare(m2.toggledAt, m1.toggledAt));
		int line = ry + 12;
		if (recent.isEmpty() && line + 10 <= tipTop) {
			Txt.draw(g, f, "Nothing toggled yet", x0, line, a(Theme.MUTED, k), false);
		}
		for (int i = 0; i < Math.min(4, recent.size()) && line + 10 <= tipTop; i++) {
			Module m = recent.get(i);
			long sec = (now - m.toggledAt) / 1000L;
			String ago = sec < 60 ? sec + "s" : sec < 3600 ? (sec / 60) + "m" : (sec / 3600) + "h";
			Gfx.circle(g, x0 + 3, line + 4, 2, a(m.isEnabled() ? accent : Theme.MUTED, k));
			Txt.draw(g, f, m.getName() + "  " + (m.isEnabled() ? "on" : "off") + "  " + ago + " ago", x0 + 10, line, a(Theme.TEXT, k), false);
			line += 11;
		}

		Gfx.round(g, x0, tipTop, x1, y1 - 2, 4, a(0xFF101016, k));
		Gfx.round(g, x0, tipTop + 5, x0 + 2, y1 - 7, 1, a(accent, k));
		String tip = TIPS[(int) ((now / 12000L) % TIPS.length)];
		Txt.draw(g, f, "TIP", x0 + 10, tipTop + 5, a(accent, k), false);
		Txt.draw(g, f, tip, x0 + 10, tipTop + 16, a(Theme.TEXT, k), false);
	}

	private void drawSidebar(GuiGraphics g, Font f, boolean lClick) {
		int accent = Theme.accent();
		Module.Category[] cats = Module.Category.values();
		int ih = Math.max(14, Math.min(19, (ph - 66) / cats.length));
		int top = py + 44;
		int sel = 0;
		for (int i = 0; i < cats.length; i++) {
			if (cats[i] == selected) {
				sel = i;
			}
		}
		float target = top + sel * ih;
		sbY = sbY < 0f ? target : Theme.approach(sbY, target, 16f);
		int by = Math.round(sbY);
		Gfx.round(g, px + 8, by, px + SB - 8, by + ih - 1, 3, a(Theme.dim(accent, 0.3), 1));
		Gfx.round(g, px + 8, by + 3, px + 10, by + ih - 4, 1, a(accent, 1));
		for (int i = 0; i < cats.length; i++) {
			int y = top + i * ih;
			boolean hover = inside(mx, my, px + 8, y, SB - 16, ih - 1);
			boolean on = cats[i] == selected;
			catHov[i] = Theme.approach(catHov[i], hover ? 1f : 0f, 18f);
			if (catHov[i] > 0.02f && !on) {
				Gfx.round(g, px + 8, y, px + SB - 8, y + ih - 1, 3, a(0xFF181821, catHov[i]));
			}
			int col = on ? Theme.TEXT : Theme.mix(Theme.MUTED, Theme.TEXT, catHov[i]);
			int ty = y + (ih - 8) / 2;
			drawGlyph(g, i, px + 17, y + (ih - 7) / 2, on ? accent : Theme.mix(0xFF6A6A78, Theme.TEXT, catHov[i]), 1.0);
			Txt.draw(g, f, cats[i].label, px + 30 + Math.round(catHov[i] * 2f), ty, a(col, 1), false);
			int cnt = 0;
			for (Module cm : ModuleManager.MODULES) {
				if (cm != ModuleManager.INTERFACE && cm.isEnabled() && (cats[i] == Module.Category.FAVS ? cm.favorite : cats[i] == Module.Category.ACTIVE || cm.getCategory() == cats[i])) {
					cnt++;
				}
			}
			if (cnt > 0) {
				String cs = String.valueOf(cnt);
				Txt.draw(g, f, cs, px + SB - 14 - Txt.width(f, cs), ty, a(on ? accent : Theme.MUTED, 1), false);
			}
			if (lClick && hover && !on) {
				select(cats[i]);
			}
		}
	}

	private void drawSwitch(GuiGraphics g, int x, int y, float anim, float k) {
		int accent = Theme.accent();
		int track = Theme.mix(0xFF2A2A34, Theme.dim(accent, 0.85), anim);
		Gfx.round(g, x, y, x + 22, y + 10, 4, a(track, k));
		int kx = x + 1 + Math.round(anim * 12f);
		Gfx.round(g, kx, y + 1, kx + 8, y + 9, 3, a(Theme.mix(0xFF9A9AA8, 0xFFFFFFFF, anim), k));
	}

	private void drawList(GuiGraphics g, Font f, Minecraft mc, boolean lClick, boolean rClick) {
		int accent = Theme.accent();
		int lx = px + SB + 10;
		int lw = 190;
		List<Module> list = mods();
		scroll = Math.max(0f, Math.min(maxScroll(), scroll));
		scrollAnim = Theme.approach(scrollAnim, scroll, 20f);
		long since = System.currentTimeMillis() - catStart;

		g.enableScissor(lx - 2, listTop, lx + lw + 2, listBottom);
		for (int i = 0; i < list.size(); i++) {
			Module m = list.get(i);
			int y = Math.round(listTop + i * ROW - scrollAnim);
			if (y + 20 < listTop || y > listBottom) {
				continue;
			}
			float k = Theme.clamp01((since - i * 30L) / 180f);
			k = 1f - (1f - k) * (1f - k);
			int x = lx - Math.round((1f - k) * 14f) + Math.round(m.guiHover * 2f);
			boolean inRow = inside(mx, my, lx, y, lw, 20) && my >= listTop && my < listBottom;
			m.guiHover = Theme.approach(m.guiHover, inRow ? 1f : 0f, 18f);
			m.guiAnim = Theme.approach(m.guiAnim, m.isEnabled() ? 1f : 0f, 14f);

			int bg = Theme.mix(Theme.CARD, Theme.CARD_HOVER, m.guiHover);
			bg = Theme.mix(bg, Theme.dim(accent, 0.28), m.guiAnim * 0.55f);
			if (m == picked) {
				Gfx.card(g, x, y, x + lw, y + 20, 3, a(accent, k), a(bg, k));
			} else {
				Gfx.round(g, x, y, x + lw, y + 20, 3, a(bg, k));
			}
			float flash = Theme.clamp01(1f - (System.currentTimeMillis() - m.toggledAt) / 450f);
			if (flash > 0f) {
				Gfx.round(g, x, y, x + lw, y + 20, 3, a(accent, k * flash * 0.35));
			}
			if (m.guiAnim > 0.02f) {
				Gfx.round(g, x + 1, y + 4, x + 3, y + 16, 1, a(accent, k * m.guiAnim));
			}
			int tc = Theme.mix(0xFFB4B4C0, Theme.TEXT, Math.max(m.guiAnim, m.guiHover * 0.6f));
			String nm = m.getName();
			String ini = nm.substring(0, 1);
			for (int ci = 1; ci < nm.length(); ci++) {
				if (Character.isUpperCase(nm.charAt(ci))) {
					ini += nm.charAt(ci);
					break;
				}
			}
			if (ini.length() < 2 && nm.length() > 1) {
				ini += nm.charAt(1);
			}
			int cc = Theme.mix(0xFF8A8A98, accent, m.guiAnim);
			Gfx.round(g, x + 5, y + 3, x + 21, y + 17, 3, a(Theme.dim(cc, 0.35), k));
			Txt.draw(g, f, ini, x + 5 + (16 - Txt.width(f, ini)) / 2, y + 6, a(cc, k), false);
			Txt.draw(g, f, m.getName(), x + 26, y + 6, a(tc, k), false);
			if (m.favorite) {
				Txt.draw(g, f, "*", x + 29 + Txt.width(f, m.getName()), y + 6, a(0xFFFFC53D, k), false);
			}

			boolean isIface = m instanceof InterfaceModule;
			int gx = x + lw - 18;
			int swx1 = gx - 4 - 22;
			if (!isIface) {
				drawSwitch(g, swx1, y + 5, m.guiAnim, k);
			}
			String hint = keyHint(m);
			if (!hint.isEmpty()) {
				int hx = (isIface ? gx - 4 : swx1 - 6) - Txt.width(f, hint);
				Txt.draw(g, f, hint, hx, y + 6, a(Theme.MUTED, k), false);
			}
			boolean gearHit = inRow && inside(mx, my, gx, y + 3, 14, 14);
			int gc = (gearHit || m == picked) ? accent : Theme.MUTED;
			for (int d = 0; d < 3; d++) {
				g.fill(gx + 2 + d * 4, y + 9, gx + 4 + d * 4, y + 11, a(gc, k));
			}

			if (inRow) {
				hoverDesc = m.getDescription();
				if (rClick) {
					picked = m;
					if (Input.shift()) {
						m.favorite = !m.favorite;
						Config.save();
						Notify.push(m.getName() + (m.favorite ? " added to favorites" : " removed from favorites"), 0xFFFFC53D);
					}
				}
				if (lClick) {
					picked = m;
					if (!gearHit && !isIface) {
						m.toggle(mc);
					}
				}
			}
		}
		g.disableScissor();

		float max = maxScroll();
		if (max > 0f) {
			int trackH = listBottom - listTop;
			int thumbH = Math.max(14, (int) (trackH * (trackH / (trackH + max))));
			int thumbY = listTop + (int) ((trackH - thumbH) * (scrollAnim / max));
			g.fill(lx + lw + 3, listTop, lx + lw + 5, listBottom, a(Theme.LINE, 1));
			Gfx.round(g, lx + lw + 3, thumbY, lx + lw + 5, thumbY + thumbH, 1, a(accent, 1));
		}
	}

	private static List<String> wrap(Font f, String text, int maxW, int maxLines) {
		List<String> out = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		for (String word : text.split(" ")) {
			String test = cur.length() == 0 ? word : cur + " " + word;
			if (Txt.width(f, test) <= maxW) {
				cur = new StringBuilder(test);
			} else {
				if (cur.length() > 0) {
					out.add(cur.toString());
				}
				cur = new StringBuilder(word);
				if (out.size() >= maxLines) {
					return out;
				}
			}
		}
		if (out.size() < maxLines && cur.length() > 0) {
			out.add(cur.toString());
		}
		return out;
	}

	private void drawPane(GuiGraphics g, Font f, Minecraft mc, boolean lClick, boolean rClick, boolean lHeld) {
		int accent = Theme.accent();
		int x = px + SB + 10 + 190 + 8;
		int w = px + PW - 10 - x;
		int top = listTop;
		int bottom = listBottom;
		Gfx.round(g, x, top, x + w, bottom, 4, a(0xFF101016, 1));
		Module m = picked;
		if (m == null) {
			return;
		}
		if (m != lastPicked) {
			lastPicked = m;
			paneStart = System.currentTimeMillis();
		}
		float pk = Theme.clamp01((System.currentTimeMillis() - paneStart) / 180f);
		pk = 1f - (1f - pk) * (1f - pk);
		float alphaSave = alpha;
		alpha = alphaSave * (0.3f + 0.7f * pk);
		g.enableScissor(x, top, x + w, bottom);
		int cx1 = x + 8;
		int cx2 = x + w - 8;
		int y = top + 8 + (int) ((1f - pk) * 8f);

		Txt.draw(g, f, m.getName(), cx1, y, a(accent, 1), false);
		y += 12;
		for (String line : wrap(f, m.getDescription(), cx2 - cx1, 3)) {
			Txt.draw(g, f, line, cx1, y, a(Theme.MUTED, 1), false);
			y += 10;
		}
		y += 3;
		g.fill(cx1, y, cx2, y + 1, a(Theme.LINE, 1));
		y += 6;

		if (!(m instanceof InterfaceModule)) {
			boolean hit = inside(mx, my, cx1, y, cx2 - cx1, 16);
			Txt.draw(g, f, "Enabled", cx1, y + 4, a(Theme.TEXT, 1), false);
			drawSwitch(g, cx2 - 22, y + 3, m.guiAnim, 1f);
			if (hit && lClick) {
				m.toggle(mc);
			}
			y += 20;
		}

		{
			boolean hit = inside(mx, my, cx1, y, cx2 - cx1, 16);
			boolean listen = listening == m;
			Txt.draw(g, f, "Keybind", cx1, y + 4, a(Theme.TEXT, 1), false);
			String v = listen ? "press a key..." : Module.keyName(m.getKey());
			int bw = Txt.width(f, v) + 12;
			int border = (hit || listen) ? accent : 0xFF2A2A36;
			Gfx.card(g, cx2 - bw, y, cx2, y + 16, 3, a(border, 1), a(0xFF171720, 1));
			Txt.draw(g, f, v, cx2 - bw + 6, y + 4, a(listen ? accent : Theme.TEXT, 1), false);
			if (hit && lClick) {
				startListening(m);
			}
			if (hit && rClick) {
				m.setKey(0);
				listening = null;
				Config.save();
			}
			y += 22;
		}

		List<Setting> toggles = new ArrayList<>();
		List<Setting> options = new ArrayList<>();
		List<Setting> sliders = new ArrayList<>();
		for (Setting s : m.getSettings()) {
			if (s.isToggle()) {
				toggles.add(s);
			} else if (s.isOptions()) {
				options.add(s);
			} else {
				sliders.add(s);
			}
		}

		int cw = (cx2 - cx1 - 6) / 2;
		for (int i = 0; i < toggles.size(); i++) {
			Setting s = toggles.get(i);
			int bx = cx1 + (i % 2) * (cw + 6);
			int by = y + (i / 2) * 19;
			if (by + 17 > bottom) {
				break;
			}
			boolean hit = inside(mx, my, bx, by, cw, 17);
			s.anim = Theme.approach(s.anim, s.bool() ? 1f : 0f, 16f);
			Gfx.round(g, bx, by, bx + cw, by + 17, 3, a(hit ? 0xFF22222E : 0xFF181821, 1));
			Txt.draw(g, f, s.name(), bx + 6, by + 5, a(Theme.mix(0xFF9A9AA8, Theme.TEXT, s.anim), 1), false);
			drawSwitch(g, bx + cw - 26, by + 3, s.anim, 1f);
			if (hit && (lClick || rClick)) {
				s.bump(1, false);
				Config.save();
			}
		}
		if (!toggles.isEmpty()) {
			y += ((toggles.size() + 1) / 2) * 19 + 4;
		}

		for (Setting s : options) {
			if (y + 18 > bottom) {
				break;
			}
			boolean hit = inside(mx, my, cx1, y, cx2 - cx1, 18);
			Gfx.round(g, cx1, y, cx2, y + 18, 3, a(hit ? 0xFF22222E : 0xFF181821, 1));
			Txt.draw(g, f, s.name(), cx1 + 6, y + 5, a(Theme.TEXT, 1), false);
			String v = s.format();
			int vw = Txt.width(f, v);
			Txt.draw(g, f, ">", cx2 - 12, y + 5, a(Theme.MUTED, 1), false);
			Txt.draw(g, f, v, cx2 - 18 - vw, y + 5, a(accent, 1), false);
			Txt.draw(g, f, "<", cx2 - 28 - vw, y + 5, a(Theme.MUTED, 1), false);
			if (hit && lClick) {
				s.bump(1, false);
				Config.save();
			}
			if (hit && rClick) {
				s.bump(-1, false);
				Config.save();
			}
			y += 20;
		}

		for (Setting s : sliders) {
			if (y + 26 > bottom) {
				break;
			}
			boolean hit = inside(mx, my, cx1, y, cx2 - cx1, 26);
			Txt.draw(g, f, s.name(), cx1 + 2, y + 2, a(Theme.TEXT, 1), false);
			String v = s.format();
			Txt.draw(g, f, v, cx2 - 2 - Txt.width(f, v), y + 2, a(accent, 1), false);
			int tx1 = cx1 + 2;
			int tx2 = cx2 - 2;
			int ty = y + 15;
			Gfx.round(g, tx1, ty, tx2, ty + 4, 2, a(0xFF2A2A34, 1));
			int fx = tx1 + (int) ((tx2 - tx1) * s.fraction());
			Gfx.round(g, tx1, ty, Math.max(tx1 + 4, fx), ty + 4, 2, a(accent, 1));
			Gfx.round(g, fx - 3, ty - 3, fx + 3, ty + 7, 2, a(0xFFFFFFFF, 1));
			if (hit && lClick) {
				dragging = s;
			}
			if (dragging == s && lHeld) {
				s.setFraction((mx - tx1) / (double) (tx2 - tx1));
			}
			y += 28;
		}
		g.disableScissor();
		alpha = alphaSave;
	}

	private void drawFooter(GuiGraphics g, Font f, Minecraft mc, boolean lClick) {
		int accent = Theme.accent();
		int y = py + ph - 24;

		int bx = px + SB + 10;
		int bw = 74;
		boolean h1 = inside(mx, my, bx, y, bw, 16);
		Gfx.round(g, bx, y, bx + bw, y + 16, 3, a(h1 ? Theme.dim(accent, 0.45) : 0xFF181821, 1));
		String t1 = "Disable all";
		Txt.draw(g, f, t1, bx + (bw - Txt.width(f, t1)) / 2, y + 4, a(Theme.TEXT, 1), false);
		if (h1 && lClick) {
			ModuleManager.disableAll(mc);
		}

		int pw2 = 58;
		int pbx = bx + bw + 6;
		boolean hp = inside(mx, my, pbx, y, pw2, 16);
		Gfx.round(g, pbx, y, pbx + pw2, y + 16, 3, a(hp ? Theme.dim(accent, 0.45) : 0xFF181821, 1));
		String tp = "Profiles";
		Txt.draw(g, f, tp, pbx + (pw2 - Txt.width(f, tp)) / 2, y + 4, a(Theme.TEXT, 1), false);
		if (hp && lClick) {
			mc.setScreen(new ProfilesScreen());
			return;
		}

		int ew = 60;
		int ebx = pbx + pw2 + 6;
		boolean he = inside(mx, my, ebx, y, ew, 16);
		Gfx.round(g, ebx, y, ebx + ew, y + 16, 3, a(he ? Theme.dim(accent, 0.45) : 0xFF181821, 1));
		String te = "Edit HUD";
		Txt.draw(g, f, te, ebx + (ew - Txt.width(f, te)) / 2, y + 4, a(Theme.TEXT, 1), false);
		if (he && lClick) {
			mc.setScreen(new HudEditorScreen());
			return;
		}

		int cw = 50;
		int cx = px + PW - 10 - cw;
		boolean h2 = inside(mx, my, cx, y, cw, 16);
		Gfx.round(g, cx, y, cx + cw, y + 16, 3, a(h2 ? Theme.dim(accent, 0.45) : 0xFF181821, 1));
		String t2 = "Close";
		Txt.draw(g, f, t2, cx + (cw - Txt.width(f, t2)) / 2, y + 4, a(Theme.TEXT, 1), false);
		if (h2 && lClick) {
			closeRequested = true;
		}

		String hint = hoverDesc.isEmpty() ? "Shift+R: favorite" : hoverDesc;
		int avail = (cx - 8) - (ebx + ew + 8);
		while (hint.length() > 3 && Txt.width(f, hint) > avail) {
			hint = hint.substring(0, hint.length() - 1);
		}
		int hx = ebx + ew + 8 + (avail - Txt.width(f, hint)) / 2;
		Txt.draw(g, f, hint, hx, y + 4, a(Theme.MUTED, 1), false);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
