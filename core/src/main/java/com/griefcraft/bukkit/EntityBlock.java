package com.griefcraft.bukkit;

import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class EntityBlock implements Block {
	
    @Deprecated
    public static final int ENTITY_BLOCK_ID = 5000;
    /**
     * To convert database offsets from Brokkonaut's fork in a foolproof manner, 
     * we're going to just flag any '5000' (unknown entity) as 6000, 
     * then convert them later when we encounter them
     */
    @Deprecated
    public static final int UNKNOWN_ENTITY_BLOCK_ID = 6000;
    public static final int POSITION_OFFSET = 50000;

    public static final String ENTITY_TYPE_PREFIX = "~ENTITY~";
    public static final String UNKNOWN_ENTITY_TYPE = ENTITY_TYPE_PREFIX + "~";

    private final Entity entity;
    private final int hash;
    private final String type;
    private final org.bukkit.World world;

    public EntityBlock(Entity entity) {
        this.entity = entity;
        if(entity != null) {
            //id = entity.getType().getTypeId();
			   type = ENTITY_TYPE_PREFIX + entity.getType().name();
            hash = calcHash(entity.getUniqueId().hashCode());
            world = this.entity.getWorld();
        } else {
            hash = 0;
				type = UNKNOWN_ENTITY_TYPE;
            world = null;
        }
    }

    public EntityBlock(String world, String type, int hash) {
        this.entity = null;
		  this.type = type;
        this.hash = hash;
        this.world = Bukkit.getWorld(world);
    }

    /**
     * Get the entity represented by this protection block <br>
     * NOTE: only works for recently created protections, not loaded protections
     * @return 
     */
    public Entity getEntity() {
        return this.entity;
    }

    public EntityType getEntityType() {
        return entity != null ? entity.getType() :
                (type.equals(UNKNOWN_ENTITY_TYPE) ? null : EntityType.valueOf(type.substring(ENTITY_TYPE_PREFIX.length())));
    }

    public static int calcHash(int hash) {
        return (POSITION_OFFSET + Math.abs(hash)) * (hash == 0 ? 1 : Integer.signum(hash));
    }

    @Override
    public int getX() {
        return hash;
    }

    @Override
    public int getY() {
        return hash;
    }

    @Override
    public int getZ() {
        return hash;
    }

    public static String calcTypeString(Entity entity) {
        return entity == null ? UNKNOWN_ENTITY_TYPE : ENTITY_TYPE_PREFIX + entity.getType().name();
    }

    public static String calcTypeString(EntityType entity) {
        return entity == null ? UNKNOWN_ENTITY_TYPE : ENTITY_TYPE_PREFIX + entity.name();
    }

    public String getTypeString() {
        return type;
    }

    public org.bukkit.World getWorld() {
        return world;
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

    public void setBlockData(BlockData data) {

    }

    public void setBlockData(BlockData data, boolean applyPhysics) {

    }

    public byte getData() {
        return 0;
    }

    public BlockData getBlockData() {
        return null;
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
        return entity != null ? entity.getLocation() : null;
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

    public void setType(Material arg0) {
    }

    public void setType(Material arg0, boolean arg1) {
    }

    public boolean isPassable() {
        return true;
    }

    public RayTraceResult rayTrace(Location lctn, Vector vector, double d, FluidCollisionMode fcm) {
        return null;
    }

    public BoundingBox getBoundingBox() {
        return null;
    }

	@Override
	public boolean applyBoneMeal(BlockFace arg0) {
		return false;
	}

	@Override
	public Collection<ItemStack> getDrops(ItemStack arg0, Entity arg1) {
		return null;
	}
}
