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
package us.myles.ViaVersion.bukkit.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.bukkit.util.NMSUtil;
import us.myles.ViaVersion.exception.CancelCodecException;
import us.myles.ViaVersion.exception.CancelDecoderException;
import us.myles.ViaVersion.exception.InformativeException;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.util.PipelineUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class BukkitDecodeHandler extends ByteToMessageDecoder {
    private final ByteToMessageDecoder minecraftDecoder;
    private final UserConnection info;

    public BukkitDecodeHandler(UserConnection info, ByteToMessageDecoder minecraftDecoder) {
        this.info = info;
        this.minecraftDecoder = minecraftDecoder;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf bytebuf, List<Object> list) throws Exception {
        if (!info.checkIncomingPacket()) {
            bytebuf.clear(); // Don't accumulate
            throw CancelDecoderException.generate(null);
        }

        ByteBuf transformedBuf = null;
        try {
            if (info.shouldTransformPacket()) {
                transformedBuf = ctx.alloc().buffer().writeBytes(bytebuf);
                info.transformIncoming(transformedBuf, CancelDecoderException::generate);
            }

            try {
                list.addAll(PipelineUtil.callDecode(this.minecraftDecoder, ctx, transformedBuf == null ? bytebuf : transformedBuf));
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof Exception) {
                    throw (Exception) e.getCause();
                } else if (e.getCause() instanceof Error) {
                    throw (Error) e.getCause();
                }
            }
        } finally {
            if (transformedBuf != null) {
                transformedBuf.release();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (PipelineUtil.containsCause(cause, CancelCodecException.class)) return; // ProtocolLib compat

        super.exceptionCaught(ctx, cause);
        if (!NMSUtil.isDebugPropertySet() && PipelineUtil.containsCause(cause, InformativeException.class)
                && (info.getProtocolInfo().getState() != State.HANDSHAKE || Via.getManager().isDebug())) {
            cause.printStackTrace(); // Print if CB doesn't already do it
        }
    }
}
