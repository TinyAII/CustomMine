/*
 * CustomMine - 挖矿监听 - 拦截区域内 BlockBreakEvent，取消原版掉落按爆率倍数产出，触发挖空检测
 * Copyright (c) 2026 TinyAII  ·  MIT License（见仓库根 LICENSE）
 *
 * 反编译恢复：源码随开发服清理丢失，本源码由已发布 jar（v1.0.0）经 CFR 0.152 反编译恢复后做
 *             开源清理（还原中文/补类头/LICENSE），逻辑与原始版一致。
 */
package com.mcadmin.custommine;

import com.mcadmin.custommine.CustomMinePlugin;
import com.mcadmin.custommine.MineRegion;
import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class MineListener
implements Listener {
    private final CustomMinePlugin plugin;

    public MineListener(CustomMinePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        boolean right;
        ItemStack item = e.getItem();
        if (item == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        if (!meta.getPersistentDataContainer().has(this.plugin.wandKey(), PersistentDataType.BYTE)) {
            return;
        }
        e.setCancelled(true);
        Block clicked = e.getClickedBlock();
        Location loc = clicked != null ? clicked.getLocation() : e.getPlayer().getLocation();
        boolean left = e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_AIR;
        boolean bl = right = e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR;
        if (left) {
            this.plugin.wandSetPoint(e.getPlayer(), loc, true);
        } else if (right) {
            this.plugin.wandSetPoint(e.getPlayer(), loc, false);
        }
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onBreak(BlockBreakEvent e) {
        MineRegion region = this.plugin.findRegion(e.getBlock());
        if (region == null) {
            return;
        }
        int multiplier = region.dropMultiplier(e.getBlock().getType());
        if (multiplier > 1) {
            e.setDropItems(false);
            ItemStack tool = e.getPlayer().getInventory().getItemInMainHand();
            Collection<ItemStack> drops = e.getBlock().getDrops(tool);
            Location dropLoc = e.getBlock().getLocation().add(0.5, 0.5, 0.5);
            for (ItemStack drop : drops) {
                drop.setAmount(drop.getAmount() * multiplier);
                e.getBlock().getWorld().dropItemNaturally(dropLoc, drop);
            }
        }
    }
}

