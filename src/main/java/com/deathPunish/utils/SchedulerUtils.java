package com.deathPunish.Utils;

import com.deathPunish.DeathPunish;
import com.tcoded.folialib.enums.EntityTaskResult;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.CompletableFuture;

public class SchedulerUtils {
    public static boolean isFolia() {
        return DeathPunish.getFoliaLib().isFolia();
    }

    public static AsyncTask runTask(Plugin plugin, Runnable runnable, Entity entity, Location location) {
        if (DeathPunish.getFoliaLib().isFolia()) {
            if (!entity.isEmpty()) {
                CompletableFuture<EntityTaskResult> task = DeathPunish.getFoliaLib().getScheduler().runAtEntity(entity, wrappedTask -> runnable.run());
                return new AsyncTask(null, null, task, -1);
            } else if (location != null) {
                CompletableFuture<Void> task = DeathPunish.getFoliaLib().getScheduler().runAtLocation(location, wrappedTask -> runnable.run());
                return new AsyncTask(null, null, task, -1);
            } else {
                CompletableFuture<Void> task = DeathPunish.getFoliaLib().getScheduler().runNextTick(wrappedTask -> runnable.run());
                return new AsyncTask(null, null, task, -1);
            }
        } else {
            BukkitTask task = Bukkit.getScheduler().runTask(plugin, runnable);
            return new AsyncTask(task, null, null, -1);
        }
    }

    public static AsyncTask runTaskAsynchronously(Plugin plugin, Runnable runnable) {
        if (DeathPunish.getFoliaLib().isFolia()) {
            CompletableFuture<Void> task = DeathPunish.getFoliaLib().getScheduler().runAsync(wrappedTask -> runnable.run());
            return new AsyncTask(null, null, task, -1);
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            return new AsyncTask(task, null, null, -1);
        }
    }

    public static AsyncTask runTaskLater(final Plugin plugin, final Runnable runnable, long delay, Entity entity, Location location) {
        if (DeathPunish.getFoliaLib().isFolia()) {
            WrappedTask task;
            if (!entity.isEmpty()) {
                task = DeathPunish.getFoliaLib().getScheduler().runAtEntityLater(entity, runnable, delay);
            } else if (location != null) {
                task = DeathPunish.getFoliaLib().getScheduler().runAtLocationLater(location, runnable, delay);
            } else {
                task = DeathPunish.getFoliaLib().getScheduler().runLater(runnable, delay);
            }
            return new AsyncTask(null, task, null, -1);
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
            return new AsyncTask(task, null, null, -1);
        }
    }

    public static AsyncTask runTaskTimerAsynchronously(final Plugin plugin, final Runnable runnable, long delay, long period) {
        if (DeathPunish.getFoliaLib().isFolia()) {
            WrappedTask task = DeathPunish.getFoliaLib().getScheduler().runTimerAsync(runnable, delay, period);
            return new AsyncTask(null, task, null, -1);
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period);
            return new AsyncTask(task, null, null, -1);
        }
    }

    public static AsyncTask runTaskLaterAsynchronously(final Plugin plugin, final Runnable runnable, long delay) {
        if (DeathPunish.getFoliaLib().isFolia()) {
            WrappedTask task = DeathPunish.getFoliaLib().getScheduler().runLaterAsync(runnable, delay);
            return new AsyncTask(null, task, null, -1);
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            return new AsyncTask(task, null, null, -1);
        }
    }

    public static AsyncTask scheduleSyncDelayedTask(Plugin plugin, Runnable runnable, long delay, Entity entity, Location location) {
        if (DeathPunish.getFoliaLib().isFolia()) {
            WrappedTask task;
            if (!entity.isEmpty()) {
                task = DeathPunish.getFoliaLib().getScheduler().runAtEntityLater(entity, runnable, delay);
            } else if (location != null) {
                task = DeathPunish.getFoliaLib().getScheduler().runAtLocationLater(location, runnable, delay);
            } else {
                task = DeathPunish.getFoliaLib().getScheduler().runLater(runnable, delay);
            }
            return new AsyncTask(null, task, null, -1);
        } else {
            int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, delay);
            return new AsyncTask(null, null, null, taskId);
        }
    }

    public static AsyncTask scheduleSyncRepeatingTask(Plugin plugin, Runnable runnable, long delay, long period, Entity entity, Location location) {
        if (DeathPunish.getFoliaLib().isFolia()) {
            WrappedTask task;
            if (!entity.isEmpty()) {
                task = DeathPunish.getFoliaLib().getScheduler().runAtEntityTimer(entity, runnable, delay, period);
            } else if (location != null) {
                task = DeathPunish.getFoliaLib().getScheduler().runAtLocationTimer(location, runnable, delay, period);
            } else {
                task = DeathPunish.getFoliaLib().getScheduler().runTimer(runnable, delay, period);
            }
            return new AsyncTask(null, task, null, -1);
        } else {
            int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, runnable, delay, period);
            return new AsyncTask(null, null, null, taskId);
        }
    }

    /**
     * 统一封装异步任务结果
     *
     * @param taskId 用于存储传统任务ID
     */
    public record AsyncTask(BukkitTask bukkitTask, WrappedTask foliaTask, CompletableFuture<?> future, int taskId) {

        /**
         * 取消任务
         */
        public void cancel() {
            if (SchedulerUtils.isFolia()) {
                if (foliaTask != null) foliaTask.cancel();
                if (future != null) future.cancel(true);
            } else {
                if (bukkitTask != null) {
                    bukkitTask.cancel();
                } else if (taskId != -1) {
                    Bukkit.getScheduler().cancelTask(taskId);
                }
            }
        }

        /**
         * 检查任务是否已取消
         */
        public boolean isCancelled() {
            if (SchedulerUtils.isFolia()) {
                return foliaTask != null && foliaTask.isCancelled();
            } else {
                if (bukkitTask != null) {
                    return bukkitTask.isCancelled();
                }
                return !Bukkit.getScheduler().isCurrentlyRunning(taskId) && !Bukkit.getScheduler().isQueued(taskId);
            }
        }
    }
}
