/*
 * CustomMine - 自定义区域矿场主类 - 选区创建矿场+方块概率+掉落倍率+定时/挖空刷新+随机/固定 seed+粒子预览+增量刷新+挖空保护+爆率上限+重叠检测
 * Copyright (c) 2026 TinyAII  ·  MIT License（见仓库根 LICENSE）
 *
 * 反编译恢复：源码随开发服清理丢失，本源码由已发布 jar（v1.0.0）经 CFR 0.152 反编译恢复后做
 *             开源清理（还原中文/补类头/LICENSE），逻辑与原始版一致。
 */
package com.mcadmin.custommine;

import com.mcadmin.custommine.MineListener;
import com.mcadmin.custommine.MineRegion;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class CustomMinePlugin
extends JavaPlugin {
    private final Map<String, MineRegion> regions = new LinkedHashMap<String, MineRegion>();
    private final Map<UUID, Selection> selections = new LinkedHashMap<UUID, Selection>();
    private final NamespacedKey wandKey = new NamespacedKey((Plugin)this, "mine_wand");
    private File dataFile;
    private YamlConfiguration data;
    private BukkitTask timerTask;

    public void onEnable() {
        this.dataFile = new File(this.getDataFolder(), "data.yml");
        this.loadData();
        this.getCommand("custommine").setExecutor((CommandExecutor)this);
        this.getServer().getPluginManager().registerEvents((Listener)new MineListener(this), (Plugin)this);
        this.startTimerTask();
        String banner = " _____ _                _    ___ ___\n|_   _(_)_ __  _   _   / \\  |_ _|_ _|\n  | | | | '_ \\| | | | / _ \\  | | | |\n  | | | | | | | |_| |/ ___ \\ | | | |\n  |_| |_|_| |_|\\__, /_/   \\_\\___|___|\n               |___/\n";
        banner.lines().forEach(line -> this.getLogger().info((String)line));
        this.getLogger().info("CustomMine 自定义区域矿场 v" + this.getDescription().getVersion() + " - TinyAII 出品");
        this.getLogger().info("已加载 " + this.regions.size() + " 个矿场（命令 /矿场 帮助）");
    }

    public void onDisable() {
        this.saveData();
        if (this.timerTask != null) {
            this.timerTask.cancel();
        }
    }

    private void loadData() {
        this.data = YamlConfiguration.loadConfiguration((File)this.dataFile);
        this.regions.clear();
        if (this.data.isConfigurationSection("mines")) {
            for (String key : this.data.getConfigurationSection("mines").getKeys(false)) {
                ConfigurationSection sec = this.data.getConfigurationSection("mines." + key);
                MineRegion r = this.fromSection(sec);
                if (r == null) continue;
                this.regions.put(r.name, r);
            }
        }
    }

    public void saveData() {
        this.data = new YamlConfiguration();
        int i = 0;
        for (MineRegion r : this.regions.values()) {
            ConfigurationSection sec = this.data.createSection("mines." + r.name);
            sec.set("world", (Object)r.world);
            sec.set("pos1", (Object)(r.minX + "," + r.minY + "," + r.minZ));
            sec.set("pos2", (Object)(r.maxX + "," + r.maxY + "," + r.maxZ));
            for (Map.Entry<Material, Double> entry : r.blocks.entrySet()) {
                sec.set("blocks." + entry.getKey().name(), (Object)entry.getValue());
            }
            for (Map.Entry<Material, Integer> entry : r.dropMultipliers.entrySet()) {
                sec.set("drop-multipliers." + entry.getKey().name(), (Object)entry.getValue());
            }
            sec.set("refresh-timer", (Object)r.refreshOnTimer);
            sec.set("refresh-interval", (Object)r.refreshIntervalSeconds);
            sec.set("refresh-on-empty", (Object)r.refreshOnEmpty);
            sec.set("empty-threshold", (Object)r.emptyThresholdPercent);
            sec.set("mode", (Object)r.regenerateMode);
            sec.set("seed", (Object)r.seed);
            ++i;
        }
        try {
            this.data.save(this.dataFile);
        }
        catch (IOException e) {
            this.getLogger().warning("保存矿场数据失败：" + e.getMessage());
        }
    }

    private MineRegion fromSection(ConfigurationSection sec) {
        if (sec == null) {
            return null;
        }
        String name = sec.getName();
        String world = sec.getString("world");
        String[] p1 = sec.getString("pos1", "0,0,0").split(",");
        String[] p2 = sec.getString("pos2", "0,0,0").split(",");
        try {
            Material m;
            MineRegion r = new MineRegion(name, world, Integer.parseInt(p1[0]), Integer.parseInt(p1[1]), Integer.parseInt(p1[2]), Integer.parseInt(p2[0]), Integer.parseInt(p2[1]), Integer.parseInt(p2[2]));
            if (sec.isConfigurationSection("blocks")) {
                for (String k : sec.getConfigurationSection("blocks").getKeys(false)) {
                    m = Material.getMaterial((String)k);
                    if (m == null || !m.isBlock()) continue;
                    r.blocks.put(m, sec.getDouble("blocks." + k));
                }
            }
            if (sec.isConfigurationSection("drop-multipliers")) {
                for (String k : sec.getConfigurationSection("drop-multipliers").getKeys(false)) {
                    m = Material.getMaterial((String)k);
                    if (m == null) continue;
                    r.dropMultipliers.put(m, sec.getInt("drop-multipliers." + k, 1));
                }
            }
            r.refreshOnTimer = sec.getBoolean("refresh-timer", false);
            r.refreshIntervalSeconds = sec.getInt("refresh-interval", 3600);
            r.refreshOnEmpty = sec.getBoolean("refresh-on-empty", false);
            r.emptyThresholdPercent = sec.getDouble("empty-threshold", 5.0);
            r.regenerateMode = sec.getString("mode", "random");
            r.seed = sec.getLong("seed", System.currentTimeMillis());
            return r;
        }
        catch (Exception e) {
            this.getLogger().warning("矿场 " + name + " 数据读取失败：" + e.getMessage());
            return null;
        }
    }

    public int refreshRegion(MineRegion r) {
        World world = Bukkit.getWorld((String)r.world);
        if (world == null) {
            this.getLogger().warning("矿场 " + r.name + " 的世界 " + r.world + " 不存在");
            return 0;
        }
        for (Player p : world.getPlayers()) {
            if (!r.contains(p.getLocation())) continue;
            Location top = new Location(world, p.getLocation().getX(), (double)r.maxY + 1.5, p.getLocation().getZ());
            p.teleport(top);
            p.sendActionBar(String.valueOf(ChatColor.GOLD) + "矿场「" + r.name + "」刷新中，已将你移至安全位置");
        }
        int count = 0;
        Random random = r.createRandom();
        for (int x = r.minX; x <= r.maxX; ++x) {
            for (int y = r.minY; y <= r.maxY; ++y) {
                for (int z = r.minZ; z <= r.maxZ; ++z) {
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                    Material mat = r.pickBlock(random);
                    world.getBlockAt(x, y, z).setType(mat, false);
                    ++count;
                }
            }
        }
        r.lastRefreshTime = System.currentTimeMillis();
        return count;
    }

    public MineRegion findRegion(Block block) {
        MineRegion first = null;
        for (MineRegion r : this.regions.values()) {
            if (!r.contains(block) || first != null) continue;
            first = r;
        }
        return first;
    }

    private void startTimerTask() {
        this.timerTask = Bukkit.getScheduler().runTaskTimer((Plugin)this, () -> {
            long now = System.currentTimeMillis();
            for (MineRegion r : this.regions.values()) {
                if (r.refreshOnTimer && now - r.lastRefreshTime >= (long)r.refreshIntervalSeconds * 1000L) {
                    this.getLogger().info("矿场 " + r.name + " 定时刷新");
                    this.refreshRegion(r);
                }
                if (!r.refreshOnEmpty) continue;
                this.checkEmptyAndRefresh(r);
            }
        }, 100L, 400L);
    }

    private void checkEmptyAndRefresh(MineRegion r) {
        World world = Bukkit.getWorld((String)r.world);
        if (world == null) {
            return;
        }
        int totalLoaded = 0;
        long solid = 0L;
        for (int x = r.minX; x <= r.maxX; ++x) {
            for (int y = r.minY; y <= r.maxY; ++y) {
                for (int z = r.minZ; z <= r.maxZ; ++z) {
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                    ++totalLoaded;
                    if (world.getBlockAt(x, y, z).getType() == Material.AIR) continue;
                    ++solid;
                }
            }
        }
        if (totalLoaded == 0) {
            return;
        }
        double remainPercent = (double)solid / (double)totalLoaded * 100.0;
        if (remainPercent <= r.emptyThresholdPercent) {
            this.getLogger().info("矿场 " + r.name + " 剩余 " + String.format("%.1f", remainPercent) + "% 低于阈值，自动刷新");
            this.refreshRegion(r);
        }
    }

    public Map<String, MineRegion> getRegions() {
        return this.regions;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("custommine.admin")) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "你没有权限。");
            return true;
        }
        if (args.length == 0) {
            this.sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "帮助": 
            case "help": {
                this.sendHelp(sender);
                break;
            }
            case "点1": {
                this.cmdPos1(sender);
                break;
            }
            case "点2": {
                this.cmdPos2(sender);
                break;
            }
            case "棒": 
            case "木棍": {
                this.cmdWand(sender);
                break;
            }
            case "创建": {
                this.cmdCreate(sender, args);
                break;
            }
            case "方块": {
                this.cmdBlock(sender, args);
                break;
            }
            case "爆率": {
                this.cmdDrop(sender, args);
                break;
            }
            case "刷新": {
                this.cmdRefresh(sender, args);
                break;
            }
            case "列表": {
                this.cmdList(sender);
                break;
            }
            case "信息": {
                this.cmdInfo(sender, args);
                break;
            }
            case "删除": {
                this.cmdDelete(sender, args);
                break;
            }
            case "模式": {
                this.cmdMode(sender, args);
                break;
            }
            case "定时": {
                this.cmdTimer(sender, args);
                break;
            }
            case "挖空": {
                this.cmdEmpty(sender, args);
                break;
            }
            case "重载": 
            case "reload": {
                this.loadData();
                this.saveData();
                sender.sendMessage(String.valueOf(ChatColor.GREEN) + "已热重载 " + this.regions.size() + " 个矿场配置（方块布局不变，需 /矿场 刷新 才重新生成）。");
                break;
            }
            case "预览": 
            case "preview": {
                this.cmdPreview(sender, args);
                break;
            }
            default: {
                this.sendHelp(sender);
            }
        }
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(String.valueOf(ChatColor.GOLD) + "===== 自定义区域矿场 =====");
        s.sendMessage(String.valueOf(ChatColor.YELLOW) + "/矿场 点1 / 点2" + String.valueOf(ChatColor.WHITE) + "  站立记录两个角（选区）");
        s.sendMessage(String.valueOf(ChatColor.YELLOW) + "/矿场 棒" + String.valueOf(ChatColor.WHITE) + "  获得选区棒（左键点1，右键点2）");
        s.sendMessage(String.valueOf(ChatColor.YELLOW) + "/矿场 创建 <名字>" + String.valueOf(ChatColor.WHITE) + "  用已选两角创建");
        s.sendMessage(String.valueOf(ChatColor.YELLOW) + "/矿场 创建 <名字> <x1 y1 z1> <x2 y2 z2>" + String.valueOf(ChatColor.WHITE) + "  坐标创建");
        s.sendMessage(String.valueOf(ChatColor.YELLOW) + "/矿场 创建 <名字> 半径 <r>" + String.valueOf(ChatColor.WHITE) + "  以你为中心半径r");
        s.sendMessage(String.valueOf(ChatColor.YELLOW) + "/矿场 方块 <名字> <方块> <权重>" + String.valueOf(ChatColor.WHITE) + "  设概率（如 STONE 80）");
        s.sendMessage(String.valueOf(ChatColor.YELLOW) + "/矿场 爆率 <名字> <方块> <倍率>" + String.valueOf(ChatColor.WHITE) + "  设掉落倍率（默认1）");
        s.sendMessage(String.valueOf(ChatColor.YELLOW) + "/矿场 模式 <名字> <随机|固定>" + String.valueOf(ChatColor.WHITE) + "  随机/固定生成");
        s.sendMessage(String.valueOf(ChatColor.YELLOW) + "/矿场 定时 <名字> <秒|关>" + String.valueOf(ChatColor.WHITE) + "  定时刷新");
        s.sendMessage(String.valueOf(ChatColor.YELLOW) + "/矿场 挖空 <名字> <开|关> [阈值%]" + String.valueOf(ChatColor.WHITE) + "  挖空自动刷新");
        s.sendMessage(String.valueOf(ChatColor.YELLOW) + "/矿场 刷新 <名字>" + String.valueOf(ChatColor.WHITE) + "  立即刷新");
        s.sendMessage(String.valueOf(ChatColor.YELLOW) + "/矿场 预览 <名字>" + String.valueOf(ChatColor.WHITE) + "  粒子预览边界");
        s.sendMessage(String.valueOf(ChatColor.YELLOW) + "/矿场 重载" + String.valueOf(ChatColor.WHITE) + "  热重载 data.yml 配置");
        s.sendMessage(String.valueOf(ChatColor.YELLOW) + "/矿场 列表 / 信息 <名字> / 删除 <名字>");
    }

    private void cmdPos1(CommandSender s) {
        if (!(s instanceof Player)) {
            return;
        }
        Player p = (Player)s;
        Selection sel = this.selections.computeIfAbsent(p.getUniqueId(), k -> new Selection());
        sel.pos1 = p.getLocation();
        p.sendMessage(String.valueOf(ChatColor.GREEN) + "已记录点1：" + this.fmt(sel.pos1));
    }

    private void cmdPos2(CommandSender s) {
        if (!(s instanceof Player)) {
            return;
        }
        Player p = (Player)s;
        Selection sel = this.selections.computeIfAbsent(p.getUniqueId(), k -> new Selection());
        sel.pos2 = p.getLocation();
        p.sendMessage(String.valueOf(ChatColor.GREEN) + "已记录点2：" + this.fmt(sel.pos2));
    }

    private void cmdWand(CommandSender s) {
        if (!(s instanceof Player)) {
            return;
        }
        Player p = (Player)s;
        ItemStack wand = new ItemStack(Material.STICK);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName(String.valueOf(ChatColor.GREEN) + "矿场选区棒");
        meta.getPersistentDataContainer().set(this.wandKey, PersistentDataType.BYTE, (byte) 1);
        wand.setItemMeta(meta);
        p.getInventory().addItem(new ItemStack[]{wand});
        p.sendMessage(String.valueOf(ChatColor.GREEN) + "拿到选区棒：左键=点1，右键=点2");
    }

    private void cmdCreate(CommandSender s, String[] args) {
        if (args.length < 2) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "用法：/矿场 创建 <名字> [坐标|半径 <r>]");
            return;
        }
        String name = args[1];
        if (this.regions.containsKey(name)) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "矿场 " + name + " 已存在。");
            return;
        }
        Location p1 = null;
        Location p2 = null;
        if (args.length == 2) {
            if (s instanceof Player) {
                Player p = (Player)s;
                Selection sel = this.selections.get(p.getUniqueId());
                p1 = sel == null ? null : sel.pos1;
                p2 = sel == null ? null : sel.pos2;
            }
        } else if (args.length >= 3 && args[2].equalsIgnoreCase("半径")) {
            if (s instanceof Player) {
                Player p = (Player)s;
                if (args.length >= 4) {
                    try {
                        int r = Integer.parseInt(args[3]);
                        Location c = p.getLocation();
                        p1 = new Location(c.getWorld(), (double)(c.getBlockX() - r), (double)(c.getBlockY() - r), (double)(c.getBlockZ() - r));
                        p2 = new Location(c.getWorld(), (double)(c.getBlockX() + r), (double)(c.getBlockY() + r), (double)(c.getBlockZ() + r));
                    }
                    catch (NumberFormatException e) {
                        s.sendMessage(String.valueOf(ChatColor.RED) + "半径必须是数字。");
                        return;
                    }
                }
            }
        } else if (args.length >= 8) {
            try {
                World world;
                if (s instanceof Player) {
                    Player p = (Player)s;
                    world = p.getWorld();
                } else {
                    world = (World)Bukkit.getWorlds().get(0);
                }
                World w = world;
                p1 = new Location(w, (double)Float.parseFloat(args[2]), (double)Float.parseFloat(args[3]), (double)Float.parseFloat(args[4]));
                p2 = new Location(w, (double)Float.parseFloat(args[5]), (double)Float.parseFloat(args[6]), (double)Float.parseFloat(args[7]));
            }
            catch (NumberFormatException e) {
                s.sendMessage(String.valueOf(ChatColor.RED) + "坐标必须是数字。");
                return;
            }
        } else {
            s.sendMessage(String.valueOf(ChatColor.RED) + "参数不完整。");
            return;
        }
        if (p1 == null || p2 == null) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "缺少选区（先 /矿场 点1 点2，或用坐标/半径）。");
            return;
        }
        if (!p1.getWorld().equals((Object)p2.getWorld())) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "两个点必须在同一世界。");
            return;
        }
        MineRegion r = new MineRegion(name, p1.getWorld().getName(), p1.getBlockX(), p1.getBlockY(), p1.getBlockZ(), p2.getBlockX(), p2.getBlockY(), p2.getBlockZ());
        StringBuilder overlaps = new StringBuilder();
        for (MineRegion other : this.regions.values()) {
            if (!other.overlaps(r)) continue;
            overlaps.append(other.name).append(" ");
        }
        if (overlaps.length() > 0) {
            s.sendMessage(String.valueOf(ChatColor.YELLOW) + "警告：矿场 " + name + " 与 " + overlaps.toString().trim() + " 空间重叠！叠加区域将按概率权重最高的矿场/两倍掉落随机判定，建议避免重叠。");
        }
        r.blocks.put(Material.STONE, 100.0);
        this.regions.put(name, r);
        this.saveData();
        s.sendMessage(String.valueOf(ChatColor.GREEN) + "已创建矿场 " + name + "（体积 " + r.fillableCount() + " 方块）。先 /矿场 方块 设概率，再 /矿场 刷新 生成。");
    }

    private void cmdBlock(CommandSender s, String[] args) {
        double w;
        if (args.length < 4) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "用法：/矿场 方块 <名字> <方块> <权重>");
            return;
        }
        MineRegion r = this.regions.get(args[1]);
        if (r == null) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "矿场不存在。");
            return;
        }
        Material m = this.parseMaterial(args[2]);
        if (m == null || !m.isBlock()) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "方块名无效（如 STONE、COAL_ORE、IRON_ORE）。");
            return;
        }
        try {
            w = Double.parseDouble(args[3]);
        }
        catch (NumberFormatException e) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "权重必须是数字。");
            return;
        }
        if (w <= 0.0) {
            r.blocks.remove(m);
            s.sendMessage(String.valueOf(ChatColor.GREEN) + "已移除方块 " + m.name() + "（权重≤0）。");
        } else {
            r.blocks.put(m, w);
            s.sendMessage(String.valueOf(ChatColor.GREEN) + "已设置 " + m.name() + " 权重 " + w);
        }
        this.saveData();
    }

    private void cmdDrop(CommandSender s, String[] args) {
        int mul;
        if (args.length < 4) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "用法：/矿场 爆率 <名字> <方块> <倍率>");
            return;
        }
        MineRegion r = this.regions.get(args[1]);
        if (r == null) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "矿场不存在。");
            return;
        }
        Material m = this.parseMaterial(args[2]);
        if (m == null) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "方块名无效。");
            return;
        }
        try {
            mul = Integer.parseInt(args[3]);
        }
        catch (NumberFormatException e) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "倍率必须是整数。");
            return;
        }
        if (mul > 1000) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "掉落倍率上限 1000。");
            return;
        }
        if (mul <= 1) {
            r.dropMultipliers.remove(m);
            s.sendMessage(String.valueOf(ChatColor.GREEN) + "已恢复 " + m.name() + " 原版掉落。");
        } else {
            r.dropMultipliers.put(m, mul);
            s.sendMessage(String.valueOf(ChatColor.GREEN) + "已设置 " + m.name() + " 掉落倍率 ×" + mul);
        }
        this.saveData();
    }

    private void cmdRefresh(CommandSender s, String[] args) {
        if (args.length < 2) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "用法：/矿场 刷新 <名字>");
            return;
        }
        MineRegion r = this.regions.get(args[1]);
        if (r == null) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "矿场不存在。");
            return;
        }
        this.refreshRegion(r);
        s.sendMessage(String.valueOf(ChatColor.GREEN) + "矿场 " + r.name + " 已刷新（" + r.blockCount() + " 方块）。");
    }

    private void cmdList(CommandSender s) {
        if (this.regions.isEmpty()) {
            s.sendMessage(String.valueOf(ChatColor.YELLOW) + "暂无矿场。");
            return;
        }
        s.sendMessage(String.valueOf(ChatColor.GOLD) + "===== 矿场列表（" + this.regions.size() + "）=====");
        for (MineRegion r : this.regions.values()) {
            s.sendMessage(String.valueOf(ChatColor.WHITE) + "- " + r.name + String.valueOf(ChatColor.GRAY) + "  [" + r.minX + "," + r.minY + "," + r.minZ + " ~ " + r.maxX + "," + r.maxY + "," + r.maxZ + "]");
        }
    }

    private void cmdInfo(CommandSender s, String[] args) {
        if (args.length < 2) {
            return;
        }
        MineRegion r = this.regions.get(args[1]);
        if (r == null) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "矿场不存在。");
            return;
        }
        s.sendMessage(String.valueOf(ChatColor.GOLD) + "===== 矿场 " + r.name + " =====");
        s.sendMessage(String.valueOf(ChatColor.WHITE) + "世界: " + r.world + "  体积: " + r.blockCount());
        StringBuilder bl = new StringBuilder("方块概率: ");
        for (Map.Entry<Material, Double> e : r.blocks.entrySet()) {
            bl.append(e.getKey().name()).append(" ").append(e.getValue()).append("%, ");
        }
        s.sendMessage(String.valueOf(ChatColor.WHITE) + bl.toString());
        StringBuilder dp = new StringBuilder("掉落倍率: ");
        if (r.dropMultipliers.isEmpty()) {
            dp.append("全部原版");
        } else {
            for (Map.Entry<Material, Integer> e : r.dropMultipliers.entrySet()) {
                dp.append(e.getKey().name()).append(" ×").append(e.getValue()).append(", ");
            }
        }
        s.sendMessage(String.valueOf(ChatColor.WHITE) + dp.toString());
        s.sendMessage(String.valueOf(ChatColor.WHITE) + "刷新模式: " + r.regenerateMode + "  定时: " + r.refreshOnTimer + "  挖空: " + r.refreshOnEmpty);
    }

    private void cmdDelete(CommandSender s, String[] args) {
        if (args.length < 2) {
            return;
        }
        MineRegion r = this.regions.remove(args[1]);
        if (r == null) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "矿场不存在。");
            return;
        }
        this.saveData();
        s.sendMessage(String.valueOf(ChatColor.GREEN) + "已删除矿场 " + r.name + "。");
    }

    private void cmdPreview(CommandSender s, String[] args) {
        if (!(s instanceof Player)) {
            return;
        }
        Player p = (Player)s;
        if (args.length < 2) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "用法：/矿场 预览 <名字>");
            return;
        }
        MineRegion r = this.regions.get(args[1]);
        if (r == null) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "矿场不存在。");
            return;
        }
        World w = p.getWorld();
        if (!w.getName().equals(r.world)) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "你不在矿场所在世界 " + r.world + "，无法预览。");
            return;
        }
        p.sendMessage(String.valueOf(ChatColor.GREEN) + "正在预览矿场 " + r.name + " 边界（黄色粒子，持续 10 秒）...");
        Particle part = Particle.CRIT;
        ArrayList<Location> edges = new ArrayList<Location>();
        int[] xs = new int[]{r.minX, r.maxX};
        int[] ys = new int[]{r.minY, r.maxY};
        int[] zs = new int[]{r.minZ, r.maxZ};
        for (int y : ys) {
            for (int z : zs) {
                for (int x = r.minX; x <= r.maxX; ++x) {
                    edges.add(new Location(w, (double)x + 0.5, (double)y + 0.5, (double)z + 0.5));
                }
            }
        }
        for (int x : xs) {
            for (int z : zs) {
                for (int y = r.minY; y <= r.maxY; ++y) {
                    edges.add(new Location(w, (double)x + 0.5, (double)y + 0.5, (double)z + 0.5));
                }
            }
        }
        for (int x : xs) {
            for (int y : ys) {
                for (int z = r.minZ; z <= r.maxZ; ++z) {
                    edges.add(new Location(w, (double)x + 0.5, (double)y + 0.5, (double)z + 0.5));
                }
            }
        }
        int step = 400;
        for (int round = 0; round < 10; ++round) {
            for (int i = 0; i < edges.size(); i += step) {
                int from = i;
                int to = Math.min(i + step, edges.size());
                Bukkit.getScheduler().runTaskLater((Plugin)this, () -> {
                    for (int j = from; j < to; ++j) {
                        p.spawnParticle(part, (Location)edges.get(j), 1, 0.0, 0.0, 0.0, 0.0);
                    }
                }, (long)round * 20L + (long)(i / step));
            }
        }
    }

    private void cmdMode(CommandSender s, String[] args) {
        if (args.length < 2) {
            return;
        }
        MineRegion r = this.regions.get(args[1]);
        if (r == null) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "矿场不存在。");
            return;
        }
        String mode = "random";
        if (args.length >= 3) {
            mode = "固定".equals(args[2]) || "fixed".equalsIgnoreCase(args[2]) ? "fixed" : "random";
        }
        r.regenerateMode = mode;
        r.seed = System.currentTimeMillis();
        this.saveData();
        s.sendMessage(String.valueOf(ChatColor.GREEN) + "矿场 " + r.name + " 刷新模式已设为 " + (mode.equals("fixed") ? "固定" : "随机"));
    }

    private void cmdTimer(CommandSender s, String[] args) {
        if (args.length < 3) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "用法：/矿场 定时 <名字> <秒|关>");
            return;
        }
        MineRegion r = this.regions.get(args[1]);
        if (r == null) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "矿场不存在。");
            return;
        }
        if ("关".equals(args[2]) || "off".equalsIgnoreCase(args[2]) || "0".equals(args[2])) {
            r.refreshOnTimer = false;
            s.sendMessage(String.valueOf(ChatColor.GREEN) + "已关闭矿场 " + r.name + " 的定时刷新。");
        } else {
            try {
                int sec = Integer.parseInt(args[2]);
                if (sec < 10) {
                    s.sendMessage(String.valueOf(ChatColor.RED) + "间隔至少 10 秒。");
                    return;
                }
                r.refreshOnTimer = true;
                r.refreshIntervalSeconds = sec;
                s.sendMessage(String.valueOf(ChatColor.GREEN) + "已设置矿场 " + r.name + " 每 " + sec + " 秒定时刷新。");
            }
            catch (NumberFormatException e) {
                s.sendMessage(String.valueOf(ChatColor.RED) + "秒数必须是数字。");
                return;
            }
        }
        this.saveData();
    }

    private void cmdEmpty(CommandSender s, String[] args) {
        if (args.length < 3) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "用法：/矿场 挖空 <名字> <开|关> [阈值%]");
            return;
        }
        MineRegion r = this.regions.get(args[1]);
        if (r == null) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "矿场不存在。");
            return;
        }
        if ("关".equals(args[2]) || "off".equalsIgnoreCase(args[2]) || "false".equalsIgnoreCase(args[2])) {
            r.refreshOnEmpty = false;
            s.sendMessage(String.valueOf(ChatColor.GREEN) + "已关闭矿场 " + r.name + " 的挖空刷新。");
        } else {
            r.refreshOnEmpty = true;
            if (args.length >= 4) {
                try {
                    r.emptyThresholdPercent = Math.max(0.1, Double.parseDouble(args[3]));
                }
                catch (NumberFormatException numberFormatException) {
                    // empty catch block
                }
            }
            s.sendMessage(String.valueOf(ChatColor.GREEN) + "已开启矿场 " + r.name + " 挖空刷新（剩余 <= " + r.emptyThresholdPercent + "% 自动刷新）。");
        }
        this.saveData();
    }

    public NamespacedKey wandKey() {
        return this.wandKey;
    }

    public void wandSetPoint(Player p, Location loc, boolean first) {
        Selection sel = this.selections.computeIfAbsent(p.getUniqueId(), k -> new Selection());
        if (first) {
            sel.pos1 = loc;
        } else {
            sel.pos2 = loc;
        }
        p.sendMessage(String.valueOf(ChatColor.GREEN) + "已记录点" + (first ? "1" : "2") + "：" + this.fmt(loc));
    }

    private Material parseMaterial(String name) {
        if (name == null) {
            return null;
        }
        return Material.getMaterial((String)name.toUpperCase());
    }

    private String fmt(Location l) {
        return l.getWorld().getName() + " " + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ();
    }

    private static final class Selection {
        Location pos1;
        Location pos2;

        private Selection() {
        }
    }
}

