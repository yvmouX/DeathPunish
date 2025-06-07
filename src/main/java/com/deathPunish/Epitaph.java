package com.deathPunish;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import java.util.Objects;

public class Epitaph {

    public static void createFloatingText(Location location, String text) {
        ArmorStand armorStand = Objects.requireNonNull(location.getWorld()).spawn(location, ArmorStand.class);

        armorStand.setVisible(false);
        armorStand.setCustomName(text);
        armorStand.setCustomNameVisible(true);
        armorStand.setMarker(true);
        armorStand.setGravity(false);
        armorStand.setCollidable(false);
        armorStand.setInvulnerable(true);

        double x = (int) (location.getX()) - 0.5;
        double y = location.getY();
        double z = (int) (location.getZ()) - 0.5;
        armorStand.teleport(new Location(location.getWorld(), x, y, z));


    }
}
