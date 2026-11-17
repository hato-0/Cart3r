package dev.cart3r.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public class CartButton extends Button {
	private static final int ACCENT = 0xFFFF1F3D;
	private static final int BG = 0xFF100206;
	private static final int BG_HOVER = 0xFF220509;
	private static final int BG_ACTIVE = 0xFF3A0812;
	private static final int BORDER = 0xFF3A0A14;

	private final BooleanSupplier state;
	private final boolean row;

	public CartButton(int x, int y, int w, int h, Component message, OnPress onPress, BooleanSupplier state, boolean row) {
		super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
		this.state = state;
		this.row = row;
	}

	@Override
	public void renderContents(GuiGraphics g, int mouseX, int mouseY, float delta) {
		boolean hover = this.isHoveredOrFocused();
		boolean on = state.getAsBoolean();
		int x = getX();
		int y = getY();
		int w = getWidth();
		int h = getHeight();

		int bg = BG;
		if (!row && on) {
			bg = BG_ACTIVE;
		} else if (hover) {
			bg = BG_HOVER;
		}
		g.fill(x, y, x + w, y + h, bg);

		int border = hover ? ACCENT : BORDER;
		g.fill(x, y, x + w, y + 1, border);
		g.fill(x, y + h - 1, x + w, y + h, border);
		g.fill(x, y, x + 1, y + h, border);
		g.fill(x + w - 1, y, x + w, y + h, border);

		Font font = Minecraft.getInstance().font;
		int textY = y + (h - 8) / 2;

		if (row) {
			if (on) {
				g.fill(x, y, x + 3, y + h, ACCENT);
			}
			g.drawString(font, getMessage(), x + 10, textY, 0xFFFFFFFF, false);
			String s = on ? "ON" : "OFF";
			int c = on ? 0xFF55FF55 : 0xFF777777;
			g.drawString(font, s, x + w - 10 - font.width(s), textY, c, false);
		} else {
			if (on) {
				g.fill(x, y + h - 2, x + w, y + h, ACCENT);
			}
			g.drawCenteredString(font, getMessage(), x + w / 2, textY, on ? ACCENT : 0xFFFFFFFF);
		}
	}
}
