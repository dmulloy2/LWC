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

package com.griefcraft.util.matchers;

import com.griefcraft.util.ProtectionFinder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;

/**
 * Matches wall entities
 * TODO fix buttons and levers
 */
public class WallMatcher implements ProtectionFinder.Matcher {

    /**
     * Possible faces around the base block that protections could be at
     */
    public static final BlockFace[] POSSIBLE_FACES = new BlockFace[]{ BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.DOWN, BlockFace.UP };

    public boolean matches(ProtectionFinder finder) {
        // The block we are working on
        Block block = finder.getBaseBlock().getBlock();

        // look to see if any adjacent protections depend on this block
        for (BlockFace blockFace : POSSIBLE_FACES) {
            Block face = block.getRelative(blockFace);
            Block matched = getAttachedTo(face);
            if(matched != null && matched.getX() == block.getX() && matched.getY() == block.getY() && matched.getZ() == block.getZ()) {
                finder.addBlock(face);
                return true;
            }
        }

        return false;
    }

    /**
     * Try and match a wall block
     *
     * @param block
     * @param matchingFace
     * @return
     */
    private Block getAttachedTo(Block block) {
        final BlockData d = block.getBlockData();
		// Paper 1.16 breaks this:
        /*if (d instanceof Switch && ((Switch) d).getFace() != Switch.Face.WALL) {
            return block.getRelative(((Switch) d).getFace() == Switch.Face.FLOOR ? BlockFace.DOWN : BlockFace.UP);
        } else */
		if (d instanceof FaceAttachable && ((FaceAttachable) d).getAttachedFace() != FaceAttachable.AttachedFace.WALL) {
            return block.getRelative(((FaceAttachable) d).getAttachedFace() == FaceAttachable.AttachedFace.FLOOR ? BlockFace.DOWN : BlockFace.UP);
        } else if (d instanceof Directional) {
            return block.getRelative(((Directional) d).getFacing().getOppositeFace());
        }
        return null;
    }

}
