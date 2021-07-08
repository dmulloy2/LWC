/*
 * Copyright 2011 Tyler Blair. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and contributors and should not be interpreted as representing official policies,
 * either expressed or implied, of anybody else.
 */

package com.griefcraft.listeners;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.griefcraft.bukkit.EntityBlock;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Flag;
import com.griefcraft.model.Protection;
import com.griefcraft.scripting.event.LWCProtectionRegisterEvent;
import com.griefcraft.scripting.event.LWCProtectionRegistrationPostEvent;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.inventory.ItemStack;

public class LWCEntityListener implements Listener {

    private final Map<String, Location> playerCreatedEntities = new HashMap<>();

    /**
     * The plugin instance
     */
    private LWCPlugin plugin;

    public LWCEntityListener(LWCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void entityInteract(EntityInteractEvent event) {
        Block block = event.getBlock();

        Protection protection = plugin.getLWC().findProtection(block.getLocation());

        if (protection != null) {
            boolean allowEntityInteract = Boolean.parseBoolean(plugin.getLWC().resolveProtectionConfiguration(block, "allowEntityInteract"));
            if (!allowEntityInteract) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void entityBreakDoor(EntityBreakDoorEvent event) {
        Block block = event.getBlock();

        // See if there is a protection there
        Protection protection = plugin.getLWC().findProtection(block.getLocation());
        if (protection != null) {
            // protections.allowEntityBreakDoor
            boolean allowEntityBreakDoor = Boolean.parseBoolean(plugin.getLWC().resolveProtectionConfiguration(block, "allowEntityBreakDoor"));
            if (!allowEntityBreakDoor) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!LWC.ENABLED || event.isCancelled()) {
            return;
        }

        LWC lwc = plugin.getLWC();

        for (Block block : event.blockList()) {
            Protection protection = lwc.findProtection(block.getLocation());
            if (protection != null) {
                boolean ignoreExplosions = Boolean.parseBoolean(lwc.resolveProtectionConfiguration(protection.getBlock(), "ignoreExplosions"));
                if (!(ignoreExplosions || protection.hasFlag(Flag.Type.ALLOWEXPLOSIONS))) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityExplodeMonitor(EntityExplodeEvent event) {
        if (!LWC.ENABLED || event.isCancelled()) {
            return;
        }

        LWC lwc = plugin.getLWC();

        for (Block block : event.blockList()) {
            Protection protection = lwc.findProtection(block.getLocation());
            if (protection != null) {
                boolean ignoreExplosions = Boolean.parseBoolean(lwc.resolveProtectionConfiguration(protection.getBlock(), "ignoreExplosions"));
                if (ignoreExplosions || protection.hasFlag(Flag.Type.ALLOWEXPLOSIONS)) {
                    protection.remove();
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (!LWC.ENABLED || event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        Entity block = event.getEntity();

        entityCreatedByPlayer(block, player);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!LWC.ENABLED || event.isCancelled()) {
            return;
        }

        final ItemStack inHand = event.getItem();
        if (inHand != null &&
            (inHand.getType() == Material.ARMOR_STAND
             || inHand.getType() == Material.MINECART
             || inHand.getType() == Material.HOPPER_MINECART
             || inHand.getType() == Material.CHEST_MINECART)) {
            // actual location of an armor stand is x+0.5 z+0.5 from the air block coords
            // minecarts spawn at y+0.0625, though
            final Location l = event.getClickedBlock().getRelative(inHand.getType() == Material.ARMOR_STAND ? event.getBlockFace() : BlockFace.SELF).getLocation();
            playerCreatedEntities.put(event.getPlayer().getName(), l);
        }
    }

    private void checkCreation(final Entity entity) {
        if (!LWC.ENABLED) {
            return;
        }

        final Location loc = entity.getLocation();
        Iterator<Map.Entry<String, Location>> iter = playerCreatedEntities.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Location> entry = iter.next();
            if (entry.getValue().getWorld().equals(loc.getWorld()) && entry.getValue().distanceSquared(loc) < 1) {
                iter.remove();

                Player player = Bukkit.getPlayerExact(entry.getKey());
                if (player != null) {
                    entityCreatedByPlayer(entity, player);
                    break;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMinecartCreate(VehicleCreateEvent event) {
        checkCreation(event.getVehicle());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCreateSpawn(CreatureSpawnEvent event) {
        checkCreation(event.getEntity());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerCreatedEntities.remove(event.getPlayer().getName());
    }

    private void entityCreatedByPlayer(Entity entity, Player player) {
        LWC lwc = plugin.getLWC();

        Protection current = lwc.findProtection(entity.getLocation());
        if (current != null) {
            if (!current.isBlockInWorld()) {
                lwc.log("Removing corrupted protection: " + current);
                current.remove();
            } else {
                if (current.getProtectionFinder() != null) {
                    current.getProtectionFinder().fullMatchBlocks();
                    lwc.getProtectionCache().addProtection(current);
                }
                return;
            }
        }

        if (!lwc.isProtectable(entity.getType())) {
            return;
        }

        String autoRegisterType = lwc.resolveProtectionConfiguration(entity.getType(), "autoRegister");

        if ((!autoRegisterType.equalsIgnoreCase("private"))
            && (!autoRegisterType.equalsIgnoreCase("public"))) {
            return;
        }

        if (!lwc.hasPermission(player, "lwc.create." + autoRegisterType, "lwc.create", "lwc.protect")) {
            return;
        }

        Protection.Type type;

        try {
            type = Protection.Type.valueOf(autoRegisterType.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§4LWC_INVALID_CONFIG_autoRegister");
            return;
        }

        try {
            LWCProtectionRegisterEvent evt = new LWCProtectionRegisterEvent(player, EntityBlock.getEntityBlock(entity));
            lwc.getModuleLoader().dispatchEvent(evt);

            if (evt.isCancelled()) {
                return;
            }

            Protection protection = lwc.getPhysicalDatabase().registerProtection(
                    entity, type, player.getUniqueId().toString(), "");

            if (!Boolean.parseBoolean(lwc.resolveProtectionConfiguration(entity.getType(), "quiet"))) {
                lwc.sendLocale(player, "protection.onplace.create.finalize",
                        "type", lwc.getPlugin().getMessageParser().parseMessage(autoRegisterType.toLowerCase()),
                        "block", LWC.entityToString(entity.getType()));
            }

            if (protection != null) {
                lwc.getModuleLoader().dispatchEvent(new LWCProtectionRegistrationPostEvent(protection));
            }
        } catch (Exception e) {
            lwc.sendLocale(player, "protection.internalerror", "id", "PLAYER_INTERACT");
            e.printStackTrace();
        }
    }
}
