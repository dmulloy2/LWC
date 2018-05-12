package com.griefcraft.bukkit;

import java.util.Collection;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class EntityBlock implements Block {

    public static final int ENTITY_BLOCK_ID = 5000;
    public static final int POSITION_OFFSET = 50000;

    private final Entity entity;

    public EntityBlock(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public EntityType getEntityType() {
        return entity.getType();
    }

    public static int calcHash(int hash) {
        return (POSITION_OFFSET + Math.abs(hash)) * (hash == 0 ? 1 : Integer.signum(hash));
    }

    public int getX() {
        final int hash = entity.getUniqueId().hashCode();
        return (POSITION_OFFSET + Math.abs(hash)) * (hash == 0 ? 1 : Integer.signum(hash));
        //return POSITION_OFFSET + this.entity.getUniqueId().hashCode();
    }

    public int getY() {
        final int hash = entity.getUniqueId().hashCode();
        return (POSITION_OFFSET + Math.abs(hash)) * (hash == 0 ? 1 : Integer.signum(hash));
        //return POSITION_OFFSET + this.entity.getUniqueId().hashCode();
    }

    public int getZ() {
        final int hash = entity.getUniqueId().hashCode();
        return (POSITION_OFFSET + Math.abs(hash)) * (hash == 0 ? 1 : Integer.signum(hash));
        //return POSITION_OFFSET + this.entity.getUniqueId().hashCode();
    }

    public int getTypeId() {
        return ENTITY_BLOCK_ID + entity.getType().getTypeId();
    }

    public org.bukkit.World getWorld() {
        return this.entity.getWorld();
    }
    public static Block getEntityBlock(Entity entity) {
        return new EntityBlock(entity);
    }

    public List<MetadataValue> getMetadata(String arg0) {
        return null;
    }

    public boolean hasMetadata(String arg0) {
        return false;
    }

    public void removeMetadata(String arg0, Plugin arg1) {
    }

    public void setMetadata(String arg0, MetadataValue arg1) {
    }

    public boolean breakNaturally() {
        return false;
    }

    public boolean breakNaturally(ItemStack arg0) {
        return false;
    }

    public Biome getBiome() {
        return null;
    }

    public int getBlockPower() {
        return 0;
    }

    public int getBlockPower(BlockFace arg0) {
        return 0;
    }

    public org.bukkit.Chunk getChunk() {
        return null;
    }

    public byte getData() {
        return 0;
    }

    public Collection<ItemStack> getDrops() {
        return null;
    }

    public Collection<ItemStack> getDrops(ItemStack arg0) {
        return null;
    }

    public BlockFace getFace(Block arg0) {
        return null;
    }

    public double getHumidity() {
        return 0.0D;
    }

    public byte getLightFromBlocks() {
        return 0;
    }

    public byte getLightFromSky() {
        return 0;
    }

    public byte getLightLevel() {
        return 0;
    }

    public Location getLocation() {
        return null;
    }

    public Location getLocation(Location arg0) {
        return null;
    }

    public PistonMoveReaction getPistonMoveReaction() {
        return null;
    }

    public Block getRelative(BlockFace arg0) {
        return null;
    }

    public Block getRelative(BlockFace arg0, int arg1) {
        return null;
    }

    public Block getRelative(int arg0, int arg1, int arg2) {
        return null;
    }

    public BlockState getState() {
        return null;
    }

    public double getTemperature() {
        return 0.0D;
    }

    public Material getType() {
        return null;
    }

    public boolean isBlockFaceIndirectlyPowered(BlockFace arg0) {
        return false;
    }

    public boolean isBlockFacePowered(BlockFace arg0) {
        return false;
    }

    public boolean isBlockIndirectlyPowered() {
        return false;
    }

    public boolean isBlockPowered() {
        return false;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean isLiquid() {
        return false;
    }

    public void setBiome(Biome arg0) {
    }

    public void setData(byte arg0) {
    }

    public void setData(byte arg0, boolean arg1) {
    }

    public void setType(Material arg0) {
    }

    public void setType(Material arg0, boolean arg1) {
    }

    public boolean setTypeId(int arg0) {
        return false;
    }

    public boolean setTypeId(int arg0, boolean arg1) {
        return false;
    }

    public boolean setTypeIdAndData(int arg0, byte arg1, boolean arg2) {
        return false;
    }
}
