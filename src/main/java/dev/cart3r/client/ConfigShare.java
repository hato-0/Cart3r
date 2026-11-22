package dev.cart3r.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class ConfigShare {
	private static final String PREFIX = "HATOALL1:";
	private static final String SEP = "\u0001";
	private static final Path DIR = FabricLoader.getInstance().getConfigDir();
	private static final String[] FILES = {"cart3r-client.properties", "hato-hud.txt", "cart3r-profiles.txt"};

	private ConfigShare() {
	}

	private static String read(String name) {
		try {
			Path p = DIR.resolve(name);
			return Files.exists(p) ? Files.readString(p) : "";
		} catch (IOException e) {
			return "";
		}
	}

	public static String export() {
		Config.save();
		HudLayout.save();
		Profiles.save();
		String all = read(FILES[0]) + SEP + read(FILES[1]) + SEP + read(FILES[2]);
		try {
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			try (GZIPOutputStream gz = new GZIPOutputStream(bo)) {
				gz.write(all.getBytes(StandardCharsets.UTF_8));
			}
			return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bo.toByteArray());
		} catch (IOException e) {
			return "";
		}
	}

	public static boolean importCode(String raw) {
		if (raw == null || !raw.trim().startsWith(PREFIX)) {
			return false;
		}
		try {
			byte[] data = Base64.getUrlDecoder().decode(raw.trim().substring(PREFIX.length()));
			String all;
			try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(data))) {
				all = new String(gz.readAllBytes(), StandardCharsets.UTF_8);
			}
			String[] parts = all.split(SEP, -1);
			if (parts.length != 3) {
				return false;
			}
			for (int i = 0; i < 3; i++) {
				Files.writeString(DIR.resolve(FILES[i]), parts[i]);
			}
			Config.load();
			HudLayout.load();
			Profiles.load();
			return true;
		} catch (IOException | IllegalArgumentException e) {
			return false;
		}
	}
}
