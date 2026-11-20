package dev.cart3r.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public final class Freecam {
	private static RemotePlayer cam;
	private static Vec3 anchor;
	private static Vec3 vel = Vec3.ZERO;

	private Freecam() {
	}

	public static boolean active() {
		return cam != null;
	}

	public static void start(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			return;
		}
		cam = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "Freecam"));
		cam.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());
		cam.xo = cam.getX();
		cam.yo = cam.getY();
		cam.zo = cam.getZ();
		syncRot(mc);
		anchor = mc.player.position();
		vel = Vec3.ZERO;
		mc.setCameraEntity(cam);
	}

	public static void stop(Minecraft mc) {
		if (cam == null) {
			return;
		}
		cam = null;
		anchor = null;
		vel = Vec3.ZERO;
		mc.setCameraEntity(mc.player);
	}

	public static void reset() {
		cam = null;
		anchor = null;
		vel = Vec3.ZERO;
	}

	private static void syncRot(Minecraft mc) {
		if (cam == null || mc.player == null) {
			return;
		}
		cam.setYRot(mc.player.getYRot());
		cam.setXRot(mc.player.getXRot());
		cam.yRotO = cam.getYRot();
		cam.xRotO = cam.getXRot();
		cam.yHeadRot = cam.getYRot();
		cam.yHeadRotO = cam.getYRot();
	}

	// Runs at the START of every tick: the real player must not move or act while the camera flies.
	public static void preTick(Minecraft mc) {
		if (cam == null || mc.player == null) {
			return;
		}
		mc.options.keyUp.setDown(false);
		mc.options.keyDown.setDown(false);
		mc.options.keyLeft.setDown(false);
		mc.options.keyRight.setDown(false);
		mc.options.keyJump.setDown(false);
		mc.options.keyShift.setDown(false);
		mc.options.keySprint.setDown(false);
		mc.options.keyAttack.setDown(false);
		mc.options.keyUse.setDown(false);
	}

	public static void tick(Minecraft mc, double speed) {
		if (cam == null || mc.player == null || anchor == null) {
			return;
		}
		mc.player.setPos(anchor.x, anchor.y, anchor.z);
		mc.player.setDeltaMovement(Vec3.ZERO);
		syncRot(mc);

		double yaw = Math.toRadians(cam.getYRot());
		Vec3 look = cam.getLookAngle();
		Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));

		boolean free = mc.screen == null;
		double f = free ? (Input.key(GLFW.GLFW_KEY_W) ? 1 : 0) - (Input.key(GLFW.GLFW_KEY_S) ? 1 : 0) : 0;
		double r = free ? (Input.key(GLFW.GLFW_KEY_D) ? 1 : 0) - (Input.key(GLFW.GLFW_KEY_A) ? 1 : 0) : 0;
		double u = free ? (Input.key(GLFW.GLFW_KEY_SPACE) ? 1 : 0)
			- ((Input.key(GLFW.GLFW_KEY_LEFT_SHIFT) || Input.key(GLFW.GLFW_KEY_C)) ? 1 : 0) : 0;
		double boost = free && Input.key(GLFW.GLFW_KEY_LEFT_CONTROL) ? 2.0 : 1.0;

		Vec3 dir = look.scale(f).add(right.scale(r)).add(0, u, 0);
		if (dir.lengthSqr() > 1.0) {
			dir = dir.normalize();
		}
		Vec3 target = dir.scale(speed * boost);
		vel = vel.add(target.subtract(vel).scale(0.4));
		if (vel.lengthSqr() < 1.0E-5 && target.lengthSqr() == 0) {
			vel = Vec3.ZERO;
		}

		cam.xo = cam.getX();
		cam.yo = cam.getY();
		cam.zo = cam.getZ();
		cam.setPos(cam.getX() + vel.x, cam.getY() + vel.y, cam.getZ() + vel.z);
	}

	public static void frame(Minecraft mc) {
		syncRot(mc);
	}
}
