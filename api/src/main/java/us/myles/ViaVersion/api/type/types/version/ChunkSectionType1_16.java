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
package us.myles.ViaVersion.api.type.types.version;

import io.netty.buffer.ByteBuf;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.util.CompactArrayUtil;

public class ChunkSectionType1_16 extends Type<ChunkSection> {
    private static final int GLOBAL_PALETTE = 15;

    public ChunkSectionType1_16() {
        super("Chunk Section Type", ChunkSection.class);
    }

    @Override
    public ChunkSection read(ByteBuf buffer) throws Exception {
        // Reaad bits per block
        int bitsPerBlock = buffer.readUnsignedByte();
        int originalBitsPerBlock = bitsPerBlock;

        if (bitsPerBlock == 0 || bitsPerBlock > 8) {
            bitsPerBlock = GLOBAL_PALETTE;
        }

        // Read palette
        ChunkSection chunkSection;
        if (bitsPerBlock != GLOBAL_PALETTE) {
            int paletteLength = Type.VAR_INT.readPrimitive(buffer);
            chunkSection = new ChunkSection(paletteLength);
            for (int i = 0; i < paletteLength; i++) {
                chunkSection.addPaletteEntry(Type.VAR_INT.readPrimitive(buffer));
            }
        } else {
            chunkSection = new ChunkSection();
        }

        // Read blocks
        long[] blockData = new long[Type.VAR_INT.readPrimitive(buffer)];
        if (blockData.length > 0) {
            char valuesPerLong = (char) (64 / bitsPerBlock);
            int expectedLength = (ChunkSection.SIZE + valuesPerLong - 1) / valuesPerLong;
            if (blockData.length != expectedLength) {
                throw new IllegalStateException("Block data length (" + blockData.length + ") does not match expected length (" + expectedLength + ")! bitsPerBlock=" + bitsPerBlock + ", originalBitsPerBlock=" + originalBitsPerBlock);
            }

            for (int i = 0; i < blockData.length; i++) {
                blockData[i] = buffer.readLong();
            }
            CompactArrayUtil.iterateCompactArrayWithPadding(bitsPerBlock, ChunkSection.SIZE, blockData,
                    bitsPerBlock == GLOBAL_PALETTE ? chunkSection::setFlatBlock : chunkSection::setPaletteIndex);
        }

        return chunkSection;
    }

    @Override
    public void write(ByteBuf buffer, ChunkSection chunkSection) throws Exception {
        int bitsPerBlock = 4;
        while (chunkSection.getPaletteSize() > 1 << bitsPerBlock) {
            bitsPerBlock += 1;
        }

        if (bitsPerBlock > 8) {
            bitsPerBlock = GLOBAL_PALETTE;
        }

        buffer.writeByte(bitsPerBlock);

        // Write pallet (or not)
        if (bitsPerBlock != GLOBAL_PALETTE) {
            Type.VAR_INT.writePrimitive(buffer, chunkSection.getPaletteSize());
            for (int i = 0; i < chunkSection.getPaletteSize(); i++) {
                Type.VAR_INT.writePrimitive(buffer, chunkSection.getPaletteEntry(i));
            }
        }

        long[] data = CompactArrayUtil.createCompactArrayWithPadding(bitsPerBlock, ChunkSection.SIZE,
                bitsPerBlock == GLOBAL_PALETTE ? chunkSection::getFlatBlock : chunkSection::getPaletteIndex);
        Type.VAR_INT.writePrimitive(buffer, data.length);
        for (long l : data) {
            buffer.writeLong(l);
        }
    }
}
