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

import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.protocol.ClientboundPacketType;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;

import java.util.HashMap;
import java.util.Map;

public abstract class RecipeRewriter {

    protected final Protocol protocol;
    protected final ItemRewriter.RewriteFunction rewriter;
    protected final Map<String, RecipeConsumer> recipeHandlers = new HashMap<>();

    protected RecipeRewriter(Protocol protocol, ItemRewriter.RewriteFunction rewriter) {
        this.protocol = protocol;
        this.rewriter = rewriter;
    }

    public void handle(PacketWrapper wrapper, String type) throws Exception {
        RecipeConsumer handler = recipeHandlers.get(type);
        if (handler != null) {
            handler.accept(wrapper);
        }
    }

    public void registerDefaultHandler(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    int size = wrapper.passthrough(Type.VAR_INT);
                    for (int i = 0; i < size; i++) {
                        String type = wrapper.passthrough(Type.STRING).replace("minecraft:", "");
                        String id = wrapper.passthrough(Type.STRING); // Recipe Identifier
                        handle(wrapper, type);
                    }
                });
            }
        });
    }

    @FunctionalInterface
    public interface RecipeConsumer {

        void accept(PacketWrapper wrapper) throws Exception;
    }
}
