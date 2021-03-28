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
package us.myles.ViaVersion.bungee.providers;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.bungee.storage.BungeeStorage;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.providers.EntityIdProvider;

import java.lang.reflect.Method;

public class BungeeEntityIdProvider extends EntityIdProvider {
    private static Method getClientEntityId;

    static {
        try {
            getClientEntityId = Class.forName("net.md_5.bungee.UserConnection").getDeclaredMethod("getClientEntityId");
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getEntityId(UserConnection user) throws Exception {
        BungeeStorage storage = user.get(BungeeStorage.class);
        ProxiedPlayer player = storage.getPlayer();

        return (int) getClientEntityId.invoke(player);
    }
}
