package dev.cart3r.client;

public final class Theme {
	public static final Setting ACCENT = Setting.options("Accent", 0, "Crimson", "Blue", "Green", "Purple", "Orange", "Cyan", "Pink", "Rainbow");
	public static final Setting ANIMATIONS = Setting.toggle("Animations", true);
	public static final Setting ANIM_MS = Setting.number("Anim ms", 100, 600, 250, 50);
	public static final Setting OPACITY = Setting.number("Panel opacity", 0.5, 1.0, 0.94, 0.02);
	public static final Setting KEY_HINTS = Setting.toggle("Key hints", true);
	public static final Setting GLOW = Setting.toggle("Glow", true);
	public static final Setting NOTIFY = Setting.toggle("Notifications", true);

	public static final int TEXT = 0xFFEDEDF2;
	public static final int MUTED = 0xFF8A8A98;
	public static final int CARD = 0xFF13131A;
	public static final int CARD_HOVER = 0xFF1C1C27;
	public static final int PANEL = 0xFF0A0A0F;
	public static final int LINE = 0xFF22222C;

	private static final int[] COLORS = {0xFFFF1F3D, 0xFF3D8BFF, 0xFF2DD36F, 0xFFA855F7, 0xFFFF9F1C, 0xFF22D3EE, 0xFFFF4FA3, 0xFFFFFFFF};
	private static long start = 0;
	private static long last = System.nanoTime();
	private static float dt = 0.016f;

	private Theme() {
	}

	public static int colorOf(int index) {
		int i = Math.max(0, Math.min(COLORS.length - 1, index));
		return i == COLORS.length - 1 ? rainbow() : COLORS[i];
	}

	public static int accent() {
		return colorOf((int) Math.round(ACCENT.num()));
	}

	private static int rainbow() {
		float h = (System.currentTimeMillis() % 6000L) / 6000f * 6f;
		int i = (int) h;
		float f = h - i;
		float v = 1f;
		float p = 0.25f;
		float q = 1f - f * 0.75f;
		float t = 0.25f + f * 0.75f;
		float r;
		float g;
		float b;
		switch (i % 6) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
	}

	public static int dim(int color, double f) {
		int r = (int) (((color >> 16) & 0xFF) * f);
		int g = (int) (((color >> 8) & 0xFF) * f);
		int b = (int) ((color & 0xFF) * f);
		return 0xFF000000 | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
	}

	public static int alpha(int color, double a) {
		int al = (int) (((color >>> 24) & 0xFF) * a);
		al = Math.max(8, Math.min(255, al));
		return (al << 24) | (color & 0xFFFFFF);
	}

	public static int mix(int a, int b, float t) {
		t = clamp01(t);
		int aa = (int) (((a >>> 24) & 0xFF) * (1 - t) + ((b >>> 24) & 0xFF) * t);
		int ar = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
		int ag = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
		int ab = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return (aa << 24) | (ar << 16) | (ag << 8) | ab;
	}

	public static float clamp01(float v) {
		return Math.max(0f, Math.min(1f, v));
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

	public static void beginFrame() {
		long n = System.nanoTime();
		dt = Math.min(0.1f, (n - last) / 1.0E9f);
		last = n;
	}

	public static float dt() {
		return dt;
	}

	public static float approach(float cur, float target, float speed) {
		if (!ANIMATIONS.bool()) {
			return target;
		}
		float k = 1f - (float) Math.exp(-speed * dt);
		float v = cur + (target - cur) * k;
		return Math.abs(target - v) < 0.002f ? target : v;
	}
}
