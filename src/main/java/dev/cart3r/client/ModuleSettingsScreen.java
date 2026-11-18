package dev.cart3r.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ModuleSettingsScreen extends Screen {
	private static final int PANEL_W = 300;
	private static final int ROW_H = 22;
	private static final int[] KEYS = buildKeys();

	private record Row(Setting setting, int x, int y, int w, int h) {
	}

	private final Screen parent;
	private final Module module;
	private final List<Row> rows = new ArrayList<>();
	private final boolean[] held = new boolean[400];
	private boolean rightWas;
	private boolean listening;

	public ModuleSettingsScreen(Screen parent, Module module) {
		super(Component.literal(module.getName()));
		this.parent = parent;
		this.module = module;
		Theme.restart();
		rightWas = Minecraft.getInstance().mouseHandler.isRightPressed();
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

	private int panelHeight() {
		return 36 + (1 + module.getSettings().size()) * ROW_H + 44;
	}

	private int panelLeft() {
		return (this.width - PANEL_W) / 2;
	}

	private int panelTop() {
		return Math.max(10, (this.height - panelHeight()) / 2);
	}

	private boolean shiftDown() {
		Minecraft mc = Minecraft.getInstance();
		return InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
			|| InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	private void bump(Setting s, int dir) {
		s.bump(dir, shiftDown());
		Config.save();
	}

	private void startListening() {
		Minecraft mc = Minecraft.getInstance();
		for (int k : KEYS) {
			held[k] = InputConstants.isKeyDown(mc.getWindow(), k);
		}
		listening = true;
	}

	private void pollKey() {
		Minecraft mc = Minecraft.getInstance();
		for (int k : KEYS) {
			boolean d = InputConstants.isKeyDown(mc.getWindow(), k);
			if (d && !held[k]) {
				held[k] = true;
				if (k == 256) {
					listening = false;
				} else if (k == 259 || k == 261) {
					module.setKey(0);
					listening = false;
					Config.save();
				} else {
					module.setKey(k);
					listening = false;
					Config.save();
				}
				return;
			}
			held[k] = d;
		}
	}

	@Override
	protected void init() {
		rows.clear();
		int left = panelLeft();
		int top = panelTop();
		int y = top + 36;

		this.addRenderableWidget(new CartButton(left + 10, y, PANEL_W - 20, 20,
			Component.literal("Keybind"), b -> startListening(),
			CartButton.Style.VALUE, () -> listening,
			() -> listening ? "press a key..." : Module.keyName(module.getKey()),
			() -> ""));
		y += ROW_H;

		for (Setting s : module.getSettings()) {
			this.addRenderableWidget(new CartButton(left + 10, y, PANEL_W - 20, 20,
				Component.literal(s.name()), b -> bump(s, 1),
				CartButton.Style.VALUE, () -> true, s::format, () -> ""));
			rows.add(new Row(s, left + 10, y, PANEL_W - 20, 20));
			y += ROW_H;
		}

		int half = (PANEL_W - 24) / 2;
		this.addRenderableWidget(new CartButton(left + 10, y + 8, half, 20,
			Component.literal("Clear key"), b -> {
				module.setKey(0);
				listening = false;
				Config.save();
			}, CartButton.Style.ACTION, () -> false, () -> "", () -> ""));
		this.addRenderableWidget(new CartButton(left + 14 + half, y + 8, half, 20,
			Component.literal("Back"), b -> this.onClose(),
			CartButton.Style.ACTION, () -> false, () -> "", () -> ""));
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.renderBackground(g, mouseX, mouseY, delta);
		float a = Theme.progress();
		int accent = Theme.accent();
		int left = panelLeft();
		int top = panelTop() + Theme.offset();
		int bottom = top + panelHeight();
		int bg = Theme.alpha(0xFF030304, Theme.OPACITY.num() * a);
		int border = Theme.alpha(accent, a);
		g.fill(left, top, left + PANEL_W, bottom, bg);
		g.fill(left, top, left + PANEL_W, top + 1, border);
		g.fill(left, bottom - 1, left + PANEL_W, bottom, border);
		g.fill(left, top, left + 1, bottom, border);
		g.fill(left + PANEL_W - 1, top, left + PANEL_W, bottom, border);
		g.fill(left + 10, top + 28, left + PANEL_W - 10, top + 29, Theme.alpha(Theme.dim(accent, 0.32), a));
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		float a = Theme.progress();
		int top = panelTop() + Theme.offset();
		int bottom = top + panelHeight();
		g.drawCenteredString(this.font, Component.literal(module.getName() + " settings"),
			this.width / 2, top + 10, Theme.alpha(Theme.accent(), a));
		g.drawCenteredString(this.font, Component.literal("Left: next  |  Right: back  |  Shift: x5"),
			this.width / 2, bottom - 12, Theme.alpha(0xFF777777, a));

		boolean right = Minecraft.getInstance().mouseHandler.isRightPressed();
		if (right && !rightWas && !listening) {
			for (Row r : rows) {
				if (mouseX >= r.x() && mouseX < r.x() + r.w() && mouseY >= r.y() && mouseY < r.y() + r.h()) {
					bump(r.setting(), -1);
					break;
				}
			}
		}
		rightWas = right;

		if (listening) {
			pollKey();
		}
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
