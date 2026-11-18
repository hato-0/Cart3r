package dev.cart3r.client;

public final class Theme {
	public static final Setting ACCENT = Setting.options("Accent", 0, "Crimson", "Blue", "Green", "Purple", "Orange");
	public static final Setting ANIMATIONS = Setting.toggle("Animations", true);
	public static final Setting ANIM_MS = Setting.number("Anim ms", 100, 600, 250, 50);
	public static final Setting OPACITY = Setting.number("Panel opacity", 0.5, 1.0, 0.94, 0.02);
	public static final Setting KEY_HINTS = Setting.toggle("Key hints", true);

	private static final int[] COLORS = {0xFFFF1F3D, 0xFF3D8BFF, 0xFF2DD36F, 0xFFA855F7, 0xFFFF9F1C};
	private static long start = 0;

	private Theme() {
	}

	public static int accent() {
		int i = (int) Math.round(ACCENT.num());
		return COLORS[Math.max(0, Math.min(COLORS.length - 1, i))];
	}

	public static int dim(int color, double f) {
		int r = (int) (((color >> 16) & 0xFF) * f);
		int g = (int) (((color >> 8) & 0xFF) * f);
		int b = (int) ((color & 0xFF) * f);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	public static int alpha(int color, double a) {
		int al = (int) (((color >>> 24) & 0xFF) * a);
		al = Math.max(8, Math.min(255, al));
		return (al << 24) | (color & 0xFFFFFF);
	}

	public static void restart() {
		start = System.currentTimeMillis();
	}

	public static float progress() {
		if (!ANIMATIONS.bool()) {
			return 1f;
		}
		double t = (System.currentTimeMillis() - start) / Math.max(1.0, ANIM_MS.num());
		if (t >= 1.0) {
			return 1f;
		}
		if (t < 0) {
			t = 0;
		}
		double inv = 1.0 - t;
		return (float) (1.0 - inv * inv * inv);
	}

	public static int offset() {
		return Math.round((1f - progress()) * 12f);
	}
}
