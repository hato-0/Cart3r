package dev.cart3r.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.lwjgl.glfw.GLFW;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Hud {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int SHADOW = 0xAA000000;
	private static final double[] TMP = new double[2];

	private static final int[] ORE_COLORS = {
		0xFFFFFFFF, 0xFF4DD8FF, 0xFFB06A3B, 0xFF45E36B, 0xFFFFD33D, 0xFFE0C3A8, 0xFFB0B0C0
	};
	private static final String[] ORE_SETTINGS = {"", "Diamond", "Debris", "Emerald", "Gold", "Iron", "Others"};
	private static final Map<Block, Integer> ORE = new HashMap<>();

	static {
		putOre(1, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
		putOre(2, Blocks.ANCIENT_DEBRIS);
		putOre(3, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
		putOre(4, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
		putOre(5, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
		putOre(6, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
			Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
			Blocks.NETHER_QUARTZ_ORE);
	}

	private static void putOre(int kind, Block... blocks) {
		for (Block b : blocks) {
			ORE.put(b, kind);
		}
	}

	private static final class Tgt {
		final Entity e;
		final int kind;
		final int color;

		Tgt(Entity e, int kind, int color) {
			this.e = e;
			this.kind = kind;
			this.color = color;
		}
	}

	private record Store(int x, int y, int z, int color, String label, double d2) {
	}

	private static final List<Tgt> targets = new ArrayList<>();
	private static List<Store> stores = new ArrayList<>();
	private static List<int[]> darkSpots = new ArrayList<>();
	private static List<int[]> ores = new ArrayList<>();
	private static List<int[]> oreNext = new ArrayList<>();
	private static int oreLayer = 0;
	private static int oreBx;
	private static int oreBy;
	private static int oreBz;
	private static int oreR;

	private Hud() {
	}

	private static double sq(double v) {
		return v * v;
	}

	private static boolean skip(Minecraft mc, Entity e) {
		return e == mc.getCameraEntity() || (e == mc.player && !Freecam.active());
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			return;
		}
		int t = mc.player.tickCount;

		boolean ent = ModuleManager.ESP.isEnabled() || ModuleManager.TRACERS.isEnabled()
			|| ModuleManager.NAMETAGS.isEnabled() || ModuleManager.HITBOXES.isEnabled()
			|| ModuleManager.RADAR.isEnabled();
		if (ent) {
			if (t % 2 == 0) {
				refreshTargets(mc);
			}
		} else if (!targets.isEmpty()) {
			targets.clear();
		}

		if (ModuleManager.LIGHT.isEnabled()) {
			if (t % 20 == 0) {
				scanLight(mc);
			}
		} else if (!darkSpots.isEmpty()) {
			darkSpots = new ArrayList<>();
		}

		if (ModuleManager.STORAGE.isEnabled()) {
			if (t % 40 == 0) {
				scanStorage(mc);
			}
		} else if (!stores.isEmpty()) {
			stores = new ArrayList<>();
		}

		if (ModuleManager.ORES.isEnabled()) {
			scanOre(mc);
		} else if (!ores.isEmpty() || oreLayer != 0) {
			ores = new ArrayList<>();
			oreNext = new ArrayList<>();
			oreLayer = 0;
		}

		if (ModuleManager.BREAD.isEnabled()) {
			if (t % 4 == 0) {
				double bx = mc.player.getX();
				double by = mc.player.getY() + 0.1;
				double bz = mc.player.getZ();
				double[] last = trail.isEmpty() ? null : trail.get(trail.size() - 1);
				if (last == null || sq(bx - last[0]) + sq(by - last[1]) + sq(bz - last[2]) > 0.64) {
					trail.add(new double[] {bx, by, bz});
				}
				int max = (int) ModuleManager.BREAD.getNum("Length");
				while (trail.size() > max) {
					trail.remove(0);
				}
			}
		} else if (!trail.isEmpty()) {
			trail.clear();
		}

		if (ModuleManager.HUD.isEnabled()) {
			double dx = mc.player.getX() - lastX;
			double dz = mc.player.getZ() - lastZ;
			speedVal += (Math.hypot(dx, dz) * 20.0 - speedVal) * 0.2;
		}
		lastX = mc.player.getX();
		lastZ = mc.player.getZ();

		if (ModuleManager.JUMPC.isEnabled()) {
			boolean air = !mc.player.onGround();
			if (wasGround && air && mc.player.getDeltaMovement().y > 0.2) {
				circles.add(new double[] {mc.player.getX(), mc.player.getY() + 0.05, mc.player.getZ(), System.currentTimeMillis()});
				while (circles.size() > 8) {
					circles.remove(0);
				}
			}
			wasGround = !air;
		} else if (!circles.isEmpty()) {
			circles.clear();
		}

		if (ModuleManager.WEATHER.isEnabled()) {
			boolean rain = mc.level.isRaining();
			boolean thunder = mc.level.isThundering();
			if (weatherInit) {
				if (rain != rainWas) {
					Notify.push(rain ? "Rain started" : "Rain stopped", 0xFF4DD8FF);
				}
				if (thunder != thunderWas) {
					Notify.push(thunder ? "Thunderstorm started" : "Thunderstorm ended", 0xFFFFD33D);
				}
			}
			rainWas = rain;
			thunderWas = thunder;
			weatherInit = true;
		} else {
			weatherInit = false;
		}

		if (ModuleManager.SPAWNN.isEnabled()) {
			if (t % 40 == 20) {
				scanSpawners(mc);
			}
		} else if (!seen.isEmpty()) {
			seen.clear();
		}

		if (ModuleManager.SPOTIFY.isEnabled()) {
			Spotify.poll();
		}
	}

	private static void refreshTargets(Minecraft mc) {
		targets.clear();
		double max = 0;
		if (ModuleManager.ESP.isEnabled()) {
			max = Math.max(max, ModuleManager.ESP.getNum("Range"));
		}
		if (ModuleManager.TRACERS.isEnabled()) {
			max = Math.max(max, ModuleManager.TRACERS.getNum("Range"));
		}
		if (ModuleManager.NAMETAGS.isEnabled()) {
			max = Math.max(max, ModuleManager.NAMETAGS.getNum("Range"));
		}
		if (ModuleManager.HITBOXES.isEnabled()) {
			max = Math.max(max, ModuleManager.HITBOXES.getNum("Range"));
		}
		if (ModuleManager.RADAR.isEnabled()) {
			max = Math.max(max, ModuleManager.RADAR.getNum("Range"));
		}
		double r2 = max * max;
		boolean others = ModuleManager.HITBOXES.isEnabled() && ModuleManager.HITBOXES.getBool("Others");
		Entity cam = mc.getCameraEntity();
		Vec3 from = cam != null ? cam.position() : mc.player.position();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (skip(mc, e)) {
				continue;
			}
			int kind = Targets.kind(e);
			if (kind == Targets.NONE && !others) {
				continue;
			}
			if (e.distanceToSqr(from) > r2) {
				continue;
			}
			targets.add(new Tgt(e, kind, kind == Targets.NONE ? WHITE : Targets.color(kind)));
			if (targets.size() >= 250) {
				break;
			}
		}
	}

	private static void scanLight(Minecraft mc) {
		int r = (int) Math.min(12, ModuleManager.LIGHT.getNum("Radius"));
		boolean night = ModuleManager.LIGHT.getBool("Night spots");
		BlockPos base = mc.player.blockPosition();
		List<int[]> next = new ArrayList<>();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				for (int dy = -4; dy <= 3; dy++) {
					pos.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
					BlockState state = mc.level.getBlockState(pos);
					if (!state.isAir()) {
						continue;
					}
					below.set(pos.getX(), pos.getY() - 1, pos.getZ());
					BlockState floor = mc.level.getBlockState(below);
					if (!floor.isFaceSturdy(mc.level, below, Direction.UP)) {
						continue;
					}
					if (mc.level.getBrightness(LightLayer.BLOCK, pos) > 0) {
						continue;
					}
					int total = mc.level.getMaxLocalRawBrightness(pos);
					if (total == 0) {
						next.add(new int[] {pos.getX(), pos.getY(), pos.getZ(), 0xFFFF3B3B});
					} else if (night) {
						next.add(new int[] {pos.getX(), pos.getY(), pos.getZ(), 0xFFFFD33D});
					}
					if (next.size() >= 500) {
						darkSpots = next;
						return;
					}
				}
			}
		}
		darkSpots = next;
	}

	private static void scanStorage(Minecraft mc) {
		List<Store> next = new ArrayList<>();
		boolean single = mc.isSingleplayer();
		Module m = ModuleManager.STORAGE;
		int range = (int) m.getNum("Range");
		double r2 = (double) range * range;
		int pcx = mc.player.getBlockX() >> 4;
		int pcz = mc.player.getBlockZ() >> 4;
		int cr = range / 16 + 1;
		BlockPos me = mc.player.blockPosition();
		for (int cx = pcx - cr; cx <= pcx + cr; cx++) {
			for (int cz = pcz - cr; cz <= pcz + cr; cz++) {
				if (!mc.level.hasChunk(cx, cz)) {
					continue;
				}
				LevelChunk chunk = mc.level.getChunk(cx, cz);
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					BlockPos p = be.getBlockPos();
					double d2 = p.distSqr(me);
					if (d2 > r2) {
						continue;
					}
					BlockState bs = be.getBlockState();
					int color;
					String label;
					if (bs.is(Blocks.TRIAL_SPAWNER)) {
						if (!m.getBool("Trials")) {
							continue;
						}
						color = 0xFF4DD8FF;
						label = "Trial spawner";
					} else if (bs.is(Blocks.VAULT)) {
						if (!m.getBool("Vaults")) {
							continue;
						}
						color = 0xFF45E36B;
						label = "Vault";
					} else if (!single) {
						continue;
					} else if (be instanceof SpawnerBlockEntity) {
						if (!m.getBool("Spawners")) {
							continue;
						}
						color = 0xFFFF3B3B;
						label = "Spawner";
					} else if (be instanceof EnderChestBlockEntity) {
						if (!m.getBool("Ender")) {
							continue;
						}
						color = 0xFFB45CFF;
						label = "Ender chest";
					} else if (be instanceof ShulkerBoxBlockEntity) {
						if (!m.getBool("Chests")) {
							continue;
						}
						color = 0xFFFF7ADA;
						label = "Shulker";
					} else if (be instanceof BarrelBlockEntity) {
						if (!m.getBool("Chests")) {
							continue;
						}
						color = 0xFFC98B4A;
						label = "Barrel";
					} else if (be instanceof ChestBlockEntity) {
						if (!m.getBool("Chests")) {
							continue;
						}
						color = 0xFFFFA43A;
						label = "Chest";
					} else {
						continue;
					}
					next.add(new Store(p.getX(), p.getY(), p.getZ(), color, label, d2));
				}
			}
		}
		next.sort(Comparator.comparingDouble(Store::d2));
		stores = next.size() > 150 ? new ArrayList<>(next.subList(0, 150)) : next;
	}

	private static void scanOre(Minecraft mc) {
		Module m = ModuleManager.ORES;
		if (oreLayer == 0) {
			oreBx = mc.player.getBlockX();
			oreBy = mc.player.getBlockY();
			oreBz = mc.player.getBlockZ();
			oreR = (int) m.getNum("Radius");
			oreNext = new ArrayList<>();
		}
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int n = 0; n < 4 && oreLayer <= 2 * oreR; n++, oreLayer++) {
			int y = oreBy - oreR + oreLayer;
			for (int dx = -oreR; dx <= oreR; dx++) {
				for (int dz = -oreR; dz <= oreR; dz++) {
					pos.set(oreBx + dx, y, oreBz + dz);
					Integer kind = ORE.get(mc.level.getBlockState(pos).getBlock());
					if (kind != null && m.getBool(ORE_SETTINGS[kind])) {
						oreNext.add(new int[] {pos.getX(), pos.getY(), pos.getZ(), kind});
					}
				}
			}
		}
		if (oreLayer > 2 * oreR) {
			final int px = mc.player.getBlockX();
			final int py = mc.player.getBlockY();
			final int pz = mc.player.getBlockZ();
			oreNext.sort(Comparator.comparingInt(o -> {
				int dx = o[0] - px;
				int dy = o[1] - py;
				int dz = o[2] - pz;
				return dx * dx + dy * dy + dz * dz;
			}));
			ores = oreNext;
			oreNext = new ArrayList<>();
			oreLayer = 0;
		}
	}

	public static void render(GuiGraphics g, float pt) {
		Theme.beginFrame();
		Minecraft mc = Minecraft.getInstance();
		Freecam.frame(mc);
		if (mc.player == null || mc.level == null || mc.options.hideGui) {
			return;
		}
		if (ModuleManager.GRADE.isEnabled()) {
			drawGrade(g);
		}
		boolean needs = ModuleManager.ESP.isEnabled() || ModuleManager.TRACERS.isEnabled()
			|| ModuleManager.NAMETAGS.isEnabled() || ModuleManager.LIGHT.isEnabled()
			|| ModuleManager.STORAGE.isEnabled() || ModuleManager.HITBOXES.isEnabled()
			|| ModuleManager.ORES.isEnabled() || ModuleManager.WAYPOINTS.isEnabled()
			|| ModuleManager.BREAD.isEnabled() || ModuleManager.BLOCKOV.isEnabled()
			|| ModuleManager.JUMPC.isEnabled() || ModuleManager.TARGET.isEnabled();
		if (needs && Projector.begin(mc, g, pt)) {
			if (ModuleManager.STORAGE.isEnabled()) {
				drawStorage(g, mc);
			}
			if (ModuleManager.ORES.isEnabled()) {
				drawOres(g);
			}
			if (ModuleManager.BREAD.isEnabled()) {
				drawTrail(g);
			}
			if (ModuleManager.BLOCKOV.isEnabled()) {
				drawBlockOverlay(g, mc);
			}
			if (ModuleManager.JUMPC.isEnabled()) {
				drawJumpCircles(g);
			}
			if (ModuleManager.TARGET.isEnabled()) {
				drawTarget(g, mc, pt);
			}
			if (ModuleManager.WAYPOINTS.isEnabled()) {
				drawWaypoints(g, mc);
			}
			if (ModuleManager.LIGHT.isEnabled()) {
				drawLight(g);
			}
			if (ModuleManager.HITBOXES.isEnabled()) {
				drawHitboxes(g, pt);
			}
			if (ModuleManager.ESP.isEnabled()) {
				drawEsp(g, mc, pt);
			}
			if (ModuleManager.NAMETAGS.isEnabled()) {
				drawNametags(g, mc, pt);
			}
			if (ModuleManager.TRACERS.isEnabled()) {
				drawTracers(g, mc, pt);
			}
		}
		if (ModuleManager.RADAR.isEnabled()) {
			drawRadar(g, mc, pt);
		}
		if (ModuleManager.ARMOR.isEnabled()) {
			drawArmor(g, mc);
		}
		if (ModuleManager.HUD.isEnabled()) {
			drawInfo(g, mc);
		}
		if (ModuleManager.SPOTIFY.isEnabled()) {
			drawSpotify(g, mc);
		}
		if (ModuleManager.CROSS.isEnabled()) {
			drawCrosshair(g, mc);
		}
		if (ModuleManager.KEYSTROKES.isEnabled()) {
			drawKeys(g, mc);
		}
		if (ModuleManager.COMPASS.isEnabled()) {
			drawCompass(g, mc);
		}
		if (ModuleManager.EFFECTS.isEnabled()) {
			drawEffects(g, mc);
		}
		Notify.draw(g, mc.font);
	}

	private static void outline(GuiGraphics g, int x1, int y1, int x2, int y2, int c) {
		g.fill(x1, y1, x2, y1 + 1, c);
		g.fill(x1, y2 - 1, x2, y2, c);
		g.fill(x1, y1, x1 + 1, y2, c);
		g.fill(x2 - 1, y1, x2, y2, c);
	}

	private static void corners(GuiGraphics g, int x1, int y1, int x2, int y2, int c) {
		int lx = Math.max(3, (x2 - x1) / 4);
		int ly = Math.max(3, (y2 - y1) / 4);
		g.fill(x1, y1, x1 + lx, y1 + 1, c);
		g.fill(x1, y1, x1 + 1, y1 + ly, c);
		g.fill(x2 - lx, y1, x2, y1 + 1, c);
		g.fill(x2 - 1, y1, x2, y1 + ly, c);
		g.fill(x1, y2 - 1, x1 + lx, y2, c);
		g.fill(x1, y2 - ly, x1 + 1, y2, c);
		g.fill(x2 - lx, y2 - 1, x2, y2, c);
		g.fill(x2 - 1, y2 - ly, x2, y2, c);
	}

	private static boolean onScreen(int x1, int y1, int x2, int y2) {
		return x2 >= 0 && y2 >= 0 && x1 <= Projector.w && y1 <= Projector.h && x2 - x1 <= Projector.w * 2;
	}

	private static void drawStorage(GuiGraphics g, Minecraft mc) {
		Font f = mc.font;
		boolean labels = ModuleManager.STORAGE.getBool("Labels");
		int n = 0;
		for (Store s : stores) {
			if (s.d2() < 1600.0) {
				if (!Projector.bounds(s.x(), s.y(), s.z(), s.x() + 1, s.y() + 1, s.z() + 1)) {
					continue;
				}
				int x1 = (int) Math.floor(Projector.box[0]);
				int y1 = (int) Math.floor(Projector.box[1]);
				int x2 = (int) Math.ceil(Projector.box[2]);
				int y2 = (int) Math.ceil(Projector.box[3]);
				if (!onScreen(x1, y1, x2, y2)) {
					continue;
				}
				outline(g, x1 - 1, y1 - 1, x2 + 1, y2 + 1, SHADOW);
				outline(g, x1, y1, x2, y2, s.color());
				if (labels) {
					g.drawString(f, s.label(), x1, y1 - 10, s.color(), true);
				}
			} else {
				if (!Projector.project(s.x() + 0.5, s.y() + 0.5, s.z() + 0.5, TMP)) {
					continue;
				}
				int x = (int) TMP[0];
				int y = (int) TMP[1];
				if (x < 0 || y < 0 || x > Projector.w || y > Projector.h) {
					continue;
				}
				g.fill(x - 3, y - 3, x + 4, y + 4, SHADOW);
				g.fill(x - 2, y - 2, x + 3, y + 3, s.color());
			}
			if (++n >= 120) {
				break;
			}
		}
	}

	private static void drawOres(GuiGraphics g) {
		int n = 0;
		for (int[] o : ores) {
			double dx = o[0] + 0.5 - Projector.eye.x;
			double dy = o[1] + 0.5 - Projector.eye.y;
			double dz = o[2] + 0.5 - Projector.eye.z;
			double d2 = dx * dx + dy * dy + dz * dz;
			int color = ORE_COLORS[o[3]];
			if (d2 < 484.0) {
				if (!Projector.bounds(o[0], o[1], o[2], o[0] + 1, o[1] + 1, o[2] + 1)) {
					continue;
				}
				int x1 = (int) Math.floor(Projector.box[0]);
				int y1 = (int) Math.floor(Projector.box[1]);
				int x2 = (int) Math.ceil(Projector.box[2]);
				int y2 = (int) Math.ceil(Projector.box[3]);
				if (!onScreen(x1, y1, x2, y2)) {
					continue;
				}
				g.fill(x1, y1, x2, y2, Theme.alpha(color, 0.18));
				outline(g, x1, y1, x2, y2, color);
			} else {
				if (!Projector.project(o[0] + 0.5, o[1] + 0.5, o[2] + 0.5, TMP)) {
					continue;
				}
				int x = (int) TMP[0];
				int y = (int) TMP[1];
				if (x < 0 || y < 0 || x > Projector.w || y > Projector.h) {
					continue;
				}
				g.fill(x - 1, y - 1, x + 2, y + 2, color);
			}
			if (++n >= 150) {
				break;
			}
		}
	}

	private static void drawWaypoints(GuiGraphics g, Minecraft mc) {
		Font f = mc.font;
		boolean dist = ModuleManager.WAYPOINTS.getBool("Distance");
		String dim = mc.level.dimension().toString();
		int accent = Theme.accent();
		for (Waypoints.Point p : Waypoints.LIST) {
			if (!p.dim().equals(dim)) {
				continue;
			}
			if (!Projector.project(p.x() + 0.5, p.y() + 1.0, p.z() + 0.5, TMP)) {
				continue;
			}
			int x = (int) TMP[0];
			int y = (int) TMP[1];
			if (x < -60 || y < -20 || x > Projector.w + 60 || y > Projector.h + 20) {
				continue;
			}
			g.fill(x, y - 5, x + 1, y - 4, accent);
			g.fill(x - 1, y - 4, x + 2, y - 3, accent);
			g.fill(x - 2, y - 3, x + 3, y - 2, accent);
			g.fill(x - 3, y - 2, x + 4, y - 1, accent);
			g.fill(x - 2, y - 1, x + 3, y, accent);
			g.fill(x - 1, y, x + 2, y + 1, accent);
			g.fill(x, y + 1, x + 1, y + 2, accent);
			String label = p.name();
			if (dist) {
				double dx = p.x() - Projector.eye.x;
				double dy = p.y() - Projector.eye.y;
				double dz = p.z() - Projector.eye.z;
				label += " " + (int) Math.sqrt(dx * dx + dy * dy + dz * dz) + "m";
			}
			int w = f.width(label);
			Gfx.round(g, x - w / 2 - 3, y - 18, x + w / 2 + 3, y - 7, 2, 0xA0000000);
			g.drawString(f, label, x - w / 2, y - 16, WHITE, false);
		}
	}

	private static void drawLight(GuiGraphics g) {
		for (int[] s : darkSpots) {
			if (!Projector.project(s[0] + 0.5, s[1] + 0.05, s[2] + 0.5, TMP)) {
				continue;
			}
			int x = (int) TMP[0];
			int y = (int) TMP[1];
			if (x < 0 || y < 0 || x > Projector.w || y > Projector.h) {
				continue;
			}
			g.fill(x - 2, y - 1, x + 3, y + 2, s[3]);
		}
	}

	private static final double[][] HB_C = new double[8][2];
	private static final boolean[] HB_VIS = new boolean[8];
	private static final double[][] HB_Q = new double[4][2];
	private static final List<double[]> trail = new ArrayList<>();
	private static final List<double[]> circles = new ArrayList<>();
	private static final java.util.Set<Long> seen = new java.util.HashSet<>();
	private static final int[] PAL = {0, 0xFFFFFFFF, 0xFFFF3B3B, 0xFF45E36B, 0xFF3D8BFF, 0xFFFFD33D, 0xFFFF4FA3};
	private static final int[] GRADES = {0xFFFFA040, 0xFF4090FF, 0xFFFF6A3D, 0xFF40FFB0, 0xFF1A2A6A, 0xFFFF6AA0};
	private static boolean wasGround = true;
	private static boolean weatherInit;
	private static boolean rainWas;
	private static boolean thunderWas;
	private static float crossDyn;
	private static final java.util.ArrayDeque<Long> clicks = new java.util.ArrayDeque<>();
	private static boolean cpsWas;
	private static double speedVal;
	private static double lastX;
	private static double lastZ;

	private static final int[][] EDGES = {
		{0, 1}, {2, 3}, {4, 5}, {6, 7},
		{0, 2}, {1, 3}, {4, 6}, {5, 7},
		{0, 4}, {1, 5}, {2, 6}, {3, 7}
	};

	private static void edge(GuiGraphics g, double x1, double y1, double x2, double y2, int color) {
		int w = Projector.w;
		int h = Projector.h;
		if ((x1 < 0 && x2 < 0) || (y1 < 0 && y2 < 0) || (x1 > w && x2 > w) || (y1 > h && y2 > h)) {
			return;
		}
		Gfx.line(g, x1, y1, x2, y2, 1.5f, color);
	}

	private static void drawHitboxes(GuiGraphics g, float pt) {
		Module m = ModuleManager.HITBOXES;
		double r2 = sq(m.getNum("Range"));
		boolean eye = m.getBool("Eye line");
		double[][] c = HB_C;
		boolean[] vis = HB_VIS;
		int shown = 0;
		for (Tgt t : targets) {
			Entity e = t.e;
			if (e.isRemoved()) {
				continue;
			}
			if (t.kind == Targets.NONE) {
				if (!m.getBool("Others")) {
					continue;
				}
			} else if (!Targets.allowed(m, t.kind)) {
				continue;
			}
			if (e.distanceToSqr(Projector.eye) > r2) {
				continue;
			}
			Vec3 p = e.getPosition(pt);
			double hw = e.getBbWidth() / 2.0;
			double x0 = p.x - hw;
			double x1 = p.x + hw;
			double z0 = p.z - hw;
			double z1 = p.z + hw;
			double y0 = p.y;
			double y1 = p.y + e.getBbHeight();
			for (int i = 0; i < 8; i++) {
				double x = (i & 1) == 0 ? x0 : x1;
				double y = (i & 2) == 0 ? y0 : y1;
				double z = (i & 4) == 0 ? z0 : z1;
				vis[i] = Projector.project(x, y, z, TMP);
				c[i][0] = TMP[0];
				c[i][1] = TMP[1];
			}
			for (int[] ed : EDGES) {
				if (vis[ed[0]] && vis[ed[1]]) {
					edge(g, c[ed[0]][0], c[ed[0]][1], c[ed[1]][0], c[ed[1]][1], t.color);
				}
			}
			if (eye) {
				double ey = p.y + e.getEyeHeight();
				double[] xs = {x0, x1, x1, x0};
				double[] zs = {z0, z0, z1, z1};
				double[][] q = HB_Q;
				boolean ok = true;
				for (int i = 0; i < 4; i++) {
					if (!Projector.project(xs[i], ey, zs[i], TMP)) {
						ok = false;
						break;
					}
					q[i][0] = TMP[0];
					q[i][1] = TMP[1];
				}
				if (ok) {
					for (int i = 0; i < 4; i++) {
						int j = (i + 1) % 4;
						edge(g, q[i][0], q[i][1], q[j][0], q[j][1], 0xFFFF3B3B);
					}
				}
			}
			if (++shown >= 60) {
				break;
			}
		}
	}

	private static void drawEsp(GuiGraphics g, Minecraft mc, float pt) {
		Module m = ModuleManager.ESP;
		double r2 = sq(m.getNum("Range"));
		boolean corner = m.getNum("Style") >= 0.5;
		boolean hp = m.getBool("Health bar");
		boolean names = m.getBool("Names");
		boolean fill = m.getBool("Fill");
		int shown = 0;
		for (Tgt t : targets) {
			Entity e = t.e;
			if (e.isRemoved() || t.kind == Targets.NONE || !Targets.allowed(m, t.kind)) {
				continue;
			}
			double d2 = e.distanceToSqr(Projector.eye);
			if (d2 > r2) {
				continue;
			}
			Vec3 p = e.getPosition(pt);
			double hw = e.getBbWidth() / 2.0;
			if (!Projector.bounds(p.x - hw, p.y, p.z - hw, p.x + hw, p.y + e.getBbHeight(), p.z + hw)) {
				continue;
			}
			int x1 = (int) Math.floor(Projector.box[0]);
			int y1 = (int) Math.floor(Projector.box[1]);
			int x2 = (int) Math.ceil(Projector.box[2]);
			int y2 = (int) Math.ceil(Projector.box[3]);
			if (!onScreen(x1, y1, x2, y2)) {
				continue;
			}
			int color = t.color;
			if (fill) {
				g.fill(x1, y1, x2, y2, Theme.alpha(color, 0.12));
			}
			if (corner) {
				corners(g, x1 - 1, y1 - 1, x2 + 1, y2 + 1, SHADOW);
				corners(g, x1, y1, x2, y2, color);
			} else {
				outline(g, x1 - 1, y1 - 1, x2 + 1, y2 + 1, SHADOW);
				outline(g, x1, y1, x2, y2, color);
			}
			if (hp && e instanceof LivingEntity le && le.getMaxHealth() > 0) {
				float fr = Math.max(0f, Math.min(1f, le.getHealth() / le.getMaxHealth()));
				int bar = (int) ((y2 - y1) * fr);
				int hc = fr > 0.5f ? 0xFF45E36B : (fr > 0.25f ? 0xFFFFD33D : 0xFFFF3B3B);
				g.fill(x1 - 5, y1 - 1, x1 - 2, y2 + 1, SHADOW);
				g.fill(x1 - 4, y2 - bar, x1 - 3, y2, hc);
			}
			if (names) {
				String label = e.getName().getString() + " " + (int) Math.sqrt(d2) + "m";
				g.drawString(mc.font, label, x1, y1 - 10, color, true);
			}
			if (++shown >= 80) {
				break;
			}
		}
	}

	private static void drawNametags(GuiGraphics g, Minecraft mc, float pt) {
		Module m = ModuleManager.NAMETAGS;
		double r2 = sq(m.getNum("Range"));
		boolean hp = m.getBool("Health");
		boolean dist = m.getBool("Distance");
		int shown = 0;
		for (Tgt t : targets) {
			Entity e = t.e;
			if (e.isRemoved() || t.kind == Targets.NONE || !Targets.allowed(m, t.kind)) {
				continue;
			}
			double d2 = e.distanceToSqr(Projector.eye);
			if (d2 > r2) {
				continue;
			}
			Vec3 p = e.getPosition(pt);
			if (!Projector.project(p.x, p.y + e.getBbHeight() + 0.4, p.z, TMP)) {
				continue;
			}
			StringBuilder sb = new StringBuilder(e.getName().getString());
			if (hp && e instanceof LivingEntity le) {
				sb.append("  ").append(Math.round(le.getHealth())).append("hp");
			}
			if (dist) {
				sb.append("  ").append((int) Math.sqrt(d2)).append("m");
			}
			String s = sb.toString();
			int w = mc.font.width(s);
			int x = (int) TMP[0] - w / 2;
			int y = (int) TMP[1] - 10;
			Gfx.round(g, x - 4, y - 3, x + w + 4, y + 11, 3, 0xB0000000);
			g.fill(x - 2, y + 10, x + w + 2, y + 11, t.color);
			g.drawString(mc.font, s, x, y, WHITE, true);
			if (++shown >= 60) {
				break;
			}
		}
	}

	private static void drawTracers(GuiGraphics g, Minecraft mc, float pt) {
		Module m = ModuleManager.TRACERS;
		double range = m.getNum("Range");
		double r2 = sq(range);
		boolean bottom = m.getNum("Origin") >= 0.5;
		float th = (float) (1.0 + m.getNum("Thickness") * 0.5);
		boolean showDist = m.getBool("Distance");
		double ox = Projector.cx;
		double oy = bottom ? Projector.h - 2 : Projector.cy;
		double[] o = new double[2];
		int shown = 0;
		for (Tgt t : targets) {
			Entity e = t.e;
			if (e.isRemoved() || t.kind == Targets.NONE || !Targets.allowed(m, t.kind)) {
				continue;
			}
			double d2 = e.distanceToSqr(Projector.eye);
			if (d2 > r2) {
				continue;
			}
			Vec3 c = e.getPosition(pt).add(0, e.getBbHeight() / 2.0, 0);
			Projector.project(c.x, c.y, c.z, o);
			Projector.clamp(o, 6);
			if (Math.hypot(o[0] - ox, o[1] - oy) < 8) {
				continue;
			}
			double dist = Math.sqrt(d2);
			double fade = Math.max(0.3, 1.0 - dist / range * 0.7);
			int col = Theme.alpha(t.color, fade);
			Gfx.line(g, ox, oy, o[0], o[1], th, col);
			int ex = (int) o[0];
			int ey = (int) o[1];
			g.fill(ex - 2, ey - 2, ex + 3, ey + 3, col);
			if (showDist) {
				g.drawString(mc.font, (int) dist + "m", ex + 5, ey - 4, col, true);
			}
			if (++shown >= 40) {
				break;
			}
		}
	}

	private static void drawRadar(GuiGraphics g, Minecraft mc, float pt) {
		Module m = ModuleManager.RADAR;
		Entity cam = mc.getCameraEntity();
		if (cam == null) {
			return;
		}
		int size = (int) m.getNum("Size");
		int r = size / 2;
		double range = m.getNum("Range");
		int cx = g.guiWidth() - r - 8;
		int cy = g.guiHeight() - r - 34;
		int accent = Theme.accent();

		Gfx.circle(g, cx, cy, r + 1, Theme.alpha(accent, 0.8));
		Gfx.circle(g, cx, cy, r, 0xC0080810);
		g.fill(cx - r, cy, cx + r, cy + 1, 0x30FFFFFF);
		g.fill(cx, cy - r, cx + 1, cy + r, 0x30FFFFFF);

		Vec3 me = cam.getPosition(pt);
		double yaw = Math.toRadians(cam.getViewYRot(pt));
		double s = Math.sin(yaw);
		double c = Math.cos(yaw);

		double nf = -c;
		double nr = s;
		g.drawString(mc.font, "N", (int) (cx + nr * r * 0.82) - 2, (int) (cy - nf * r * 0.82) - 4, accent, false);

		for (Tgt t : targets) {
			Entity e = t.e;
			if (e.isRemoved() || t.kind == Targets.NONE || !Targets.allowed(m, t.kind)) {
				continue;
			}
			Vec3 p = e.getPosition(pt);
			double rx = p.x - me.x;
			double rz = p.z - me.z;
			double fwd = rx * -s + rz * c;
			double rgt = rx * -c + rz * -s;
			double d = Math.hypot(fwd, rgt);
			if (d > range) {
				continue;
			}
			int x = (int) (cx + rgt / range * r);
			int y = (int) (cy - fwd / range * r);
			g.fill(x - 1, y - 1, x + 2, y + 2, t.color);
		}
		g.fill(cx - 1, cy - 1, cx + 2, cy + 2, WHITE);
		g.fill(cx, cy - 5, cx + 1, cy - 2, WHITE);
	}

	private static void drawArmor(GuiGraphics g, Minecraft mc) {
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		int x = g.guiWidth() / 2 + 100;
		int y = g.guiHeight() - 20;
		for (EquipmentSlot slot : slots) {
			ItemStack s = mc.player.getItemBySlot(slot);
			if (s.isEmpty()) {
				continue;
			}
			g.renderItem(s, x, y);
			g.renderItemDecorations(mc.font, s, x, y);
			x += 18;
		}
	}

	private static void drawInfo(GuiGraphics g, Minecraft mc) {
		Module hud = ModuleManager.HUD;
		Font f = mc.font;
		int accent = Theme.accent();

		List<String> lines = new ArrayList<>();
		if (hud.getBool("FPS")) {
			lines.add(mc.getFps() + " FPS");
		}
		if (hud.getBool("Coords")) {
			lines.add(String.format("%.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
		}
		if (hud.getBool("Speed")) {
			lines.add(String.format("%.1f b/s", speedVal));
		}
		if (hud.getBool("Ping") && mc.getConnection() != null) {
			PlayerInfo pi = mc.getConnection().getPlayerInfo(mc.player.getUUID());
			if (pi != null) {
				lines.add(pi.getLatency() + " ms");
			}
		}
		if (hud.getBool("Time")) {
			lines.add(java.time.LocalTime.now().format(CLOCK));
		}
		String prof = (hud.getBool("Profile") && !Profiles.active.isEmpty()) ? Profiles.active : "";
		int w = f.width("CART3R") + (prof.isEmpty() ? 0 : f.width(prof) + 6);
		for (String s : lines) {
			w = Math.max(w, f.width(s));
		}
		int h = 12 + lines.size() * 11;
		Gfx.round(g, 4, 4, 4 + w + 12, 4 + h, 3, Theme.hudBg());
		g.fill(4, 7, 6, 4 + h - 3, accent);
		g.drawString(f, "CART3R", 11, 7, accent, false);
		if (!prof.isEmpty()) {
			g.drawString(f, prof, 11 + f.width("CART3R") + 6, 7, Theme.MUTED, false);
		}
		int y = 18;
		for (String s : lines) {
			g.drawString(f, s, 11, y, WHITE, false);
			y += 11;
		}

		if (hud.getBool("Modules")) {
			List<Module> list = new ArrayList<>();
			for (Module m : ModuleManager.MODULES) {
				float target = (m.isEnabled() && m != ModuleManager.INTERFACE && m != hud) ? 1f : 0f;
				m.hudAnim = Theme.approach(m.hudAnim, target, 14f);
				if (m.hudAnim > 0.02f) {
					list.add(m);
				}
			}
			list.sort(Comparator.comparingInt(m -> -f.width(m.getName())));
			int right = g.guiWidth() - 4;
			int ry = 4;
			for (Module m : list) {
				int mw = f.width(m.getName());
				float an = m.hudAnim;
				int off = (int) ((1f - an) * (mw + 14));
				Gfx.round(g, right - mw - 8 + off, ry, right + off, ry + 11, 2, Theme.alpha(Theme.hudBg(), an));
				g.fill(right - 2 + off, ry, right + off, ry + 11, Theme.alpha(accent, an));
				g.drawString(f, m.getName(), right - mw - 5 + off, ry + 2, Theme.alpha(accent, an), false);
				ry += Math.round(12 * an);
			}
		}
	}

	private static final java.time.format.DateTimeFormatter CLOCK = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
	private static final int[] KEYCODES = {GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_SPACE};
	private static final String[] KEYLABELS = {"W", "A", "S", "D", ""};
	private static final int[][] KEYPOS = {{20, 0, 18, 18}, {0, 20, 18, 18}, {20, 20, 18, 18}, {40, 20, 18, 18}, {0, 40, 58, 12}};
	private static final int[][] MOUSEPOS = {{0, 54, 28, 16}, {30, 54, 28, 16}};
	private static final float[] KEYANIM = new float[7];

	private static void keyBox(GuiGraphics g, Font f, int i, int x, int y, int w, int h, boolean down, String label) {
		KEYANIM[i] = Theme.approach(KEYANIM[i], down ? 1f : 0f, 22f);
		int c = Theme.mix(0xA0080810, Theme.accent(), KEYANIM[i] * 0.85f);
		Gfx.round(g, x, y, x + w, y + h, 3, c);
		if (!label.isEmpty()) {
			g.drawString(f, label, x + (w - f.width(label)) / 2, y + (h - 8) / 2 + 1, Theme.mix(Theme.MUTED, WHITE, KEYANIM[i]), false);
		}
	}

	private static void drawKeys(GuiGraphics g, Minecraft mc) {
		Font f = mc.font;
		boolean free = mc.screen == null;
		int ox = g.guiWidth() - 68;
		int oy = g.guiHeight() / 2 + 10;
		for (int i = 0; i < 5; i++) {
			int[] p = KEYPOS[i];
			keyBox(g, f, i, ox + p[0], oy + p[1], p[2], p[3], free && Input.key(KEYCODES[i]), KEYLABELS[i]);
		}
		if (ModuleManager.KEYSTROKES.getBool("Mouse")) {
			int[] a = MOUSEPOS[0];
			int[] b = MOUSEPOS[1];
			keyBox(g, f, 5, ox + a[0], oy + a[1], a[2], a[3], free && Input.left(), "LMB");
			keyBox(g, f, 6, ox + b[0], oy + b[1], b[2], b[3], free && Input.right(), "RMB");
		}
		if (ModuleManager.KEYSTROKES.getBool("CPS")) {
			long now = System.currentTimeMillis();
			boolean down = free && Input.left();
			if (down && !cpsWas) {
				clicks.addLast(now);
			}
			cpsWas = down;
			while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) {
				clicks.pollFirst();
			}
			String s = clicks.size() + " CPS";
			int sy = oy + (ModuleManager.KEYSTROKES.getBool("Mouse") ? 74 : 56);
			g.drawString(f, s, ox + (58 - f.width(s)) / 2, sy, Theme.MUTED, false);
		}
	}

	private static int pick(Module m) {
		int i = (int) m.getNum("Color");
		return i <= 0 || i >= PAL.length ? Theme.accent() : PAL[i];
	}

	private static void scanSpawners(Minecraft mc) {
		Module m = ModuleManager.SPAWNN;
		int range = (int) m.getNum("Range");
		double r2 = (double) range * range;
		boolean single = mc.isSingleplayer();
		int pcx = mc.player.getBlockX() >> 4;
		int pcz = mc.player.getBlockZ() >> 4;
		int cr = range / 16 + 1;
		BlockPos me = mc.player.blockPosition();
		if (seen.size() > 500) {
			seen.clear();
		}
		for (int cx = pcx - cr; cx <= pcx + cr; cx++) {
			for (int cz = pcz - cr; cz <= pcz + cr; cz++) {
				if (!mc.level.hasChunk(cx, cz)) {
					continue;
				}
				for (BlockEntity be : mc.level.getChunk(cx, cz).getBlockEntities().values()) {
					boolean trial = be.getBlockState().is(Blocks.TRIAL_SPAWNER);
					boolean reg = be instanceof SpawnerBlockEntity;
					if (!(trial || (reg && single))) {
						continue;
					}
					BlockPos p = be.getBlockPos();
					double d2 = p.distSqr(me);
					if (d2 > r2 || !seen.add(p.asLong())) {
						continue;
					}
					Notify.push((trial ? "Trial spawner " : "Spawner ") + (int) Math.sqrt(d2) + "m", trial ? 0xFF4DD8FF : 0xFFFF3B3B);
				}
			}
		}
	}

	private static void drawGrade(GuiGraphics g) {
		Module m = ModuleManager.GRADE;
		int w = g.guiWidth();
		int h = g.guiHeight();
		g.fill(0, 0, w, h, Theme.alpha(GRADES[(int) m.getNum("Preset")], m.getNum("Strength")));
		if (m.getBool("Vignette")) {
			int band = (int) (h * 0.28);
			int c = Theme.alpha(0xFF000000, m.getNum("Vignette strength"));
			g.fillGradient(0, 0, w, band, c, 0x00000000);
			g.fillGradient(0, h - band, w, h, 0x00000000, c);
		}
	}

	private static void drawBlockOverlay(GuiGraphics g, Minecraft mc) {
		if (!(mc.hitResult instanceof BlockHitResult bh) || bh.getType() != HitResult.Type.BLOCK) {
			return;
		}
		BlockPos p = bh.getBlockPos();
		if (!Projector.bounds(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 1, p.getZ() + 1)) {
			return;
		}
		int x1 = (int) Math.floor(Projector.box[0]);
		int y1 = (int) Math.floor(Projector.box[1]);
		int x2 = (int) Math.ceil(Projector.box[2]);
		int y2 = (int) Math.ceil(Projector.box[3]);
		if (!onScreen(x1, y1, x2, y2)) {
			return;
		}
		Module m = ModuleManager.BLOCKOV;
		int c = pick(m);
		if (m.getBool("Fill")) {
			g.fill(x1, y1, x2, y2, Theme.alpha(c, m.getNum("Fill opacity")));
		}
		int th = (int) m.getNum("Thickness");
		for (int i = 0; i < th; i++) {
			outline(g, x1 - i, y1 - i, x2 + i, y2 + i, c);
		}
	}

	private static void ring(GuiGraphics g, double x, double y, double z, double r, double spin, int color, boolean dashed) {
		int segs = 28;
		boolean prevOk = false;
		double ppx = 0;
		double ppy = 0;
		for (int i = 0; i <= segs; i++) {
			double a = spin + Math.PI * 2.0 * i / segs;
			boolean ok = Projector.project(x + Math.cos(a) * r, y, z + Math.sin(a) * r, TMP);
			if (ok && prevOk && (!dashed || (i % 4) < 3)) {
				edge(g, ppx, ppy, TMP[0], TMP[1], color);
			}
			prevOk = ok;
			ppx = TMP[0];
			ppy = TMP[1];
		}
	}

	private static void drawJumpCircles(GuiGraphics g) {
		Module m = ModuleManager.JUMPC;
		long now = System.currentTimeMillis();
		double dur = m.getNum("Duration");
		double max = m.getNum("Size");
		int c = pick(m);
		circles.removeIf(o -> now - (long) o[3] > dur);
		for (double[] o : circles) {
			double t = (now - (long) o[3]) / dur;
			double e = 1.0 - Math.pow(1.0 - t, 3);
			ring(g, o[0], o[1], o[2], 0.2 + max * e, 0, Theme.alpha(c, 1.0 - t), false);
		}
	}

	private static void drawTarget(GuiGraphics g, Minecraft mc, float pt) {
		Entity e = mc.crosshairPickEntity;
		if (e == null || e.isRemoved() || Freecam.active()) {
			return;
		}
		Module m = ModuleManager.TARGET;
		int c = pick(m);
		Vec3 p = e.getPosition(pt);
		if (m.getNum("Style") >= 0.5) {
			double hw = e.getBbWidth() / 2.0;
			if (Projector.bounds(p.x - hw, p.y, p.z - hw, p.x + hw, p.y + e.getBbHeight(), p.z + hw)) {
				int x1 = (int) Math.floor(Projector.box[0]);
				int y1 = (int) Math.floor(Projector.box[1]);
				int x2 = (int) Math.ceil(Projector.box[2]);
				int y2 = (int) Math.ceil(Projector.box[3]);
				if (onScreen(x1, y1, x2, y2)) {
					corners(g, x1 - 2, y1 - 2, x2 + 2, y2 + 2, c);
				}
			}
			return;
		}
		long now = System.currentTimeMillis();
		double spin = (now % 2400L) / 2400.0 * Math.PI * 2.0;
		double bob = (Math.sin(now / 350.0) + 1.0) * 0.5 * e.getBbHeight() * 0.9;
		double r = e.getBbWidth() * 0.8 + 0.1;
		ring(g, p.x, p.y + 0.05, p.z, r, spin, c, true);
		ring(g, p.x, p.y + 0.05 + bob, p.z, r * 0.9, -spin, Theme.alpha(c, 0.55), true);
	}

	private static void crossShape(GuiGraphics g, int cx, int cy, int style, int size, int gap, int th, int color) {
		int lo = cy - th / 2;
		int lx = cx - th / 2;
		if (style == 0 || style == 3) {
			g.fill(cx - gap - size, lo, cx - gap, lo + th, color);
			g.fill(cx + gap, lo, cx + gap + size, lo + th, color);
			g.fill(lx, cy - gap - size, lx + th, cy - gap, color);
			g.fill(lx, cy + gap, lx + th, cy + gap + size, color);
		}
		if (style == 1 || style == 3) {
			int d = Math.max(1, th);
			g.fill(cx - d / 2, cy - d / 2, cx - d / 2 + d + 1, cy - d / 2 + d + 1, color);
		}
		if (style == 2) {
			int r = gap + size;
			for (int i = 0; i < 32; i++) {
				double a = Math.PI * 2.0 * i / 32.0;
				int x = cx + (int) Math.round(Math.cos(a) * r);
				int y = cy + (int) Math.round(Math.sin(a) * r);
				g.fill(x, y, x + th, y + th, color);
			}
		}
	}

	private static void drawCrosshair(GuiGraphics g, Minecraft mc) {
		if (mc.screen != null || !mc.options.getCameraType().isFirstPerson()) {
			return;
		}
		Module m = ModuleManager.CROSS;
		double v = Math.hypot(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().z) * 20.0;
		float target = m.getBool("Dynamic") ? (float) Math.min(5.0, v * 0.8) : 0f;
		crossDyn = Theme.approach(crossDyn, target, 12f);
		int cx = g.guiWidth() / 2;
		int cy = g.guiHeight() / 2;
		int style = (int) m.getNum("Style");
		int size = (int) m.getNum("Size");
		int gap = (int) (m.getNum("Gap") + crossDyn);
		int th = (int) m.getNum("Thick");
		if (m.getBool("Outline")) {
			int s = 0xB0000000;
			crossShape(g, cx - 1, cy, style, size, gap, th, s);
			crossShape(g, cx + 1, cy, style, size, gap, th, s);
			crossShape(g, cx, cy - 1, style, size, gap, th, s);
			crossShape(g, cx, cy + 1, style, size, gap, th, s);
		}
		crossShape(g, cx, cy, style, size, gap, th, pick(m));
	}

	private static void drawTrail(GuiGraphics g) {
		int n = trail.size();
		if (n < 2) {
			return;
		}
		boolean prevOk = false;
		double ppx = 0;
		double ppy = 0;
		int accent = Theme.accent();
		for (int i = 0; i < n; i++) {
			double[] p = trail.get(i);
			boolean ok = Projector.project(p[0], p[1], p[2], TMP);
			if (ok && prevOk) {
				edge(g, ppx, ppy, TMP[0], TMP[1], Theme.alpha(accent, 0.2 + 0.8 * i / n));
			}
			prevOk = ok;
			ppx = TMP[0];
			ppy = TMP[1];
		}
	}

	private static void drawCompass(GuiGraphics g, Minecraft mc) {
		Font f = mc.font;
		int cx = g.guiWidth() / 2;
		int y = 32;
		int half = 90;
		int accent = Theme.accent();
		double heading = (((mc.player.getYRot() + 180.0) % 360.0) + 360.0) % 360.0;
		String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
		Gfx.round(g, cx - half - 8, y, cx + half + 8, y + 16, 3, Theme.hudBg());
		for (int i = 0; i < 24; i++) {
			double d = ((i * 15 - heading + 540.0) % 360.0) - 180.0;
			double dx = d * 1.5;
			if (Math.abs(dx) > half) {
				continue;
			}
			double k = 1.0 - Math.abs(dx) / half;
			int x = cx + (int) dx;
			if (i % 3 == 0) {
				String s = names[i / 3];
				int col = i % 6 == 0 ? accent : WHITE;
				g.drawString(f, s, x - f.width(s) / 2, y + 4, Theme.alpha(col, 0.3 + 0.7 * k), false);
			} else {
				g.fill(x, y + 6, x + 1, y + 10, Theme.alpha(Theme.MUTED, k));
			}
		}
		g.fill(cx - 1, y + 13, cx + 2, y + 16, accent);
		String deg = (int) heading + "\u00B0";
		g.drawString(f, deg, cx - f.width(deg) / 2, y + 19, Theme.MUTED, false);
	}

	private static void drawEffects(GuiGraphics g, Minecraft mc) {
		Font f = mc.font;
		int y = 80;
		for (MobEffectInstance e : mc.player.getActiveEffects()) {
			String name = e.getEffect().value().getDisplayName().getString();
			if (e.getAmplifier() > 0) {
				name += " " + (e.getAmplifier() + 1);
			}
			int secs = e.getDuration() / 20;
			String time = e.isInfiniteDuration() ? "--" : String.format("%d:%02d", secs / 60, secs % 60);
			int w = f.width(name) + f.width(time) + 16;
			int c = e.getEffect().value().isBeneficial() ? 0xFF45E36B : 0xFFFF3B3B;
			Gfx.round(g, 4, y, 4 + w, y + 12, 2, Theme.hudBg());
			g.fill(4, y + 1, 6, y + 11, c);
			g.drawString(f, name, 10, y + 2, WHITE, false);
			g.drawString(f, time, 4 + w - 4 - f.width(time), y + 2, Theme.MUTED, false);
			y += 13;
		}
	}

	private static void drawSpotify(GuiGraphics g, Minecraft mc) {
		Font f = mc.font;
		String t = Spotify.track();
		if (t.isEmpty()) {
			t = "Spotify";
		}
		if (t.length() > 46) {
			t = t.substring(0, 46) + "...";
		}
		int w = Math.max(f.width(t), f.width("Now playing"));
		int x = (g.guiWidth() - w) / 2;
		int y = 4;
		Gfx.round(g, x - 12, y, x + w + 8, y + 24, 4, 0xC0080810);
		Gfx.round(g, x - 12, y + 2, x - 9, y + 22, 1, 0xFF1DB954);
		g.drawString(f, "Now playing", x, y + 3, 0xFF1DB954, false);
		g.drawString(f, t, x, y + 13, WHITE, false);
	}
}
