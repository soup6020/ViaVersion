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
package us.myles.ViaVersion.protocols.protocol1_13to1_12_2.blockconnections;

import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.minecraft.BlockFace;
import us.myles.ViaVersion.api.minecraft.Position;

import java.util.HashSet;
import java.util.Set;

class VineConnectionHandler extends ConnectionHandler {
    private static final Set<Integer> vines = new HashSet<>();

    static ConnectionData.ConnectorInitAction init() {
        final VineConnectionHandler connectionHandler = new VineConnectionHandler();
        return blockData -> {
            if (!blockData.getMinecraftKey().equals("minecraft:vine")) return;

            vines.add(blockData.getSavedBlockStateId());
            ConnectionData.connectionHandlerMap.put(blockData.getSavedBlockStateId(), connectionHandler);
        };
    }

    @Override
    public int connect(UserConnection user, Position position, int blockState) {
        if (isAttachedToBlock(user, position)) return blockState;

        Position upperPos = position.getRelative(BlockFace.TOP);
        int upperBlock = getBlockData(user, upperPos);
        if (vines.contains(upperBlock) && isAttachedToBlock(user, upperPos)) return blockState;

        // Map to air if not attached to block, and upper block is also not a vine attached to a block
        return 0;
    }

    private boolean isAttachedToBlock(UserConnection user, Position position) {
        return isAttachedToBlock(user, position, BlockFace.EAST)
                || isAttachedToBlock(user, position, BlockFace.WEST)
                || isAttachedToBlock(user, position, BlockFace.NORTH)
                || isAttachedToBlock(user, position, BlockFace.SOUTH);
    }

    private boolean isAttachedToBlock(UserConnection user, Position position, BlockFace blockFace) {
        return ConnectionData.occludingStates.contains(getBlockData(user, position.getRelative(blockFace)));
    }
}
