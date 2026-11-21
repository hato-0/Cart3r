package dev.cart3r.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HudLayout {
	public static final int INFO = 0;
	public static final int MODS = 1;
	public static final int KEYS = 2;
	public static final int COMPASS = 3;
	public static final int EFFECTS = 4;
	public static final int RADAR = 5;
	public static final int ARMOR = 6;
	public static final int SPOTIFY = 7;
	public static final int NOTIFY = 8;
	public static final int COUNTERS = 9;
	public static final int FRAME = 10;
	public static final String[] NAMES = {
		"Info", "Module list", "Keystrokes", "Compass", "Effects", "Radar", "Armor", "Spotify", "Notifications", "Counters", "Frame graph"
	};

	private static final int N = NAMES.length;
	private static final int[][] OFF = new int[N][2];
	private static final int[][] RECT = new int[N][4];
	private static final long[] SEEN = new long[N];
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hato-hud.txt");

	private HudLayout() {
	}

	public static void begin(GuiGraphics g, int id) {
		var p = g.pose();
		p.pushMatrix();
		p.translate((float) OFF[id][0], (float) OFF[id][1]);
	}

	public static void end(GuiGraphics g) {
		g.pose().popMatrix();
	}

	public static void report(int id, int x1, int y1, int x2, int y2) {
		RECT[id][0] = x1 + OFF[id][0];
		RECT[id][1] = y1 + OFF[id][1];
		RECT[id][2] = x2 + OFF[id][0];
		RECT[id][3] = y2 + OFF[id][1];
		SEEN[id] = System.currentTimeMillis();
	}

	public static boolean visible(int id) {
		return System.currentTimeMillis() - SEEN[id] < 400L;
	}

	public static int[] rect(int id) {
		return RECT[id];
	}

	public static void move(int id, int dx, int dy, int sw, int sh) {
		int[] r = RECT[id];
		if (r[0] + dx < 0) {
			dx = -r[0];
		}
		if (r[2] + dx > sw) {
			dx = sw - r[2];
		}
		if (r[1] + dy < 0) {
			dy = -r[1];
		}
		if (r[3] + dy > sh) {
			dy = sh - r[3];
		}
		OFF[id][0] += dx;
		OFF[id][1] += dy;
		r[0] += dx;
		r[2] += dx;
		r[1] += dy;
		r[3] += dy;
	}

	public static void reset() {
		for (int i = 0; i < N; i++) {
			OFF[i][0] = 0;
			OFF[i][1] = 0;
		}
		save();
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			return;
		}
		try {
			for (String line : Files.readAllLines(FILE)) {
				String[] p = line.split("\t");
				if (p.length < 3) {
					continue;
				}
				for (int i = 0; i < N; i++) {
					if (NAMES[i].equals(p[0])) {
						OFF[i][0] = Integer.parseInt(p[1].trim());
						OFF[i][1] = Integer.parseInt(p[2].trim());
					}
				}
			}
		} catch (IOException | NumberFormatException ignored) {
		}
	}

	public static void save() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < N; i++) {
			sb.append(NAMES[i]).append('\t').append(OFF[i][0]).append('\t').append(OFF[i][1]).append('\n');
		}
		try {
			Files.writeString(FILE, sb.toString());
		} catch (IOException ignored) {
		}
	}
}
