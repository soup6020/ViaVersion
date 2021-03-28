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

public class StairConnectionHandler extends ConnectionHandler {
    private static final Map<Integer, StairData> stairDataMap = new HashMap<>();
    private static final Map<Short, Integer> connectedBlocks = new HashMap<>();

    static ConnectionData.ConnectorInitAction init() {
        final List<String> baseStairs = new LinkedList<>();
        baseStairs.add("minecraft:oak_stairs");
        baseStairs.add("minecraft:cobblestone_stairs");
        baseStairs.add("minecraft:brick_stairs");
        baseStairs.add("minecraft:stone_brick_stairs");
        baseStairs.add("minecraft:nether_brick_stairs");
        baseStairs.add("minecraft:sandstone_stairs");
        baseStairs.add("minecraft:spruce_stairs");
        baseStairs.add("minecraft:birch_stairs");
        baseStairs.add("minecraft:jungle_stairs");
        baseStairs.add("minecraft:quartz_stairs");
        baseStairs.add("minecraft:acacia_stairs");
        baseStairs.add("minecraft:dark_oak_stairs");
        baseStairs.add("minecraft:red_sandstone_stairs");
        baseStairs.add("minecraft:purpur_stairs");
        baseStairs.add("minecraft:prismarine_stairs");
        baseStairs.add("minecraft:prismarine_brick_stairs");
        baseStairs.add("minecraft:dark_prismarine_stairs");

        final StairConnectionHandler connectionHandler = new StairConnectionHandler();
        return blockData -> {
            int type = baseStairs.indexOf(blockData.getMinecraftKey());
            if (type == -1) return;

            if (blockData.getValue("waterlogged").equals("true")) return;

            byte shape;
            switch (blockData.getValue("shape")) {
                case "straight":
                    shape = 0;
                    break;
                case "inner_left":
                    shape = 1;
                    break;
                case "inner_right":
                    shape = 2;
                    break;
                case "outer_left":
                    shape = 3;
                    break;
                case "outer_right":
                    shape = 4;
                    break;
                default:
                    return;
            }

            StairData stairData = new StairData(
                    blockData.getValue("half").equals("bottom"),
                    shape, (byte) type,
                    BlockFace.valueOf(blockData.getValue("facing").toUpperCase(Locale.ROOT))
            );

            stairDataMap.put(blockData.getSavedBlockStateId(), stairData);
            connectedBlocks.put(getStates(stairData), blockData.getSavedBlockStateId());

            ConnectionData.connectionHandlerMap.put(blockData.getSavedBlockStateId(), connectionHandler);
        };
    }

    private static short getStates(StairData stairData) {
        short s = 0;
        if (stairData.isBottom()) s |= 1;
        s |= stairData.getShape() << 1;
        s |= stairData.getType() << 4;
        s |= stairData.getFacing().ordinal() << 9;
        return s;
    }

    @Override
    public int connect(UserConnection user, Position position, int blockState) {
        StairData stairData = stairDataMap.get(blockState);
        if (stairData == null) return blockState;

        short s = 0;
        if (stairData.isBottom()) s |= 1;
        s |= getShape(user, position, stairData) << 1;
        s |= stairData.getType() << 4;
        s |= stairData.getFacing().ordinal() << 9;

        Integer newBlockState = connectedBlocks.get(s);
        return newBlockState == null ? blockState : newBlockState;
    }

    private int getShape(UserConnection user, Position position, StairData stair) {
        BlockFace facing = stair.getFacing();

        StairData relativeStair = stairDataMap.get(getBlockData(user, position.getRelative(facing)));
        if (relativeStair != null && relativeStair.isBottom() == stair.isBottom()) {
            BlockFace facing2 = relativeStair.getFacing();
            if (facing.getAxis() != facing2.getAxis() && checkOpposite(user, stair, position, facing2.opposite())) {
                return facing2 == rotateAntiClockwise(facing) ? 3 : 4; // outer_left : outer_right
            }
        }

        relativeStair = stairDataMap.get(getBlockData(user, position.getRelative(facing.opposite())));
        if (relativeStair != null && relativeStair.isBottom() == stair.isBottom()) {
            BlockFace facing2 = relativeStair.getFacing();
            if (facing.getAxis() != facing2.getAxis() && checkOpposite(user, stair, position, facing2)) {
                return facing2 == rotateAntiClockwise(facing) ? 1 : 2; // inner_left : inner_right
            }
        }

        return 0; // straight
    }

    private boolean checkOpposite(UserConnection user, StairData stair, Position position, BlockFace face) {
        StairData relativeStair = stairDataMap.get(getBlockData(user, position.getRelative(face)));
        return relativeStair == null || relativeStair.getFacing() != stair.getFacing() || relativeStair.isBottom() != stair.isBottom();
    }

    private BlockFace rotateAntiClockwise(BlockFace face) {
        switch (face) {
            case NORTH:
                return BlockFace.WEST;
            case SOUTH:
                return BlockFace.EAST;
            case EAST:
                return BlockFace.NORTH;
            case WEST:
                return BlockFace.SOUTH;
            default:
                return face;
        }
    }

    private static final class StairData {
        private final boolean bottom;
        private final byte shape, type;
        private final BlockFace facing;

        private StairData(boolean bottom, byte shape, byte type, BlockFace facing) {
            this.bottom = bottom;
            this.shape = shape;
            this.type = type;
            this.facing = facing;
        }

        public boolean isBottom() {
            return bottom;
        }

        public byte getShape() {
            return shape;
        }

        public byte getType() {
            return type;
        }

        public BlockFace getFacing() {
            return facing;
        }
    }
}
