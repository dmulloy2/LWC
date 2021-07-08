/*
 * Copyright 2018 Max Lee (Phoenix616). All rights reserved.
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

package com.griefcraft.cache;


import com.griefcraft.lwc.LWC;
import org.bukkit.Material;

public class MaterialCache {

    /**
     * The LWC instance this set belongs to
     */
    private final LWC lwc;

    /**
     * Map numeric IDs to types
     */
    private final LRUCache<Integer, Material> idToType;

    /**
     * Map numeric types to numeric Ids
     */
    private final LRUCache<Material, Integer> typeToId;

    /**
     * The capacity of the cache
     */
    private int capacity;

    public MaterialCache(LWC lwc) {
        this.lwc = lwc;
        this.capacity = lwc.getConfiguration().getInt("core.cacheSize", 10000);

        this.idToType = new LRUCache<>(capacity);
        this.typeToId = new LRUCache<>(capacity);
    }

    /**
     * Gets the default capacity of the cache
     *
     * @return
     */
    public int capacity() {
        return capacity;
    }

    public int getSize() {
        return typeToId.size();
    }

    public boolean isEmpty() {
        return typeToId.isEmpty();
    }

    /**
     * Clears the entire protection cache
     */
    public void clear() {
        // remove hard refs
        idToType.clear();
        typeToId.clear();
    }

    /**
     * Cache a mapping
     *
     * @param material
     */
    public void addTypeId(Material material, int id) {
        typeToId.put(material, id);
        idToType.put(id, material);
    }

    /**
     * Remove the material from the cache
     *
     * @param material
     */
    public void removeType(Material material) {
        Integer id = typeToId.remove(material);
        if (id != null) {
            idToType.remove(id);
        }
    }

    /**
     * Remove the material from the cache
     *
     * @param id
     */
    public void removeType(int id) {
        Material type = idToType.remove(id);
        if (type != null) {
            typeToId.remove(type);
        }
    }

    /**
     * Get the ID of a type
     *
     * @param type
     * @return The cached ID or -1 if not found
     */
    public int getId(Material type) {
        Integer id = typeToId.get(type);
        return id != null ? id : -1;
    }

    /**
     * Get the Material type of an ID
     *
     * @param id
     * @return The cached Material type or null if not found
     */
    public Material getType(int id) {
        return idToType.get(id);
    }
}
