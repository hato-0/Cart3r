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
			m.setEnabled(Boolean.parseBoolean(p.getProperty(m.getName(), "false")));
		}
	}

	public static void save() {
		Properties p = new Properties();
		for (Module m : ModuleManager.MODULES) {
			p.setProperty(m.getName(), Boolean.toString(m.isEnabled()));
		}
		try (OutputStream out = Files.newOutputStream(FILE)) {
			p.store(out, "CART3R Client");
		} catch (IOException ignored) {
		}
	}
}
