package dev.cart3r.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Waypoints {
	public record Point(String name, double x, double y, double z, String dim) {
	}

	public static final List<Point> LIST = new ArrayList<>();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("cart3r-waypoints.txt");

	private Waypoints() {
	}

	public static void load() {
		LIST.clear();
		if (!Files.exists(FILE)) {
			return;
		}
		try {
			for (String line : Files.readAllLines(FILE)) {
				String[] p = line.split("\t");
				if (p.length < 5) {
					continue;
				}
				try {
					LIST.add(new Point(p[0], Double.parseDouble(p[1]), Double.parseDouble(p[2]),
						Double.parseDouble(p[3]), p[4]));
				} catch (NumberFormatException ignored) {
				}
			}
		} catch (IOException ignored) {
		}
	}

	public static void save() {
		StringBuilder sb = new StringBuilder();
		for (Point p : LIST) {
			sb.append(p.name()).append('\t').append(p.x()).append('\t').append(p.y()).append('\t')
				.append(p.z()).append('\t').append(p.dim()).append('\n');
		}
		try {
			Files.writeString(FILE, sb.toString());
		} catch (IOException ignored) {
		}
	}

	public static void add(Minecraft mc, String name) {
		if (mc.player == null || mc.level == null) {
			return;
		}
		remove(name);
		LIST.add(new Point(name, mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.level.dimension().toString()));
		save();
	}

	public static boolean remove(String name) {
		boolean removed = LIST.removeIf(p -> p.name().equalsIgnoreCase(name));
		if (removed) {
			save();
		}
		return removed;
	}

	public static void clear() {
		LIST.clear();
		save();
	}
}
