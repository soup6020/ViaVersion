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
package us.myles.ViaVersion.protocols.protocol1_15to1_14_4.data;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.data.Mappings;

public class MappingData extends us.myles.ViaVersion.api.data.MappingData {

    public MappingData() {
        super("1.14", "1.15", true);
    }

    @Override
    protected Mappings loadFromArray(JsonObject oldMappings, JsonObject newMappings, @Nullable JsonObject diffMappings, String key) {
        if (!key.equals("sounds")) {
            return super.loadFromArray(oldMappings, newMappings, diffMappings, key);
        }

        // Ignore removed sounds
        return new Mappings(oldMappings.getAsJsonArray(key), newMappings.getAsJsonArray(key), false);
    }
}
