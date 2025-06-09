package com.deathPunish.Utils.manager;

import com.deathPunish.DeathPunish;
import com.deathPunish.Utils.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Objects;

import static com.deathPunish.DeathPunish.log;

public class WorldManger {
    private final DeathPunish pl;
    private final FileConfiguration config;

    public WorldManger(DeathPunish pl) {
        this.pl = pl;
        this.config = pl.getConfig();
        setWorldRule();
    }

    public void setWorldRule() {
        SchedulerUtils.runTask(pl, () -> {
            List<String> worlds = config.getStringList("punishOnDeath.enableWorlds");
            // 死亡不掉落
            boolean x = config.getBoolean("autoSetRule");
            if (config.getBoolean("punishOnDeath.enable")) {
                for (String world : worlds) {
                    Objects.requireNonNull(Bukkit.getWorld(world)).setGameRule(GameRule.KEEP_INVENTORY, x);
                    log.warn("发现启用死亡惩罚的世界未开启死亡不掉落");
                    log.warn("已自动设置世界 " + world + " 的游戏规则为" + x);
                }
            }
            // 立即重生
            boolean y = SchedulerUtils.isFolia() ? true : config.getBoolean("doImmediateRespawn");
            if (config.getBoolean("punishOnDeath.enable")) {
                for (String world : worlds) {
                    Objects.requireNonNull(Bukkit.getWorld(world)).setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, y);
                    log.warn("发现启用死亡惩罚的世界未开启立即重生");
                    log.warn("已自动设置世界 " + world + " 的游戏规则为" + y);
                }
            }
        });
    }
}
