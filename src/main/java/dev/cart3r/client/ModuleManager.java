package dev.cart3r.client;

import dev.cart3r.client.Module.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class ModuleManager {
	private static boolean isGoodFood(ItemStack s) {
		return s.get(DataComponents.FOOD) != null
			&& !s.is(Items.GOLDEN_APPLE)
			&& !s.is(Items.ENCHANTED_GOLDEN_APPLE)
			&& !s.is(Items.ROTTEN_FLESH)
			&& !s.is(Items.SPIDER_EYE)
			&& !s.is(Items.PUFFERFISH)
			&& !s.is(Items.POISONOUS_POTATO);
	}

	public static final List<Module> MODULES = List.of(
		new Module("Sprint", Category.MOVEMENT, "Always sprint while moving forward") {
			@Override
			public void onTick(Minecraft mc) {
				if (mc.player.input.hasForwardImpulse() && !mc.player.isShiftKeyDown()) {
					mc.player.setSprinting(true);
				}
			}
		},
		new Module("Flight", Category.MOVEMENT, "Creative-style flying") {
			@Override
			public void onTick(Minecraft mc) {
				mc.player.getAbilities().mayfly = true;
			}

			@Override
			public void onDisable(Minecraft mc) {
				if (!mc.player.isCreative() && !mc.player.isSpectator()) {
					mc.player.getAbilities().mayfly = false;
					mc.player.getAbilities().flying = false;
				}
			}
		},
		new Module("Speed", Category.MOVEMENT, "Faster ground movement") {
			@Override
			public void onTick(Minecraft mc) {
				if (mc.player.onGround() && mc.player.input.hasForwardImpulse()) {
					Vec3 v = mc.player.getDeltaMovement();
					mc.player.setDeltaMovement(v.x * 1.25, v.y, v.z * 1.25);
				}
			}
		},
		new Module("Step", Category.MOVEMENT, "Walk up full blocks") {
			@Override
			public void onTick(Minecraft mc) {
				AttributeInstance a = mc.player.getAttribute(Attributes.STEP_HEIGHT);
				if (a != null && a.getBaseValue() != 1.0) {
					a.setBaseValue(1.0);
				}
			}

			@Override
			public void onDisable(Minecraft mc) {
				AttributeInstance a = mc.player.getAttribute(Attributes.STEP_HEIGHT);
				if (a != null) {
					a.setBaseValue(0.6);
				}
			}
		},
		new Module("Spider", Category.MOVEMENT, "Climb walls you touch") {
			@Override
			public void onTick(Minecraft mc) {
				if (mc.player.horizontalCollision) {
					Vec3 v = mc.player.getDeltaMovement();
					mc.player.setDeltaMovement(v.x, 0.2, v.z);
				}
			}
		},
		new Module("NoFall", Category.MOVEMENT, "Cancel fall damage") {
			@Override
			public void onTick(Minecraft mc) {
				if (mc.player.fallDistance > 2.0f && mc.player.connection != null) {
					mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, mc.player.horizontalCollision));
				}
			}
		},
		new Module("Fullbright", Category.VISUAL, "See clearly in the dark") {
			@Override
			public void onTick(Minecraft mc) {
				MobEffectInstance cur = mc.player.getEffect(MobEffects.NIGHT_VISION);
				if (cur == null || cur.getDuration() < 220) {
					mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, false));
				}
			}

			@Override
			public void onDisable(Minecraft mc) {
				mc.player.removeEffect(MobEffects.NIGHT_VISION);
			}
		},
		new Module("ESP", Category.VISUAL, "Outline entities through walls") {
			@Override
			public void onTick(Minecraft mc) {
				for (Entity e : mc.level.entitiesForRendering()) {
					if (e != mc.player) {
						e.setGlowingTag(true);
					}
				}
			}

			@Override
			public void onDisable(Minecraft mc) {
				if (mc.level != null) {
					for (Entity e : mc.level.entitiesForRendering()) {
						e.setGlowingTag(false);
					}
				}
			}
		},
		new Module("Zoom", Category.VISUAL, "Narrow field of view to zoom in") {
			private int previous = 0;

			@Override
			public void onTick(Minecraft mc) {
				if (mc.options.fov().get() != 30) {
					if (previous == 0) {
						previous = mc.options.fov().get();
					}
					mc.options.fov().set(30);
				}
			}

			@Override
			public void onDisable(Minecraft mc) {
				mc.options.fov().set(previous >= 30 ? previous : 70);
				previous = 0;
			}
		},
		new Module("Tracers", Category.VISUAL, "Draw lines to nearby players and monsters") {
		},
		new Module("HUD", Category.VISUAL, "Show FPS, coordinates and active modules") {
		},
		new Module("AutoTool", Category.UTILITY, "Switch to the best hotbar tool while mining") {
			private int previous = -1;

			@Override
			public void onTick(Minecraft mc) {
				Inventory inv = mc.player.getInventory();
				BlockHitResult hit = mc.hitResult instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK ? b : null;
				boolean mining = hit != null && mc.options.keyAttack.isDown();
				if (!mining) {
					if (previous >= 0) {
						inv.setSelectedSlot(previous);
						previous = -1;
					}
					return;
				}
				BlockState state = mc.level.getBlockState(hit.getBlockPos());
				int current = inv.getSelectedSlot();
				int best = current;
				float bestSpeed = inv.getItem(current).getDestroySpeed(state);
				for (int i = 0; i < 9; i++) {
					float speed = inv.getItem(i).getDestroySpeed(state);
					if (speed > bestSpeed + 0.01f) {
						bestSpeed = speed;
						best = i;
					}
				}
				if (best != current) {
					if (previous < 0) {
						previous = current;
					}
					inv.setSelectedSlot(best);
				}
			}

			@Override
			public void onDisable(Minecraft mc) {
				if (previous >= 0) {
					mc.player.getInventory().setSelectedSlot(previous);
					previous = -1;
				}
			}
		},
		new Module("AutoEat", Category.UTILITY, "Eat food from your hotbar when hungry") {
			private boolean eating;
			private int previous = -1;

			private void stop(Minecraft mc) {
				mc.options.keyUse.setDown(false);
				if (previous >= 0) {
					mc.player.getInventory().setSelectedSlot(previous);
				}
				previous = -1;
				eating = false;
			}

			@Override
			public void onTick(Minecraft mc) {
				Inventory inv = mc.player.getInventory();
				int food = mc.player.getFoodData().getFoodLevel();
				if (!eating) {
					if (food <= 14 && !mc.player.isUsingItem() && mc.screen == null) {
						for (int i = 0; i < 9; i++) {
							if (isGoodFood(inv.getItem(i))) {
								previous = inv.getSelectedSlot();
								inv.setSelectedSlot(i);
								mc.options.keyUse.setDown(true);
								eating = true;
								return;
							}
						}
					}
				} else if (food >= 18 || !isGoodFood(inv.getItem(inv.getSelectedSlot()))) {
					stop(mc);
				}
			}

			@Override
			public void onDisable(Minecraft mc) {
				if (eating) {
					stop(mc);
				}
			}
		},
		new Module("AutoRespawn", Category.UTILITY, "Respawn immediately after dying") {
			@Override
			public void onTick(Minecraft mc) {
				if (mc.screen instanceof DeathScreen) {
					mc.player.respawn();
				}
			}
		},
		new Module("AutoJump", Category.UTILITY, "Jump automatically while walking") {
			@Override
			public void onTick(Minecraft mc) {
				mc.options.keyJump.setDown(mc.player.onGround() && mc.player.input.hasForwardImpulse());
			}

			@Override
			public void onDisable(Minecraft mc) {
				mc.options.keyJump.setDown(false);
			}
		},
		new Module("EdgeJump", Category.UTILITY, "Jump when you reach a ledge") {
			private boolean pulsed;

			@Override
			public void onTick(Minecraft mc) {
				if (pulsed) {
					mc.options.keyJump.setDown(false);
					pulsed = false;
					return;
				}
				LocalPlayer p = mc.player;
				if (p.onGround() && !p.isShiftKeyDown() && p.input.hasForwardImpulse()
					&& mc.level.noCollision(p, p.getBoundingBox().move(0, -0.5, 0).inflate(-0.001, 0, -0.001))) {
					mc.options.keyJump.setDown(true);
					pulsed = true;
				}
			}

			@Override
			public void onDisable(Minecraft mc) {
				mc.options.keyJump.setDown(false);
				pulsed = false;
			}
		},
		new Module("Sneak", Category.UTILITY, "Hold sneak for you") {
			@Override
			public void onTick(Minecraft mc) {
				mc.options.keyShift.setDown(true);
			}

			@Override
			public void onDisable(Minecraft mc) {
				mc.options.keyShift.setDown(false);
			}
		},
		new Module("AutoWalk", Category.UTILITY, "Hold the forward key for you") {
			@Override
			public void onTick(Minecraft mc) {
				mc.options.keyUp.setDown(true);
			}

			@Override
			public void onDisable(Minecraft mc) {
				mc.options.keyUp.setDown(false);
			}
		},
		new Module("AntiAFK", Category.UTILITY, "Move a little so you aren't kicked") {
			private int ticks;

			@Override
			public void onTick(Minecraft mc) {
				if (++ticks >= 600) {
					ticks = 0;
					mc.player.setYRot(mc.player.getYRot() + 3.0f);
					mc.player.swing(InteractionHand.MAIN_HAND);
				}
			}
		},
		new Module("LowParticles", Category.PERFORMANCE, "Minimal particles for more FPS") {
			private ParticleStatus previous;

			@Override
			public void onEnable(Minecraft mc) {
				previous = mc.options.particles().get();
				mc.options.particles().set(ParticleStatus.MINIMAL);
			}

			@Override
			public void onDisable(Minecraft mc) {
				if (previous != null) {
					mc.options.particles().set(previous);
				}
			}
		},
		new Module("EntityCulling", Category.PERFORMANCE, "Render fewer distant entities") {
			private double previous = 1.0;

			@Override
			public void onEnable(Minecraft mc) {
				previous = mc.options.entityDistanceScaling().get();
				mc.options.entityDistanceScaling().set(0.5);
			}

			@Override
			public void onDisable(Minecraft mc) {
				mc.options.entityDistanceScaling().set(previous);
			}
		}
	);

	private ModuleManager() {
	}

	public static boolean isOn(String name) {
		for (Module m : MODULES) {
			if (m.getName().equals(name)) {
				return m.isEnabled();
			}
		}
		return false;
	}

	public static void disableAll(Minecraft mc) {
		for (Module m : MODULES) {
			if (m.isEnabled()) {
				m.toggle(mc);
			}
		}
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			return;
		}
		for (Module m : MODULES) {
			if (m.isEnabled()) {
				m.onTick(mc);
			}
		}
	}
}
