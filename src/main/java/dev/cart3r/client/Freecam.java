package dev.cart3r.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class Freecam {
	private static RemotePlayer cam;
	private static Vec3 anchor;

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
		cam.setYRot(mc.player.getYRot());
		cam.setXRot(mc.player.getXRot());
		cam.yRotO = cam.getYRot();
		cam.xRotO = cam.getXRot();
		anchor = mc.player.position();
		mc.setCameraEntity(cam);
	}

	public static void stop(Minecraft mc) {
		if (cam == null) {
			return;
		}
		cam = null;
		anchor = null;
		mc.setCameraEntity(mc.player);
	}

	public static void reset() {
		cam = null;
		anchor = null;
	}

	public static void tick(Minecraft mc, double speed) {
		if (cam == null || mc.player == null) {
			return;
		}
		mc.player.setPos(anchor.x, anchor.y, anchor.z);
		mc.player.setDeltaMovement(Vec3.ZERO);

		cam.setYRot(mc.player.getYRot());
		cam.setXRot(mc.player.getXRot());

		double yaw = Math.toRadians(cam.getYRot());
		Vec3 look = cam.getLookAngle();
		Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));

		double f = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
		double r = (mc.options.keyRight.isDown() ? 1 : 0) - (mc.options.keyLeft.isDown() ? 1 : 0);
		double u = (mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0);

		Vec3 move = look.scale(f * speed).add(right.scale(r * speed)).add(0, u * speed, 0);

		cam.xo = cam.getX();
		cam.yo = cam.getY();
		cam.zo = cam.getZ();
		cam.setPos(cam.getX() + move.x, cam.getY() + move.y, cam.getZ() + move.z);
	}

	public static void frame(Minecraft mc) {
		if (cam == null || mc.player == null) {
			return;
		}
		cam.setYRot(mc.player.getYRot());
		cam.setXRot(mc.player.getXRot());
		cam.yRotO = cam.getYRot();
		cam.xRotO = cam.getXRot();
	}
}
