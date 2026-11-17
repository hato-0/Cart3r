package dev.cart3r.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Hud {
	private static final int ACCENT = 0xFFFF1F3D;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int MONSTER = 0xFFFFD33D;
	private static final double MAX_DIST = 128.0;

	private Hud() {
	}

	public static void render(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.options.hideGui) {
			return;
		}
		if (ModuleManager.isOn("Tracers")) {
			tracers(g, mc);
		}
		if (ModuleManager.isOn("HUD")) {
			info(g, mc);
		}
	}

	private static void info(GuiGraphics g, Minecraft mc) {
		int y = 4;
		g.drawString(mc.font, "CART3R", 4, y, ACCENT, true);
		y += 11;
		g.drawString(mc.font, mc.getFps() + " FPS", 4, y, TEXT, true);
		y += 11;
		String xyz = String.format("%.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
		g.drawString(mc.font, xyz, 4, y, TEXT, true);

		int right = g.guiWidth() - 4;
		int ry = 4;
		for (Module m : ModuleManager.MODULES) {
			if (m.isEnabled() && !m.getName().equals("HUD")) {
				int w = mc.font.width(m.getName());
				g.drawString(mc.font, m.getName(), right - w, ry, ACCENT, true);
				ry += 10;
			}
		}
	}

	private static void tracers(GuiGraphics g, Minecraft mc) {
		int w = g.guiWidth();
		int h = g.guiHeight();
		double cx = w / 2.0;
		double cy = h / 2.0;

		double yaw = Math.toRadians(mc.player.getYRot());
		double pitch = Math.toRadians(mc.player.getXRot());
		double sy = Math.sin(yaw);
		double cyaw = Math.cos(yaw);
		double cp = Math.cos(pitch);
		double sp = Math.sin(pitch);

		double fx = -sy * cp;
		double fy = -sp;
		double fz = cyaw * cp;
		double rx = -cyaw;
		double ry = 0.0;
		double rz = -sy;
		double ux = ry * fz - rz * fy;
		double uy = rz * fx - rx * fz;
		double uz = rx * fy - ry * fx;

		double focal = (h / 2.0) / Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
		Vec3 eye = mc.player.getEyePosition();

		for (Entity e : mc.level.entitiesForRendering()) {
			if (e == mc.player) {
				continue;
			}
			int color;
			if (e instanceof Player p) {
				if (p.isSpectator()) {
					continue;
				}
				color = ACCENT;
			} else if (e instanceof Enemy) {
				color = MONSTER;
			} else {
				continue;
			}

			Vec3 d = e.getBoundingBox().getCenter().subtract(eye);
			double dist = d.length();
			if (dist > MAX_DIST || dist < 1.0) {
				continue;
			}

			double x = d.x * rx + d.y * ry + d.z * rz;
			double y = d.x * ux + d.y * uy + d.z * uz;
			double z = d.x * fx + d.y * fy + d.z * fz;

			double dx;
			double dy;
			if (z > 0.1) {
				dx = x / z * focal;
				dy = -y / z * focal;
			} else {
				double len = Math.hypot(x, y);
				if (len < 1.0E-6) {
					continue;
				}
				dx = x / len * Math.max(w, h);
				dy = -y / len * Math.max(w, h);
			}

			double scale = 1.0;
			double maxX = cx - 8;
			double maxY = cy - 8;
			if (Math.abs(dx) > maxX) {
				scale = Math.min(scale, maxX / Math.abs(dx));
			}
			if (Math.abs(dy) > maxY) {
				scale = Math.min(scale, maxY / Math.abs(dy));
			}
			dx *= scale;
			dy *= scale;

			double len = Math.hypot(dx, dy);
			if (len < 12.0) {
				continue;
			}
			int steps = (int) Math.min(300, len / 2.0);
			for (int i = 5; i <= steps; i++) {
				int px = (int) (cx + dx * i / steps);
				int py = (int) (cy + dy * i / steps);
				g.fill(px, py, px + 1, py + 1, color);
			}
			int ex = (int) (cx + dx);
			int ey = (int) (cy + dy);
			g.fill(ex - 1, ey - 1, ex + 2, ey + 2, color);
			g.drawString(mc.font, (int) dist + "m", ex + 4, ey - 4, color, true);
		}
	}
}
