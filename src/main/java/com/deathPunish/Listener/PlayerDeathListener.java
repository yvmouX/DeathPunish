package com.deathPunish.Listener;

import com.deathPunish.DeathPunish;
import com.deathPunish.Utils.EpitaphUtils;
import com.deathPunish.Utils.SchedulerUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.deathPunish.DeathPunish.*;
import static com.deathPunish.DeathPunish.log;


public class PlayerDeathListener implements Listener {
    private AttributeInstance playerMaxHealth;
    private final Random rand = new Random();
    private boolean isDeath = false;
    private final Plugin pl;
    private int food;
    private final List<Material> materials = new ArrayList<>();

    public PlayerDeathListener(DeathPunish plugin) {
        this.pl = plugin;
        List<String> whitelist = config.getStringList("punishments.Inventory.whitelist");
        for (String wl : whitelist) {
            try {
                Material material = Material.valueOf(wl);
                materials.add(material);
            } catch (IllegalArgumentException e) {
                log.warn("§c无效的物品: " + wl);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (SchedulerUtils.isFolia()) return;
        var player = event.getPlayer();
        playerDeath(player);
    }

    @EventHandler
    public void isFoliaPlayerDeath(PlayerDeathEvent event) {
        if (!SchedulerUtils.isFolia()) return;
        Player player = event.getEntity();
        playerDeath(player);
    }

    private void playerDeath(Player player) {
        Runnable r = () -> {
            if (this.isDeath) {
                if (config.getBoolean("punishOnDeath.enable")) {
                    List<String> deathMsg = Objects.requireNonNull(config.getStringList("punishments.deathMsg"));
                    for (String msg : deathMsg) {
                        player.sendMessage(msg);
                    }
                    // 读取当前玩家的最大生命值
                    double maxHealth = playerMaxHealth.getValue();
                    double reduceHealthAmount = config.getDouble("punishments.reduceHealthAmount");
                    if (config.getBoolean("punishments.banOnDeath") && maxHealth == 1) {
                        Date expiration = new Date(System.currentTimeMillis() + (long) config.getInt("punishments.banDuration") * 60 * 1000);
                        Bukkit.getBanList(BanList.Type.NAME).addBan(
                                player.getName(),
                                config.getString("punishments.banReason"),
                                expiration,
                                "DeathPunish");
                        player.kickPlayer(Objects.requireNonNull(config.getString("punishments.banReason")));
                    }
                    // 减少最大生命值
                    if (config.getBoolean("punishments.reduceMaxHealthOnDeath")) {
                        double newMaxHealth = Math.max(maxHealth - reduceHealthAmount, 1.0); // 最小值为1.0}
                        // 设置玩家的新最大生命值
                        playerMaxHealth.setBaseValue(newMaxHealth);
                        player.setHealth(newMaxHealth); // 重置当前生命值为新的最大值
                    }
                    if (config.getBoolean("punishments.foodLevel.save")) {
                        SchedulerUtils.runTaskLater(pl,() -> player.setFoodLevel(food), 1L, null, null);
                    } else {
                        SchedulerUtils.runTaskLater(pl,() -> player.setFoodLevel(config.getInt("punishments.foodLevel.value")), 1L, null, null);
                    }
                    if (config.getBoolean("punishments.debuff.enable")) {
                        List<String> debuff = config.getStringList("punishments.debuff.potions");
                        SchedulerUtils.runTask(pl, () -> {
                            for (String effect : debuff) {
                                String[] parts = effect.split(" ");
                                if (parts.length == 3) {
                                    PotionEffectType type = PotionEffectType.getByKey(NamespacedKey.minecraft(parts[0]));
                                    int duration = Integer.parseInt(parts[1]);
                                    int amplifier = Integer.parseInt(parts[2]);
                                    if (type != null) {
                                        if (SchedulerUtils.isFolia()) {
                                            DeathPunish.getFoliaLib().getScheduler().runAtEntity(player, wrappedTask ->
                                                    player.addPotionEffect(new PotionEffect(type, duration, amplifier)));
                                        } else player.addPotionEffect(new PotionEffect(type, duration, amplifier));
                                    } else {
                                        log.info("无效的药水效果: " + parts[0]);
                                    }
                                }
                            }
                        }, null, null);
                    }
                    this.isDeath = false;
                }
            }
        };
        if (SchedulerUtils.isFolia()) {
            SchedulerUtils.runTaskLater(pl, r, 5L, null, null);
        } else r.run();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        var player = event.getEntity();
        World world = player.getWorld();
        List<String> worlds = config.getStringList("punishOnDeath.enableWorlds");
        if (!worlds.contains(world.getName())) {
            log.info("§c玩家 " + player.getName() + " 在未启用死亡惩罚的 " + world.getName() + " 世界中死亡");
            return;
        }
        playerMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        String method;
        if (!player.hasPermission("deathpunish.bypass")) {

            // FileConfiguration epitaphConfig = plugin.getEpitaphConfig(); // 获取 epitaph.yml 配置
            if (config.getBoolean("punishOnDeath.enable")) {
                @Nullable ItemStack[] contents = player.getInventory().getContents();
                @Nullable ItemStack[] contents1 = player.getEnderChest().getContents();
                Material material1 = Material.valueOf(config.getString("customItems.protect_item.material"));
                Material material2 = Material.valueOf(config.getString("customItems.ender_protect_item.material"));
                for (ItemStack item : contents) {

                    if (item == null || item.getType() == Material.AIR || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName())
                        continue;
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.hasDisplayName()) {
                        if (item.getType() == material1 && item.getItemMeta().getDisplayName().replace("§", "&").equalsIgnoreCase(config.getString("customItems.protect_item.name"))) {
                            item.setAmount(item.getAmount() - 1);
                            player.sendMessage(Objects.requireNonNull(config.getString("punishments.skipPunishMsg")));
                            method = item.getItemMeta().getDisplayName();
                            log.info("玩家 " + player.getName() + " 因为§a" + method + " §b跳过死亡惩罚");
                            return;
                        }
                    }
                }
                for (ItemStack item : contents1) {
                    if (item == null || item.getType() == Material.AIR || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName())
                        continue;
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.hasDisplayName()) {
                        if (item.getType() == material2 && item.getItemMeta()
                                .getDisplayName()
                                .replace("§", "&")
                                .equalsIgnoreCase(config.getString("customItems.ender_protect_item.name"))) {
                            item.setAmount(item.getAmount() - 1);
                            player.sendMessage(Objects.requireNonNull(config.getString("punishments.skipPunishMsg")));
                            method = item.getItemMeta().getDisplayName();
                            log.info("玩家 " + player.getName() + " 因为§a " + method + " §b跳过死亡惩罚");
                            return;
                        }
                    }
                }

                this.isDeath = true;
                this.food = player.getFoodLevel();
                log.info("§c玩家 §r" + player.getName() + "§c 受到了死亡惩罚");
                if (config.getBoolean("punishments.enableEpitaph")) {
                    var position = player.getLocation();

                    position.getBlock().setType(Material.BEDROCK);

                    String epitaph = config.getString("punishments.epitaph");
                    if (epitaph != null && epitaph.contains("%player%")) {
                        epitaph = epitaph.replace("%player%", player.getName());
                    }
                    EpitaphUtils.createFloatingText(position.clone().add(0, 1, 0), epitaph);
                }

                // 背包
                if (config.getBoolean("punishments.Inventory.enable")) {
                    if (Objects.requireNonNull(config.getString("punishments.Inventory.mode")).equalsIgnoreCase("all")) {
                        for (ItemStack item : player.getInventory().getContents()) {
                            if (item != null && !materials.contains(item.getType())) {
                                player.getInventory().remove(item);
                                if (!config.getBoolean("punishments.Inventory.clean")) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                                }
                            }
                        }
                    }

                    if (Objects.requireNonNull(config.getString("punishments.Inventory.mode")).equalsIgnoreCase("part")) {
                        int min = config.getInt("punishments.Inventory.amount.min");
                        int max = config.getInt("punishments.Inventory.amount.max");
                        int dropAmount = rand.nextInt(min, max+1);
                        if (min == max) dropAmount = min;


                        List<ItemStack> inventory = new ArrayList<>(Arrays.asList(player.getInventory().getContents()));
                        inventory.removeIf(Objects::isNull);
                        inventory.removeIf(item -> materials.contains(item.getType()));
                        Collections.shuffle(inventory);

                        dropAmount = Math.min(dropAmount, inventory.size());

                        for (int i = 0; i < dropAmount; i++) {
                            ItemStack item = inventory.get(i);
                            player.getInventory().clear(player.getInventory().first(item));
                            if (!config.getBoolean("punishments.Inventory.clean")) {
                                player.getWorld().dropItemNaturally(player.getLocation(), item);
                            }
                        }
                    }
                }
                if (config.getBoolean("punishments.clearEnderchestOnDeath")) {
                    player.getEnderChest().clear();
                }

                if (config.getBoolean("punishments.reduceExpOnDeath.enable")) {
                    player.setTotalExperience(0);
                    int level = player.getLevel();
                    player.setLevel((int) (level * (1 - config.getDouble("punishments.reduceExpOnDeath.value"))));
                }

                if (config.getBoolean("punishments.reduceMoneyOnDeath.enable")) {
                    double balance = econ.getBalance(player);
                    if (config.getInt("punishments.reduceMoneyOnDeath.mode") == 1) {
                        econ.withdrawPlayer(player, balance * (1 - config.getDouble("punishments.reduceMoneyOnDeath.value")));
                    } else if (config.getInt("punishments.reduceMoneyOnDeath.mode") == 2) {
                        econ.withdrawPlayer(player, (config.getDouble("punishments.reduceMoneyOnDeath.value")));
                    } else {
                        log.err("punishments.reduceMoneyOnDeath.mode 配置错误，值应为1或2");
                    }
                }
            }
        } else {
            method = "拥有bypass权限";
            log.info("玩家 " + player.getName() + " 因为§a " + method + " §b跳过死亡惩罚");
            player.sendMessage(Objects.requireNonNull(config.getString("punishments.skipPunishMsg")));
        }
    }

}