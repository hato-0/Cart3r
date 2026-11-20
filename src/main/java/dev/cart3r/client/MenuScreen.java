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
	private static final int PW = 440;
	private static final int ROW = 22;
	private static final int[] KEYS = buildKeys();

	private static Module.Category selected = Module.Category.MOVEMENT;
	private static Module picked = null;

	private float scroll;
	private float scrollAnim;
	private float tabX = -1f;
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
		super(Component.literal("CART3R CLIENT"));
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
		search = new EditBox(this.font, px0() + PW - 12 - 30 - 112, 0, 104, 12, Component.literal("Search"));
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
			if (!q.isEmpty() ? m.getName().toLowerCase().contains(q) : m.getCategory() == selected) {
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
		ph = Math.min(318, this.height - 16);
		px = (this.width - PW) / 2;
		py = (this.height - ph) / 2;
		listTop = py + 58;
		listBottom = py + ph - 28;

		float p = Theme.progress();
		alpha = p;
		float s = 0.94f + 0.06f * p;
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
		Gfx.pushScale(g, cx, cy, s);
		drawFrame(g, f);
		drawSearch(g, f, mouseX, mouseY, delta);
		drawTabs(g, f, lClick);
		drawList(g, f, mc, lClick, rClick);
		drawPane(g, f, mc, lClick, rClick, l);
		drawFooter(g, f, mc, lClick);
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
		int sx = px + PW - 12 - 30 - 112;
		boolean focus = search.isFocused();
		Gfx.round(g, sx, py + 5, sx + 112, py + 20, 3, a(focus ? Theme.dim(accent, 0.5) : 0xFF181821, 1));
		search.setX(sx + 6);
		search.setY(py + 9);
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
		if (Theme.GLOW.bool()) {
			Gfx.glow(g, px, py, px + PW, py + ph, 5, accent, alpha);
		}
		Gfx.card(g, px, py, px + PW, py + ph, 5, a(Theme.dim(accent, 0.55), 1), a(Theme.PANEL, Theme.OPACITY.num()));
		g.drawString(f, "CART3R", px + 12, py + 9, a(accent, 1), false);
		g.drawString(f, "CLIENT", px + 12 + f.width("CART3R") + 5, py + 9, a(Theme.MUTED, 1), false);
		String ver = "v9.0";
		g.drawString(f, ver, px + PW - 12 - f.width(ver), py + 9, a(Theme.MUTED, 1), false);
		g.fill(px + 10, py + 27, px + PW - 10, py + 28, a(Theme.LINE, 1));
	}

	private void drawTabs(GuiGraphics g, Font f, boolean lClick) {
		int accent = Theme.accent();
		Module.Category[] cats = Module.Category.values();
		int tw = (PW - 20) / cats.length;
		int ty = py + 32;
		int sel = 0;
		for (int i = 0; i < cats.length; i++) {
			int x1 = px + 10 + i * tw;
			boolean hover = inside(mx, my, x1, ty, tw, 18);
			boolean on = cats[i] == selected;
			if (on) {
				sel = i;
			}
			int col = on ? accent : (hover ? Theme.TEXT : Theme.MUTED);
			String label = cats[i].label;
			g.drawString(f, label, x1 + (tw - f.width(label)) / 2, ty + 5, a(col, 1), false);
			if (lClick && hover && !on) {
				select(cats[i]);
			}
		}
		float target = px + 10 + sel * tw;
		tabX = tabX < 0 ? target : Theme.approach(tabX, target, 16f);
		Gfx.round(g, Math.round(tabX) + 8, ty + 17, Math.round(tabX) + tw - 8, ty + 19, 1, a(accent, 1));
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
		int lx = px + 10;
		int lw = 196;
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
			int x = lx - Math.round((1f - k) * 14f);
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
			if (m.guiAnim > 0.02f) {
				Gfx.round(g, x + 3, y + 4, x + 5, y + 16, 1, a(accent, k * m.guiAnim));
			}
			int tc = Theme.mix(0xFFB4B4C0, Theme.TEXT, Math.max(m.guiAnim, m.guiHover * 0.6f));
			g.drawString(f, m.getName(), x + 11, y + 6, a(tc, k), false);

			boolean isIface = m instanceof InterfaceModule;
			int gx = x + lw - 18;
			int swx1 = gx - 4 - 22;
			if (!isIface) {
				drawSwitch(g, swx1, y + 5, m.guiAnim, k);
			}
			String hint = keyHint(m);
			if (!hint.isEmpty()) {
				int hx = (isIface ? gx - 4 : swx1 - 6) - f.width(hint);
				g.drawString(f, hint, hx, y + 6, a(Theme.MUTED, k), false);
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
			g.fill(px + 207, listTop, px + 209, listBottom, a(Theme.LINE, 1));
			Gfx.round(g, px + 207, thumbY, px + 209, thumbY + thumbH, 1, a(accent, 1));
		}
	}

	private static List<String> wrap(Font f, String text, int maxW, int maxLines) {
		List<String> out = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		for (String word : text.split(" ")) {
			String test = cur.length() == 0 ? word : cur + " " + word;
			if (f.width(test) <= maxW) {
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
		int x = px + 214;
		int w = PW - 224;
		int top = listTop;
		int bottom = listBottom;
		Gfx.round(g, x, top, x + w, bottom, 4, a(0xFF101016, 1));
		Module m = picked;
		if (m == null) {
			return;
		}
		g.enableScissor(x, top, x + w, bottom);
		int cx1 = x + 8;
		int cx2 = x + w - 8;
		int y = top + 8;

		g.drawString(f, m.getName(), cx1, y, a(accent, 1), false);
		y += 12;
		for (String line : wrap(f, m.getDescription(), cx2 - cx1, 3)) {
			g.drawString(f, line, cx1, y, a(Theme.MUTED, 1), false);
			y += 10;
		}
		y += 3;
		g.fill(cx1, y, cx2, y + 1, a(Theme.LINE, 1));
		y += 6;

		if (!(m instanceof InterfaceModule)) {
			boolean hit = inside(mx, my, cx1, y, cx2 - cx1, 16);
			g.drawString(f, "Enabled", cx1, y + 4, a(Theme.TEXT, 1), false);
			drawSwitch(g, cx2 - 22, y + 3, m.guiAnim, 1f);
			if (hit && lClick) {
				m.toggle(mc);
			}
			y += 20;
		}

		{
			boolean hit = inside(mx, my, cx1, y, cx2 - cx1, 16);
			boolean listen = listening == m;
			g.drawString(f, "Keybind", cx1, y + 4, a(Theme.TEXT, 1), false);
			String v = listen ? "press a key..." : Module.keyName(m.getKey());
			int bw = f.width(v) + 12;
			int border = (hit || listen) ? accent : 0xFF2A2A36;
			Gfx.card(g, cx2 - bw, y, cx2, y + 16, 3, a(border, 1), a(0xFF171720, 1));
			g.drawString(f, v, cx2 - bw + 6, y + 4, a(listen ? accent : Theme.TEXT, 1), false);
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
			g.drawString(f, s.name(), bx + 6, by + 5, a(Theme.mix(0xFF9A9AA8, Theme.TEXT, s.anim), 1), false);
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
			g.drawString(f, s.name(), cx1 + 6, y + 5, a(Theme.TEXT, 1), false);
			String v = s.format();
			int vw = f.width(v);
			g.drawString(f, ">", cx2 - 12, y + 5, a(Theme.MUTED, 1), false);
			g.drawString(f, v, cx2 - 18 - vw, y + 5, a(accent, 1), false);
			g.drawString(f, "<", cx2 - 28 - vw, y + 5, a(Theme.MUTED, 1), false);
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
			g.drawString(f, s.name(), cx1 + 2, y + 2, a(Theme.TEXT, 1), false);
			String v = s.format();
			g.drawString(f, v, cx2 - 2 - f.width(v), y + 2, a(accent, 1), false);
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
	}

	private void drawFooter(GuiGraphics g, Font f, Minecraft mc, boolean lClick) {
		int accent = Theme.accent();
		int y = py + ph - 24;

		int bx = px + 10;
		int bw = 74;
		boolean h1 = inside(mx, my, bx, y, bw, 16);
		Gfx.round(g, bx, y, bx + bw, y + 16, 3, a(h1 ? Theme.dim(accent, 0.45) : 0xFF181821, 1));
		String t1 = "Disable all";
		g.drawString(f, t1, bx + (bw - f.width(t1)) / 2, y + 4, a(Theme.TEXT, 1), false);
		if (h1 && lClick) {
			ModuleManager.disableAll(mc);
		}

		int pw2 = 58;
		int pbx = bx + bw + 6;
		boolean hp = inside(mx, my, pbx, y, pw2, 16);
		Gfx.round(g, pbx, y, pbx + pw2, y + 16, 3, a(hp ? Theme.dim(accent, 0.45) : 0xFF181821, 1));
		String tp = "Profiles";
		g.drawString(f, tp, pbx + (pw2 - f.width(tp)) / 2, y + 4, a(Theme.TEXT, 1), false);
		if (hp && lClick) {
			mc.setScreen(new ProfilesScreen());
			return;
		}

		int cw = 50;
		int cx = px + PW - 10 - cw;
		boolean h2 = inside(mx, my, cx, y, cw, 16);
		Gfx.round(g, cx, y, cx + cw, y + 16, 3, a(h2 ? Theme.dim(accent, 0.45) : 0xFF181821, 1));
		String t2 = "Close";
		g.drawString(f, t2, cx + (cw - f.width(t2)) / 2, y + 4, a(Theme.TEXT, 1), false);
		if (h2 && lClick) {
			closeRequested = true;
		}

		String hint = hoverDesc.isEmpty() ? "Left: toggle   Right: settings" : hoverDesc;
		int avail = (cx - 8) - (pbx + pw2 + 8);
		while (hint.length() > 3 && f.width(hint) > avail) {
			hint = hint.substring(0, hint.length() - 1);
		}
		int hx = pbx + pw2 + 8 + (avail - f.width(hint)) / 2;
		g.drawString(f, hint, hx, y + 4, a(Theme.MUTED, 1), false);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
