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
package us.myles.ViaVersion.protocols.base;

import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.type.Type;

import java.util.UUID;

public class BaseProtocol1_16 extends BaseProtocol1_7 {

    @Override
    protected UUID passthroughLoginUUID(final PacketWrapper wrapper) throws Exception {
        return wrapper.passthrough(Type.UUID_INT_ARRAY);
    }
}
