package com.deathPunish;

import com.tcoded.folialib.FoliaLib;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public final class DeathPunish extends JavaPlugin {

    public final static String VERSION = "1.4.1";
    public static Map<Boolean, String> ifNeedUpdate = new HashMap<>();;
    public static Economy econ = null;
    public static boolean enableEco = false;
    private FileConfiguration epitaphConfig;
    public static FileConfiguration config;
    private static File playerDataFile;
    private static FileConfiguration playerData;
    public ShapedRecipe enchantedGoldenAppleRecipe;
    public static com.deathPunish.Utils.LoggerUtils log;
    public static List<String> worlds;

    private static FoliaLib foliaLib;

    public static FoliaLib getFoliaLib() {return foliaLib;}

    @Override
    public void onEnable() {
        foliaLib = new FoliaLib(this);

        log = new com.deathPunish.Utils.LoggerUtils();
        int pluginId = 24171;
        com.deathPunish.Utils.Metrics metrics = new com.deathPunish.Utils.Metrics(this, pluginId);
        setupEconomy();
        // 保存默认配置文件
        saveDefaultConfig();
        config = getConfig();
        // 加载玩家数据
        loadPlayerData();
        // 注册自定义物品配方
        registerCustomRecipes(config);
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new com.deathPunish.Listener.PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new com.deathPunish.Listener.EatCustomItemListener(), this);
        // 注册命令
        Objects.requireNonNull(this.getCommand("deathpunish")).setExecutor(new DeathPunishCommand(this));
        Objects.requireNonNull(this.getCommand("deathpunish")).setTabCompleter(new DeathPunishCommand(this));
        setWorldRule();
        // 清除所有在线玩家的血量修饰符
        clearAllPlayersHealthModifiers();
        log.info("插件已启用");
        checkForUpdates();
    }

    @Override
    public void onDisable() {
        // 保存玩家数据
        savePlayerData();
        if (log != null) log.info("插件已禁用");
    }

    public void registerCustomRecipes(FileConfiguration config) {
        enchantedGoldenAppleRecipe = CustomItems.createEnchantedGoldenApple(config);
        getServer().resetRecipes(); // 重置配方
        getServer().addRecipe(enchantedGoldenAppleRecipe);
    }

    @Override
    public void saveDefaultConfig() {
        File configFile = new File(getDataFolder(), "config.yml");

        // 检查配置文件是否存在
        if (!configFile.exists()) {
            // 如果文件不存在，写入默认配置
            getConfig().options().copyDefaults(true);
            saveConfig();
        } else {
            // 如果文件存在，检查版本
            try {
                FileConfiguration config = getConfig();
                // 检查配置文件的版本
                if (!Objects.requireNonNull(config.getString("version")).equalsIgnoreCase(VERSION)) {
                    configFile.delete();
                    getConfig().options().copyDefaults(true);
                    saveConfig();
//                    log.info("[DeathPunish] §a已更新配置文件至 v" + VERSION);
                }
            } catch (Exception e) {
                // 如果配置文件读取失败，删除文件并写入默认配置
                configFile.delete();
                getConfig().options().copyDefaults(true);
                saveConfig();
                log.err("配置文件读取失败，已恢复默认配置");
            }
        }
    }

    private void setupEconomy() {
        boolean result = false;
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                econ = rsp.getProvider();
                result = true;
            }
        }
        if (result) {
            enableEco = true;
            log.info("经济内容已启动");
        } else {
            log.err("未检测到Vault，经济相关功能无法使用");
        }
    }

    public static Economy getEconomy() {
        return econ;
    }

    public void setWorldRule() {
        if (config.getBoolean("autoSetRule") && config.getBoolean("punishOnDeath.enable")) {
            worlds = config.getStringList("punishOnDeath.enableWorlds");
            for (String world : worlds) {
                if (Boolean.FALSE.equals(Objects.requireNonNull(Bukkit.getWorld(world)).getGameRuleValue(GameRule.KEEP_INVENTORY))) {
                    Objects.requireNonNull(Bukkit.getWorld(world)).setGameRule(GameRule.KEEP_INVENTORY, true);
                    log.warn("发现启用死亡惩罚的世界未开启死亡不掉落");
                    log.warn("已自动设置世界 " + world + " 的游戏规则");
                }
            }

        }
    }

    private void checkForUpdates() {
        Runnable runnable = () -> {
            try {
                URL url = new URL("https://api.github.com/repos/Findoutsider/DeathPunish/releases/latest");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    JSONObject jsonObject = getJsonObject(connection);
                    String latestVersion = (String) jsonObject.get("tag_name");
                    String info = (String) jsonObject.get("body");
                    if (latestVersion != null) {
                        int latestVersionInt = Integer.parseInt(latestVersion.replace("v", "").replace(".", ""));
                        int currentVersion = Integer.parseInt(VERSION.replace(".", ""));
                        if (latestVersionInt > currentVersion) {
                            log.info("检测到新版本: " + latestVersion + "，请前往 https://github.com/Findoutsider/DeathPunish 更新");

                            ifNeedUpdate.put(true, latestVersion);

                            if (!info.equalsIgnoreCase("")) {
                                log.info("新版本信息: " + info);
                            }
                        } else if (latestVersionInt < currentVersion) {
                            log.info("你正在使用开发版本！v" + VERSION);
                        } else {
                            log.info("当前版本已是最新: v" + VERSION);
                            ifNeedUpdate.put(false, null);
                        }
                    }
                } else {
                    log.err("获取最新版本失败: " + responseCode);
                }
            } catch (IOException | org.json.simple.parser.ParseException e) {
                log.err("获取最新版本时发生异常: " + e.getMessage() + Arrays.toString(e.getStackTrace()));
            }
        };
        if (DeathPunish.getFoliaLib().isFolia()) {
            DeathPunish.getFoliaLib().getScheduler().runAsync(wrappedTask -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(this, runnable);
        }
    }

    private static JSONObject getJsonObject(HttpURLConnection connection) throws IOException, org.json.simple.parser.ParseException {
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(content.toString());
    }
    
    /**
     * 加载玩家数据文件
     */
    private void loadPlayerData() {
        playerDataFile = new File(getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
            } catch (IOException e) {
                log.err("创建玩家数据文件失败: " + e.getMessage());
            }
        }
        playerData = YamlConfiguration.loadConfiguration(playerDataFile);
    }
    
    /**
     * 保存玩家数据
     */
    public static void savePlayerData() {
        try {
            if (playerData != null && playerDataFile != null) {
                playerData.save(playerDataFile);
                log.info("玩家数据已保存");
            }
        } catch (IOException e) {
            log.err("保存玩家数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存玩家的血量上限
     */
    public static void savePlayerMaxHealth(Player player, double maxHealth) {
        if (playerData != null) {
            // 保存玩家UUID和血量值
            String playerKey = "players." + player.getUniqueId();
            playerData.set(playerKey + ".maxHealth", maxHealth);
            playerData.set(playerKey + ".name", player.getName()); // 保存玩家名称，便于调试
            playerData.set(playerKey + ".lastSaved", System.currentTimeMillis()); // 记录保存时间
            
            log.info("保存玩家 " + player.getName() + " 的血量上限为: " + maxHealth);
            
            // 立即同步保存一次，确保数据不丢失
            try {
                if (playerDataFile != null) {
                    playerData.save(playerDataFile);
                    log.info("已同步保存玩家 " + player.getName() + " 的血量数据");
                }
            } catch (IOException e) {
                log.err("同步保存玩家数据失败: " + e.getMessage());
            }
        } else {
            log.err("无法保存玩家 " + player.getName() + " 的血量上限，playerData为空");
        }
    }
    
    /**
     * 获取玩家的血量上限
     */
    public static double getPlayerMaxHealth(Player player) {
        if (playerData == null) {
            log.err("无法获取玩家 " + player.getName() + " 的血量上限，playerData为空");
            return 20.0;
        }
        
        String playerKey = "players." + player.getUniqueId();
        
        if (playerData.contains(playerKey + ".maxHealth")) {
            double savedHealth = playerData.getDouble(playerKey + ".maxHealth");
            log.info("从数据文件读取到玩家 " + player.getName() + " 的血量上限: " + savedHealth);
            return savedHealth;
        } else {
            log.info("玩家 " + player.getName() + " 没有保存的血量上限数据，使用默认值20.0");
            return 20.0; // 默认最大生命值
        }
    }

    /**
     * 清除所有在线玩家的血量修饰符，确保插件启动时血量设置正确
     */
    private void clearAllPlayersHealthModifiers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHealth != null) {
                // 记录当前状态
                double currentBaseValue = maxHealth.getBaseValue();
                double currentValue = maxHealth.getValue();
                
                // 获取保存的血量值
                double savedMaxHealth = getPlayerMaxHealth(player);
                
                // 记录修饰符数量
                int modifierCount = 0;
                for (AttributeModifier modifier : maxHealth.getModifiers()) {
                    maxHealth.removeModifier(modifier);
                    modifierCount++;
                }
                
                // 如果有保存的血量值，使用它
                if (savedMaxHealth != 20.0) {
                    maxHealth.setBaseValue(savedMaxHealth);
                    log.info("插件启动时恢复玩家 " + player.getName() + " 的血量上限为: " + savedMaxHealth + 
                            "（原基础值: " + currentBaseValue + ", 实际值: " + currentValue + "，清除了 " + modifierCount + " 个修饰符）");
                    
                    // 使用PlayerListener同步血量显示
                    com.deathPunish.Listener.PlayerListener.syncHealthDisplay(this, player, savedMaxHealth);
                } else if (modifierCount > 0) {
                    log.info("插件启动时清除了玩家 " + player.getName() + " 的 " + modifierCount + " 个血量修饰符");
                }
            }
        }
    }
}
