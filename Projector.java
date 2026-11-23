package dev.cart3r.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class Projector {
	public static Vec3 eye = Vec3.ZERO;
	public static int w;
	public static int h;
	public static double cx;
	public static double cy;
	public static final double[] box = new double[4];

	private static final double[] tmp = new double[2];
	private static double rx;
	private static double ry;
	private static double rz;
	private static double ux;
	private static double uy;
	private static double uz;
	private static double fx;
	private static double fy;
	private static double fz;
	private static double focal;

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

	public static boolean bounds(double x0, double y0, double z0, double x1, double y1, double z1) {
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;
		for (int i = 0; i < 8; i++) {
			double x = (i & 1) == 0 ? x0 : x1;
			double y = (i & 2) == 0 ? y0 : y1;
			double z = (i & 4) == 0 ? z0 : z1;
			if (!project(x, y, z, tmp)) {
				return false;
			}
			minX = Math.min(minX, tmp[0]);
			minY = Math.min(minY, tmp[1]);
			maxX = Math.max(maxX, tmp[0]);
			maxY = Math.max(maxY, tmp[1]);
		}
		box[0] = minX;
		box[1] = minY;
		box[2] = maxX;
		box[3] = maxY;
		return true;
	}
}
