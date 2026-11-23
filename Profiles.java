package dev.cart3r.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Profiles {
	public record Profile(String name, int accent, List<String> mods, int key, Map<String, String> settings) {
	}

	public static final List<Profile> LIST = new ArrayList<>();
	public static String active = "";
	private static final Map<String, Boolean> WAS = new HashMap<>();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("cart3r-profiles.txt");
	private static final String PREFIX = "HATO1:";

	private Profiles() {
	}

	private static Profile simple(String name, int accent, String... mods) {
		return new Profile(name, accent, List.of(mods), 0, new LinkedHashMap<>());
	}

	private static String serialize(Profile p) {
		StringBuilder st = new StringBuilder();
		for (Map.Entry<String, String> e : p.settings().entrySet()) {
			if (st.length() > 0) {
				st.append(';');
			}
			st.append(e.getKey()).append('=').append(e.getValue());
		}
		return p.name() + "\t" + p.accent() + "\t" + String.join(",", p.mods()) + "\t" + p.key() + "\t" + st;
	}

	private static Profile parse(String line) {
		String[] p = line.split("\t", -1);
		if (p.length < 3 || p[0].isBlank()) {
			return null;
		}
		int accent = 0;
		int key = 0;
		try {
			accent = Integer.parseInt(p[1].trim());
			if (p.length > 3) {
				key = Integer.parseInt(p[3].trim());
			}
		} catch (NumberFormatException ignored) {
		}
		List<String> mods = new ArrayList<>();
		for (String m : p[2].split(",")) {
			if (!m.isBlank()) {
				mods.add(m.trim());
			}
		}
		Map<String, String> settings = new LinkedHashMap<>();
		if (p.length > 4) {
			for (String part : p[4].split(";")) {
				int eq = part.indexOf('=');
				if (eq > 0) {
					settings.put(part.substring(0, eq), part.substring(eq + 1));
				}
			}
		}
		return new Profile(clean(p[0]), accent, mods, key, settings);
	}

	public static void load() {
		LIST.clear();
		if (!Files.exists(FILE)) {
			LIST.add(simple("ESP", 0, "ESP", "Tracers", "Nametags"));
			LIST.add(simple("FLY", 1, "Flight", "NoFall"));
			LIST.add(simple("DIGGER", 2, "AutoTool", "SpeedMine", "Fullbright"));
			save();
			return;
		}
		try {
			for (String line : Files.readAllLines(FILE)) {
				Profile p = parse(line);
				if (p != null) {
					LIST.add(p);
				}
			}
		} catch (IOException ignored) {
		}
	}

	public static void save() {
		StringBuilder sb = new StringBuilder();
		for (Profile p : LIST) {
			sb.append(serialize(p)).append('\n');
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
		String n = name == null ? "" : name.replace('\t', ' ').replace(',', ' ').replace(';', ' ').replace('\n', ' ').trim();
		return n.length() > 20 ? n.substring(0, 20) : n;
	}

	private static void replace(Profile old, Profile fresh) {
		int i = old == null ? -1 : LIST.indexOf(old);
		if (i >= 0) {
			LIST.set(i, fresh);
		} else {
			LIST.add(fresh);
		}
	}

	public static boolean saveCurrent(String raw) {
		String name = clean(raw);
		if (name.isEmpty()) {
			return false;
		}
		List<String> mods = new ArrayList<>();
		Map<String, String> settings = new LinkedHashMap<>();
		for (Module m : ModuleManager.MODULES) {
			if (m == ModuleManager.INTERFACE || !m.persistent() || !m.isEnabled()) {
				continue;
			}
			mods.add(m.getName());
			for (Setting s : m.getSettings()) {
				settings.put(m.getName() + "|" + s.name(), Double.toString(s.num()));
			}
		}
		Profile old = find(name);
		replace(old, new Profile(name, (int) Math.round(Theme.ACCENT.num()), mods, old == null ? 0 : old.key(), settings));
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
			for (Map.Entry<String, String> e : p.settings().entrySet()) {
				int bar = e.getKey().indexOf('|');
				if (bar <= 0) {
					continue;
				}
				for (Module m : ModuleManager.MODULES) {
					if (m.getName().equals(e.getKey().substring(0, bar))) {
						Setting s = m.setting(e.getKey().substring(bar + 1));
						if (s != null) {
							try {
								s.setValue(Double.parseDouble(e.getValue()));
							} catch (NumberFormatException ignored) {
							}
						}
					}
				}
			}
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

	public static void setKey(String name, int key) {
		Profile p = find(name);
		if (p != null) {
			replace(p, new Profile(p.name(), p.accent(), p.mods(), key, p.settings()));
			save();
		}
	}

	public static String exportCode(Profile p) {
		return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(serialize(p).getBytes(StandardCharsets.UTF_8));
	}

	public static Profile importCode(String raw) {
		if (raw == null) {
			return null;
		}
		String s = raw.trim();
		if (!s.startsWith(PREFIX)) {
			return null;
		}
		try {
			String line = new String(Base64.getUrlDecoder().decode(s.substring(PREFIX.length())), StandardCharsets.UTF_8);
			Profile p = parse(line);
			if (p == null || p.name().isEmpty()) {
				return null;
			}
			Profile fresh = new Profile(p.name(), p.accent(), p.mods(), 0, p.settings());
			replace(find(p.name()), fresh);
			save();
			return fresh;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null) {
			return;
		}
		for (Profile p : new ArrayList<>(LIST)) {
			if (p.key() <= 0) {
				continue;
			}
			boolean down = mc.screen == null && Input.key(p.key());
			boolean was = WAS.getOrDefault(p.name(), false);
			WAS.put(p.name(), down);
			if (down && !was) {
				apply(mc, p);
			}
		}
	}
}
