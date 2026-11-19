package dev.cart3r.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class Input {
	private Input() {
	}

	public static boolean mouse(int button) {
		long w = GLFW.glfwGetCurrentContext();
		return w != 0L && GLFW.glfwGetMouseButton(w, button) == GLFW.GLFW_PRESS;
	}

	public static boolean left() {
		return mouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
	}

	public static boolean right() {
		return mouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
	}

	public static boolean key(int k) {
		return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), k);
	}

	public static boolean shift() {
		return key(GLFW.GLFW_KEY_LEFT_SHIFT) || key(GLFW.GLFW_KEY_RIGHT_SHIFT);
	}
}
