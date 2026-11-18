package dev.cart3r.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Hud {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int SHADOW = 0xAA000000;

	private record Store(AABB box, int color, String label) {
	}

	private static List<int[]> darkSpots = new ArrayList<>();
	private static List<Store> stores = new ArrayList<>();

	private Hud() {
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			return;
		}
		int t = mc.player.tickCount;
		if (ModuleManager.LIGHT.isEnabled()) {
			if (t % 10 == 0) {
				scanLight(mc);
			}
		} else if (!darkSpots.isEmpty()) {
			darkSpots = new ArrayList<>();
		}
		if (ModuleManager.STORAGE.isEnabled()) {
			if (t % 20 == 0) {
				scanStorage(mc);
			}
		} else if (!stores.isEmpty()) {
			stores = new ArrayList<>();
		}
		if (ModuleManager.SPOTIFY.isEnabled()) {
			Spotify.poll();
		}
	}

	private static void scanLight(Minecraft mc) {
		int r = (int) ModuleManager.LIGHT.getNum("Radius");
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
					int blockLight = mc.level.getBrightness(LightLayer.BLOCK, pos);
					if (blockLight > 0) {
						continue;
					}
					int total = mc.level.getMaxLocalRawBrightness(pos);
					if (total == 0) {
						next.add(new int[] {pos.getX(), pos.getY(), pos.getZ(), 0xFFFF3B3B});
					} else if (night) {
						next.add(new int[] {pos.getX(), pos.getY(), pos.getZ(), 0xFFFFD33D});
					}
					if (next.size() >= 700) {
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
		{
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
						if (p.distSqr(me) > r2) {
							continue;
						}
						BlockState bs = be.getBlockState();
						if (bs.is(Blocks.TRIAL_SPAWNER)) {
							if (m.getBool("Trial spawners")) {
								next.add(new Store(new AABB(p), 0xFF4DD8FF, "Trial spawner"));
							}
						} else if (bs.is(Blocks.VAULT)) {
							if (m.getBool("Vaults")) {
								next.add(new Store(new AABB(p), 0xFF45E36B, "Vault"));
							}
						} else if (!single) {
							continue;
						} else if (be instanceof SpawnerBlockEntity) {
							if (m.getBool("Spawners")) {
								next.add(new Store(new AABB(p), 0xFFFF3B3B, "Spawner"));
							}
						} else if (be instanceof EnderChestBlockEntity) {
							if (m.getBool("Ender chests")) {
								next.add(new Store(new AABB(p), 0xFFB45CFF, "Ender chest"));
							}
						} else if (be instanceof ShulkerBoxBlockEntity) {
							if (m.getBool("Chests")) {
								next.add(new Store(new AABB(p), 0xFFFF7ADA, "Shulker"));
							}
						} else if (be instanceof BarrelBlockEntity) {
							if (m.getBool("Chests")) {
								next.add(new Store(new AABB(p), 0xFFC98B4A, "Barrel"));
							}
						} else if (be instanceof ChestBlockEntity) {
							if (m.getBool("Chests")) {
								next.add(new Store(new AABB(p), 0xFFFFA43A, "Chest"));
							}
						}
					}
				}
			}
		}
		stores = next;
	}

	public static void render(GuiGraphics g, float pt) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.options.hideGui) {
			return;
		}
		boolean needs = ModuleManager.ESP.isEnabled() || ModuleManager.TRACERS.isEnabled()
			|| ModuleManager.NAMETAGS.isEnabled() || ModuleManager.LIGHT.isEnabled()
			|| ModuleManager.STORAGE.isEnabled() || ModuleManager.HITBOXES.isEnabled();
		if (needs && Projector.begin(mc, g, pt)) {
			if (ModuleManager.STORAGE.isEnabled()) {
				drawStorage(g, mc);
			}
			if (ModuleManager.LIGHT.isEnabled()) {
				drawLight(g);
			}
			if (ModuleManager.HITBOXES.isEnabled()) {
				drawHitboxes(g, mc, pt);
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
		if (ModuleManager.HUD.isEnabled()) {
			drawInfo(g, mc);
		}
		if (ModuleManager.SPOTIFY.isEnabled()) {
			drawSpotify(g, mc);
		}
	}

	private static boolean skip(Minecraft mc, Entity e) {
		return e == mc.getCameraEntity() || (e == mc.player && !Freecam.active());
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

	private static void line(GuiGraphics g, double x1, double y1, double x2, double y2, int th, int color) {
		double dx = x2 - x1;
		double dy = y2 - y1;
		double len = Math.hypot(dx, dy);
		if (len < 1.0) {
			return;
		}
		int steps = (int) Math.min(1500, Math.ceil(len / Math.max(1.0, th * 0.8)));
		int half = th / 2;
		for (int i = 0; i <= steps; i++) {
			int px = (int) (x1 + dx * i / steps) - half;
			int py = (int) (y1 + dy * i / steps) - half;
			g.fill(px, py, px + th, py + th, color);
		}
	}

	private static void drawStorage(GuiGraphics g, Minecraft mc) {
		boolean labels = ModuleManager.STORAGE.getBool("Labels");
		for (Store s : stores) {
			double[] b = Projector.bounds(s.box());
			if (b == null) {
				continue;
			}
			int x1 = (int) Math.floor(b[0]);
			int y1 = (int) Math.floor(b[1]);
			int x2 = (int) Math.ceil(b[2]);
			int y2 = (int) Math.ceil(b[3]);
			if (x2 < 0 || y2 < 0 || x1 > Projector.w || y1 > Projector.h || x2 - x1 > Projector.w * 2) {
				continue;
			}
			outline(g, x1 - 1, y1 - 1, x2 + 1, y2 + 1, SHADOW);
			outline(g, x1, y1, x2, y2, s.color());
			if (labels) {
				g.drawString(mc.font, s.label(), x1, y1 - 10, s.color(), true);
			}
		}
	}

	private static void drawLight(GuiGraphics g) {
		double[] o = new double[2];
		for (int[] s : darkSpots) {
			if (!Projector.project(s[0] + 0.5, s[1] + 0.05, s[2] + 0.5, o)) {
				continue;
			}
			int x = (int) o[0];
			int y = (int) o[1];
			if (x < 0 || y < 0 || x > Projector.w || y > Projector.h) {
				continue;
			}
			g.fill(x - 2, y - 1, x + 3, y + 2, s[3]);
		}
	}

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
		line(g, x1, y1, x2, y2, 1, color);
	}

	private static void drawHitboxes(GuiGraphics g, Minecraft mc, float pt) {
		Module m = ModuleManager.HITBOXES;
		double range = m.getNum("Range");
		double r2 = range * range;
		boolean eye = m.getBool("Eye line");
		boolean others = m.getBool("Others");
		double[][] c = new double[8][2];
		boolean[] vis = new boolean[8];
		double[] o = new double[2];
		int shown = 0;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (skip(mc, e)) {
				continue;
			}
			int kind = Targets.kind(e);
			int color;
			if (kind == Targets.NONE) {
				if (!others) {
					continue;
				}
				color = WHITE;
			} else {
				if (!Targets.allowed(m, kind)) {
					continue;
				}
				color = Targets.color(kind);
			}
			if (e.distanceToSqr(Projector.eye) > r2) {
				continue;
			}
			AABB bb = e.getBoundingBox().move(e.getPosition(pt).subtract(e.position()));
			for (int i = 0; i < 8; i++) {
				double x = (i & 1) == 0 ? bb.minX : bb.maxX;
				double y = (i & 2) == 0 ? bb.minY : bb.maxY;
				double z = (i & 4) == 0 ? bb.minZ : bb.maxZ;
				vis[i] = Projector.project(x, y, z, o);
				c[i][0] = o[0];
				c[i][1] = o[1];
			}
			for (int[] ed : EDGES) {
				if (vis[ed[0]] && vis[ed[1]]) {
					edge(g, c[ed[0]][0], c[ed[0]][1], c[ed[1]][0], c[ed[1]][1], color);
				}
			}
			if (eye) {
				double ey = e.getPosition(pt).y + e.getEyeHeight();
				double[][] q = new double[4][2];
				boolean ok = true;
				double[] xs = {bb.minX, bb.maxX, bb.maxX, bb.minX};
				double[] zs = {bb.minZ, bb.minZ, bb.maxZ, bb.maxZ};
				for (int i = 0; i < 4; i++) {
					if (!Projector.project(xs[i], ey, zs[i], o)) {
						ok = false;
						break;
					}
					q[i][0] = o[0];
					q[i][1] = o[1];
				}
				if (ok) {
					for (int i = 0; i < 4; i++) {
						int j = (i + 1) % 4;
						edge(g, q[i][0], q[i][1], q[j][0], q[j][1], 0xFFFF3B3B);
					}
				}
			}
			if (++shown >= 100) {
				break;
			}
		}
	}

	private static void drawEsp(GuiGraphics g, Minecraft mc, float pt) {
		Module m = ModuleManager.ESP;
		double range = m.getNum("Range");
		double r2 = range * range;
		boolean corner = m.getNum("Style") >= 0.5;
		boolean hp = m.getBool("Health bar");
		boolean names = m.getBool("Names");
		int shown = 0;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (skip(mc, e)) {
				continue;
			}
			int kind = Targets.kind(e);
			if (kind == Targets.NONE || !Targets.allowed(m, kind)) {
				continue;
			}
			double d2 = e.distanceToSqr(Projector.eye);
			if (d2 > r2) {
				continue;
			}
			AABB bb = e.getBoundingBox().move(e.getPosition(pt).subtract(e.position()));
			double[] b = Projector.bounds(bb);
			if (b == null) {
				continue;
			}
			int x1 = (int) Math.floor(b[0]);
			int y1 = (int) Math.floor(b[1]);
			int x2 = (int) Math.ceil(b[2]);
			int y2 = (int) Math.ceil(b[3]);
			if (x2 < 0 || y2 < 0 || x1 > Projector.w || y1 > Projector.h || x2 - x1 > Projector.w * 2) {
				continue;
			}
			int color = Targets.color(kind);
			if (corner) {
				corners(g, x1 - 1, y1 - 1, x2 + 1, y2 + 1, SHADOW);
				corners(g, x1, y1, x2, y2, color);
			} else {
				outline(g, x1 - 1, y1 - 1, x2 + 1, y2 + 1, SHADOW);
				outline(g, x1, y1, x2, y2, color);
			}
			if (hp && e instanceof LivingEntity le && le.getMaxHealth() > 0) {
				float f = Math.max(0f, Math.min(1f, le.getHealth() / le.getMaxHealth()));
				int bar = (int) ((y2 - y1) * f);
				int hc = f > 0.5f ? 0xFF45E36B : (f > 0.25f ? 0xFFFFD33D : 0xFFFF3B3B);
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
		double range = m.getNum("Range");
		double r2 = range * range;
		boolean hp = m.getBool("Health");
		boolean dist = m.getBool("Distance");
		double[] o = new double[2];
		int shown = 0;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (skip(mc, e)) {
				continue;
			}
			int kind = Targets.kind(e);
			if (kind == Targets.NONE || !Targets.allowed(m, kind)) {
				continue;
			}
			double d2 = e.distanceToSqr(Projector.eye);
			if (d2 > r2) {
				continue;
			}
			Vec3 p = e.getPosition(pt);
			if (!Projector.project(p.x, p.y + e.getBbHeight() + 0.4, p.z, o)) {
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
			int x = (int) o[0] - w / 2;
			int y = (int) o[1] - 10;
			int color = Targets.color(kind);
			g.fill(x - 3, y - 2, x + w + 3, y + 10, 0xA0000000);
			g.fill(x - 3, y + 10, x + w + 3, y + 11, color);
			g.drawString(mc.font, s, x, y, WHITE, true);
			if (++shown >= 60) {
				break;
			}
		}
	}

	private static void drawTracers(GuiGraphics g, Minecraft mc, float pt) {
		Module m = ModuleManager.TRACERS;
		double range = m.getNum("Range");
		double r2 = range * range;
		boolean bottom = m.getNum("Origin") >= 0.5;
		int th = (int) m.getNum("Thickness");
		boolean showDist = m.getBool("Distance");
		double ox = Projector.cx;
		double oy = bottom ? Projector.h - 2 : Projector.cy;
		double[] o = new double[2];
		int shown = 0;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (skip(mc, e)) {
				continue;
			}
			int kind = Targets.kind(e);
			if (kind == Targets.NONE || !Targets.allowed(m, kind)) {
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
			int color = Targets.color(kind);
			line(g, ox, oy, o[0], o[1], th, color);
			int ex = (int) o[0];
			int ey = (int) o[1];
			g.fill(ex - 2, ey - 2, ex + 3, ey + 3, color);
			if (showDist) {
				g.drawString(mc.font, (int) Math.sqrt(d2) + "m", ex + 5, ey - 4, color, true);
			}
			if (++shown >= 40) {
				break;
			}
		}
	}

	private static void drawInfo(GuiGraphics g, Minecraft mc) {
		Module hud = ModuleManager.HUD;
		int accent = Theme.accent();
		int y = 4;
		g.drawString(mc.font, "CART3R", 4, y, accent, true);
		y += 11;
		if (hud.getBool("FPS")) {
			g.drawString(mc.font, mc.getFps() + " FPS", 4, y, WHITE, true);
			y += 11;
		}
		if (hud.getBool("Coords")) {
			String xyz = String.format("%.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
			g.drawString(mc.font, xyz, 4, y, WHITE, true);
		}
		if (hud.getBool("Modules")) {
			List<Module> active = new ArrayList<>();
			for (Module m : ModuleManager.MODULES) {
				if (m.isEnabled() && m != ModuleManager.INTERFACE && m != hud) {
					active.add(m);
				}
			}
			active.sort(Comparator.comparingInt(m -> -mc.font.width(m.getName())));
			int right = g.guiWidth() - 4;
			int ry = 4;
			for (Module m : active) {
				int w = mc.font.width(m.getName());
				g.fill(right - w - 3, ry - 1, right + 2, ry + 9, 0x70000000);
				g.fill(right + 1, ry - 1, right + 2, ry + 9, accent);
				g.drawString(mc.font, m.getName(), right - w, ry, accent, true);
				ry += 10;
			}
		}
	}

	private static void drawSpotify(GuiGraphics g, Minecraft mc) {
		String t = Spotify.track();
		if (t.isEmpty()) {
			t = "Spotify";
		}
		if (t.length() > 46) {
			t = t.substring(0, 46) + "...";
		}
		String text = "Now playing: " + t;
		int w = mc.font.width(text);
		int x = (g.guiWidth() - w) / 2;
		int y = 4;
		g.fill(x - 8, y - 2, x + w + 6, y + 11, 0xB0000000);
		g.fill(x - 8, y - 2, x - 5, y + 11, 0xFF1DB954);
		g.drawString(mc.font, text, x, y + 1, WHITE, true);
	}
}
