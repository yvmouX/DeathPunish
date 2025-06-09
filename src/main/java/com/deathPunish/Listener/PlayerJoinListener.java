package com.deathPunish.Listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import static com.deathPunish.DeathPunish.*;
import static com.deathPunish.DeathPunish.ifNeedUpdate;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onAdminLogin(PlayerJoinEvent event) {
        var player = event.getPlayer();
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
    }



}
