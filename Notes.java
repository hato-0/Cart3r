package dev.cart3r.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Notes {
	public static final List<String> LIST = new ArrayList<>();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hato-notes.txt");

	private Notes() {
	}

	public static void load() {
		LIST.clear();
		try {
			if (Files.exists(FILE)) {
				for (String s : Files.readAllLines(FILE)) {
					if (!s.isBlank() && LIST.size() < 12) {
						LIST.add(s);
					}
				}
			}
		} catch (IOException ignored) {
		}
	}

	public static void save() {
		try {
			Files.writeString(FILE, String.join("\n", LIST));
		} catch (IOException ignored) {
		}
	}

	public static boolean add(String text) {
		String t = text == null ? "" : text.replace('\n', ' ').trim();
		if (t.isEmpty() || LIST.size() >= 12) {
			return false;
		}
		LIST.add(t.length() > 60 ? t.substring(0, 60) : t);
		save();
		return true;
	}

	public static boolean remove(int index) {
		if (index < 1 || index > LIST.size()) {
			return false;
		}
		LIST.remove(index - 1);
		save();
		return true;
	}

	public static void clear() {
		LIST.clear();
		save();
	}
}
