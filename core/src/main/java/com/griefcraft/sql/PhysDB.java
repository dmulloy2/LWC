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

package com.griefcraft.sql;

import com.griefcraft.cache.LRUCache;
import com.griefcraft.cache.ProtectionCache;
import com.griefcraft.lwc.LWC;
import com.griefcraft.migration.RowHandler;
import com.griefcraft.migration.SimpleTableWalker;
import com.griefcraft.migration.TableWalker;
import com.griefcraft.migration.uuid.HistoryRowHandler;
import com.griefcraft.migration.uuid.PlayerRowHandler;
import com.griefcraft.migration.PlayerTableWalker;
import com.griefcraft.migration.uuid.ProtectionRowHandler;
import com.griefcraft.model.Flag;
import com.griefcraft.model.History;
import com.griefcraft.model.Permission;
import com.griefcraft.model.PlayerInfo;
import com.griefcraft.model.Protection;
import com.griefcraft.modules.limits.LimitsModule;
import com.griefcraft.scripting.Module;
import com.griefcraft.util.PlayerRegistry;
import com.griefcraft.util.config.Configuration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

public class PhysDB extends Database {

    /**
     * The JSON Parser object
     */
    private final JSONParser jsonParser = new JSONParser();

    /**
     * The database version
     */
    private int databaseVersion = 0;

    /**
     * The number of protections that should exist
     */
    private int protectionCount = 0;

    /**
     * The current stage of UUID conversions (if any). MC 1.7+
     */
    private String uuidConversionStage = "";

    public PhysDB() {
        super();
    }

    public PhysDB(Type currentType) {
        super(currentType);
    }

    /**
     * Decrement the known protection counter
     */
    public void decrementProtectionCount() {
        protectionCount--;
    }

    /**
     * Check if the protection cache has all of the known protections cached
     *
     * @return
     */
    public boolean hasAllProtectionsCached() {
        ProtectionCache cache = LWC.getInstance().getProtectionCache();

        return cache.size() >= protectionCount;
    }

    /**
     * Fetch an object from the sql database
     *
     * @param sql
     * @param column
     * @return
     */
    private Object fetch(String sql, String column, Object... toBind) {
        try {
            int index = 1;
            PreparedStatement statement = prepare(sql);

            for (Object bind : toBind) {
                statement.setObject(index, bind);
                index++;
            }

            ResultSet set = statement.executeQuery();

            if (set.next()) {
                Object object = set.getObject(column);
                set.close();
                return object;
            }

            set.close();
        } catch (Exception e) {
            printException(e);
        }

        return null;
    }

    /**
     * Get the total amount of protections
     *
     * @return the number of protections
     */
    public int getProtectionCount() {
        return Integer.decode(fetch("SELECT COUNT(*) AS count FROM " + prefix + "protections", "count").toString());
    }

    /**
     * Get the amount of protections for the given protection type
     *
     * @param type
     * @return the number of protected chests
     */
    public int getProtectionCount(Protection.Type type) {
        return Integer.decode(fetch("SELECT COUNT(*) AS count FROM " + prefix + "protections WHERE type = " + type.ordinal(), "count").toString());
    }

    /**
     * @return the number of history items stored
     */
    public int getHistoryCount() {
        return Integer.decode(fetch("SELECT COUNT(*) AS count FROM " + prefix + "history", "count").toString());
    }

    /**
     * Get the amount of protections a player has
     *
     * @param player
     * @return the amount of protections they have
     */
    public int getProtectionCount(String player) {
        int count = 0;

        PlayerInfo playerInfo = PlayerRegistry.getPlayerInfo(player);

        try {
            PreparedStatement statement = prepare("SELECT COUNT(*) as count FROM " + prefix + "protections WHERE owner = ?");
            statement.setInt(1, playerInfo.getId());

            ResultSet set = statement.executeQuery();

            if (set.next()) {
                count = set.getInt("count");
            }

            set.close();
        } catch (SQLException e) {
            printException(e);
        }

        return count;
    }

    /**
     * Get the amount of protections a player has
     *
     * @param player
     * @return the amount of protections they have
     */
    public int getHistoryCount(String player) {
        int count = 0;

        PlayerInfo playerInfo = PlayerRegistry.getPlayerInfo(player);

        try {
            PreparedStatement statement = prepare("SELECT COUNT(*) AS count FROM " + prefix + "history WHERE player = ?");
            statement.setInt(1, playerInfo.getId());

            ResultSet set = statement.executeQuery();

            if (set.next()) {
                count = set.getInt("count");
            }

            set.close();
        } catch (SQLException e) {
            printException(e);
        }

        return count;
    }

    /**
     * Get the amount of chests a player has of a specific block id
     *
     * @param player
     * @return the amount of protections they have of blockId
     */
    public int getProtectionCount(String player, int blockId) {
        int count = 0;

        PlayerInfo playerInfo = PlayerRegistry.getPlayerInfo(player);

        try {
            PreparedStatement statement = prepare("SELECT COUNT(*) AS count FROM " + prefix + "protections WHERE owner = ? AND blockId = ?");
            statement.setInt(1, playerInfo.getId());
            statement.setInt(2, blockId);

            ResultSet set = statement.executeQuery();

            if (set.next()) {
                count = set.getInt("count");
            }

            set.close();
        } catch (SQLException e) {
            printException(e);
        }

        return count;
    }

    /**
     * Get the menu style for a player
     *
     * @param player
     * @return
     * @deprecated
     */
    public String getMenuStyle(String player) {
        return "basic";
    }

    /**
     * Load the database and do any updating required or create the tables
     */
    @Override
    public void load() {
        if (loaded) {
            return;
        }

        Column column;

        Table protections = new Table(this, "protections");
        {
            column = new Column("id");
            column.setType("INTEGER");
            column.setPrimary(true);
            protections.add(column);

            column = new Column("owner");
            column.setType("int");
            protections.add(column);

            column = new Column("type");
            column.setType("INTEGER");
            protections.add(column);

            column = new Column("x");
            column.setType("INTEGER");
            protections.add(column);

            column = new Column("y");
            column.setType("INTEGER");
            protections.add(column);

            column = new Column("z");
            column.setType("INTEGER");
            protections.add(column);

            column = new Column("flags");
            column.setType("INTEGER");
            protections.add(column);

            column = new Column("data");
            column.setType("TEXT");
            protections.add(column);

            column = new Column("blockId");
            column.setType("INTEGER");
            protections.add(column);

            column = new Column("world");
            column.setType("VARCHAR(255)");
            protections.add(column);

            column = new Column("password");
            column.setType("VARCHAR(255)");
            protections.add(column);

            column = new Column("date");
            column.setType("VARCHAR(255)");
            protections.add(column);

            column = new Column("last_accessed");
            column.setType("INTEGER");
            protections.add(column);
        }

        Table players = new Table(this, "players");
        {
            column = new Column("id");
            column.setType("INTEGER");
            column.setPrimary(true);
            players.add(column);

            column = new Column("uuid");
            column.setType("VARCHAR(40)");
            players.add(column);

            column = new Column("name");
            column.setType("VARCHAR(40)");
            players.add(column);
        }

        Table history = new Table(this, "history");
        {
            column = new Column("id");
            column.setType("INTEGER");
            column.setPrimary(true);
            history.add(column);

            column = new Column("protectionId");
            column.setType("INTEGER");
            history.add(column);

            column = new Column("player");
            column.setType("INTEGER");
            history.add(column);

            column = new Column("x");
            column.setType("INTEGER");
            history.add(column);

            column = new Column("y");
            column.setType("INTEGER");
            history.add(column);

            column = new Column("z");
            column.setType("INTEGER");
            history.add(column);

            column = new Column("type");
            column.setType("INTEGER");
            history.add(column);

            column = new Column("status");
            column.setType("INTEGER");
            history.add(column);

            column = new Column("metadata");
            column.setType("VARCHAR(255)");
            history.add(column);

            column = new Column("timestamp");
            column.setType("long");
            history.add(column);
        }

        Table internal = new Table(this, "internal");
        {
            column = new Column("name");
            column.setType("VARCHAR(40)");
            column.setPrimary(true);
            column.setAutoIncrement(false);
            internal.add(column);

            column = new Column("value");
            column.setType("VARCHAR(40)");
            internal.add(column);
        }

        protections.execute();
        players.execute();
        history.execute();
        internal.execute();

        // Load the database version
        loadDatabaseVersion();

        // perform database upgrades
        performDatabaseUpdates();

        // get the amount of protections
        protectionCount = getProtectionCount();

        loaded = true;
    }

    /**
     * Perform any database updates
     */
    public void performDatabaseUpdates() {
        LWC lwc = LWC.getInstance();

        // Indexes
        if (databaseVersion == 0) {
            // Drop old, old indexes
            log("Dropping old indexes (One time, may take a while!)");
            dropIndex("protections", "in1");
            dropIndex("protections", "in6");
            dropIndex("protections", "in7");
            dropIndex("history", "in8");
            dropIndex("history", "in9");
            dropIndex("protections", "in10");
            dropIndex("history", "in12");
            dropIndex("history", "in13");
            dropIndex("history", "in14");

            // Create our updated (good) indexes
            log("Creating new indexes (One time, may take a while!)");
            createIndex("protections", "protections_main", "x, y, z, world");
            createIndex("protections", "protections_utility", "owner");
            createIndex("history", "history_main", "protectionId");
            createIndex("history", "history_utility", "player");
            createIndex("history", "history_utility2", "x, y, z");

            // increment the database version
            incrementDatabaseVersion();
        }

        if (databaseVersion == 1) {
            log("Creating index on internal");
            createIndex("internal", "internal_main", "name");
            incrementDatabaseVersion();
        }

        if (databaseVersion == 2) {
            incrementDatabaseVersion();
        }

        if (databaseVersion == 3) {
            createIndex("protections", "protections_type", "type");
            incrementDatabaseVersion();
        }

        if (databaseVersion == 4) {
            List<String> blacklistedBlocks = lwc.getConfiguration().getStringList("optional.blacklistedBlocks", new ArrayList<String>());

            if (!blacklistedBlocks.contains("154")) {
                blacklistedBlocks.add(Integer.toString(Material.HOPPER.getId()));
                lwc.getConfiguration().setProperty("optional.blacklistedBlocks", blacklistedBlocks);
                lwc.getConfiguration().save();
                Configuration.reload();

                lwc.log("Added Hoppers to Blacklisted Blocks in core.yml (optional.blacklistedBlocks)");
                lwc.log("This means that Hoppers CANNOT be placed around protections a player does not have access to");
                lwc.log("If you DO NOT want this feature, simply remove " + Material.HOPPER.getId() + " (Hoppers) from blacklistedBlocks :-)");
            }

            incrementDatabaseVersion();
        }

        if (databaseVersion == 5) {
            boolean foundTrappedChest = false;

            for (String key : lwc.getConfiguration().getNode("protections.blocks").getKeys(null)) {
                if (key.equalsIgnoreCase("trapped_chest") || key.equals(Integer.toString(Material.TRAPPED_CHEST.getId()))) {
                    foundTrappedChest = true;
                    break;
                }
            }

            if (!foundTrappedChest) {
                lwc.getConfiguration().setProperty("protections.blocks.trapped_chest.enabled", true);
                lwc.getConfiguration().setProperty("protections.blocks.trapped_chest.autoRegister", "private");
                lwc.getConfiguration().save();
                Configuration.reload();

                lwc.log("Added Trapped Chests to core.yml as default protectable (ENABLED & AUTO REGISTERED)");
                lwc.log("Trapped chests are nearly the same as reg chests but can light up! They can also be double chests.");
                lwc.log("If you DO NOT want this as protected, simply remove it from core.yml! (search/look for trapped_chests under protections -> blocks");
            }

            incrementDatabaseVersion();
        }

        if (databaseVersion == 6) {
            log("Adding indexes to players table");
            createIndex("players", "player_uuid", "uuid");
            createIndex("players", "player_name", "name");

            incrementDatabaseVersion();
        }

        uuidConversionStage = getInternal("uuidConversionStage");

        if (uuidConversionStage == null) {
            uuidConversionStage = Integer.toString(0);
            setInternal("uuidConversionStage", uuidConversionStage);
            setInternal("uuidConversionOffset", Integer.toString(0));
        }

        if (!uuidConversionStage.equals("complete")) {
            final int stage = Integer.parseInt(uuidConversionStage);
            int offset = Integer.parseInt(getInternal("uuidConversionOffset"));

            // conversion steps to undergo
            final TableWalker[] conversionSteps = {
                    // converts player names => unique ids
                    new PlayerTableWalker(this, new PlayerRowHandler()),

                    // protections table converter
                    new SimpleTableWalker(this, new ProtectionRowHandler(), "protections_old_converting", offset),

                    // history table converter
                    new SimpleTableWalker(this, new HistoryRowHandler(), "history_old_converting", offset)
            };

            Observer walkerObserver = new Observer() {
                private int currentStep = stage;

                public void update(Observable o, Object arg) {
                    TableWalker walker = (TableWalker) o;
                    RowHandler handler = walker.getHandler();

                    boolean isComplete = "complete".equals(arg);
                    int offset = -1;

                    if (!isComplete) {
                        offset = (Integer) arg;
                    }

                    if (handler instanceof PlayerRowHandler) {
                        if (isComplete) {
                            log("Completed conversion for player names");
                        }
                    } else if (handler instanceof ProtectionRowHandler) {
                        if (isComplete) {
                            log("Completed conversion of " + getPrefix() + "protections");
                        }
                    }

                    if (isComplete) {
                        if (currentStep < conversionSteps.length - 1) {
                            log("Moving to next stage");
                            conversionSteps[currentStep].getHandler().onComplete();
                            TableWalker next = conversionSteps[++currentStep];
                            uuidConversionStage = Integer.toString(currentStep);
                            setInternal("uuidConversionStage", uuidConversionStage);
                            setInternal("uuidConversionOffset", Integer.toString(0));
                            conversionSteps[currentStep].getHandler().onStart();
                            next.addObserver(this);
                            next.start();
                        } else {
                            log("Completed database conversion for UUIDs");
                            conversionSteps[currentStep].getHandler().onComplete();
                            uuidConversionStage = "complete";
                            setInternal("uuidConversionStage", uuidConversionStage);
                            setInternal("uuidConversionOffset", Integer.toString(0));
                        }
                    } else {
                        setInternal("uuidConversionOffset", Integer.toString(offset));
                    }
                }
            };

            if (stage < conversionSteps.length) {
                TableWalker walker = conversionSteps[stage];
                walker.addObserver(walkerObserver);
                conversionSteps[stage].getHandler().onStart();
                walker.start();
            }
        }
    }

    /**
     * Increment the database version
     */
    public void incrementDatabaseVersion() {
        setDatabaseVersion(++databaseVersion);
    }

    /**
     * Set the database version and sync it to the database
     *
     * @param databaseVersion
     */
    public void setDatabaseVersion(int databaseVersion) {
        // set it locally
        this.databaseVersion = databaseVersion;

        // ship it to the database
        try {
            PreparedStatement statement = prepare("UPDATE " + prefix + "internal SET value = ? WHERE name = ?");
            statement.setInt(1, databaseVersion);
            statement.setString(2, "version");

            // ok
            statement.executeUpdate();
        } catch (SQLException e) {
        }
    }

    /**
     * Get a value in the internal table
     *
     * @param key
     * @return the value found, otherwise NULL if none exists
     */
    public String getInternal(String key) {
        try {
            PreparedStatement statement = prepare("SELECT value FROM " + prefix + "internal WHERE name = ?");
            statement.setString(1, key);

            ResultSet set = statement.executeQuery();
            if (set.next()) {
                String value = set.getString("value");
                set.close();
                return value;
            }
            set.close();
        } catch (SQLException e) {
            printException(e);
        }

        return null;
    }

    /**
     * Set a value in the internal table
     *
     * @param key
     * @param value
     */
    public void setInternal(String key, String value) {
        try {
            PreparedStatement statement = prepare("INSERT INTO " + prefix + "internal (name, value) VALUES (?, ?)");
            statement.setString(1, key);
            statement.setString(2, value);

            statement.executeUpdate();
        } catch (SQLException e) {
            // Already exists
            try {
                PreparedStatement statement = prepare("UPDATE " + prefix + "internal SET value = ? WHERE name = ?");
                statement.setString(1, value);
                statement.setString(2, key);

                statement.executeUpdate();
            } catch (SQLException ex) {
                // Something bad went wrong
                printException(ex);
            }
        }
    }

    /**
     * Load the database internal version
     *
     * @return
     */
    public int loadDatabaseVersion() {
        try {
            PreparedStatement statement = prepare("SELECT value FROM " + prefix + "internal WHERE name = ?");
            statement.setString(1, "version");

            // Execute it
            ResultSet set = statement.executeQuery();

            // load the version
            if (set.next()) {
                databaseVersion = Integer.parseInt(set.getString("value"));
            } else {
                throw new IllegalStateException("Internal is empty");
            }

            // close everything
            set.close();
        } catch (Exception e) {
            // Doesn't exist, create it
            try {
                PreparedStatement statement = prepare("INSERT INTO " + prefix + "internal (name, value) VALUES(?, ?)");
                statement.setString(1, "version");
                statement.setInt(2, databaseVersion);

                // ok
                statement.executeUpdate();
            } catch (SQLException ex) {
            }
        }

        return databaseVersion;
    }

    /**
     * Load a protection with the given id
     *
     * @param id
     * @return the protection found, or null if it does not exist
     */
    public Protection loadProtection(int id) {
        // the protection cache
        ProtectionCache cache = LWC.getInstance().getProtectionCache();

        // check if the protection is already cached
        Protection cached = cache.getProtectionById(id);
        if (cached != null) {
            return cached;
        }

        try {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections WHERE id = ?");
            statement.setInt(1, id);

            Protection protection = resolveProtection(statement);

            if (protection == null && "1".equals(uuidConversionStage)) {
                protection = legacyLoadProtection(id);
            }

            if (protection != null) {
                cache.addProtection(protection);
                return protection;
            }
        } catch (SQLException e) {
            printException(e);
        }

        return null;
    }

    /**
     * Load a protection with the given id in the old database format
     *
     * @param id
     * @return the protection found, or null if it does not exist
     */
    public Protection legacyLoadProtection(int id) {
        try {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections_old_converting WHERE id = ?");
            statement.setInt(1, id);

            Protection protection = resolveProtection(statement);

            if (protection != null) {
                return protection;
            }
        } catch (SQLException e) {
            printException(e);
        }

        return null;
    }

    /**
     * Load protections using a specific type
     *
     * @param type
     * @return the Protection object
     */
    public List<Protection> loadProtectionsUsingType(Protection.Type type) {
        try {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections WHERE type = ?");
            statement.setInt(1, type.ordinal());

            return resolveProtections(statement);
        } catch (SQLException e) {
            printException(e);
        }

        return new ArrayList<Protection>();
    }

    /**
     * Resolve one protection from a ResultSet. The ResultSet is not closed.
     *
     * @param set
     * @return
     */
    public Protection resolveProtection(ResultSet set) {
        try {
            Protection protection = new Protection();

            int protectionId = set.getInt("id");
            int x = set.getInt("x");
            int y = set.getInt("y");
            int z = set.getInt("z");
            int blockId = set.getInt("blockId");
            int type = set.getInt("type");
            String world = set.getString("world");
            String password = set.getString("password");
            String date = set.getString("date");
            long lastAccessed = set.getLong("last_accessed");

            Object owner = set.getObject("owner");
            PlayerInfo ownerInfo;

            if (owner instanceof String) {
                ownerInfo = PlayerRegistry.getPlayerInfo(owner.toString());
            } else {
                ownerInfo = PlayerRegistry.getPlayerInfo((Integer) owner);
            }

            protection.setId(protectionId);
            protection.setX(x);
            protection.setY(y);
            protection.setZ(z);
            protection.setBlockId(blockId);
            protection.setType(Protection.Type.values()[type]);
            protection.setWorld(world);
            protection.setOwner(ownerInfo);
            protection.setPassword(password);
            protection.setCreation(date);
            protection.setLastAccessed(lastAccessed);

            // check for oh so beautiful data!
            String data = set.getString("data");

            if (data == null || data.trim().isEmpty()) {
                return protection;
            }

            // rev up them JSON parsers!
            Object object = null;

            try {
                object = jsonParser.parse(data);
            } catch (Exception e) {
                return protection;
            } catch (Error e) {
                return protection;
            }

            if (!(object instanceof JSONObject)) {
                return protection;
            }

            // obtain the root
            JSONObject root = (JSONObject) object;
            protection.getData().putAll(root);

            // Attempt to parse rights
            Object rights = root.get("rights");

            if (rights != null && (rights instanceof JSONArray)) {
                JSONArray array = (JSONArray) rights;

                for (Object node : array) {
                    // we only want to use the maps
                    if (!(node instanceof JSONObject)) {
                        continue;
                    }

                    JSONObject map = (JSONObject) node;

                    // decode the map
                    Permission permission = Permission.decodeJSON(map);

                    // bingo!
                    if (permission != null) {
                        protection.addPermission(permission);
                    }
                }
            }

            // Attempt to parse flags
            Object flags = root.get("flags");
            if (flags != null && (rights instanceof JSONArray)) {
                JSONArray array = (JSONArray) flags;

                for (Object node : array) {
                    if (!(node instanceof JSONObject)) {
                        continue;
                    }

                    JSONObject map = (JSONObject) node;

                    Flag flag = Flag.decodeJSON(map);

                    if (flag != null) {
                        protection.addFlag(flag);
                    }
                }
            }

            return protection;
        } catch (SQLException e) {
            printException(e);
            return null;
        }
    }

    /**
     * Resolve every protection from a result set
     *
     * @param set
     * @return
     */
    private List<Protection> resolveProtections(ResultSet set) {
        List<Protection> protections = new ArrayList<Protection>();

        try {
            while (set.next()) {
                Protection protection = resolveProtection(set);

                if (protection != null) {
                    protections.add(protection);
                }
            }
        } catch (SQLException e) {
            printException(e);
        }

        return protections;
    }

    /**
     * Resolve a list of protections from a statement
     *
     * @param statement
     * @return
     */
    private List<Protection> resolveProtections(PreparedStatement statement) {
        List<Protection> protections = new ArrayList<Protection>();
        ResultSet set = null;

        try {
            set = statement.executeQuery();
            protections = resolveProtections(set);
        } catch (SQLException e) {
            printException(e);
        } finally {
            if (set != null) {
                try {
                    set.close();
                } catch (SQLException e) {
                }
            }
        }

        return protections;
    }

    /**
     * Resolve the first protection from a statement
     *
     * @param statement
     * @return
     */
    private Protection resolveProtection(PreparedStatement statement) {
        List<Protection> protections = resolveProtections(statement);

        if (protections.size() == 0) {
            return null;
        }

        return protections.get(0);
    }

    /**
     * Fill the protection cache as much as possible with protections
     * Caches the most recent protections
     */
    public void precache() {
        LWC lwc = LWC.getInstance();
        ProtectionCache cache = lwc.getProtectionCache();

        // clear the cache incase we're working on a dirty cache
        cache.clear();

        int precacheSize = lwc.getConfiguration().getInt("core.precache", -1);

        if (precacheSize == -1) {
            precacheSize = lwc.getConfiguration().getInt("core.cacheSize", 10000);
        }

        try {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections ORDER BY id DESC LIMIT ?");
            statement.setInt(1, precacheSize);
            statement.setFetchSize(10);

            // scrape the protections from the result set now
            List<Protection> protections = resolveProtections(statement);

            // throw all of the protections in
            for (Protection protection : protections) {
                cache.addProtection(protection);
            }
        } catch (SQLException e) {
            printException(e);
        }
    }

    /**
     * Load a protection at the given coordinates
     *
     * @param worldName
     * @param x
     * @param y
     * @param z
     * @return the Protection object
     */
    public Protection loadProtection(String worldName, int x, int y, int z) {
        return loadProtection(worldName, x, y, z, false);
    }

    /**
     * Load a protection at the given coordinates
     *
     * @param worldName
     * @param x
     * @param y
     * @param z
     * @param ignoreProtectionCount
     * @return the Protection object
     */
    private Protection loadProtection(String worldName, int x, int y, int z, boolean ignoreProtectionCount) {
        // the unique key to use in the cache
        String cacheKey = worldName + ":" + x + ":" + y + ":" + z;

        // the protection cache
        ProtectionCache cache = LWC.getInstance().getProtectionCache();

        // check if the protection is already cached
        Protection cached = cache.getProtection(cacheKey);
        if (cached != null) {
            // System.out.println("loadProtection() => CACHE HIT");
            return cached;
        }

        // Is it possible that there are protections in the cache?
        if (!ignoreProtectionCount && hasAllProtectionsCached()) {
            // System.out.println("loadProtection() => HAS_ALL_PROTECTIONS_CACHED");
            return null; // nothing was in the cache, nothing assumed to be in the database
        }
        // System.out.println("loadProtection() => QUERYING");

        try {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections WHERE x = ? AND y = ? AND z = ? AND world = ?");
            statement.setInt(1, x);
            statement.setInt(2, y);
            statement.setInt(3, z);
            statement.setString(4, worldName);

            Protection protection = resolveProtection(statement);

            if (protection == null && "1".equals(uuidConversionStage)) {
                protection = legacyLoadProtection(worldName, x, y, z);
            }

            if (protection != null) {
                // cache the protection
                cache.addProtection(protection);
            }

            return protection;
        } catch (SQLException e) {
            printException(e);
        }

        return null;
    }

    /**
     * Load a protection from the old database format. This is only usable while the old database is still being converted.
     *
     * @param worldName
     * @param x
     * @param y
     * @param z
     * @return
     */
    private Protection legacyLoadProtection(String worldName, int x, int y, int z) {
        try {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections_old_converting WHERE x = ? AND y = ? AND z = ? AND world = ?");
            statement.setInt(1, x);
            statement.setInt(2, y);
            statement.setInt(3, z);
            statement.setString(4, worldName);

            Protection protection = resolveProtection(statement);

            if (protection != null) {
                protection.convertPlayerNamesToUUIDs();
            }

            return protection;
        } catch (SQLException e) {
            printException(e);
        }

        return null;
    }

    /**
     * Load all protections (use sparingly !!)
     *
     * @return
     */
    public List<Protection> loadProtections() {
        try {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections");

            return resolveProtections(statement);
        } catch (Exception e) {
            printException(e);
        }

        return new ArrayList<Protection>();
    }

    /**
     * Load the first protection within a block's radius
     *
     * @param world
     * @param baseX
     * @param baseY
     * @param baseZ
     * @param radius
     * @return list of Protection objects found
     */
    public List<Protection> loadProtections(String world, int baseX, int baseY, int baseZ, int radius) {
        if (hasAllProtectionsCached()) {
            ProtectionCache cache = LWC.getInstance().getProtectionCache();
            List<Protection> protections = new ArrayList<Protection>();

            if (cache.size() < 1000) {
                for (Protection protection : cache.getReferences().keySet()) {
                    int x = protection.getX();
                    int y = protection.getY();
                    int z = protection.getZ();

                    if (x >= baseX - radius && x <= baseX + radius && y >= baseY - radius && y <= baseY + radius && z >= baseZ - radius && z <= baseZ + radius) {
                        protections.add(protection);
                    }
                }
            } else {
                for (int x = baseX - radius; x < baseX + radius; x++) {
                    for (int y = baseY - radius; y < baseY + radius; y++) {
                        for (int z = baseZ - radius; z < baseZ + radius; z++) {
                            Protection protection = cache.getProtection(world + ":" + x + ":" + y + ":" + z);

                            if (protection != null) {
                                protections.add(protection);
                            }
                        }
                    }
                }
            }

            return protections;
        }

        try {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections WHERE world = ? AND x >= ? AND x <= ? AND y >= ? AND y <= ? AND z >= ? AND z <= ?");

            statement.setString(1, world);
            statement.setInt(2, baseX - radius);
            statement.setInt(3, baseX + radius);
            statement.setInt(4, baseY - radius);
            statement.setInt(5, baseY + radius);
            statement.setInt(6, baseZ - radius);
            statement.setInt(7, baseZ + radius);

            return resolveProtections(statement);
        } catch (Exception e) {
            printException(e);
        }

        return new ArrayList<Protection>();
    }

    /**
     * Remove all protections for a given player
     *
     * @param player
     * @return the amount of protections removed
     */
    public int removeProtectionsByPlayer(String player) {
        int removed = 0;

        for (Protection protection : loadProtectionsByPlayer(player)) {
            protection.remove();
            removed++;
        }

        return removed;
    }

    /**
     * Load all protections in the coordinate ranges
     *
     * @param world
     * @param x1
     * @param x2
     * @param y1
     * @param y2
     * @param z1
     * @param z2
     * @return list of Protection objects found
     */
    public List<Protection> loadProtections(String world, int x1, int x2, int y1, int y2, int z1, int z2) {
        try {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections WHERE world = ? AND x >= ? AND x <= ? AND y >= ? AND y <= ? AND z >= ? AND z <= ?");

            statement.setString(1, world);
            statement.setInt(2, x1);
            statement.setInt(3, x2);
            statement.setInt(4, y1);
            statement.setInt(5, y2);
            statement.setInt(6, z1);
            statement.setInt(7, z2);

            return resolveProtections(statement);
        } catch (Exception e) {
            printException(e);
        }

        return new ArrayList<Protection>();
    }

    /**
     * Load protections by a player
     *
     * @param player
     * @return
     */
    public List<Protection> loadProtectionsByPlayer(String player) {
        List<Protection> protections = new ArrayList<Protection>();

        PlayerInfo playerInfo = PlayerRegistry.getPlayerInfo(player);

        try {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections WHERE owner = ?");
            statement.setInt(1, playerInfo.getId());

            return resolveProtections(statement);
        } catch (Exception e) {
            printException(e);
        }

        return protections;
    }

    /**
     * Load protections by a player
     *
     * @param player
     * @param start
     * @param count
     * @return
     */
    public List<Protection> loadProtectionsByPlayer(String player, int start, int count) {
        List<Protection> protections = new ArrayList<Protection>();

        PlayerInfo playerInfo = PlayerRegistry.getPlayerInfo(player);

        try {
            PreparedStatement statement = prepare("SELECT id, owner, type, x, y, z, data, blockId, world, password, date, last_accessed FROM " + prefix + "protections WHERE owner = ? ORDER BY id DESC limit ?,?");
            statement.setInt(1, playerInfo.getId());
            statement.setInt(2, start);
            statement.setInt(3, count);

            return resolveProtections(statement);
        } catch (Exception e) {
            printException(e);
        }

        return protections;
    }

    /**
     * Register a protection
     *
     * @param blockId
     * @param type
     * @param world
     * @param player
     * @param data
     * @param x
     * @param y
     * @param z
     * @return
     */
    @Deprecated
    public Protection registerProtection(int blockId, int type, String world, String player, String data, int x, int y, int z) {
        return registerProtection(blockId, Protection.Type.values()[type], world, player, data, x, y, z);
    }

    /**
     * Register a protection
     *
     * @param blockId
     * @param type
     * @param world
     * @param player
     * @param data
     * @param x
     * @param y
     * @param z
     * @return
     */
    public Protection registerProtection(int blockId, Protection.Type type, String world, String player, String data, int x, int y, int z) {
        ProtectionCache cache = LWC.getInstance().getProtectionCache();
        PlayerInfo playerInfo = PlayerRegistry.getPlayerInfo(player);

        try {
            PreparedStatement statement = prepare("INSERT INTO " + prefix + "protections (blockId, type, world, owner, password, x, y, z, date, last_accessed) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            statement.setInt(1, blockId);
            statement.setInt(2, type.ordinal());
            statement.setString(3, world);
            statement.setInt(4, playerInfo.getId());
            statement.setString(5, data);
            statement.setInt(6, x);
            statement.setInt(7, y);
            statement.setInt(8, z);
            statement.setString(9, new Timestamp(new Date().getTime()).toString());
            statement.setLong(10, System.currentTimeMillis() / 1000L);

            statement.executeUpdate();

            // We need to create the initial transaction for this protection
            // this transaction is viewable and modifiable during POST_REGISTRATION
            Protection protection = loadProtection(world, x, y, z, true);
            protection.removeCache();

            // if history logging is enabled, create it
            if (LWC.getInstance().isHistoryEnabled() && protection != null) {
                History transaction = protection.createHistoryObject();

                transaction.setPlayer(playerInfo);
                transaction.setType(History.Type.TRANSACTION);
                transaction.setStatus(History.Status.ACTIVE);

                // store the player that created the protection
                transaction.addMetaData("creator=" + player);

                // now sync the history object to the database
                transaction.saveNow();
            }

            // Cache it
            if (protection != null) {
                cache.addProtection(protection);
                protectionCount++;
            }

            // return the newly created protection
            return protection;
        } catch (SQLException e) {
            printException(e);
        }

        return null;
    }

    /**
     * Sync a History object to the database or save a newly created one
     *
     * @param history
     */
    public void saveHistory(History history) {
        try {
            PreparedStatement statement;

            if (history.doesExist()) {
                statement = prepare("UPDATE " + prefix + "history SET protectionId = ?, player = ?, x = ?, y = ?, z = ?, type = ?, status = ?, metadata = ?, timestamp = ? WHERE id = ?");
            } else {
                statement = prepare("INSERT INTO " + prefix + "history (protectionId, player, x, y, z, type, status, metadata, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", true);
                history.setTimestamp(System.currentTimeMillis() / 1000L);
            }

            statement.setInt(1, history.getProtectionId());
            statement.setInt(2, history.getPlayer().getId());
            statement.setInt(3, history.getX());
            statement.setInt(4, history.getY());
            statement.setInt(5, history.getZ());
            statement.setInt(6, history.getType().ordinal());
            statement.setInt(7, history.getStatus().ordinal());
            statement.setString(8, history.getSafeMetaData());
            statement.setLong(9, history.getTimestamp());

            if (history.doesExist()) {
                statement.setInt(10, history.getId());
            }

            int affectedRows = statement.executeUpdate();

            // set the history id if inserting
            if (!history.doesExist()) {
                if (affectedRows > 0) {
                    ResultSet generatedKeys = statement.getGeneratedKeys();

                    // get the key inserted
                    if (generatedKeys.next()) {
                        history.setId(generatedKeys.getInt(1));
                    }

                    generatedKeys.close();
                }
            }
        } catch (SQLException e) {
            printException(e);
        }
    }

    /**
     * Invalid all history objects for a player
     *
     * @param player
     */
    public void invalidateHistory(String player) {
        PlayerInfo playerInfo = PlayerRegistry.getPlayerInfo(player);

        try {
            PreparedStatement statement = prepare("UPDATE " + prefix + "history SET status = ? WHERE player = ?");
            statement.setInt(1, History.Status.INACTIVE.ordinal());
            statement.setInt(2, playerInfo.getId());

            statement.executeUpdate();
        } catch (SQLException e) {
            printException(e);
        }
    }

    /**
     * Resolve 1 history object from the result set but do not close it
     *
     * @return
     */
    private History resolveHistory(History history, ResultSet set) throws SQLException {
        if (history == null) {
            return null;
        }

        int historyId = set.getInt("id");
        int protectionId = set.getInt("protectionId");
        int x = set.getInt("x");
        int y = set.getInt("y");
        int z = set.getInt("z");
        int type_ord = set.getInt("type");
        int status_ord = set.getInt("status");
        String[] metadata = set.getString("metadata").split(",");
        long timestamp = set.getLong("timestamp");

        Object player = set.getObject("player");
        PlayerInfo playerInfo;

        if (player instanceof String) {
            playerInfo = PlayerRegistry.getPlayerInfo(player.toString());
        } else {
            playerInfo = PlayerRegistry.getPlayerInfo((Integer) player);
        }

        History.Type type = History.Type.values()[type_ord];
        History.Status status = History.Status.values()[status_ord];

        history.setId(historyId);
        history.setProtectionId(protectionId);
        history.setType(type);
        history.setPlayer(playerInfo);
        history.setX(x);
        history.setY(y);
        history.setZ(z);
        history.setStatus(status);
        history.setMetaData(metadata);
        history.setTimestamp(timestamp);

        return history;
    }

    /**
     * Load all of the History objects for a given protection
     *
     * @param protection
     * @return
     */
    public List<History> loadHistory(Protection protection) {
        List<History> temp = new ArrayList<History>();

        if (!LWC.getInstance().isHistoryEnabled()) {
            return temp;
        }

        try {
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history WHERE protectionId = ? ORDER BY id DESC");
            statement.setInt(1, protection.getId());

            ResultSet set = statement.executeQuery();

            while (set.next()) {
                History history = resolveHistory(protection.createHistoryObject(), set);

                if (history != null) {
                    // seems ok
                    temp.add(history);
                }
            }

            set.close();
        } catch (SQLException e) {
            printException(e);
        }

        return temp;
    }

    /**
     * Load all protection history that the given player created
     *
     * @param player
     * @return
     */
    public List<History> loadHistory(Player player) {
        return loadHistory(player.getName());
    }

    /**
     * Load all protection history that the given player created
     *
     * @param player
     * @return
     */
    public List<History> loadHistory(String player) {
        List<History> temp = new ArrayList<History>();

        if (!LWC.getInstance().isHistoryEnabled()) {
            return temp;
        }

        PlayerInfo playerInfo = PlayerRegistry.getPlayerInfo(player);

        try {
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history WHERE player = ? ORDER BY id DESC");
            statement.setInt(1, playerInfo.getId());

            ResultSet set = statement.executeQuery();

            while (set.next()) {
                History history = resolveHistory(new History(), set);

                if (history != null) {
                    // seems ok
                    temp.add(history);
                }
            }

            set.close();
        } catch (SQLException e) {
            printException(e);
        }

        return temp;
    }

    /**
     * Load all protection history that has the given history id
     *
     * @param historyId
     * @return
     */
    public History loadHistory(int historyId) {
        if (!LWC.getInstance().isHistoryEnabled()) {
            return null;
        }

        try {
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history WHERE id = ?");
            statement.setInt(1, historyId);

            ResultSet set = statement.executeQuery();

            if (set.next()) {
                History history = resolveHistory(new History(), set);

                set.close();
                return history;
            }

            set.close();

            if ("2".equals(uuidConversionStage)) {
                return legacyLoadHistory(historyId);
            }
        } catch (SQLException e) {
            printException(e);
        }

        return null;
    }

    /**
     * Load all protection history that has the given history id from the legacy database
     *
     * @param historyId
     * @return
     */
    public History legacyLoadHistory(int historyId) {
        if (!LWC.getInstance().isHistoryEnabled()) {
            return null;
        }

        try {
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history_old_converting WHERE id = ?");
            statement.setInt(1, historyId);

            ResultSet set = statement.executeQuery();

            if (set.next()) {
                History history = resolveHistory(new History(), set);

                set.close();
                return history;
            }

            set.close();
        } catch (SQLException e) {
            printException(e);
        }

        return null;
    }

    /**
     * Load all protection history that the given player created for a given page, getting count history items.
     *
     * @param player
     * @param start
     * @param count
     * @return
     */
    public List<History> loadHistory(Player player, int start, int count) {
        return loadHistory(player.getName(), start, count);
    }

    /**
     * Load all protection history that the given player created for a given page, getting count history items.
     *
     * @param player
     * @param start
     * @param count
     * @return
     */
    public List<History> loadHistory(String player, int start, int count) {
        List<History> temp = new ArrayList<History>();

        if (!LWC.getInstance().isHistoryEnabled()) {
            return temp;
        }

        PlayerInfo playerInfo = PlayerRegistry.getPlayerInfo(player);

        try {
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history WHERE player = ? ORDER BY id DESC LIMIT ?,?");
            statement.setInt(1, playerInfo.getId());
            statement.setInt(2, start);
            statement.setInt(3, count);

            ResultSet set = statement.executeQuery();

            while (set.next()) {
                History history = resolveHistory(new History(), set);

                if (history != null) {
                    // seems ok
                    temp.add(history);
                }
            }

            set.close();
        } catch (SQLException e) {
            printException(e);
        }

        return temp;
    }

    /**
     * Load all protection history
     *
     * @return
     */
    public List<History> loadHistory() {
        List<History> temp = new ArrayList<History>();

        if (!LWC.getInstance().isHistoryEnabled()) {
            return temp;
        }

        try {
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history ORDER BY id DESC");
            ResultSet set = statement.executeQuery();

            while (set.next()) {
                History history = resolveHistory(new History(), set);

                if (history != null) {
                    // seems ok
                    temp.add(history);
                }
            }

            set.close();
        } catch (SQLException e) {
            printException(e);
        }

        return temp;
    }

    /**
     * Load all protection history for the given status
     *
     * @return
     */
    public List<History> loadHistory(History.Status status) {
        List<History> temp = new ArrayList<History>();

        if (!LWC.getInstance().isHistoryEnabled()) {
            return temp;
        }

        try {
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history WHERE status = ? ORDER BY id DESC");
            statement.setInt(1, status.ordinal());
            ResultSet set = statement.executeQuery();

            while (set.next()) {
                History history = resolveHistory(new History(), set);

                if (history != null) {
                    // seems ok
                    temp.add(history);
                }
            }

            set.close();
        } catch (SQLException e) {
            printException(e);
        }

        return temp;
    }

    /**
     * Load all of the history at the given location
     *
     * @param x
     * @param y
     * @param z
     * @return
     */
    public List<History> loadHistory(int x, int y, int z) {
        List<History> temp = new ArrayList<History>();

        if (!LWC.getInstance().isHistoryEnabled()) {
            return temp;
        }

        try {
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history WHERE x = ? AND y = ? AND z = ?");
            statement.setInt(1, x);
            statement.setInt(2, y);
            statement.setInt(3, z);

            ResultSet set = statement.executeQuery();

            while (set.next()) {
                History history = resolveHistory(new History(), set);

                if (history != null) {
                    // seems ok
                    temp.add(history);
                }
            }

            set.close();
        } catch (SQLException e) {
            printException(e);
        }

        return temp;
    }

    /**
     * Load all of the history at the given location
     *
     * @param player
     * @param x
     * @param y
     * @param z
     * @return
     */
    public List<History> loadHistory(String player, int x, int y, int z) {
        List<History> temp = new ArrayList<History>();

        if (!LWC.getInstance().isHistoryEnabled()) {
            return temp;
        }

        PlayerInfo playerInfo = PlayerRegistry.getPlayerInfo(player);

        try {
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history WHERE player = ? AND x = ? AND y = ? AND z = ?");
            statement.setInt(1, playerInfo.getId());
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);

            ResultSet set = statement.executeQuery();

            while (set.next()) {
                History history = resolveHistory(new History(), set);

                if (history != null) {
                    // seems ok
                    temp.add(history);
                }
            }

            set.close();
        } catch (SQLException e) {
            printException(e);
        }

        return temp;
    }

    /**
     * Load all protection history
     *
     * @return
     */
    public List<History> loadHistory(int start, int count) {
        List<History> temp = new ArrayList<History>();

        if (!LWC.getInstance().isHistoryEnabled()) {
            return temp;
        }

        try {
            PreparedStatement statement = prepare("SELECT * FROM " + prefix + "history ORDER BY id DESC LIMIT ?,?");
            statement.setInt(1, start);
            statement.setInt(2, count);

            ResultSet set = statement.executeQuery();

            while (set.next()) {
                History history = resolveHistory(new History(), set);

                if (history != null) {
                    // seems ok
                    temp.add(history);
                }
            }

            set.close();
        } catch (SQLException e) {
            printException(e);
        }

        return temp;
    }

    /**
     * Get a player's info from the database
     *
     * @param id
     * @return
     */
    public PlayerInfo getPlayerInfo(int id) {
        try {
            PreparedStatement statement = prepare("SELECT id, uuid, name FROM " + prefix + "players WHERE id = ?");
            statement.setInt(1, id);
            ResultSet set = statement.executeQuery();

            if (set.next()) {
                String uuid = set.getString("uuid");
                PlayerInfo playerInfo = new PlayerInfo(set.getInt("id"), uuid != null ? UUID.fromString(uuid) : null, set.getString("name"));
                set.close();
                return playerInfo;
            }

            set.close();
        } catch (SQLException e) {
            printException(e);
        }

        return null;
    }

    /**
     * Create a player info and return it
     *
     * @param uuid
     * @param playerName
     * @return
     */
    public PlayerInfo getOrCreatePlayerInfo(UUID uuid, String playerName) {
        if (uuid != null) {
            PlayerInfo playerInfo = getPlayerInfo(uuid);

            if (playerInfo != null) {
                return playerInfo;
            }
        } else {
            List<PlayerInfo> found = getPlayerInfo(playerName);

            for (PlayerInfo playerInfo : found) {
                if (playerInfo.getUUID() == null) {
                    return playerInfo;
                }
            }
        }

        try {
            PreparedStatement statement = prepare("INSERT INTO " + prefix + "players (uuid, name) VALUES (?, ?)");

            statement.setString(1, uuid == null ? null : uuid.toString());
            statement.setString(2, playerName);

            statement.executeUpdate();
        } catch (SQLException e) {
            printException(e);
        }

        if (uuid != null) {
            return getPlayerInfo(uuid);
        } else {
            return getPlayerInfo(playerName).get(0);
        }
    }

    /**
     * Search for a player given their name. More than one player might be known to have that name.
     *
     * @param playerName
     * @return
     */
    public List<PlayerInfo> getPlayerInfo(String playerName) {
        List<PlayerInfo> res = new ArrayList<PlayerInfo>();

        try {
            PreparedStatement statement = prepare("SELECT id, uuid, name FROM " + prefix + "players WHERE LOWER(name) = LOWER(?)");
            statement.setString(1, playerName);
            ResultSet set = statement.executeQuery();

            while (set.next()) {
                String uuid = set.getString("uuid");
                PlayerInfo playerInfo = new PlayerInfo(set.getInt("id"), uuid != null ? UUID.fromString(uuid) : null, set.getString("name"));
                res.add(playerInfo);
            }

            set.close();
        } catch (SQLException e) {
            printException(e);
        }

        return res;
    }

    /**
     * Search for a player given their UUID
     *
     * @param uuid
     * @return
     */
    public PlayerInfo getPlayerInfo(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        try {
            PreparedStatement statement = prepare("SELECT id, uuid, name FROM " + prefix + "players WHERE LOWER(uuid) = LOWER(?)");
            statement.setString(1, uuid.toString());
            ResultSet set = statement.executeQuery();

            while (set.next()) {
                PlayerInfo playerInfo = new PlayerInfo(set.getInt("id"), UUID.fromString(set.getString("uuid")), set.getString("name"));
                set.close();
                return playerInfo;
            }

            set.close();
        } catch (SQLException e) {
            printException(e);
        }

        return null;
    }

    /**
     * Save a player's info to the database
     *
     * @param playerInfo
     */
    public void savePlayerInfo(PlayerInfo playerInfo) {
        try {
            PreparedStatement statement = prepare("REPLACE INTO " + prefix + "players (id, uuid, name) VALUES (?, ?, ?)");

            statement.setInt(1, playerInfo.getId());
            statement.setString(2, playerInfo.getUUID().toString());
            statement.setString(3, playerInfo.getName());

            statement.executeUpdate();
        } catch (SQLException e) {
            printException(e);
        }
    }

    /**
     * Save a protection to the database
     *
     * @param protection
     */
    public void saveProtection(Protection protection) {
        try {
            PreparedStatement statement = prepare("REPLACE INTO " + prefix + "protections (id, type, blockId, world, data, owner, password, x, y, z, date, last_accessed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            statement.setInt(1, protection.getId());
            statement.setInt(2, protection.getType().ordinal());
            statement.setInt(3, protection.getBlockId());
            statement.setString(4, protection.getWorld());
            statement.setString(5, protection.getData().toJSONString());
            statement.setInt(6, protection.getOwner().getId());
            statement.setString(7, protection.getPassword());
            statement.setInt(8, protection.getX());
            statement.setInt(9, protection.getY());
            statement.setInt(10, protection.getZ());
            statement.setString(11, protection.getCreation());
            statement.setLong(12, protection.getLastAccessed());

            statement.executeUpdate();
        } catch (SQLException e) {
            printException(e);
        }
    }

    /**
     * Free a chest from protection
     *
     * @param protectionId the protection Id
     */
    public void removeProtection(int protectionId) {
        try {
            PreparedStatement statement = prepare("DELETE FROM " + prefix + "protections WHERE id = ?");
            statement.setInt(1, protectionId);

            int affected = statement.executeUpdate();

            if (affected > 0) {
                protectionCount -= affected;
            }

            if ("1".equals(uuidConversionStage)) {
                statement = prepare("DELETE FROM " + prefix + "protections_old_converting WHERE id = ?");
                statement.setInt(1, protectionId);

                affected = statement.executeUpdate();

                if (affected > 0) {
                    protectionCount -= affected;
                }
            }
        } catch (SQLException e) {
            printException(e);
        }

        // removeProtectionHistory(protectionId);
    }

    public void removeProtectionHistory(int protectionId) {
        try {
            PreparedStatement statement = prepare("DELETE FROM " + prefix + "history WHERE protectionId = ?");
            statement.setInt(1, protectionId);

            statement.executeUpdate();
        } catch (SQLException e) {
            printException(e);
        }
    }

    public void removeHistory(int historyId) {
        try {
            PreparedStatement statement = prepare("DELETE FROM " + prefix + "history WHERE id = ?");
            statement.setInt(1, historyId);

            statement.executeUpdate();
        } catch (SQLException e) {
            printException(e);
        }
    }

    /**
     * Remove **<b>ALL</b>** all of the protections registered by LWC
     */
    public void removeAllProtections() {
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate("DELETE FROM " + prefix + "protections");
            protectionCount = 0;
            statement.close();
        } catch (SQLException e) {
            printException(e);
        }
    }

}
