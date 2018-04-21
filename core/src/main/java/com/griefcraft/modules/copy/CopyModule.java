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

package com.griefcraft.modules.copy;

import com.griefcraft.lwc.LWC;
import com.griefcraft.model.*;
import com.griefcraft.scripting.JavaModule;
import com.griefcraft.scripting.event.LWCBlockInteractEvent;
import com.griefcraft.scripting.event.LWCCommandEvent;
import com.griefcraft.scripting.event.LWCProtectionInteractEvent;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CopyModule extends JavaModule {

    @Override
    public void onProtectionInteract(LWCProtectionInteractEvent event) {
        if (event.getResult() != Result.DEFAULT) {
            return;
        }

        if (!(event.hasAction("copy") || event.hasAction("paste"))) {
            return;
        }

        LWC lwc = event.getLWC();
        Protection protection = event.getProtection();
        Player player = event.getPlayer();
        LWCPlayer lwcPlayer = lwc.wrapPlayer(player);
        event.setResult(Result.CANCEL);

        if (event.canAdmin()) {
            if (event.hasAction("copy")) {
                Protection copy = new Protection();
                if (protection.getPermissions() != null) {
                    copy.setPermissions(protection.getPermissions());
                }
                if (protection.getFlags() != null) {
                    copy.setFlags(protection.getFlags());
                }
                if (protection.getPassword() != null) {
                    copy.setPassword(protection.getPassword());
                }
                if (protection.getType() != null) {
                    copy.setType(protection.getType());
                }

                Action action = new PasteAction(copy);
                action.setName("paste");
                action.setPlayer(lwcPlayer);

                lwcPlayer.removeAllActions();
                lwcPlayer.addAction(action);

                lwc.sendLocale(player, "protection.copy.paste");
            } else if (event.hasAction("paste")) {
                Action action = lwcPlayer.getAction("paste");

                // They were!
                if (action instanceof PasteAction) {
                    Protection paste = action.getProtection();

                    if (paste.getPermissions() != null) {
                        protection.setPermissions(paste.getPermissions());
                    }
                    if (paste.getFlags() != null) {
                        protection.setFlags(paste.getFlags());
                    }
                    if (paste.getPassword() != null) {
                        protection.setPassword(paste.getPassword());
                    }
                    if (paste.getType() != null) {
                        protection.setType(paste.getType());
                    }
                    protection.save();
                    lwc.sendLocale(player, "protection.copy.finalize");
                }

                lwc.removeModes(player);
            }
        }
    }

    @Override
    public void onBlockInteract(LWCBlockInteractEvent event) {
        if (event.getResult() != Result.DEFAULT) {
            return;
        }

        if (!event.hasAction("copy") || !event.hasAction("paste")) {
            return;
        }

        LWC lwc = event.getLWC();
        Block block = event.getBlock();
        Player player = event.getPlayer();
        event.setResult(Result.CANCEL);

        lwc.sendLocale(player, "protection.interact.error.notregistered", "block", LWC.materialToString(block));
        lwc.removeModes(player);
    }

    @Override
    public void onCommand(LWCCommandEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!event.hasFlag("copy")) {
            return;
        }

        LWC lwc = event.getLWC();
        CommandSender sender = event.getSender();

        if (!(sender instanceof Player)) {
            return;
        }

        event.setCancelled(true);

        if (!lwc.hasPlayerPermission(sender, "lwc.copy")) {
            lwc.sendLocale(sender, "protection.accessdenied");
            return;
        }

        LWCPlayer player = lwc.wrapPlayer(sender);

        Action action = new Action();
        action.setName("copy");
        action.setPlayer(player);

        player.removeAllActions();
        player.addAction(action);

        lwc.sendLocale(player, "protection.copy.copy");

    }

}
