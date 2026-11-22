package dev.cart3r.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Config {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("cart3r-client.properties");

	private Config() {
	}

	private static double parse(String s, double def) {
		if (s == null) {
			return def;
		}
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			return;
		}
		Properties p = new Properties();
		try (InputStream in = Files.newInputStream(FILE)) {
			p.load(in);
		} catch (IOException ignored) {
			return;
		}
		for (Module m : ModuleManager.MODULES) {
			if (m.persistent()) {
				m.setEnabled(Boolean.parseBoolean(p.getProperty(m.getName(), "false")));
			}
			m.setKey((int) parse(p.getProperty(m.getName() + ".key"), m.getKey()));
			m.favorite = Boolean.parseBoolean(p.getProperty(m.getName() + ".fav", "false"));
			for (Setting s : m.getSettings()) {
				String raw = p.getProperty(m.getName() + ".s." + s.name());
				if (raw != null) {
					s.setValue(parse(raw, s.num()));
				}
			}
		}
	}

	public static void save() {
		Properties p = new Properties();
		for (Module m : ModuleManager.MODULES) {
			p.setProperty(m.getName(), Boolean.toString(m.persistent() && m.isEnabled()));
			p.setProperty(m.getName() + ".key", Integer.toString(m.getKey()));
			p.setProperty(m.getName() + ".fav", Boolean.toString(m.favorite));
			for (Setting s : m.getSettings()) {
				p.setProperty(m.getName() + ".s." + s.name(), Double.toString(s.num()));
			}
		}
		try (OutputStream out = Files.newOutputStream(FILE)) {
			p.store(out, "HATO Client");
		} catch (IOException ignored) {
		}
	}
}
