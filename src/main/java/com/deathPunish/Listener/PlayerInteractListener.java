package com.deathPunish.Listener;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static com.deathPunish.DeathPunish.config;

public class PlayerInteractListener implements Listener {
    private static final Set<Action> ACTIONS = Collections.unmodifiableSet(EnumSet.of(Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK));

    @EventHandler
    public void onPlayerRightClick(PlayerInteractEvent event) {
        if (ACTIONS.contains(event.getAction())) {
            ItemStack itemInMainHand = event.getPlayer().getInventory().getItemInMainHand();
            if ((itemInMainHand.getType() == Material.valueOf(config.getString("customItems.protect_item.material"))
                    && itemInMainHand.getItemMeta().getDisplayName().replace("§", "&").equalsIgnoreCase(config.getString("customItems.protect_item.name")))
                    || (itemInMainHand.getType() == Material.valueOf(config.getString("customItems.ender_protect_item.material"))
                    && itemInMainHand.getItemMeta().getDisplayName().replace("§", "&").equalsIgnoreCase(config.getString("customItems.ender_protect_item.name")))) {
                event.setCancelled(true);
            }
        }
    }
}
