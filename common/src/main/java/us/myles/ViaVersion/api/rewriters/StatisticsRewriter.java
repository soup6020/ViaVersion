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
package us.myles.ViaVersion.api.rewriters;

import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.protocol.ClientboundPacketType;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;

public class StatisticsRewriter {
    private final Protocol protocol;
    private final IdRewriteFunction entityRewriter;
    private final int customStatsCategory = 8; // Make this changeable if it differs in a future version

    public StatisticsRewriter(Protocol protocol, @Nullable IdRewriteFunction entityRewriter) {
        this.protocol = protocol;
        this.entityRewriter = entityRewriter;
    }

    public void register(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    int size = wrapper.passthrough(Type.VAR_INT);
                    int newSize = size;
                    for (int i = 0; i < size; i++) {
                        int categoryId = wrapper.read(Type.VAR_INT);
                        int statisticId = wrapper.read(Type.VAR_INT);
                        int value = wrapper.read(Type.VAR_INT);
                        if (categoryId == customStatsCategory && protocol.getMappingData().getStatisticsMappings() != null) {
                            // Rewrite custom statistics id
                            statisticId = protocol.getMappingData().getStatisticsMappings().getNewId(statisticId);
                            if (statisticId == -1) {
                                // Remove entry
                                newSize--;
                                continue;
                            }
                        } else {
                            // Rewrite the block/item/entity id
                            RegistryType type = getRegistryTypeForStatistic(categoryId);
                            IdRewriteFunction statisticsRewriter;
                            if (type != null && (statisticsRewriter = getRewriter(type)) != null) {
                                statisticId = statisticsRewriter.rewrite(statisticId);
                            }
                        }

                        wrapper.write(Type.VAR_INT, categoryId);
                        wrapper.write(Type.VAR_INT, statisticId);
                        wrapper.write(Type.VAR_INT, value);
                    }

                    if (newSize != size) {
                        wrapper.set(Type.VAR_INT, 0, newSize);
                    }
                });
            }
        });
    }

    @Nullable
    protected IdRewriteFunction getRewriter(RegistryType type) {
        switch (type) {
            case BLOCK:
                return protocol.getMappingData().getBlockMappings() != null ? id -> protocol.getMappingData().getNewBlockId(id) : null;
            case ITEM:
                return protocol.getMappingData().getItemMappings() != null ? id -> protocol.getMappingData().getNewItemId(id) : null;
            case ENTITY:
                return entityRewriter;
        }
        throw new IllegalArgumentException("Unknown registry type in statistics packet: " + type);
    }

    @Nullable
    public RegistryType getRegistryTypeForStatistic(int statisticsId) {
        switch (statisticsId) {
            case 0:
                return RegistryType.BLOCK;
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return RegistryType.ITEM;
            case 6:
            case 7:
                return RegistryType.ENTITY;
            default:
                return null;
        }
    }
}
