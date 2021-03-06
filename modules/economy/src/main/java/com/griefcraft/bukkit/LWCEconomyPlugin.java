/**
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
package com.griefcraft.bukkit;

import java.util.logging.Logger;

import com.griefcraft.lwc.EconomyModule;
import com.griefcraft.lwc.LWC;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class LWCEconomyPlugin extends JavaPlugin {
    private Logger logger = Logger.getLogger("LWC-Economy");

    /**
     * The LWC object
     */
    private LWC lwc;
    public LWC getLWC() { return lwc; }

    private boolean initialized;
    public boolean isInitialized() { return initialized; }

    /**
     * Our server listener, listens for iConomy to be loaded
     */
    private Listener serverListener = null;

    public LWCEconomyPlugin() {
        serverListener = new LWCEconomyServerListener(this);
    }

    /**
     * Initialize LWC-iConomy
     */
    public void init() {
        LWC.getInstance().getModuleLoader().registerModule(this, new EconomyModule(this));
        log("Hooked into LWC!");
        this.initialized = true;
    }

    public void onEnable() {
        Plugin lwc = getServer().getPluginManager().getPlugin("LWC");

        if (lwc != null) {
            this.lwc = LWC.getInstance();
            init();
        } else {
            // register the server listener
            getServer().getPluginManager().registerEvents(serverListener, this);
            log("Waiting for LWC to be enabled...");
        }
    }

    public void onDisable() {

    }

    private void log(String message) {
        logger.info("LWC-Economy: " + message);
    }
}
