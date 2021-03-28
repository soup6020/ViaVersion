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

import java.util.*;

public abstract class AbstractStempConnectionHandler extends ConnectionHandler {
    private static final BlockFace[] BLOCK_FACES = {BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST};

    private final int baseStateId;
    private final Set<Integer> blockId = new HashSet<>();

    private final Map<BlockFace, Integer> stemps = new HashMap<>();

    protected AbstractStempConnectionHandler(String baseStateId) {
        this.baseStateId = ConnectionData.getId(baseStateId);
    }

    public ConnectionData.ConnectorInitAction getInitAction(final String blockId, final String toKey) {
        final AbstractStempConnectionHandler handler = this;
        return blockData -> {
            if (blockData.getSavedBlockStateId() == baseStateId || blockId.equals(blockData.getMinecraftKey())) {
                if (blockData.getSavedBlockStateId() != baseStateId) {
                    handler.blockId.add(blockData.getSavedBlockStateId());
                }
                ConnectionData.connectionHandlerMap.put(blockData.getSavedBlockStateId(), handler);
            }
            if (blockData.getMinecraftKey().equals(toKey)) {
                String facing = blockData.getValue("facing").toUpperCase(Locale.ROOT);
                stemps.put(BlockFace.valueOf(facing), blockData.getSavedBlockStateId());
            }
        };
    }

    @Override
    public int connect(UserConnection user, Position position, int blockState) {
        if (blockState != baseStateId) {
            return blockState;
        }
        for (BlockFace blockFace : BLOCK_FACES) {
            if (blockId.contains(getBlockData(user, position.getRelative(blockFace)))) {
                return stemps.get(blockFace);
            }
        }
        return baseStateId;
    }
}
