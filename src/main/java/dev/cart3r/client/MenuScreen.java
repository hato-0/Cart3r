package dev.cart3r.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class MenuScreen extends Screen {
	private static final int ACCENT = 0xFFFF1F3D;
	private static final int PANEL = 0xF0030003;
	private static final int PANEL_W = 340;
	private static final int ROW_H = 22;

	private static Module.Category selected = Module.Category.MOVEMENT;

	public MenuScreen() {
		super(Component.literal("CART3R CLIENT"));
	}

	private int panelLeft() {
		return (this.width - PANEL_W) / 2;
	}

	private int panelTop() {
		return Math.max(10, this.height / 2 - 130);
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

	@Override
	protected void init() {
		int left = panelLeft();
		int top = panelTop();
		Module.Category[] cats = Module.Category.values();
		int tabW = (PANEL_W - 20) / cats.length;
		for (int i = 0; i < cats.length; i++) {
			Module.Category c = cats[i];
			this.addRenderableWidget(new CartButton(left + 10 + i * tabW, top + 30, tabW - 2, 18,
				Component.literal(c.label), b -> {
					selected = c;
					this.rebuildWidgets();
				}, () -> c == selected, false));
		}

		int y = top + 58;
		for (Module m : modules()) {
			CartButton b = new CartButton(left + 10, y, PANEL_W - 20, 20,
				Component.literal(m.getName()),
				btn -> m.toggle(Minecraft.getInstance()),
				m::isEnabled, true);
			b.setTooltip(Tooltip.create(Component.literal(m.getDescription())));
			this.addRenderableWidget(b);
			y += ROW_H;
		}

		int half = (PANEL_W - 24) / 2;
		this.addRenderableWidget(new CartButton(left + 10, y + 8, half, 20,
			Component.literal("Disable all"), btn -> {
				ModuleManager.disableAll(Minecraft.getInstance());
				this.rebuildWidgets();
			}, () -> false, false));
		this.addRenderableWidget(new CartButton(left + 14 + half, y + 8, half, 20,
			Component.literal("Close"), btn -> this.onClose(), () -> false, false));
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.renderBackground(g, mouseX, mouseY, delta);
		int left = panelLeft();
		int top = panelTop();
		int bottom = top + 58 + modules().size() * ROW_H + 44;
		g.fill(left, top, left + PANEL_W, bottom, PANEL);
		g.fill(left, top, left + PANEL_W, top + 1, ACCENT);
		g.fill(left, bottom - 1, left + PANEL_W, bottom, ACCENT);
		g.fill(left, top, left + 1, bottom, ACCENT);
		g.fill(left + PANEL_W - 1, top, left + PANEL_W, bottom, ACCENT);
		g.fill(left + 10, top + 24, left + PANEL_W - 10, top + 25, 0xFF3A0A14);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		g.drawCenteredString(this.font, this.title, this.width / 2, panelTop() + 9, ACCENT);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
