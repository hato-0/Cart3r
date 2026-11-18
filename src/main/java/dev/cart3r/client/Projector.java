package dev.cart3r.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Projector {
	public static Vec3 eye = Vec3.ZERO;
	public static int w;
	public static int h;
	public static double cx;
	public static double cy;

	private static double rx, ry, rz, ux, uy, uz, fx, fy, fz, focal;

	private Projector() {
	}

	public static boolean begin(Minecraft mc, GuiGraphics g, float pt) {
		Entity cam = mc.getCameraEntity();
		if (cam == null) {
			return false;
		}
		if (!Freecam.active() && !mc.options.getCameraType().isFirstPerson()) {
			return false;
		}
		double yaw = Math.toRadians(cam.getViewYRot(pt));
		double pitch = Math.toRadians(cam.getViewXRot(pt));
		double sy = Math.sin(yaw);
		double cyw = Math.cos(yaw);
		double cp = Math.cos(pitch);
		double sp = Math.sin(pitch);

		fx = -sy * cp;
		fy = -sp;
		fz = cyw * cp;
		rx = -cyw;
		ry = 0.0;
		rz = -sy;
		ux = ry * fz - rz * fy;
		uy = rz * fx - rx * fz;
		uz = rx * fy - ry * fx;

		eye = cam.getEyePosition(pt);
		w = g.guiWidth();
		h = g.guiHeight();
		cx = w / 2.0;
		cy = h / 2.0;
		focal = (h / 2.0) / Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
		return true;
	}

	public static boolean project(double x, double y, double z, double[] out) {
		double dx = x - eye.x;
		double dy = y - eye.y;
		double dz = z - eye.z;
		double px = dx * rx + dy * ry + dz * rz;
		double py = dx * ux + dy * uy + dz * uz;
		double pz = dx * fx + dy * fy + dz * fz;
		if (pz > 0.1) {
			out[0] = cx + px / pz * focal;
			out[1] = cy - py / pz * focal;
			return true;
		}
		double len = Math.hypot(px, py);
		double far = Math.max(w, h);
		if (len < 1.0E-6) {
			out[0] = cx;
			out[1] = cy + far;
			return false;
		}
		out[0] = cx + px / len * far;
		out[1] = cy - py / len * far;
		return false;
	}

	public static void clamp(double[] out, int margin) {
		double dx = out[0] - cx;
		double dy = out[1] - cy;
		double s = 1.0;
		double mx = cx - margin;
		double my = cy - margin;
		if (Math.abs(dx) > mx) {
			s = Math.min(s, mx / Math.abs(dx));
		}
		if (Math.abs(dy) > my) {
			s = Math.min(s, my / Math.abs(dy));
		}
		out[0] = cx + dx * s;
		out[1] = cy + dy * s;
	}

	public static double[] bounds(AABB b) {
		double[] o = new double[2];
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;
		for (int i = 0; i < 8; i++) {
			double x = (i & 1) == 0 ? b.minX : b.maxX;
			double y = (i & 2) == 0 ? b.minY : b.maxY;
			double z = (i & 4) == 0 ? b.minZ : b.maxZ;
			if (!project(x, y, z, o)) {
				return null;
			}
			minX = Math.min(minX, o[0]);
			minY = Math.min(minY, o[1]);
			maxX = Math.max(maxX, o[0]);
			maxY = Math.max(maxY, o[1]);
		}
		return new double[] {minX, minY, maxX, maxY};
	}
}
