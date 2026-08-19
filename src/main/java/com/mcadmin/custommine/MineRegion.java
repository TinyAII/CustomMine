/*
 * CustomMine - 矿场区域 - 存方块概率表+掉落倍率+刷新开关+seed; 刷新时按概率洗出布局
 * Copyright (c) 2026 TinyAII  ·  MIT License（见仓库根 LICENSE）
 *
 * 反编译恢复：源码随开发服清理丢失，本源码由已发布 jar（v1.0.0）经 CFR 0.152 反编译恢复后做
 *             开源清理（还原中文/补类头/LICENSE），逻辑与原始版一致。
 */
package com.mcadmin.custommine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class MineRegion {
    public final String name;
    public final String world;
    public final int minX;
    public final int minY;
    public final int minZ;
    public final int maxX;
    public final int maxY;
    public final int maxZ;
    public final Map<Material, Double> blocks = new LinkedHashMap<Material, Double>();
    public final Map<Material, Integer> dropMultipliers = new LinkedHashMap<Material, Integer>();
    public boolean refreshOnTimer = false;
    public int refreshIntervalSeconds = 3600;
    public boolean refreshOnEmpty = false;
    public double emptyThresholdPercent = 5.0;
    public String regenerateMode = "random";
    public long seed = System.currentTimeMillis();
    public long lastRefreshTime = System.currentTimeMillis();

    public MineRegion(String name, String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.name = name;
        this.world = world;
        this.minX = Math.min(x1, x2);
        this.maxX = Math.max(x1, x2);
        this.minY = Math.min(y1, y2);
        this.maxY = Math.max(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxZ = Math.max(z1, z2);
    }

    public boolean contains(Block block) {
        if (!block.getWorld().getName().equals(this.world)) {
            return false;
        }
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ;
    }

    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(this.world)) {
            return false;
        }
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ;
    }

    public long blockCount() {
        return (long)(this.maxX - this.minX + 1) * (long)(this.maxY - this.minY + 1) * (long)(this.maxZ - this.minZ + 1);
    }

    public Material pickBlock(Random random) {
        if (this.blocks.isEmpty()) {
            return Material.STONE;
        }
        double total = 0.0;
        for (double w : this.blocks.values()) {
            total += w;
        }
        if (total <= 0.0) {
            return this.blocks.keySet().iterator().next();
        }
        double roll = random.nextDouble() * total;
        for (Map.Entry<Material, Double> e : this.blocks.entrySet()) {
            if (!((roll -= e.getValue().doubleValue()) <= 0.0)) continue;
            return e.getKey();
        }
        return this.blocks.keySet().iterator().next();
    }

    public int dropMultiplier(Material mat) {
        Integer m = this.dropMultipliers.get(mat);
        return m == null ? 1 : Math.max(1, m);
    }

    public int generate(World world, Random random) {
        int count = 0;
        for (int x = this.minX; x <= this.maxX; ++x) {
            for (int y = this.minY; y <= this.maxY; ++y) {
                for (int z = this.minZ; z <= this.maxZ; ++z) {
                    Material mat = this.pickBlock(random);
                    world.getBlockAt(x, y, z).setType(mat, false);
                    ++count;
                }
            }
        }
        return count;
    }

    public long fillableCount() {
        return ((long)(this.maxX - this.minX) + 1L) * ((long)(this.maxY - this.minY) + 1L) * ((long)(this.maxZ - this.minZ) + 1L);
    }

    public boolean overlaps(MineRegion other) {
        if (!this.world.equalsIgnoreCase(other.world)) {
            return false;
        }
        return this.minX <= other.maxX && this.maxX >= other.minX && this.minY <= other.maxY && this.maxY >= other.minY && this.minZ <= other.maxZ && this.maxZ >= other.minZ;
    }

    public Random createRandom() {
        if ("fixed".equalsIgnoreCase(this.regenerateMode)) {
            return new Random(this.seed);
        }
        return new Random();
    }
}

