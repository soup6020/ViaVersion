/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2016-2021 ViaVersion and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package us.myles.ViaVersion.api.type.types.minecraft;

import io.netty.buffer.ByteBuf;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.type.Type;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMetaListType extends MetaListTypeTemplate {
    protected abstract Type<Metadata> getType();

    @Override
    public List<Metadata> read(final ByteBuf buffer) throws Exception {
        final Type<Metadata> type = this.getType();
        final List<Metadata> list = new ArrayList<>();
        Metadata meta;
        do {
            meta = type.read(buffer);
            if (meta != null) {
                list.add(meta);
            }
        } while (meta != null);
        return list;
    }

    @Override
    public void write(final ByteBuf buffer, final List<Metadata> object) throws Exception {
        final Type<Metadata> type = this.getType();

        for (final Metadata metadata : object) {
            type.write(buffer, metadata);
        }

        this.writeEnd(type, buffer);
    }

    protected abstract void writeEnd(final Type<Metadata> type, final ByteBuf buffer) throws Exception;
}
