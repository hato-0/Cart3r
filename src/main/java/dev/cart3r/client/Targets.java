package dev.cart3r.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public final class Targets {
	public static final int NONE = 0;
	public static final int PLAYER = 1;
	public static final int MONSTER = 2;
	public static final int PASSIVE = 3;
	public static final int ITEM = 4;

	private Targets() {
	}

	public static int kind(Entity e) {
		if (e instanceof Player p) {
			return p.isSpectator() ? NONE : PLAYER;
		}
		if (e instanceof Enemy) {
			return MONSTER;
		}
		if (e instanceof ItemEntity) {
			return ITEM;
		}
		if (e instanceof LivingEntity && e.getType() != EntityType.ARMOR_STAND) {
			return PASSIVE;
		}
		return NONE;
	}

	public static int color(int kind) {
		return switch (kind) {
			case PLAYER -> Theme.accent();
			case MONSTER -> 0xFFFFD33D;
			case PASSIVE -> 0xFF45E36B;
			case ITEM -> 0xFF4DD8FF;
			default -> 0xFFFFFFFF;
		};
	}

	public static boolean allowed(Module m, int kind) {
		return switch (kind) {
			case PLAYER -> m.getBool("Players");
			case MONSTER -> m.getBool("Monsters");
			case PASSIVE -> m.getBool("Passive");
			case ITEM -> m.getBool("Items");
			default -> false;
		};
	}
}
