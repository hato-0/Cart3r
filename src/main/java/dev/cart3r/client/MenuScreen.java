package dev.cart3r.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class MenuScreen extends Screen {
	private static final int PANEL_W = 340;
	private static final int ROW_H = 22;

	private static Module.Category selected = Module.Category.MOVEMENT;

	private record Row(Module module, int x, int y, int w, int h) {
	}

	private final List<Row> rows = new ArrayList<>();
	private boolean rightWas;
	private Module pending;

	public MenuScreen() {
		super(Component.literal("CART3R CLIENT"));
		Theme.restart();
		rightWas = Minecraft.getInstance().mouseHandler.isRightPressed();
	}

	private int panelLeft() {
		return (this.width - PANEL_W) / 2;
	}

	private int panelTop() {
		return Math.max(10, this.height / 2 - 140);
	}

	private List<Module> modules() {
		List<Module> out = new ArrayList<>();
		for (Module m : ModuleManager.MODULES) {
			if (m.getCategory() == selected) {
				out.add(m);
			}
		}
		return out;
	}

	private void openSettings(Module m) {
		Minecraft.getInstance().setScreen(new ModuleSettingsScreen(this, m));
	}

	private static String keyHint(Module m) {
		if (!Theme.KEY_HINTS.bool() || m == ModuleManager.INTERFACE || m.getKey() <= 0) {
			return "";
		}
		return "[" + Module.keyName(m.getKey()) + "]";
	}

	@Override
	protected void init() {
		rows.clear();
		int left = panelLeft();
		int top = panelTop();
		Module.Category[] cats = Module.Category.values();
		int tabW = (PANEL_W - 20) / cats.length;
		for (int i = 0; i < cats.length; i++) {
			Module.Category c = cats[i];
			this.addRenderableWidget(new CartButton(left + 10 + i * tabW, top + 30, tabW - 2, 18,
				Component.literal(c.label), b -> {
					selected = c;
					Theme.restart();
					this.rebuildWidgets();
				}, CartButton.Style.TAB, () -> c == selected, () -> "", () -> ""));
		}

		int y = top + 58;
		for (Module m : modules()) {
			CartButton b = new CartButton(left + 10, y, PANEL_W - 20, 20,
				Component.literal(m.getName()),
				btn -> {
					if (m instanceof InterfaceModule) {
						openSettings(m);
					} else {
						m.toggle(Minecraft.getInstance());
					}
				},
				CartButton.Style.ROW,
				m::isEnabled,
				() -> m instanceof InterfaceModule ? "OPEN" : (m.isEnabled() ? "ON" : "OFF"),
				() -> keyHint(m));
			b.setTooltip(Tooltip.create(Component.literal(m.getDescription() + "\nRight-click: settings and keybind")));
			this.addRenderableWidget(b);
			rows.add(new Row(m, left + 10, y, PANEL_W - 20, 20));
			y += ROW_H;
		}

		int half = (PANEL_W - 24) / 2;
		this.addRenderableWidget(new CartButton(left + 10, y + 8, half, 20,
			Component.literal("Disable all"), btn -> {
				ModuleManager.disableAll(Minecraft.getInstance());
				this.rebuildWidgets();
			}, CartButton.Style.ACTION, () -> false, () -> "", () -> ""));
		this.addRenderableWidget(new CartButton(left + 14 + half, y + 8, half, 20,
			Component.literal("Close"), btn -> this.onClose(),
			CartButton.Style.ACTION, () -> false, () -> "", () -> ""));
	}

	@Override
	public void tick() {
		super.tick();
		if (pending != null) {
			Module m = pending;
			pending = null;
			openSettings(m);
		}
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.renderBackground(g, mouseX, mouseY, delta);
		float a = Theme.progress();
		int accent = Theme.accent();
		int off = Theme.offset();
		int left = panelLeft();
		int top = panelTop() + off;
		int bottom = top + 58 + modules().size() * ROW_H + 44;
		int bg = Theme.alpha(0xFF030304, Theme.OPACITY.num() * a);
		int border = Theme.alpha(accent, a);
		g.fill(left, top, left + PANEL_W, bottom, bg);
		g.fill(left, top, left + PANEL_W, top + 1, border);
		g.fill(left, bottom - 1, left + PANEL_W, bottom, border);
		g.fill(left, top, left + 1, bottom, border);
		g.fill(left + PANEL_W - 1, top, left + PANEL_W, bottom, border);
		g.fill(left + 10, top + 24, left + PANEL_W - 10, top + 25, Theme.alpha(Theme.dim(accent, 0.32), a));
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		float a = Theme.progress();
		int top = panelTop() + Theme.offset();
		g.drawCenteredString(this.font, this.title, this.width / 2, top + 9, Theme.alpha(Theme.accent(), a));
		int bottom = top + 58 + modules().size() * ROW_H + 44;
		g.drawCenteredString(this.font, Component.literal("Left click: toggle  |  Right click: settings"),
			this.width / 2, bottom - 12, Theme.alpha(0xFF777777, a));

		boolean right = Minecraft.getInstance().mouseHandler.isRightPressed();
		if (right && !rightWas && pending == null) {
			for (Row r : rows) {
				if (mouseX >= r.x() && mouseX < r.x() + r.w() && mouseY >= r.y() && mouseY < r.y() + r.h()) {
					pending = r.module();
					break;
				}
			}
		}
		rightWas = right;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
