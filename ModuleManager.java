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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

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

	public static final InterfaceModule INTERFACE = new InterfaceModule();

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
			private final Setting speed = num("Speed", 0.5, 5.0, 1.0, 0.25);

			@Override
			public void onTick(Minecraft mc) {
				mc.player.getAbilities().mayfly = true;
				mc.player.getAbilities().setFlyingSpeed((float) (0.05 * speed.num()));
			}

			@Override
			public void onDisable(Minecraft mc) {
				mc.player.getAbilities().setFlyingSpeed(0.05f);
				if (!mc.player.isCreative() && !mc.player.isSpectator()) {
					mc.player.getAbilities().mayfly = false;
					mc.player.getAbilities().flying = false;
				}
			}
		},
		new Module("Speed", Category.MOVEMENT, "Faster ground movement") {
			private final Setting mult = num("Multiplier", 1.0, 1.8, 1.25, 0.05);

			@Override
			public void onTick(Minecraft mc) {
				if (mc.player.onGround() && mc.player.input.hasForwardImpulse()) {
					Vec3 v = mc.player.getDeltaMovement();
					mc.player.setDeltaMovement(v.x * mult.num(), v.y, v.z * mult.num());
				}
			}
		},
		new Module("Step", Category.MOVEMENT, "Walk up full blocks") {
			private final Setting height = num("Height", 0.6, 2.5, 1.0, 0.1);

			@Override
			public void onTick(Minecraft mc) {
				AttributeInstance a = mc.player.getAttribute(Attributes.STEP_HEIGHT);
				if (a != null && Math.abs(a.getBaseValue() - height.num()) > 0.001) {
					a.setBaseValue(height.num());
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
		new Module("HighJump", Category.MOVEMENT, "Jump higher") {
			private final Setting power = num("Power", 0.42, 1.0, 0.6, 0.02);

			@Override
			public void onTick(Minecraft mc) {
				AttributeInstance a = mc.player.getAttribute(Attributes.JUMP_STRENGTH);
				if (a != null && Math.abs(a.getBaseValue() - power.num()) > 0.001) {
					a.setBaseValue(power.num());
				}
			}

			@Override
			public void onDisable(Minecraft mc) {
				AttributeInstance a = mc.player.getAttribute(Attributes.JUMP_STRENGTH);
				if (a != null) {
					a.setBaseValue(0.42);
				}
			}
		},
		new Module("Float", Category.MOVEMENT, "Stay on top of water (sneak to dive)") {
			@Override
			public void onTick(Minecraft mc) {
				if (mc.player.isInWater() && !mc.player.isShiftKeyDown()) {
					Vec3 v = mc.player.getDeltaMovement();
					mc.player.setDeltaMovement(v.x, Math.max(v.y, 0.12), v.z);
				}
			}
		},
		new Module("FastLadder", Category.MOVEMENT, "Climb ladders and vines faster") {
			@Override
			public void onTick(Minecraft mc) {
				if (mc.player.onClimbable() && mc.player.horizontalCollision && !mc.player.isShiftKeyDown()) {
					Vec3 v = mc.player.getDeltaMovement();
					mc.player.setDeltaMovement(v.x, 0.3, v.z);
				}
			}
		},
		new Module("SafeWalk", Category.MOVEMENT, "Never fall off edges (auto sneak at ledges)") {
			private boolean forced;

			@Override
			public void onTick(Minecraft mc) {
				LocalPlayer p = mc.player;
				Vec3 v = p.getDeltaMovement();
				boolean edge = p.onGround()
					&& mc.level.noCollision(p, p.getBoundingBox().move(v.x * 3.0, -0.6, v.z * 3.0));
				if (edge) {
					mc.options.keyShift.setDown(true);
					forced = true;
				} else if (forced) {
					mc.options.keyShift.setDown(false);
					forced = false;
				}
			}

			@Override
			public void onDisable(Minecraft mc) {
				if (forced) {
					mc.options.keyShift.setDown(false);
					forced = false;
				}
			}
		},
		new Module("Glide", Category.MOVEMENT, "Fall slowly (combine with NoFall to avoid damage)") {
			private final Setting fall = num("Fall speed", 0.02, 0.3, 0.08, 0.02);

			@Override
			public void onTick(Minecraft mc) {
				LocalPlayer p = mc.player;
				if (!p.onGround() && !p.isInWater() && !p.isFallFlying() && !p.getAbilities().flying) {
					Vec3 v = p.getDeltaMovement();
					if (v.y < -fall.num()) {
						p.setDeltaMovement(v.x, -fall.num(), v.z);
					}
				}
			}
		},
		new Module("ElytraBoost", Category.MOVEMENT, "Hold forward while gliding to speed up") {
			private final Setting power = num("Power", 0.01, 0.1, 0.04, 0.01);

			@Override
			public void onTick(Minecraft mc) {
				LocalPlayer p = mc.player;
				if (p.isFallFlying() && mc.options.keyUp.isDown()) {
					p.setDeltaMovement(p.getDeltaMovement().add(p.getLookAngle().scale(power.num())));
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
		new Module("BoatFly", Category.MOVEMENT, "Fly while riding a boat or other vehicle") {
			private final Setting speed = num("Speed", 0.2, 3.0, 1.0, 0.1);
			private final Setting vertical = num("Vertical", 0.1, 2.0, 0.6, 0.1);
			private Entity last;

			private void release() {
				if (last != null) {
					last.setNoGravity(false);
					last = null;
				}
			}

			@Override
			public void onTick(Minecraft mc) {
				Entity v = mc.player.getVehicle();
				if (v == null) {
					release();
					return;
				}
				last = v;
				v.setNoGravity(true);
				double yaw = Math.toRadians(mc.player.getYRot());
				double f = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
				double sp = speed.num();
				double y = (mc.options.keyJump.isDown() ? vertical.num() : 0)
					- (mc.options.keySprint.isDown() ? vertical.num() : 0);
				v.setDeltaMovement(-Math.sin(yaw) * f * sp, y, Math.cos(yaw) * f * sp);
			}

			@Override
			public void onDisable(Minecraft mc) {
				release();
			}
		},
		new Module("Freecam", Category.VISUAL, "Fly the camera around without moving") {
			private final Setting speed = num("Speed", 0.2, 3.0, 0.8, 0.1);

			@Override
			public boolean persistent() {
				return false;
			}

			@Override
			public void onEnable(Minecraft mc) {
				Freecam.start(mc);
			}

			@Override
			public void onTick(Minecraft mc) {
				Freecam.tick(mc, speed.num());
			}

			@Override
			public void onDisable(Minecraft mc) {
				Freecam.stop(mc);
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
		new Module("ESP", Category.RENDER, "Boxes around entities, visible through walls") {
			private final Setting players = toggle("Players", true);
			private final Setting monsters = toggle("Monsters", true);
			private final Setting passive = toggle("Passive", false);
			private final Setting items = toggle("Items", false);
			private final Setting range = num("Range", 16, 256, 96, 8);
			private final Setting style = options("Style", 0, "Box", "Corners");
			private final Setting health = toggle("Health bar", true);
			private final Setting names = toggle("Names", false);
			private final Setting fill = toggle("Fill", true);
			private final Setting glow = toggle("Glow", false);
			private boolean glowing;

			private void clearGlow(Minecraft mc) {
				if (mc.level != null) {
					for (Entity e : mc.level.entitiesForRendering()) {
						e.setGlowingTag(false);
					}
				}
				glowing = false;
			}

			@Override
			public void onTick(Minecraft mc) {
				if (glow.bool()) {
					glowing = true;
					for (Entity e : mc.level.entitiesForRendering()) {
						if (e != mc.player && Targets.allowed(this, Targets.kind(e))) {
							e.setGlowingTag(true);
						}
					}
				} else if (glowing) {
					clearGlow(mc);
				}
			}

			@Override
			public void onDisable(Minecraft mc) {
				clearGlow(mc);
			}
		},
		new Module("Tracers", Category.RENDER, "Lines from you to nearby entities") {
			private final Setting players = toggle("Players", true);
			private final Setting monsters = toggle("Monsters", false);
			private final Setting passive = toggle("Passive", false);
			private final Setting items = toggle("Items", false);
			private final Setting range = num("Range", 16, 256, 128, 8);
			private final Setting origin = options("Origin", 0, "Crosshair", "Bottom");
			private final Setting thickness = num("Thickness", 1, 3, 1, 1);
			private final Setting distance = toggle("Distance", true);
		},
		new Module("Nametags", Category.RENDER, "Name, health and distance above entities") {
			private final Setting players = toggle("Players", true);
			private final Setting monsters = toggle("Monsters", false);
			private final Setting passive = toggle("Passive", false);
			private final Setting range = num("Range", 8, 128, 64, 4);
			private final Setting health = toggle("Health", true);
			private final Setting distance = toggle("Distance", true);
		},
		new Module("LightDebug", Category.WORLD, "Mark nearby spots where monsters can spawn") {
			private final Setting radius = num("Radius", 4, 16, 8, 1);
			private final Setting night = toggle("Night spots", true);
		},
		new Module("StorageESP", Category.WORLD, "Find trial chambers and vaults (singleplayer: also spawners and chests)") {
			private final Setting trial = toggle("Trials", true);
			private final Setting vaults = toggle("Vaults", true);
			private final Setting spawners = toggle("Spawners", true);
			private final Setting chests = toggle("Chests", true);
			private final Setting ender = toggle("Ender", true);
			private final Setting labels = toggle("Labels", true);
			private final Setting range = num("Range", 16, 256, 128, 8);
		},
		new Module("Hitboxes", Category.RENDER, "Show the hitbox of every entity") {
			private final Setting players = toggle("Players", true);
			private final Setting monsters = toggle("Monsters", true);
			private final Setting passive = toggle("Passive", true);
			private final Setting items = toggle("Items", false);
			private final Setting others = toggle("Others", false);
			private final Setting eye = toggle("Eye line", true);
			private final Setting range = num("Range", 8, 128, 48, 4);
		},
		new Module("OreESP", Category.WORLD, "Mark ores nearby. Most servers forbid this - use in singleplayer!") {
			private final Setting radius = num("Radius", 8, 24, 16, 2);
			private final Setting diamond = toggle("Diamond", true);
			private final Setting debris = toggle("Debris", true);
			private final Setting emerald = toggle("Emerald", true);
			private final Setting gold = toggle("Gold", true);
			private final Setting iron = toggle("Iron", false);
			private final Setting others = toggle("Others", false);
		},
		new Module("Waypoints", Category.WORLD, "Show saved waypoints. Chat: /cart3r wp add <name>") {
			private final Setting distance = toggle("Distance", true);
			private final Setting death = toggle("Death point", true);
			private boolean saved;

			@Override
			public void onTick(Minecraft mc) {
				if (mc.screen instanceof DeathScreen) {
					if (!saved && death.bool()) {
						saved = true;
						Waypoints.add(mc, "Death");
					}
				} else {
					saved = false;
				}
			}
		},
		new Module("Breadcrumbs", Category.WORLD, "Draw a trail of where you walked") {
			private final Setting length = num("Length", 20, 200, 100, 10);
		},
		new Module("BlockOverlay", Category.RENDER, "Highlight the block you are looking at") {
			private final Setting color = options("Color", 0, "Accent", "White", "Red", "Green", "Blue", "Yellow", "Pink");
			private final Setting fill = toggle("Fill", true);
			private final Setting opacity = num("Fill opacity", 0.05, 0.6, 0.2, 0.05);
			private final Setting thick = num("Thickness", 1, 3, 1, 1);
		},
		new Module("JumpCircles", Category.RENDER, "Expanding ring where you jump") {
			private final Setting color = options("Color", 0, "Accent", "White", "Red", "Green", "Blue", "Yellow", "Pink");
			private final Setting size = num("Size", 1, 4, 1.8, 0.2);
			private final Setting duration = num("Duration", 300, 1500, 700, 100);
		},
		new Module("TargetESP", Category.RENDER, "Animated marker on the entity you look at") {
			private final Setting color = options("Color", 0, "Accent", "White", "Red", "Green", "Blue", "Yellow", "Pink");
			private final Setting style = options("Style", 0, "Ring", "Corners");
		},
		new Module("WeatherNotifier", Category.WORLD, "Message when rain or thunder starts and stops") {
		},
		new Module("SpawnerNotifier", Category.WORLD, "Message when a spawner is nearby (singleplayer; trial spawners everywhere)") {
			private final Setting range = num("Range", 16, 128, 64, 8);
		},
		new Module("ColorGrade", Category.VISUAL, "Screen tint and vignette") {
			private final Setting preset = options("Preset", 0, "Warm", "Cool", "Sunset", "Mint", "Night", "Rose");
			private final Setting strength = num("Strength", 0.02, 0.4, 0.12, 0.02);
			private final Setting vignette = toggle("Vignette", true);
			private final Setting vstrength = num("Vignette strength", 0.1, 0.8, 0.4, 0.1);
		},
		new Module("Crosshair+", Category.HUD, "Custom crosshair that replaces the vanilla one") {
			private final Setting style = options("Style", 0, "Cross", "Dot", "Circle", "Plus", "Square", "Diamond", "Chevron", "T-Cross");
			private final Setting color = options("Color", 0, "Accent", "White", "Red", "Green", "Blue", "Yellow", "Pink");
			private final Setting size = num("Size", 2, 12, 5, 1);
			private final Setting gap = num("Gap", 0, 8, 3, 1);
			private final Setting thick = num("Thick", 1, 3, 1, 1);
			private final Setting dynamic = toggle("Dynamic", true);
			private final Setting outline = toggle("Outline", true);
		},
		new Module("ChunkBorders", Category.WORLD, "Show chunk borders around you") {
			private final Setting color = options("Color", 0, "Accent", "White", "Red", "Green", "Blue", "Yellow", "Pink");
			private final Setting radius = num("Radius", 0, 3, 1, 1);
		},
		new Module("LowHealth", Category.VISUAL, "Pulsing red screen edges when your health is low") {
			private final Setting health = num("Health", 2, 12, 8, 1);
			private final Setting strength = num("Strength", 0.1, 0.8, 0.45, 0.05);
		},
		new Module("DurabilityAlert", Category.UTILITY, "Warn when your tool or armor is about to break") {
			private final Setting percent = num("Percent", 5, 50, 15, 5);
			private final long[] last = new long[6];

			@Override
			public void onTick(Minecraft mc) {
				if (mc.player.tickCount % 20 != 0) {
					return;
				}
				long now = System.currentTimeMillis();
				EquipmentSlot[] slots = {EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
					EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND};
				for (int i = 0; i < slots.length; i++) {
					ItemStack s = mc.player.getItemBySlot(slots[i]);
					if (s.isEmpty() || !s.isDamageableItem() || s.getMaxDamage() <= 0) {
						continue;
					}
					int left = (s.getMaxDamage() - s.getDamageValue()) * 100 / s.getMaxDamage();
					if (left <= percent.num() && now - last[i] > 30000L) {
						last[i] = now;
						Notify.push("Low durability: " + s.getHoverName().getString() + " (" + left + "%)", 0xFFFFD33D);
					}
				}
			}
		},
		new Module("HitParticles", Category.VISUAL, "Extra particles when a creature gets hurt") {
			private final Setting type = options("Type", 0, "Crit", "Enchant", "Heart", "Flame", "Soul", "Star", "Totem");
			private final Setting amount = num("Amount", 3, 30, 10, 1);
			private final Setting range = num("Range", 8, 48, 24, 4);
		},
		new Module("DamageNumbers", Category.RENDER, "Floating damage numbers above hurt creatures") {
			private final Setting duration = num("Duration", 500, 2000, 1000, 100);
			private final Setting range = num("Range", 8, 48, 32, 4);
		},
		new Module("ItemLabels", Category.RENDER, "Show names and counts above dropped items") {
			private final Setting range = num("Range", 8, 48, 24, 4);
			private final Setting count = toggle("Count", true);
		},
		new Module("Stopwatch", Category.HUD, "Timer on the HUD, starts when you turn it on") {
			@Override
			public void onEnable(Minecraft mc) {
				Hud.stopStart = System.currentTimeMillis();
			}
		},
		new Module("FOV", Category.VISUAL, "Change your field of view") {
			private final Setting fov = num("FOV", 30, 110, 90, 5);
			private int previous = 0;

			@Override
			public void onTick(Minecraft mc) {
				int target = (int) fov.num();
				if (mc.options.fov().get() != target) {
					if (previous == 0) {
						previous = mc.options.fov().get();
					}
					mc.options.fov().set(target);
				}
			}

			@Override
			public void onDisable(Minecraft mc) {
				mc.options.fov().set(previous >= 30 ? previous : 70);
				previous = 0;
			}
		},
		new Module("Zoom", Category.VISUAL, "Narrow field of view to zoom in") {
			private final Setting fov = num("FOV", 30, 60, 30, 5);
			private int previous = 0;

			@Override
			public void onTick(Minecraft mc) {
				int target = (int) fov.num();
				if (mc.options.fov().get() != target) {
					if (previous == 0) {
						previous = mc.options.fov().get();
					}
					mc.options.fov().set(target);
				}
			}

			@Override
			public void onDisable(Minecraft mc) {
				mc.options.fov().set(previous >= 30 ? previous : 70);
				previous = 0;
			}
		},
		new Module("SwingSpeed", Category.VISUAL, "Faster arm swing animation") {
			private final Setting speed = num("Speed", 1, 4, 2, 1);

			@Override
			public void onTick(Minecraft mc) {
				if (mc.player.swinging) {
					mc.player.swingTime = Math.min(mc.player.swingTime + (int) speed.num() - 1, 5);
				}
			}
		},
		new Module("NoWeather", Category.WORLD, "Remove rain and thunder visuals") {
			@Override
			public void onTick(Minecraft mc) {
				mc.level.setRainLevel(0.0f);
				mc.level.setThunderLevel(0.0f);
			}
		},
		new Module("SpeedMine", Category.PLAYER, "Mine faster (client-side, servers may correct it)") {
			private final Setting mult = num("Multiplier", 1.0, 3.0, 1.4, 0.1);

			@Override
			public void onTick(Minecraft mc) {
				AttributeInstance a = mc.player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
				if (a != null && Math.abs(a.getBaseValue() - mult.num()) > 0.001) {
					a.setBaseValue(mult.num());
				}
			}

			@Override
			public void onDisable(Minecraft mc) {
				AttributeInstance a = mc.player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
				if (a != null) {
					a.setBaseValue(1.0);
				}
			}
		},
		new Module("Reach", Category.PLAYER, "Hit mobs from further away (never players)") {
			private final Setting distance = num("Distance", 3.5, 6.0, 4.5, 0.1);
			private boolean attackWas;

			@Override
			public void onTick(Minecraft mc) {
				boolean down = mc.options.keyAttack.isDown();
				boolean press = down && !attackWas;
				attackWas = down;
				if (!press || mc.screen != null || mc.gameMode == null) {
					return;
				}
				if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) {
					return;
				}
				if (mc.player.getAttackStrengthScale(0.0f) < 0.9f) {
					return;
				}
				Vec3 eye = mc.player.getEyePosition();
				Vec3 look = mc.player.getViewVector(1.0f);
				double reach = distance.num();
				Vec3 end = eye.add(look.scale(reach));
				Entity best = null;
				double bestD = reach * reach + 1.0;
				for (Entity e : mc.level.entitiesForRendering()) {
					if (e == mc.player || !(e instanceof LivingEntity) || e instanceof Player
						|| !e.isAlive() || !e.isPickable() || e.getType() == EntityType.ARMOR_STAND) {
						continue;
					}
					AABB bb = e.getBoundingBox().inflate(e.getPickRadius());
					Optional<Vec3> hit = bb.clip(eye, end);
					if (hit.isEmpty()) {
						continue;
					}
					double d = eye.distanceToSqr(hit.get());
					if (d < 9.0) {
						continue;
					}
					if (d < bestD && mc.player.hasLineOfSight(e)) {
						best = e;
						bestD = d;
					}
				}
				if (best != null) {
					mc.gameMode.attack(mc.player, best);
					mc.player.swing(InteractionHand.MAIN_HAND);
				}
			}
		},
		new Module("AutoTool", Category.PLAYER, "Switch to the best hotbar tool while mining") {
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
		new Module("AutoEat", Category.PLAYER, "Eat food from your hotbar when hungry") {
			private final Setting hunger = num("Hunger", 4, 19, 14, 1);
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
					if (food <= (int) hunger.num() && !mc.player.isUsingItem() && mc.screen == null) {
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
		new Module("AutoTotem", Category.PLAYER, "Keep a totem of undying in your offhand") {
			private final Setting delay = num("Delay", 0, 10, 1, 1);
			private int wait;

			@Override
			public void onTick(Minecraft mc) {
				if (mc.gameMode == null) {
					return;
				}
				if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
					wait = 0;
					return;
				}
				if (++wait <= delay.num()) {
					return;
				}
				Inventory inv = mc.player.getInventory();
				for (int i = 0; i < 36; i++) {
					if (inv.getItem(i).is(Items.TOTEM_OF_UNDYING)) {
						int slot = i < 9 ? 36 + i : i;
						mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot, 40, ClickType.SWAP, mc.player);
						wait = 0;
						return;
					}
				}
			}
		},
		new Module("AutoRespawn", Category.PLAYER, "Respawn immediately after dying") {
			@Override
			public void onTick(Minecraft mc) {
				if (mc.screen instanceof DeathScreen) {
					mc.player.respawn();
				}
			}
		},
		new Module("AutoMine", Category.PLAYER, "Hold the attack button for you") {
			@Override
			public void onTick(Minecraft mc) {
				if (mc.screen == null) {
					mc.options.keyAttack.setDown(true);
				}
			}

			@Override
			public void onDisable(Minecraft mc) {
				mc.options.keyAttack.setDown(false);
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
			private final Setting seconds = num("Seconds", 5, 120, 30, 5);
			private int ticks;

			@Override
			public void onTick(Minecraft mc) {
				if (++ticks >= seconds.num() * 20) {
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
			private final Setting distance = num("Distance", 0.5, 1.0, 0.5, 0.1);
			private double previous = 1.0;

			@Override
			public void onEnable(Minecraft mc) {
				previous = mc.options.entityDistanceScaling().get();
				mc.options.entityDistanceScaling().set(distance.num());
			}

			@Override
			public void onDisable(Minecraft mc) {
				mc.options.entityDistanceScaling().set(previous);
			}
		},
		new Module("LowDetail", Category.PERFORMANCE, "Disable shadows and view bobbing") {
			private boolean shadows;
			private boolean bob;
			private boolean saved;

			@Override
			public void onEnable(Minecraft mc) {
				shadows = mc.options.entityShadows().get();
				bob = mc.options.bobView().get();
				saved = true;
				mc.options.entityShadows().set(false);
				mc.options.bobView().set(false);
			}

			@Override
			public void onDisable(Minecraft mc) {
				if (saved) {
					mc.options.entityShadows().set(shadows);
					mc.options.bobView().set(bob);
					saved = false;
				}
			}
		},
		new Module("FPSBoost", Category.PERFORMANCE, "Lower render distance and details for more FPS") {
			private final Setting render = num("Render distance", 4, 16, 8, 1);
			private final Setting simulation = num("Simulation", 5, 12, 6, 1);
			private int pr;
			private int ps;
			private int pb;
			private boolean pShadow;
			private boolean pBob;
			private ParticleStatus pParticles;
			private boolean saved;

			@Override
			public void onEnable(Minecraft mc) {
				pr = mc.options.renderDistance().get();
				ps = mc.options.simulationDistance().get();
				pb = mc.options.biomeBlendRadius().get();
				pShadow = mc.options.entityShadows().get();
				pBob = mc.options.bobView().get();
				pParticles = mc.options.particles().get();
				saved = true;
				apply(mc);
			}

			private void apply(Minecraft mc) {
				if (mc.options.renderDistance().get() != (int) render.num()) {
					mc.options.renderDistance().set((int) render.num());
				}
				if (mc.options.simulationDistance().get() != (int) simulation.num()) {
					mc.options.simulationDistance().set((int) simulation.num());
				}
				mc.options.biomeBlendRadius().set(0);
				mc.options.entityShadows().set(false);
				mc.options.bobView().set(false);
				mc.options.particles().set(ParticleStatus.MINIMAL);
			}

			@Override
			public void onTick(Minecraft mc) {
				if (mc.player.tickCount % 40 == 0 && saved) {
					apply(mc);
				}
			}

			@Override
			public void onDisable(Minecraft mc) {
				if (!saved) {
					return;
				}
				saved = false;
				mc.options.renderDistance().set(pr);
				mc.options.simulationDistance().set(ps);
				mc.options.biomeBlendRadius().set(pb);
				mc.options.entityShadows().set(pShadow);
				mc.options.bobView().set(pBob);
				if (pParticles != null) {
					mc.options.particles().set(pParticles);
				}
			}
		},
		new Module("FPSLimit", Category.PERFORMANCE, "Cap your frame rate to save power and heat") {
			private final Setting max = num("Max FPS", 30, 260, 120, 10);
			private final Setting vsync = toggle("VSync off", false);
			private int prev = 120;
			private boolean prevSync;
			private boolean saved;

			@Override
			public void onEnable(Minecraft mc) {
				prev = mc.options.framerateLimit().get();
				prevSync = mc.options.enableVsync().get();
				saved = true;
			}

			@Override
			public void onTick(Minecraft mc) {
				int t = (int) max.num();
				if (mc.options.framerateLimit().get() != t) {
					mc.options.framerateLimit().set(t);
				}
				if (vsync.bool() && mc.options.enableVsync().get()) {
					mc.options.enableVsync().set(false);
				}
			}

			@Override
			public void onDisable(Minecraft mc) {
				if (saved) {
					mc.options.framerateLimit().set(prev);
					mc.options.enableVsync().set(prevSync);
					saved = false;
				}
			}
		},
		new Module("HUD", Category.HUD, "Show FPS, coordinates and active modules") {
			private final Setting fps = toggle("FPS", true);
			private final Setting coords = toggle("Coords", true);
			private final Setting modules = toggle("Modules", true);
			private final Setting ping = toggle("Ping", false);
			private final Setting time = toggle("Time", false);
			private final Setting profile = toggle("Profile", true);
			private final Setting speed = toggle("Speed", false);
			private final Setting light = toggle("Light", false);
			private final Setting facing = toggle("Facing", false);
			private final Setting biome = toggle("Biome", false);
			private final Setting mobs = toggle("Mobs", false);
		},
		new Module("Spotify", Category.HUD, "Now playing overlay (Windows desktop app)") {
		},
		new Module("Radar", Category.HUD, "Mini radar in the corner") {
			private final Setting players = toggle("Players", true);
			private final Setting monsters = toggle("Monsters", true);
			private final Setting passive = toggle("Passive", false);
			private final Setting items = toggle("Items", false);
			private final Setting range = num("Range", 10, 100, 40, 5);
			private final Setting size = num("Size", 40, 120, 80, 10);
			private final Setting rotate = toggle("Rotate", true);
		},
		new Module("Compass", Category.HUD, "Direction bar at the top of the screen") {
		},
		new Module("Effects", Category.HUD, "Show your active potion effects") {
		},
		new Module("Counters", Category.HUD, "Item counters: totems, pearls, apples, arrows, XP") {
			private final Setting totems = toggle("Totems", true);
			private final Setting pearls = toggle("Pearls", true);
			private final Setting apples = toggle("Apples", true);
			private final Setting arrows = toggle("Arrows", false);
			private final Setting xp = toggle("XP bottles", false);
		},
		new Module("FrameGraph", Category.HUD, "Live frame time graph") {
		},
		new Module("StreamerMode", Category.CLIENT, "Hide your coordinates on the HUD") {
		},
		new Module("Keystrokes", Category.HUD, "Show pressed keys and mouse buttons") {
			private final Setting mouse = toggle("Mouse", true);
			private final Setting cps = toggle("CPS", true);
		},
		new Module("ArmorHUD", Category.HUD, "Show your armor next to the hotbar") {
		},
		new Module("Panic", Category.CLIENT, "Turn off every module at once") {
			@Override
			public boolean persistent() {
				return false;
			}

			@Override
			public void onEnable(Minecraft mc) {
				setEnabled(false);
				disableAll(mc);
			}
		},
		new Module("Notes", Category.HUD, "Show your quick notes on screen (/hato note add <text>)") {
		},
		new Module("EffectAlert", Category.UTILITY, "Warn when a potion effect is about to run out") {
			private final Setting secs = num("Seconds", 5, 30, 10, 1);
			private final java.util.Set<String> warned = new java.util.HashSet<>();

			@Override
			public void onTick(Minecraft mc) {
				if (mc.player == null || mc.player.tickCount % 10 != 0) {
					return;
				}
				java.util.Set<String> now = new java.util.HashSet<>();
				for (MobEffectInstance e : mc.player.getActiveEffects()) {
					if (e.isInfiniteDuration()) {
						continue;
					}
					String id = e.getEffect().value().getDescriptionId();
					if (e.getDuration() <= secs.num() * 20) {
						now.add(id);
						if (warned.add(id)) {
							Notify.push("Effect ending: " + e.getEffect().value().getDisplayName().getString(), 0xFFFFC53D);
						}
					}
				}
				warned.retainAll(now);
			}
		},
		new Module("InventoryAlert", Category.UTILITY, "Tell you when your inventory is full") {
			private boolean was;

			@Override
			public void onTick(Minecraft mc) {
				if (mc.player == null || mc.player.tickCount % 40 != 0) {
					return;
				}
				boolean full = mc.player.getInventory().getFreeSlot() < 0;
				if (full && !was) {
					Notify.push("Inventory full", 0xFFFF3B5C);
				}
				was = full;
			}
		},
		new Module("BiomeNotifier", Category.WORLD, "Notification when you enter a new biome") {
			private String last = "";

			@Override
			public void onTick(Minecraft mc) {
				if (mc.player == null || mc.level == null || mc.player.tickCount % 20 != 0) {
					return;
				}
				String b = Hud.biomeName(mc);
				if (!b.equals(last)) {
					if (!last.isEmpty()) {
						Notify.push("Biome: " + b, Theme.accent());
					}
					last = b;
				}
			}
		},
		new Module("SmoothZoom", Category.VISUAL, "Smooth animated zoom (use this instead of Zoom)") {
			private final Setting fov = num("FOV", 10, 60, 30, 5);
			private final Setting smooth = num("Smoothness", 3, 20, 10, 1);
		},
		new Module("Freelook", Category.VISUAL, "Look around freely while your player keeps facing forward") {
			@Override
			public boolean persistent() {
				return false;
			}

			@Override
			public void onEnable(Minecraft mc) {
				Smooth.lookStart(mc);
			}

			@Override
			public void onDisable(Minecraft mc) {
				Smooth.lookStop(mc);
			}
		},
		new Module("DeathNote", Category.UTILITY, "Saves your death coordinates as a note") {
			private boolean dead;

			@Override
			public void onTick(Minecraft mc) {
				boolean d = mc.player.isDeadOrDying();
				if (d && !dead) {
					String where = "Death " + (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ();
					Notes.add(where);
					Notify.push(where, 0xFFFF3B5C);
				}
				dead = d;
			}
		},
		new Module("HungerAlert", Category.UTILITY, "Warn when your food bar is low") {
			private final Setting level = num("Food level", 2, 14, 6, 1);
			private boolean low;

			@Override
			public void onTick(Minecraft mc) {
				if (mc.player.tickCount % 20 != 0) {
					return;
				}
				boolean l = mc.player.getFoodData().getFoodLevel() <= (int) level.num();
				if (l && !low) {
					Notify.push("Hungry - eat something", 0xFFFFC53D);
				}
				low = l;
			}
		},
		new Module("AirAlert", Category.UTILITY, "Warn when you are running out of air") {
			private boolean low;

			@Override
			public void onTick(Minecraft mc) {
				boolean l = mc.player.getAirSupply() < mc.player.getMaxAirSupply() / 3;
				if (l && !low) {
					Notify.push("Low air - surface now", 0xFF3D8BFF);
				}
				low = l;
			}
		},
		new Module("BreakReminder", Category.UTILITY, "Reminds you to take a break") {
			private final Setting mins = num("Minutes", 10, 120, 45, 5);
			private long next;

			@Override
			public void onEnable(Minecraft mc) {
				next = System.currentTimeMillis() + (long) (mins.num() * 60000L);
			}

			@Override
			public void onTick(Minecraft mc) {
				if (next == 0L) {
					next = System.currentTimeMillis() + (long) (mins.num() * 60000L);
				}
				if (System.currentTimeMillis() >= next) {
					Notify.push("Time for a short break!", Theme.accent());
					next = System.currentTimeMillis() + (long) (mins.num() * 60000L);
				}
			}
		},
		new Module("CoordsNote", Category.UTILITY, "Saves your current position as a note when enabled") {
			@Override
			public void onEnable(Minecraft mc) {
				if (mc.player != null) {
					Notes.add("Pos " + (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ());
					Notify.push("Position saved to notes", Theme.accent());
				}
				setEnabled(false);
			}
		},
		INTERFACE
	);

	public static final Module ESP = get("ESP");
	public static final Module TRACERS = get("Tracers");
	public static final Module NAMETAGS = get("Nametags");
	public static final Module LIGHT = get("LightDebug");
	public static final Module STORAGE = get("StorageESP");
	public static final Module HITBOXES = get("Hitboxes");
	public static final Module HUD = get("HUD");
	public static final Module SPOTIFY = get("Spotify");
	public static final Module ORES = get("OreESP");
	public static final Module WAYPOINTS = get("Waypoints");
	public static final Module RADAR = get("Radar");
	public static final Module ARMOR = get("ArmorHUD");
	public static final Module KEYSTROKES = get("Keystrokes");
	public static final Module BREAD = get("Breadcrumbs");
	public static final Module COMPASS = get("Compass");
	public static final Module NOTES = get("Notes");
	public static final Module SMOOTHZOOM = get("SmoothZoom");
	public static final Module EFFECTS = get("Effects");
	public static final Module BLOCKOV = get("BlockOverlay");
	public static final Module JUMPC = get("JumpCircles");
	public static final Module TARGET = get("TargetESP");
	public static final Module WEATHER = get("WeatherNotifier");
	public static final Module SPAWNN = get("SpawnerNotifier");
	public static final Module GRADE = get("ColorGrade");
	public static final Module CROSS = get("Crosshair+");
	public static final Module CHUNKS = get("ChunkBorders");
	public static final Module LOWHP = get("LowHealth");
	public static final Module COUNTERS = get("Counters");
	public static final Module FRAMES = get("FrameGraph");
	public static final Module STREAMER = get("StreamerMode");
	public static final Module HITP = get("HitParticles");
	public static final Module ITEMLAB = get("ItemLabels");
	public static final Module STOPW = get("Stopwatch");
	public static final Module DMG = get("DamageNumbers");

	private ModuleManager() {
	}

	public static Module get(String name) {
		for (Module m : MODULES) {
			if (m.getName().equals(name)) {
				return m;
			}
		}
		throw new IllegalArgumentException("No module " + name);
	}

	public static boolean isOn(String name) {
		return get(name).isEnabled();
	}

	public static void disableAll(Minecraft mc) {
		for (Module m : MODULES) {
			if (m != INTERFACE && m.isEnabled()) {
				m.toggle(mc);
			}
		}
	}

	public static void onJoin(Minecraft mc) {
		for (Module m : MODULES) {
			if (m != INTERFACE && m.isEnabled()) {
				m.onEnable(mc);
			}
		}
	}

	public static void onLeave() {
		Freecam.reset();
		Smooth.reset();
		for (Module m : MODULES) {
			if (m != INTERFACE && !m.persistent()) {
				m.setEnabled(false);
			}
		}
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			return;
		}
		for (Module m : MODULES) {
			if (m != INTERFACE && m.isEnabled()) {
				try {
					m.onTick(mc);
				} catch (RuntimeException ex) {
					m.setEnabled(false);
					Notify.push(m.getName() + " crashed and was turned off", 0xFFFF3B5C);
				}
			}
		}
	}
}
