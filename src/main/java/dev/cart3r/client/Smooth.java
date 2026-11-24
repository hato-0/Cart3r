package dev.cart3r.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.util.Mth;

import java.util.UUID;

// Per-frame camera helpers: smooth zoom and freelook (look around without turning the player).
public final class Smooth {
	private static float zoomCur = -1f;
	private static int zoomBase = 70;
	private static long lastNs = System.nanoTime();

	private static RemotePlayer lcam;
	private static float savedYaw;
	private static float savedPitch;
	private static float camYaw;
	private static float camPitch;

	private Smooth() {
	}

	public static boolean lookActive() {
		return lcam != null;
	}

	public static void lookStart(Minecraft mc) {
		if (mc.player == null || mc.level == null || Freecam.active() || lcam != null) {
			return;
		}
		lcam = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "Freelook"));
		savedYaw = mc.player.getYRot();
		savedPitch = mc.player.getXRot();
		camYaw = savedYaw;
		camPitch = savedPitch;
		lcam.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());
		mc.setCameraEntity(lcam);
	}

	public static void lookStop(Minecraft mc) {
		if (lcam == null) {
			return;
		}
		lcam = null;
		mc.setCameraEntity(mc.player);
	}

	public static void reset() {
		lcam = null;
		zoomCur = -1f;
	}

	public static void frame(Minecraft mc, float pt) {
		long now = System.nanoTime();
		float dt = Math.min(0.1f, (now - lastNs) / 1.0E9f);
		lastNs = now;
		if (mc.player == null || mc.level == null) {
			reset();
			return;
		}
		zoom(mc, dt);
		look(mc, pt);
	}

	private static void zoom(Minecraft mc, float dt) {
		Module zm = ModuleManager.SMOOTHZOOM;
		boolean want = zm.isEnabled();
		if (want && zoomCur < 0f) {
			zoomBase = mc.options.fov().get();
			zoomCur = zoomBase;
		}
		if (zoomCur < 0f) {
			return;
		}
		float target = want ? (float) zm.getNum("FOV") : zoomBase;
		float speed = (float) zm.getNum("Smoothness");
		zoomCur += (target - zoomCur) * (1f - (float) Math.exp(-dt * speed));
		if (!want && Math.abs(zoomCur - zoomBase) < 0.6f) {
			mc.options.fov().set(zoomBase);
			zoomCur = -1f;
			return;
		}
		mc.options.fov().set(Math.round(zoomCur));
	}

	private static void look(Minecraft mc, float pt) {
		if (lcam == null) {
			return;
		}
		if (Freecam.active()) {
			lcam = null;
			return;
		}
		var p = mc.player;
		float dy = p.getYRot() - savedYaw;
		float dp = p.getXRot() - savedPitch;
		if (dy != 0f || dp != 0f) {
			camYaw += dy;
			camPitch = Mth.clamp(camPitch + dp, -90f, 90f);
			p.setYRot(savedYaw);
			p.setXRot(savedPitch);
			p.yRotO = savedYaw;
			p.xRotO = savedPitch;
		}
		double x = Mth.lerp(pt, p.xo, p.getX());
		double y = Mth.lerp(pt, p.yo, p.getY());
		double z = Mth.lerp(pt, p.zo, p.getZ());
		lcam.setPos(x, y, z);
		lcam.xo = x;
		lcam.yo = y;
		lcam.zo = z;
		lcam.setYRot(camYaw);
		lcam.setXRot(camPitch);
		lcam.yRotO = camYaw;
		lcam.xRotO = camPitch;
		lcam.yHeadRot = camYaw;
		lcam.yHeadRotO = camYaw;
	}
}
