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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.griefcraft.cache.ProtectionCache;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Flag;
import com.griefcraft.model.Protection;
import com.griefcraft.scripting.event.LWCProtectionDestroyEvent;
import com.griefcraft.scripting.event.LWCProtectionRegisterEvent;
import com.griefcraft.scripting.event.LWCProtectionRegistrationPostEvent;
import com.griefcraft.scripting.event.LWCRedstoneEvent;
import com.griefcraft.util.Colors;
import com.griefcraft.util.LegacyMaterials;
import com.griefcraft.util.matchers.DoubleChestMatcher;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.material.Hopper;
import org.bukkit.material.MaterialData;

public class LWCBlockListener implements Listener {

    /**
     * The plugin instance
     */
    private LWCPlugin plugin;

    /**
     * A set of blacklisted blocks
     */
    private final Set<MaterialData> blacklistedBlocks = new HashSet<>();

    public LWCBlockListener(LWCPlugin plugin) {
        this.plugin = plugin;
        loadAndProcessConfig();
    }

    @EventHandler
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        LWC lwc = plugin.getLWC();
        Block block = event.getBlock();

        if (block == null) {
            return;
        }

        Protection protection = lwc.findProtection(block.getLocation());

        if (protection == null) {
            return;
        }

        LWCRedstoneEvent evt = new LWCRedstoneEvent(event, protection);
        lwc.getModuleLoader().dispatchEvent(evt);

        if (evt.isCancelled()) {
            event.setNewCurrent(event.getOldCurrent());
        }
    }

    @EventHandler
    public void onStructureGrow(StructureGrowEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        LWC lwc = LWC.getInstance();
        // the blocks that were changed / replaced
        List<BlockState> blocks = event.getBlocks();

        for (BlockState block : blocks) {
            if (!lwc.isProtectable(block.getBlock())) {
                continue;
            }

            // we don't have the block id of the block before it
            // so we have to do some raw lookups (these are usually cache hits however, at least!)
            Protection protection = lwc.getPhysicalDatabase().loadProtection(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());

            if (protection != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (!LWC.ENABLED || event.isCancelled()) {
            return;
        }

        LWC lwc = plugin.getLWC();
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block == null) {
            return;
        }

        Protection protection = lwc.findProtection(block.getLocation());

        if (protection == null) {
            return;
        }

        boolean canAccess = lwc.canAccessProtection(player, protection);

        if (!canAccess) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!LWC.ENABLED || event.isCancelled()) {
            return;
        }

        LWC lwc = plugin.getLWC();
        Player player = event.getPlayer();
        Block block = event.getBlock();

        boolean ignoreBlockDestruction = Boolean.parseBoolean(lwc.resolveProtectionConfiguration(block, "ignoreBlockDestruction"));

        if (ignoreBlockDestruction) {
            return;
        }

        ProtectionCache cache = lwc.getProtectionCache();
        String cacheKey = cache.cacheKey(block.getLocation());

        // In the event they place a block, remove any known nulls there
        if (cache.isKnownNull(cacheKey)) {
            cache.remove(cacheKey);
        }

        Protection protection = lwc.findProtection(block.getLocation());

        if (protection == null) {
            return;
        } else if (protection.getBlockType() != block.getType()) {
            // this block is no longer the block that's supposed to be protected
            protection.remove();
            return;
        }

        boolean canAccess = lwc.canAccessProtection(player, protection);
        boolean canAdmin = lwc.canAdminProtection(player, protection);

        // when destroying a chest, it's possible they are also destroying a double chest
        // in the event they're trying to destroy a double chest, we should just move
        // the protection to the chest that is not destroyed, if it is not that one already.
        if (protection.isOwner(player) && DoubleChestMatcher.PROTECTABLES_CHESTS.contains(block.getType())) {
            Block doubleChest = lwc.findAdjacentDoubleChest(block);

            if (doubleChest != null) {
                // if they destroyed the protected block we want to move it aye?
                if (lwc.blockEquals(protection.getBlock(), block)) {
                    // correct the block
                    protection.setBlockId(lwc.getPhysicalDatabase().getTypeId(block.getType()));
                    protection.setX(doubleChest.getX());
                    protection.setY(doubleChest.getY());
                    protection.setZ(doubleChest.getZ());
                    protection.saveNow();
                }

                // Repair the cache
                protection.radiusRemoveCache();

                if (protection.getProtectionFinder() != null) {
                    protection.getProtectionFinder().removeBlock(block.getState());
                }

                lwc.getProtectionCache().addProtection(protection);

                return;
            }
        }

        try {
            LWCProtectionDestroyEvent evt = new LWCProtectionDestroyEvent(player, protection, LWCProtectionDestroyEvent.Method.BLOCK_DESTRUCTION, canAccess, canAdmin);
            lwc.getModuleLoader().dispatchEvent(evt);

            if (evt.isCancelled() || !canAccess) {
                event.setCancelled(true);
            }
        } catch (Exception e) {
            event.setCancelled(true);
            lwc.sendLocale(player, "protection.internalerror", "id", "BLOCK_BREAK");
            e.printStackTrace();
        }
    }
    
    /**
     * Check to see if the given block ID can match the material type <br> (some blocks have different IDs for state changes)
     * @param blockID
     * @param blockMat
     * @return 
     */
    static boolean blockMatches(int blockID, Material blockMat) {
        return LWC.getInstance().getPhysicalDatabase().getType(blockID) == blockMat;
    }

    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        if (!LWC.ENABLED || event.isCancelled()) {
            return;
        }

        LWC lwc = plugin.getLWC();

        // Check the affected blocks
        for (Block moved : event.getBlocks()) {
            if (lwc.findProtection(moved.getLocation()) != null) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        if (!LWC.ENABLED || event.isCancelled()) {
            return;
        }

        LWC lwc = plugin.getLWC();

        // Check the affected blocks
        for (Block moved : event.getBlocks()) {
            if (lwc.findProtection(moved.getLocation()) != null) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        LWC lwc = plugin.getLWC();
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        ProtectionCache cache = lwc.getProtectionCache();
        String cacheKey = cache.cacheKey(block.getLocation());

        // In the event they place a block, remove any known nulls there
        if (cache.isKnownNull(cacheKey)) {
            cache.remove(cacheKey);
        }

        // check if the block is blacklisted
        BlockState state = block.getState();

        if (blacklistedBlocks.contains(state.getData())) {
            // it's blacklisted, check for a protected chest
            for (Protection protection : lwc.findAdjacentProtectionsOnAllSides(block)) {
                if (protection != null) {
                    // double-check protection is valid
                    if(!protection.isBlockInWorld()) {
                        protection.remove();
                    } else {
                        // is this protecting a block with an inventory?
                        switch (protection.getBlock().getType()) {
                            case CHEST:
                            case TRAPPED_CHEST:
                            case HOPPER:
                            case DISPENSER:
                            case DROPPER:
                            case BREWING_STAND:
                            case FURNACE:
                            case WHITE_SHULKER_BOX:
                            case ORANGE_SHULKER_BOX:
                            case MAGENTA_SHULKER_BOX:
                            case LIGHT_BLUE_SHULKER_BOX:
                            case YELLOW_SHULKER_BOX:
                            case LIME_SHULKER_BOX:
                            case PINK_SHULKER_BOX:
                            case GRAY_SHULKER_BOX:
                            case LIGHT_GRAY_SHULKER_BOX:
                            case CYAN_SHULKER_BOX:
                            case PURPLE_SHULKER_BOX:
                            case BLUE_SHULKER_BOX:
                            case BROWN_SHULKER_BOX:
                            case GREEN_SHULKER_BOX:
                            case RED_SHULKER_BOX:
                            case BLACK_SHULKER_BOX:
                                if (!lwc.canAccessProtection(player, protection) || (protection.getType() == Protection.Type.DONATION && !lwc.canAdminProtection(player, protection))) {
                                    // they can't access the protection ..
                                    event.setCancelled(true);
                                    lwc.sendLocale(player, "protection.general.locked.private", "block", LWC.materialToString(protection.getBlock()));
                                    return;
                                }
                        }
                    }
                }
            }
        }

        if (lwc.useAlternativeHopperProtection() && block.getType() == Material.HOPPER) {
            // we use the alternative hopper protection, check if the hopper is placed below a container!
            Block above = block.getRelative(BlockFace.UP);
            if (checkForHopperProtection(player, above)) {
                event.setCancelled(true);
                return;
            }
            
            // also check if the hopper is pointing into a protection
            Hopper hopperData = (Hopper) state.getData();
            Block target = block.getRelative(hopperData.getFacing());
            if (checkForHopperProtection(player, target)) {
                event.setCancelled(true);
                return;
            }
        }
    }
    
    private boolean checkForHopperProtection(Player player, Block block) {
        if (block.getState() instanceof InventoryHolder) { // only care if block has an inventory
            LWC lwc = plugin.getLWC();
            Protection protection = lwc.findProtection(block.getLocation());
            if (protection != null) { // found protection
                boolean denyHoppers = Boolean.parseBoolean(lwc.resolveProtectionConfiguration(block, "denyHoppers"));
                if (!lwc.canAccessProtection(player, protection) || (denyHoppers != protection.hasFlag(Flag.Type.HOPPER) && !lwc.canAdminProtection(player, protection))) {
                    // player can't access the protection and hoppers aren't enabled for it
                    lwc.enforceAccess(player, protection, block, false);
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        LWC lwc = plugin.getLWC();
        Block block = event.getBlock();

        if (block.getType().name().contains("BED")) {
            for (BlockState state : event.getReplacedBlockStates()) {
                Protection protection = lwc.findProtection(state);

                if (protection != null) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    /**
     * Used for auto registering placed protections
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlaceMonitor(BlockPlaceEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        LWC lwc = plugin.getLWC();
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
		
        // Update the cache if a protection is matched here
        Protection current = lwc.findProtection(block.getLocation());
        if (current != null) {
            // no use checking if the block id matches.
            // This is a build event because it didn't exist before, and does now
            //lwc.log("Removing corrupted protection: " + current);
            current.remove();
            /*
            if (!current.isBlockInWorld()) {
                // Corrupted protection
                lwc.log("Removing corrupted protection: " + current);
                current.remove();
            } else {
                if (current.getProtectionFinder() != null) {
                    current.getProtectionFinder().fullMatchBlocks();
                    lwc.getProtectionCache().addProtection(current);
                }

                return;
            }
            */
        }

        // The placable block must be protectable
        if (!lwc.isProtectable(block)) {
            return;
        }

        String autoRegisterType = lwc.resolveProtectionConfiguration(block, "autoRegister");
        // is it auto protectable?
        if (!autoRegisterType.equalsIgnoreCase("private") && !autoRegisterType.equalsIgnoreCase("public")) {
            return;
        }

        if (!lwc.hasPermission(player, "lwc.create." + autoRegisterType, "lwc.create", "lwc.protect")) {
            return;
        }

        // Parse the type
        Protection.Type type;

        try {
            type = Protection.Type.valueOf(autoRegisterType.toUpperCase());
        } catch (IllegalArgumentException e) {
            // No auto protect type found
            return;
        }

        // Is it okay?
        if (type == null) {
            player.sendMessage(Colors.Red + "LWC_INVALID_CONFIG_autoRegister");
            return;
        }

        // If it's a chest, make sure they aren't placing it beside an already registered chest
        if (DoubleChestMatcher.PROTECTABLES_CHESTS.contains(block.getType())) {
            BlockFace[] faces = new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

            for (BlockFace blockFace : faces) {
                Block face = block.getRelative(blockFace);

                //They're placing it beside a chest, check if it's already protected
                if (face.getType() == block.getType()) {
                    if (lwc.findProtection(face.getLocation()) != null) {
                        return;
                    }
                }
            }
        }

        try {
            LWCProtectionRegisterEvent evt = new LWCProtectionRegisterEvent(player, block);
            lwc.getModuleLoader().dispatchEvent(evt);

            // something cancelled registration
            if (evt.isCancelled()) {
                return;
            }

            // All good!
            Protection protection = lwc.getPhysicalDatabase().registerProtection(block.getType(), type, block.getWorld().getName(), player.getUniqueId().toString(), "", block.getX(), block.getY(), block.getZ());

            if (!Boolean.parseBoolean(lwc.resolveProtectionConfiguration(block, "quiet"))) {
                lwc.sendLocale(player, "protection.onplace.create.finalize", "type", lwc.getPlugin().getMessageParser().parseMessage(autoRegisterType.toLowerCase()), "block", LWC.materialToString(block));
            }

            if (protection != null) {
                lwc.getModuleLoader().dispatchEvent(new LWCProtectionRegistrationPostEvent(protection));
            }
        } catch (Exception e) {
            lwc.sendLocale(player, "protection.internalerror", "id", "PLAYER_INTERACT");
            e.printStackTrace();
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!LWC.ENABLED) {
            return;
        }

        LWC lwc = LWC.getInstance();

        Block block = event.getBlock();
        if (!lwc.isProtectable(block)) {
            return;
        }

        Protection protection = lwc.findProtection(block);
        if (protection != null) {
            event.setCancelled(true);
        }
    }

    /**
     * Load and process the configuration
     */
    private void loadAndProcessConfig() {
        List<String> ids = LWC.getInstance().getConfiguration().getStringList("optional.blacklistedBlocks", new ArrayList<>());

        for (String sId : ids) {
            try {
                String[] idParts = sId.trim().split(":");
                Material material = Material.matchMaterial(idParts[0].trim());
                byte data = 0;

                if (idParts.length > 1) {
                    data = Byte.parseByte(idParts[1].trim());
                }

                blacklistedBlocks.add(new MaterialData(material, data));
            } catch (Exception ex) {
                LWC.getInstance().log("Failed to parse \"" + sId + "\" from optional.blacklistedBlocks:");
                ex.printStackTrace();
            }
        }
    }
}
