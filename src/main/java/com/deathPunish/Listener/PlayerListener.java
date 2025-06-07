package com.deathPunish.Listener;

import com.deathPunish.DeathPunish;
import com.deathPunish.Epitaph;


import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.UUID;

import static com.deathPunish.DeathPunish.*;
import static com.deathPunish.DeathPunish.log;
import static com.deathPunish.DeathPunish.worlds;
import static com.deathPunish.DeathPunish.ifNeedUpdate;


public class PlayerListener implements Listener {

    private static final Set<Action> ACTIONS = Collections.unmodifiableSet(EnumSet.of(Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK));
    private boolean isDeath = false;
    private final Plugin pl;
    private int food;
    public static final List<Material> materials = new ArrayList<>();
    private AttributeInstance playerMaxHealth;
    private final Random rand = new Random();

    public PlayerListener(DeathPunish plugin) {
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
    public void onAdminLogin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        // 管理员登录时显示插件信息
        if (player.isOp()) {
            player.sendMessage("");
            player.sendMessage("[deathpunish] §a当前插件版本为" + VERSION);
            player.sendMessage("[deathpunish] §a配置文件版本为" + config.getString("version"));
            player.sendMessage("[deathpunish] §a若二者版本不同请手动删除配置文件后重启服务器");
            player.sendMessage("[deathpunish] §a前往 https://github.com/Findoutsider/DeathPunish 获取更新");
            player.sendMessage("");
            if (ifNeedUpdate.get(true) != null) {
                player.sendMessage("§8[§bDeathPunish§8] §a检测到新版本: §6" + ifNeedUpdate.get(true) + "§a，请前往§b https://github.com/Findoutsider/DeathPunish §a更新");
            }
        }
        
        // 无条件恢复玩家的血量上限，不再依赖于配置文件中的设置
        log.info("玩家 " + player.getName() + " 加入游戏，尝试恢复血量上限");
        restorePlayerMaxHealth(player);
    }


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        var player = event.getEntity();
        World world = player.getWorld();
        if (!worlds.contains(world.getName())) {
            log.info("§c玩家 " + player.getName() + " 在未启用死亡惩罚的 " + world.getName() + " 世界中死亡");
            return;
        }
        // 获取玩家当前的血量上限属性
        playerMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (playerMaxHealth == null) {
            log.err("无法获取玩家 " + player.getName() + " 的血量属性");
            return;
        }
        
        // 记录玩家死亡前的血量上限
        double baseMaxHealth = playerMaxHealth.getBaseValue();
        log.info("玩家 " + player.getName() + " 死亡前的血量上限: " + baseMaxHealth);
        
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
//                List<String> epitaphs = epitaphConfig.getStringList("defaultEpitaph");
//                if (!epitaphs.isEmpty()) {
//                    Random random = new Random();
//                    String selectedEpitaph = epitaphs.get(random.nextInt(epitaphs.size()));
//                    // 创建悬浮文本
//                    NMSUtil.createFloatingText(position.clone().add(0, 1, 0), selectedEpitaph);
//                }
                    String epitaph = config.getString("punishments.epitaph");
                    if (epitaph != null && epitaph.contains("%player%")) {
                        epitaph = epitaph.replace("%player%", player.getName());
                    }
                    Epitaph.createFloatingText(position.clone().add(0, 1, 0), epitaph);
                }

                // 背包
                if (config.getBoolean("punishments.Inventory.enable")) {
                    if (Objects.requireNonNull(config.getString("punishments.Inventory.mode")).equalsIgnoreCase("all")) {
                        // 处理主物品栏
                        ItemStack[] inventoryContents = player.getInventory().getContents().clone(); // 克隆数组以防止修改时的并发问题
                        for (int slot = 0; slot < inventoryContents.length; slot++) {
                            ItemStack item = inventoryContents[slot];
                            if (item != null && !materials.contains(item.getType())) {
                                ItemStack itemToDrop = item.clone(); // 克隆物品以防止引用问题
                                player.getInventory().setItem(slot, null);
                                if (!config.getBoolean("punishments.Inventory.clean")) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), itemToDrop);
                                }
                            }
                        }
                        
                        // 处理装备栏
                        ItemStack[] armorContents = player.getInventory().getArmorContents().clone();
                        for (int slot = 0; slot < armorContents.length; slot++) {
                            ItemStack item = armorContents[slot];
                            if (item != null && !materials.contains(item.getType())) {
                                ItemStack itemToDrop = item.clone();
                                armorContents[slot] = null;
                                if (!config.getBoolean("punishments.Inventory.clean")) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), itemToDrop);
                                }
                            }
                        }
                        player.getInventory().setArmorContents(armorContents);
                        
                        // 处理副手
                        ItemStack offHandItem = player.getInventory().getItemInOffHand();
                        if (offHandItem != null && offHandItem.getType() != Material.AIR && !materials.contains(offHandItem.getType())) {
                            ItemStack itemToDrop = offHandItem.clone();
                            player.getInventory().setItemInOffHand(null);
                            if (!config.getBoolean("punishments.Inventory.clean")) {
                                player.getWorld().dropItemNaturally(player.getLocation(), itemToDrop);
                            }
                        }
                    }

                    if (Objects.requireNonNull(config.getString("punishments.Inventory.mode")).equalsIgnoreCase("part")) {
                        int min = config.getInt("punishments.Inventory.amount.min");
                        int max = config.getInt("punishments.Inventory.amount.max");
                        int dropAmount = rand.nextInt(min, max+1);
                        if (min == max) dropAmount = min;

                        // 收集所有物品：主物品栏、装备栏和副手
                        List<ItemStack> inventory = new ArrayList<>();
                        
                        // 添加主物品栏物品
                        for (ItemStack item : player.getInventory().getContents()) {
                            if (item != null && item.getType() != Material.AIR && !materials.contains(item.getType())) {
                                inventory.add(item);
                            }
                        }
                        
                        // 添加装备栏物品
                        for (ItemStack item : player.getInventory().getArmorContents()) {
                            if (item != null && item.getType() != Material.AIR && !materials.contains(item.getType())) {
                                inventory.add(item);
                            }
                        }
                        
                        // 添加副手物品
                        ItemStack offHandItem = player.getInventory().getItemInOffHand();
                        if (offHandItem != null && offHandItem.getType() != Material.AIR && !materials.contains(offHandItem.getType())) {
                            inventory.add(offHandItem);
                        }

                        Collections.shuffle(inventory);
                        dropAmount = Math.min(dropAmount, inventory.size());

                        for (int i = 0; i < dropAmount && i < inventory.size(); i++) {
                            ItemStack item = inventory.get(i);
                            // 检查物品在哪个位置（主物品栏、装备栏或副手）
                            // 主物品栏
                            int slot = player.getInventory().first(item);
                            if (slot >= 0) {
                                ItemStack itemToDrop = player.getInventory().getItem(slot).clone();
                                player.getInventory().setItem(slot, null);
                                if (!config.getBoolean("punishments.Inventory.clean")) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), itemToDrop);
                                }
                                continue;
                            }
                            
                            // 装备栏
                            ItemStack[] armorContents = player.getInventory().getArmorContents();
                            for (int armorSlot = 0; armorSlot < armorContents.length; armorSlot++) {
                                if (item.equals(armorContents[armorSlot])) {
                                    ItemStack itemToDrop = armorContents[armorSlot].clone();
                                    armorContents[armorSlot] = null;
                                    player.getInventory().setArmorContents(armorContents);
                                    if (!config.getBoolean("punishments.Inventory.clean")) {
                                        player.getWorld().dropItemNaturally(player.getLocation(), itemToDrop);
                                    }
                                    break;
                                }
                            }
                            
                            // 副手
                            if (item.equals(player.getInventory().getItemInOffHand())) {
                                ItemStack itemToDrop = player.getInventory().getItemInOffHand().clone();
                                player.getInventory().setItemInOffHand(null);
                                if (!config.getBoolean("punishments.Inventory.clean")) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), itemToDrop);
                                }
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


    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        var player = event.getPlayer();
        if (this.isDeath) {
            if (config.getBoolean("punishOnDeath.enable")) {
                List<String> deathMsg = Objects.requireNonNull(config.getStringList("punishments.deathMsg"));
                for (String msg : deathMsg) {
                    player.sendMessage(msg);
                }
                
                // 死亡惩罚逻辑
                if (playerMaxHealth != null) {
                    double maxHealth = playerMaxHealth.getBaseValue(); // 使用基础值
                double reduceHealthAmount = config.getDouble("punishments.reduceHealthAmount");
                    
                    log.info("玩家 " + player.getName() + " 复活时的血量上限: " + maxHealth);
                    
                    if (config.getBoolean("punishments.banOnDeath") && maxHealth <= 1) {
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
                    if (DeathPunish.getFoliaLib().isFolia()) {
                        DeathPunish.getFoliaLib().getScheduler().runLater(wrappedTask -> {
                            applyHealthPunishment(player);
                        }, 1L);
                    } else {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                applyHealthPunishment(player);
                            }
                        }.runTaskLater(pl, 1L); // 延迟1个tick执行，确保玩家已完全复活
                    }
                    }
                } else {
                    log.err("无法获取玩家 " + player.getName() + " 的血量属性，跳过血量惩罚");
                }
                
                // 处理饥饿度
                if (config.getBoolean("punishments.foodLevel.save")) {
                    if (DeathPunish.getFoliaLib().isFolia()) {
                        DeathPunish.getFoliaLib().getScheduler().runLater(wrappedTask -> {
                            player.setFoodLevel(food);
                        }, 1L);
                    } else {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.setFoodLevel(food);
                            }
                        }.runTaskLater(pl, 1L);
                    }
                } else {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.setFoodLevel(config.getInt("punishments.foodLevel.value"));
                        }
                    }.runTaskLater(pl, 1L);
                }
                
                // 处理负面效果
                if (config.getBoolean("punishments.debuff.enable")) {
                    List<String> debuff = config.getStringList("punishments.debuff.potions");
                    Runnable runnable = () -> {
                        for (String effect : debuff) {
                            String[] parts = effect.split(" ");
                            if (parts.length == 3) {
                                PotionEffectType type = PotionEffectType.getByKey(NamespacedKey.minecraft(parts[0]));
                                int duration = Integer.parseInt(parts[1]);
                                int amplifier = Integer.parseInt(parts[2]);
                                if (type != null) {
                                    boolean res = player.addPotionEffect(new PotionEffect(type, duration, amplifier));
                                } else {
                                    log.info("无效的药水效果: " + parts[0]);
                                }
                            }
                        }
                    };
                    if (DeathPunish.getFoliaLib().isFolia()) {
                        DeathPunish.getFoliaLib().getScheduler().runLater(wrappedTask -> runnable.run(), 1L);
                    } else {
                        Bukkit.getScheduler().runTaskLater(pl, runnable, 1L);
                    }
                }

                this.isDeath = false;
            }
        }
    }


    @EventHandler
    public void onPlayerRightClick(PlayerInteractEvent event) {
        if (ACTIONS.contains(event.getAction())) {
            ItemStack itemInMainHand = event.getPlayer().getInventory().getItemInMainHand();
            if ((itemInMainHand.getType() == Material.valueOf(config.getString("customItems.protect_item.material"))
                    && Objects.requireNonNull(itemInMainHand.getItemMeta()).getDisplayName().replace("§", "&").equalsIgnoreCase(config.getString("customItems.protect_item.name")))
                    || (itemInMainHand.getType() == Material.valueOf(config.getString("customItems.ender_protect_item.material"))
                    && Objects.requireNonNull(itemInMainHand.getItemMeta()).getDisplayName().replace("§", "&").equalsIgnoreCase(config.getString("customItems.ender_protect_item.name")))) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 从玩家数据中恢复正确的血量上限，在玩家重新连接时调用
     */
    private void restorePlayerMaxHealth(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth == null) {
            log.err("无法获取玩家 " + player.getName() + " 的血量属性");
            return;
        }
        
        // 获取并显示当前血量状态
        double currentBaseValue = maxHealth.getBaseValue();
        double currentValue = maxHealth.getValue();
        log.info("玩家 " + player.getName() + " 当前基础血量上限: " + currentBaseValue + ", 实际血量上限: " + currentValue);
        
        // 从数据文件获取玩家的保存血量
        double savedMaxHealth = DeathPunish.getPlayerMaxHealth(player);
        log.info("玩家 " + player.getName() + " 从数据文件读取的血量上限: " + savedMaxHealth);
        
        // 记录玩家所有的血量修饰符，帮助调试
        StringBuilder modifiersInfo = new StringBuilder();
        for (AttributeModifier modifier : maxHealth.getModifiers()) {
            modifiersInfo.append("\n  - ").append(modifier.getName())
                        .append(": ").append(modifier.getAmount())
                        .append(", 操作: ").append(modifier.getOperation())
                        .append(", UUID: ").append(modifier.getUniqueId());
        }
        if (modifiersInfo.length() > 0) {
            log.info("玩家 " + player.getName() + " 的血量修饰符:" + modifiersInfo);
        }
        
        // 如果玩家有保存的血量上限数据，始终强制应用它
        if (savedMaxHealth != 20.0) {
            // 清除所有修饰符，避免其他插件的干扰
            int modifierCount = 0;
            for (AttributeModifier modifier : maxHealth.getModifiers()) {
                maxHealth.removeModifier(modifier);
                modifierCount++;
            }
            if (modifierCount > 0) {
                log.info("已清除玩家 " + player.getName() + " 的 " + modifierCount + " 个血量修饰符");
            }
            
            // 设置为保存的血量上限
            maxHealth.setBaseValue(savedMaxHealth);
            
            log.info("玩家 " + player.getName() + " 重新连接，已将血量上限从 " + currentBaseValue + " 强制设置为: " + savedMaxHealth);
            
            // 同步血量显示
            syncHealthDisplay(pl, player, savedMaxHealth);
            
            // 在下一个tick再次检查，确保没有其他插件修改了血量
            Runnable runnable = () -> {
                // 再次获取当前血量状态
                double newBaseValue = maxHealth.getBaseValue();
                double newValue = maxHealth.getValue();

                // 如果值不匹配，再次尝试设置
                if (Math.abs(newBaseValue - savedMaxHealth) > 0.1 || Math.abs(newValue - savedMaxHealth) > 0.1) {
                    log.warn("玩家 " + player.getName() + " 的血量值在延迟检查中不匹配！基础值: " +
                            newBaseValue + ", 实际值: " + newValue + ", 期望值: " + savedMaxHealth);

                    // 再次清除所有修饰符
                    for (AttributeModifier modifier : maxHealth.getModifiers()) {
                        maxHealth.removeModifier(modifier);
                    }

                    // 再次设置基础值
                    maxHealth.setBaseValue(savedMaxHealth);
                    log.info("已再次强制设置玩家 " + player.getName() + " 的血量上限为: " + savedMaxHealth);

                    // 再次同步血量显示
                    syncHealthDisplay(pl, player, savedMaxHealth);
                }

                // 确保当前生命值不超过最大值
                if (player.getHealth() > savedMaxHealth) {
                    player.setHealth(savedMaxHealth);
                    log.info("调整玩家 " + player.getName() + " 当前生命值为: " + savedMaxHealth);
                }
            };
            if (DeathPunish.getFoliaLib().isFolia()) {
                DeathPunish.getFoliaLib().getScheduler().runLater(runnable, 10L);
            } else {
                Bukkit.getScheduler().runTaskLater(pl, runnable, 10L); // 延迟10个tick检查，确保其他插件有机会运行
            }
        } else {
            log.info("玩家 " + player.getName() + " 没有保存的血量上限数据，保持当前值");
        }
        
        // 最后检查实际的血量上限
        log.info("最终检查 - 玩家 " + player.getName() + " 基础血量上限: " + maxHealth.getBaseValue() + ", 实际血量上限: " + maxHealth.getValue());
    }

    /**
     * 使用属性修饰符应用血量惩罚，与任何插件兼容
     */
    private void applyHealthPunishment(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth == null) {
            log.err("无法获取玩家 " + player.getName() + " 的血量属性");
            return;
        }

        // 记录所有当前的修饰符
        StringBuilder modifiersInfo = new StringBuilder();
        for (AttributeModifier modifier : maxHealth.getModifiers()) {
            modifiersInfo.append("\n  - ").append(modifier.getName())
                      .append(": ").append(modifier.getAmount())
                      .append(", 操作: ").append(modifier.getOperation())
                      .append(", UUID: ").append(modifier.getUniqueId());
        }
        if (modifiersInfo.length() > 0) {
            log.info("应用血量惩罚前，玩家 " + player.getName() + " 的血量修饰符:" + modifiersInfo);
        }

        // 先获取保存的血量值，如果有的话，应该是更可靠的初始值
        double savedMaxHealth = DeathPunish.getPlayerMaxHealth(player);
        
        // 获取配置中设置的减少量
        double reduceHealthAmount = config.getDouble("punishments.reduceHealthAmount");
        
        // 获取当前实际的最大生命值
        double currentBaseValue = maxHealth.getBaseValue();
        double currentMaxHealth = maxHealth.getValue();
        
        log.info("死亡惩罚计算 - 玩家 " + player.getName() + " 当前基础血量上限: " + currentBaseValue + 
                ", 实际血量上限: " + currentMaxHealth + ", 保存的血量上限: " + savedMaxHealth);
        
        // 确定起始血量值（优先使用保存的值，除非是默认值20）
        double startingHealth = (savedMaxHealth != 20.0) ? savedMaxHealth : currentBaseValue;
        
        // 计算新的最大生命值（确保不低于1）
        double newMaxHealth = Math.max(1.0, startingHealth - reduceHealthAmount);
        
        // 完全清除所有修饰符，避免其他插件的干扰
        int modifierCount = 0;
        for (AttributeModifier modifier : maxHealth.getModifiers()) {
            maxHealth.removeModifier(modifier);
            modifierCount++;
        }
        if (modifierCount > 0) {
            log.info("已清除玩家 " + player.getName() + " 的 " + modifierCount + " 个血量修饰符");
        }
        
        // 确保血量是减少的
        if (newMaxHealth >= startingHealth) {
            log.warn("警告：计算的新血量上限(" + newMaxHealth + ")大于或等于起始血量上限(" + startingHealth + ")，强制减少血量");
            newMaxHealth = startingHealth - reduceHealthAmount;
            // 再次确保不小于1
            newMaxHealth = Math.max(1.0, newMaxHealth);
        }
        
        // 使用四舍五入保留2位小数，避免浮点数精度问题
        newMaxHealth = Math.round(newMaxHealth * 100) / 100.0;
        
        // 设置新的血量上限
        maxHealth.setBaseValue(newMaxHealth);
        
        // 同步血量显示，确保界面上显示正确的红心数
        syncHealthDisplay(pl, player, newMaxHealth);
        
        // 保存玩家的新血量上限到数据文件
        DeathPunish.savePlayerMaxHealth(player, newMaxHealth);
        
        log.info("玩家 " + player.getName() + " 血量上限从 " + startingHealth + " 减少到 " + newMaxHealth + 
                "（减少了 " + reduceHealthAmount + "）");
        
        // 确保当前生命值不超过新的最大值
        if (player.getHealth() > newMaxHealth) {
            player.setHealth(newMaxHealth);
            log.info("调整玩家 " + player.getName() + " 当前生命值为: " + newMaxHealth);
        }
        
        // 延迟检查，确保没有其他插件干扰我们的设置
        double finalNewMaxHealth = newMaxHealth;
        Runnable runnable = () -> {
            // 再次获取当前血量状态
            double finalBaseValue = maxHealth.getBaseValue();
            double finalValue = maxHealth.getValue();

            // 如果值不匹配，记录警告
            if (Math.abs(finalBaseValue - finalNewMaxHealth) > 0.1) {
                log.warn("警告：玩家 " + player.getName() + " 的血量基础值在延迟检查中不匹配！" +
                        "应为: " + finalNewMaxHealth + ", 实际为: " + finalBaseValue);

                // 记录修饰符
                StringBuilder finalModifiers = new StringBuilder();
                for (AttributeModifier modifier : maxHealth.getModifiers()) {
                    finalModifiers.append("\n  - ").append(modifier.getName())
                            .append(": ").append(modifier.getAmount())
                            .append(", 操作: ").append(modifier.getOperation())
                            .append(", UUID: ").append(modifier.getUniqueId());
                }
                if (finalModifiers.length() > 0) {
                    log.info("延迟检查时，玩家 " + player.getName() + " 的血量修饰符:" + finalModifiers);
                }

                // 再次尝试设置
                for (AttributeModifier modifier : maxHealth.getModifiers()) {
                    maxHealth.removeModifier(modifier);
                }
                maxHealth.setBaseValue(finalNewMaxHealth);
                log.info("已再次强制设置玩家 " + player.getName() + " 的血量上限为: " + finalNewMaxHealth);

                // 再次同步血量显示
                syncHealthDisplay(pl, player, finalNewMaxHealth);
            }
        };
        if (DeathPunish.getFoliaLib().isFolia()) {
            DeathPunish.getFoliaLib().getScheduler().runLater(runnable, 20L);
        } else {
            Bukkit.getScheduler().runTaskLater(pl, runnable, 20L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 获取当前血量上限
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            double currentBaseValue = maxHealth.getBaseValue();
            double currentValue = maxHealth.getValue();
            
            // 记录所有修饰符
            StringBuilder modifiersInfo = new StringBuilder();
            for (AttributeModifier modifier : maxHealth.getModifiers()) {
                modifiersInfo.append("\n  - ").append(modifier.getName())
                          .append(": ").append(modifier.getAmount())
                          .append(", 操作: ").append(modifier.getOperation())
                          .append(", UUID: ").append(modifier.getUniqueId());
            }
            
            log.info("玩家 " + player.getName() + " 退出游戏，基础血量上限: " + currentBaseValue + 
                    ", 实际血量上限: " + currentValue);
            
            if (modifiersInfo.length() > 0) {
                log.info("玩家 " + player.getName() + " 退出时的血量修饰符:" + modifiersInfo);
            }
            
            // 保存当前血量上限
            DeathPunish.savePlayerMaxHealth(player, currentBaseValue);
            log.info("玩家 " + player.getName() + " 退出游戏，保存血量上限: " + currentBaseValue);
        }
    }

    /**
     * 同步玩家的实际血量和显示的红心
     * 确保界面上的红心数与实际生命值一致
     */
    public static void syncHealthDisplay(Plugin plugin, Player player, double healthValue) {
        // 首先确保玩家的当前生命值不超过最大值
        if (player.getHealth() > healthValue) {
            player.setHealth(healthValue);
        }
        
        // 通过先设置为1再设回正常值，强制刷新客户端界面
        if (DeathPunish.getFoliaLib().isFolia()) {
            DeathPunish.getFoliaLib().getScheduler().runLater(() -> {
                // 先保存当前生命值
                double currentHealth = player.getHealth();
                // 设置一个临时的低值，强制客户端更新
                player.setHealth(Math.min(1.0, currentHealth));

                // 再设回正常值
                DeathPunish.getFoliaLib().getScheduler().runLater(() -> {
                    player.setHealth(currentHealth);
                    log.info("已同步玩家 " + player.getName() + " 的血量显示，当前生命值: " + currentHealth);
                }, 2L);
            }, 2L);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    // 先保存当前生命值
                    double currentHealth = player.getHealth();
                    // 设置一个临时的低值，强制客户端更新
                    player.setHealth(Math.min(1.0, currentHealth));

                    // 再设回正常值
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.setHealth(currentHealth);
                            log.info("已同步玩家 " + player.getName() + " 的血量显示，当前生命值: " + currentHealth);
                        }
                    }.runTaskLater(plugin, 2L);
                }
            }.runTaskLater(plugin, 2L);
        }
    }
}