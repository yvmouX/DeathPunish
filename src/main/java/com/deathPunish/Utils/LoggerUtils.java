package com.deathPunish.Utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;


public class LoggerUtils {
    public static CommandSender console;
    public static final String prefix = "§8[§bDeathPunish§8]§r ";

    public LoggerUtils() {
        console = Bukkit.getConsoleSender();
    }

    public void info(String s) {
        s = "§a " + s;
        console.sendMessage(prefix + s);
    }

    public void warn(String s) {
        console.sendMessage(prefix + "§e" + s);
    }

    public void serve_warn(String s, Boolean italic) {
        if (italic) s = "§o" + s;
        console.sendMessage(prefix + "§e§l" + s);
    }

    public void err(String s) {
        console.sendMessage(prefix + "§c"  + s);
    }

    public void serve_err(String s, Boolean italic) {
        if (italic) s = "§o" + s;
        console.sendMessage(prefix + "§c§l" + s);
    }
}
