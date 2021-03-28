/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2016-2021 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package us.myles.ViaVersion.bungee.platform;

import us.myles.ViaVersion.AbstractViaConfig;
import us.myles.ViaVersion.api.protocol.ProtocolVersion;
import us.myles.ViaVersion.bungee.providers.BungeeVersionProvider;

import java.io.File;
import java.net.URL;
import java.util.*;

public class BungeeViaConfig extends AbstractViaConfig {
    private static final List<String> UNSUPPORTED = Arrays.asList("nms-player-ticking", "item-cache", "anti-xray-patch", "quick-move-action-fix", "velocity-ping-interval", "velocity-ping-save", "velocity-servers", "blockconnection-method", "change-1_9-hitbox", "change-1_14-hitbox");
    private int bungeePingInterval;
    private boolean bungeePingSave;
    private Map<String, Integer> bungeeServerProtocols;

    public BungeeViaConfig(File configFile) {
        super(new File(configFile, "config.yml"));
        reloadConfig();
    }

    @Override
    protected void loadFields() {
        super.loadFields();
        bungeePingInterval = getInt("bungee-ping-interval", 60);
        bungeePingSave = getBoolean("bungee-ping-save", true);
        bungeeServerProtocols = get("bungee-servers", Map.class, new HashMap<>());
    }

    @Override
    public URL getDefaultConfigURL() {
        return BungeeViaConfig.class.getClassLoader().getResource("assets/viaversion/config.yml");
    }

    @Override
    protected void handleConfig(Map<String, Object> config) {
        // Parse servers
        Map<String, Object> servers;
        if (!(config.get("bungee-servers") instanceof Map)) {
            servers = new HashMap<>();
        } else {
            servers = (Map) config.get("bungee-servers");
        }
        // Convert any bad Protocol Ids
        for (Map.Entry<String, Object> entry : new HashSet<>(servers.entrySet())) {
            if (!(entry.getValue() instanceof Integer)) {
                if (entry.getValue() instanceof String) {
                    ProtocolVersion found = ProtocolVersion.getClosest((String) entry.getValue());
                    if (found != null) {
                        servers.put(entry.getKey(), found.getOriginalVersion());
                    } else {
                        servers.remove(entry.getKey()); // Remove!
                    }
                } else {
                    servers.remove(entry.getKey()); // Remove!
                }
            }
        }
        // Ensure default exists
        if (!servers.containsKey("default")) {
            servers.put("default", BungeeVersionProvider.getLowestSupportedVersion());
        }
        // Put back
        config.put("bungee-servers", servers);
    }

    @Override
    public List<String> getUnsupportedOptions() {
        return UNSUPPORTED;
    }

    @Override
    public boolean isItemCache() {
        return false;
    }

    @Override
    public boolean isNMSPlayerTicking() {
        return false;
    }

    /**
     * What is the interval for checking servers via ping
     * -1 for disabled
     *
     * @return Ping interval in seconds
     */
    public int getBungeePingInterval() {
        return bungeePingInterval;
    }

    /**
     * Should the bungee ping be saved to the config on change.
     *
     * @return True if it should save
     */
    public boolean isBungeePingSave() {
        return bungeePingSave;
    }

    /**
     * Get the listed server protocols in the config.
     * default will be listed as default.
     *
     * @return Map of String, Integer
     */
    public Map<String, Integer> getBungeeServerProtocols() {
        return bungeeServerProtocols;
    }
}
