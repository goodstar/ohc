/*
 *      Copyright (C) 2014 Robert Stupp, Koeln, Germany, robert-stupp.de
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.caffinitas.ohc.segment;

import java.io.EOFException;
import java.io.IOException;

import org.caffinitas.ohc.api.AbstractDataOutput;
import org.caffinitas.ohc.internal.Util;

final class HashEntryOutput extends AbstractDataOutput
{
    private long blkAdr;
    private long blkOff;
    private final long blkEnd;

    HashEntryOutput(long hashEntryAdr, long keyLen, long valueLen)
    {
        if (hashEntryAdr == 0L)
            throw new NullPointerException();
        if (keyLen < 0L || valueLen < 0L)
            throw new IllegalArgumentException();
        if (valueLen > Integer.MAX_VALUE)
            throw new IllegalStateException("integer overflow");

        this.blkAdr = hashEntryAdr;
        this.blkOff = Constants.ENTRY_OFF_DATA + Util.roundUpTo8(keyLen);
        this.blkEnd = this.blkOff + valueLen;
    }

    private void assertAvail(int req) throws IOException
    {
        if (avail() < req || req < 0)
            throw new EOFException();
    }

    private long avail()
    {
        return blkEnd - blkOff;
    }

    public void write(byte[] b, int off, int len) throws IOException
    {
        if (b == null)
            throw new NullPointerException();
        if (off < 0 || off + len > b.length || len < 0)
            throw new ArrayIndexOutOfBoundsException();

        assertAvail(len);

        Uns.copyMemory(b, off, blkAdr, blkOff, len);
        blkOff += len;
    }

    public void write(int b) throws IOException
    {
        assertAvail(1);

        Uns.putByte(blkAdr, blkOff++, (byte) b);
    }
}
