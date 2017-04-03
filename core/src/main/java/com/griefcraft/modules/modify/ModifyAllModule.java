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

package com.griefcraft.modules.modify;

import com.griefcraft.lwc.LWC;
import com.griefcraft.model.LWCPlayer;
import com.griefcraft.model.Permission;
import com.griefcraft.model.Protection;
import com.griefcraft.scripting.JavaModule;
import com.griefcraft.scripting.event.LWCCommandEvent;
import com.griefcraft.util.Colors;
import com.griefcraft.util.StringUtil;
import com.griefcraft.util.UUIDRegistry;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ModifyAllModule extends JavaModule {

    @Override
    public void onCommand(LWCCommandEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!event.hasFlag("modifyall")) {
            return;
        }

        final LWC lwc = event.getLWC();
        final CommandSender sender = event.getSender();
        final String[] args = event.getArgs();
        event.setCancelled(true);

        if (!lwc.hasPlayerPermission(sender, "lwc.modify")) {
            lwc.sendLocale(sender, "protection.accessdenied");
            return;
        }

        if (args.length < 1) {
            lwc.sendLocale(sender, "help.modify");
            return;
        }

        if (!(sender instanceof Player)) {
            lwc.sendLocale(sender, "lwc.onlyrealplayers");
            return;
        }
        final LWCPlayer player = lwc.wrapPlayer(sender);
        Bukkit.getScheduler().runTaskAsynchronously(lwc.getPlugin(), new Runnable() {
        	public void run() {
		    	int count = 0;
		        // Get all of the player's protections
		        for (Protection protection : lwc.getPhysicalDatabase().loadProtectionsByPlayer(player.getUniqueId().toString())) {
		        	// Does it match a protection type?
		            try {
		                Protection.Type protectionType = Protection.Type.matchType(args[0]);

		                if (protectionType != null) {
		                    protection.setType(protectionType);
		                    protection.save();
			                protection.removeCache(); 
			            	LWC.getInstance().getProtectionCache().addProtection(protection);
		                    count++;

		                    // If it's being passworded, we need to set the password
		                    if (protectionType == Protection.Type.PASSWORD) {
		                        String password = StringUtil.join(args, 1);
		                        protection.setPassword(LWC.getInstance().encrypt(password));
		                    }
		                }
		            } catch (IllegalArgumentException e) {
		                // It's normal for this to be thrown if nothing was matched
		            }
		        }

                if (count > 0) {
                	// Modified the protections above, no need to continue processing
                	sender.sendMessage("§6Modified " + count + " protections");
                	return;
                }

		        // Get all of the player's protections
		        for (Protection protection : lwc.getPhysicalDatabase().loadProtectionsByPlayer(player.getUniqueId().toString())) {

		            for (String value : args) {
		                boolean remove = false;
		                boolean isAdmin = false;
		                Permission.Type type = Permission.Type.PLAYER;

		                // Gracefully ignore id
		                if (value.startsWith("id:")) {
		                    continue;
		                }

		                if (value.startsWith("-")) {
		                    remove = true;
		                    value = value.substring(1);
		                }

		                if (value.startsWith("@")) {
		                    isAdmin = true;
		                    value = value.substring(1);
		                }

		                if (value.toLowerCase().startsWith("p:")) {
		                    type = Permission.Type.PLAYER;
		                    value = value.substring(2);
		                }

		                if (value.toLowerCase().startsWith("g:")) {
		                    type = Permission.Type.GROUP;
		                    value = value.substring(2);
		                }

		                if (value.toLowerCase().startsWith("t:")) {
		                    type = Permission.Type.TOWN;
		                    value = value.substring(2);
		                }

		                if (value.toLowerCase().startsWith("town:")) {
		                    type = Permission.Type.TOWN;
		                    value = value.substring(5);
		                }

		                if (value.toLowerCase().startsWith("item:")) {
		                    type = Permission.Type.ITEM;
		                    value = value.substring(5);
		                }

		                if (value.toLowerCase().startsWith("r:")) {
		                    type = Permission.Type.REGION;
		                    value = value.substring(2);
		                }

		                if (value.toLowerCase().startsWith("region:")) {
		                    type = Permission.Type.REGION;
		                    value = value.substring(7);
		                }

		                if (value.trim().isEmpty()) {
		                    continue;
		                }

		                // If it's a player, convert it to UUID
		                if (type == Permission.Type.PLAYER) {
		                    UUID uuid = UUIDRegistry.getUUID(value);

		                    if (uuid != null) {
		                        value = uuid.toString();
		                    }
		                }

		                if (!remove) {
		                    Permission permission = new Permission(value, type);
		                    permission.setAccess(isAdmin ? Permission.Access.ADMIN : Permission.Access.PLAYER);

		                    // add it to the protection and queue it to be saved
		                    protection.addPermission(permission);
		                    protection.save();
		                    count++;
		                } else {
		                    protection.removePermissions(value, type);
		                    protection.save();
		                    count++;
		                }
		                protection.removeCache(); 
		            	LWC.getInstance().getProtectionCache().addProtection(protection);
		            }
		        }
		        sender.sendMessage("§6Modified " + count + " protections");
        	}
        });
        player.removeAllActions();
   }
}
