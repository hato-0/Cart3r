package dev.cart3r.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class Spotify {
	private static volatile String track = "";
	private static volatile boolean busy = false;
	private static long next = 0;

	private Spotify() {
	}

	public static String track() {
		return track;
	}

	public static void poll() {
		long now = System.currentTimeMillis();
		if (busy || now < next) {
			return;
		}
		next = now + 3000;
		if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
			track = "Windows only";
			return;
		}
		busy = true;
		Thread t = new Thread(Spotify::read, "cart3r-spotify");
		t.setDaemon(true);
		t.start();
	}

	private static void read() {
		try {
			ProcessBuilder pb = new ProcessBuilder(
				"powershell", "-NoProfile", "-NonInteractive", "-Command",
				"[Console]::OutputEncoding=[Text.Encoding]::UTF8;"
					+ "(Get-Process Spotify -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle } | Select-Object -First 1).MainWindowTitle");
			pb.redirectErrorStream(true);
			Process p = pb.start();
			byte[] out = p.getInputStream().readAllBytes();
			if (!p.waitFor(4, TimeUnit.SECONDS)) {
				p.destroyForcibly();
			}
			String s = new String(out, StandardCharsets.UTF_8).trim();
			if (s.isEmpty()) {
				track = "Spotify not running";
			} else if (s.startsWith("Spotify")) {
				track = "Paused";
			} else {
				track = s;
			}
		} catch (IOException | InterruptedException e) {
			track = "";
		} finally {
			busy = false;
		}
	}
}
