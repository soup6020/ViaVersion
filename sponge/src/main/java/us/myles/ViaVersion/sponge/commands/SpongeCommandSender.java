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
package us.myles.ViaVersion.sponge.commands;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.Identifiable;
import us.myles.ViaVersion.SpongePlugin;
import us.myles.ViaVersion.api.command.ViaCommandSender;

import java.util.UUID;

public class SpongeCommandSender implements ViaCommandSender {
    private final CommandSource source;

    public SpongeCommandSender(CommandSource source) {
        this.source = source;
    }

    @Override
    public boolean hasPermission(String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public void sendMessage(String msg) {
        String serialized = SpongePlugin.COMPONENT_SERIALIZER.serialize(SpongePlugin.COMPONENT_SERIALIZER.deserialize(msg));
        source.sendMessage(TextSerializers.JSON.deserialize(serialized)); // Hacky way to fix links
    }

    @Override
    public UUID getUUID() {
        if (source instanceof Identifiable) {
            return ((Identifiable) source).getUniqueId();
        } else {
            return UUID.fromString(getName());
        }

    }

    @Override
    public String getName() {
        return source.getName();
    }
}
