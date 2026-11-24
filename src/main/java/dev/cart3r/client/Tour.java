package dev.cart3r.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Tour {
	public static boolean shown;
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hato-first-run.txt");

	private Tour() {
	}

	public static boolean needed() {
		return !Files.exists(FILE);
	}

	public static void markDone() {
		try {
			Files.writeString(FILE, "done");
		} catch (IOException ignored) {
		}
	}
}
