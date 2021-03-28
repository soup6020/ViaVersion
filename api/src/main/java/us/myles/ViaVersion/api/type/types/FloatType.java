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
package us.myles.ViaVersion.api.type.types;

import io.netty.buffer.ByteBuf;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.TypeConverter;

public class FloatType extends Type<Float> implements TypeConverter<Float> {

    public FloatType() {
        super(Float.class);
    }

    public float readPrimitive(ByteBuf buffer) {
        return buffer.readFloat();
    }

    public void writePrimitive(ByteBuf buffer, float object) {
        buffer.writeFloat(object);
    }

    /**
     * @deprecated use {@link #readPrimitive(ByteBuf)} for manual reading to avoid wrapping
     */
    @Override
    @Deprecated
    public Float read(ByteBuf buffer) {
        return buffer.readFloat();
    }

    /**
     * @deprecated use {@link #writePrimitive(ByteBuf, float)} for manual reading to avoid wrapping
     */
    @Override
    @Deprecated
    public void write(ByteBuf buffer, Float object) {
        buffer.writeFloat(object);
    }

    @Override
    public Float from(Object o) {
        if (o instanceof Number) {
            return ((Number) o).floatValue();
        }
        if (o instanceof Boolean) {
            return ((Boolean) o) ? 1F : 0;
        }
        return (Float) o;
    }
}
