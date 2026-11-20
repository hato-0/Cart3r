package dev.cart3r.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Profiles {
	public record Profile(String name, int accent, List<String> mods) {
	}

	public static final List<Profile> LIST = new ArrayList<>();
	public static String active = "";
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("cart3r-profiles.txt");

	private Profiles() {
	}

	public static void load() {
		LIST.clear();
		if (!Files.exists(FILE)) {
			LIST.add(new Profile("ESP", 0, List.of("ESP", "Tracers", "Nametags")));
			LIST.add(new Profile("FLY", 1, List.of("Flight", "NoFall")));
			LIST.add(new Profile("DIGGER", 2, List.of("AutoTool", "SpeedMine", "Fullbright")));
			save();
			return;
		}
		try {
			for (String line : Files.readAllLines(FILE)) {
				String[] p = line.split("\t", -1);
				if (p.length < 3 || p[0].isBlank()) {
					continue;
				}
				int accent = 0;
				try {
					accent = Integer.parseInt(p[1].trim());
				} catch (NumberFormatException ignored) {
				}
				List<String> mods = new ArrayList<>();
				for (String m : p[2].split(",")) {
					if (!m.isBlank()) {
						mods.add(m.trim());
					}
				}
				LIST.add(new Profile(p[0], accent, mods));
			}
		} catch (IOException ignored) {
		}
	}

	public static void save() {
		StringBuilder sb = new StringBuilder();
		for (Profile p : LIST) {
			sb.append(p.name()).append('\t').append(p.accent()).append('\t').append(String.join(",", p.mods())).append('\n');
		}
		try {
			Files.writeString(FILE, sb.toString());
		} catch (IOException ignored) {
		}
	}

	public static Profile find(String name) {
		for (Profile p : LIST) {
			if (p.name().equalsIgnoreCase(name)) {
				return p;
			}
		}
		return null;
	}

	public static String clean(String name) {
		String n = name == null ? "" : name.replace('\t', ' ').replace(',', ' ').replace('\n', ' ').trim();
		return n.length() > 20 ? n.substring(0, 20) : n;
	}

	public static boolean saveCurrent(String raw) {
		String name = clean(raw);
		if (name.isEmpty()) {
			return false;
		}
		List<String> mods = new ArrayList<>();
		for (Module m : ModuleManager.MODULES) {
			if (m != ModuleManager.INTERFACE && m.persistent() && m.isEnabled()) {
				mods.add(m.getName());
			}
		}
		Profile old = find(name);
		if (old != null) {
			LIST.remove(old);
		}
		LIST.add(new Profile(name, (int) Math.round(Theme.ACCENT.num()), mods));
		active = name;
		save();
		Notify.push("Profile saved: " + name, Theme.accent());
		return true;
	}

	public static void apply(Minecraft mc, Profile p) {
		if (mc.player == null || mc.level == null) {
			return;
		}
		Notify.mute = true;
		try {
			for (Module m : ModuleManager.MODULES) {
				if (m == ModuleManager.INTERFACE || !m.persistent()) {
					continue;
				}
				boolean want = p.mods().contains(m.getName());
				if (m.isEnabled() != want) {
					m.toggle(mc);
				}
			}
		} finally {
			Notify.mute = false;
		}
		Theme.ACCENT.setValue(p.accent());
		active = p.name();
		Config.save();
		Notify.push("Profile: " + p.name(), Theme.accent());
	}

	public static boolean delete(String name) {
		Profile p = find(name);
		if (p == null) {
			return false;
		}
		LIST.remove(p);
		if (active.equalsIgnoreCase(name)) {
			active = "";
		}
		save();
		return true;
	}
}
