package dev.cart3r.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class CartButton extends Button {
	public enum Style {
		TAB, ROW, VALUE, ACTION
	}

	private final Style style;
	private final BooleanSupplier on;
	private final Supplier<String> right;
	private final Supplier<String> hint;

	public CartButton(int x, int y, int w, int h, Component message, OnPress onPress, Style style,
					  BooleanSupplier on, Supplier<String> right, Supplier<String> hint) {
		super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
		this.style = style;
		this.on = on;
		this.right = right;
		this.hint = hint;
	}

	@Override
	public void renderContents(GuiGraphics g, int mouseX, int mouseY, float delta) {
		float a = Theme.progress();
		int accent = Theme.accent();
		boolean hover = this.isHoveredOrFocused();
		boolean active = on.getAsBoolean();
		int x = getX();
		int y = getY() + Theme.offset();
		int w = getWidth();
		int h = getHeight();

		int bg = 0xFF0C0C0F;
		if (style == Style.TAB && active) {
			bg = Theme.dim(accent, 0.30);
		} else if (hover) {
			bg = Theme.dim(accent, 0.20);
		}
		g.fill(x, y, x + w, y + h, Theme.alpha(bg, a));

		int border = Theme.alpha(hover ? accent : Theme.dim(accent, 0.32), a);
		g.fill(x, y, x + w, y + 1, border);
		g.fill(x, y + h - 1, x + w, y + h, border);
		g.fill(x, y, x + 1, y + h, border);
		g.fill(x + w - 1, y, x + w, y + h, border);

		Font font = Minecraft.getInstance().font;
		int textY = y + (h - 8) / 2;
		int white = Theme.alpha(0xFFFFFFFF, a);

		if (style == Style.TAB) {
			if (active) {
				g.fill(x, y + h - 2, x + w, y + h, Theme.alpha(accent, a));
			}
			g.drawCenteredString(font, getMessage(), x + w / 2, textY, active ? Theme.alpha(accent, a) : white);
			return;
		}
		if (style == Style.ACTION) {
			g.drawCenteredString(font, getMessage(), x + w / 2, textY, white);
			return;
		}

		if (style == Style.ROW && active) {
			g.fill(x, y, x + 3, y + h, Theme.alpha(accent, a));
		}
		g.drawString(font, getMessage(), x + 10, textY, white, false);

		String r = right.get();
		int rc;
		if (r.equals("ON")) {
			rc = 0xFF55FF55;
		} else if (r.equals("OFF")) {
			rc = 0xFF777777;
		} else {
			rc = accent;
		}
		int rw = font.width(r);
		g.drawString(font, r, x + w - 10 - rw, textY, Theme.alpha(rc, a), false);

		String hs = hint.get();
		if (!hs.isEmpty()) {
			g.drawString(font, hs, x + w - 10 - rw - 8 - font.width(hs), textY, Theme.alpha(0xFF888888, a), false);
		}
	}
}
