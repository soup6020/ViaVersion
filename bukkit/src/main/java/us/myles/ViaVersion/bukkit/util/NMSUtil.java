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
package us.myles.ViaVersion.bukkit.util;

import org.bukkit.Bukkit;

public class NMSUtil {
    private static final String BASE = Bukkit.getServer().getClass().getPackage().getName();
    private static final String NMS = BASE.replace("org.bukkit.craftbukkit", "net.minecraft.server");
    private static final boolean DEBUG_PROPERTY = loadDebugProperty();

    private static boolean loadDebugProperty() {
        try {
            Class<?> serverClass = nms("MinecraftServer");
            Object server = serverClass.getDeclaredMethod("getServer").invoke(null);
            return (boolean) serverClass.getMethod("isDebugging").invoke(server);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    public static Class<?> nms(String className) throws ClassNotFoundException {
        return Class.forName(NMS + "." + className);
    }

    public static Class<?> obc(String className) throws ClassNotFoundException {
        return Class.forName(BASE + "." + className);
    }

    public static String getVersion() {
        return BASE.substring(BASE.lastIndexOf('.') + 1);
    }

    /**
     * @return true if debug=true is set in the server.properties (added by CB)
     */
    public static boolean isDebugPropertySet() {
        return DEBUG_PROPERTY;
    }
}
