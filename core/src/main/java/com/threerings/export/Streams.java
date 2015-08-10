//
// $Id$

package com.threerings.export;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.OutputStream;
import java.io.StreamCorruptedException;

import java.util.Iterator;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;

import static com.threerings.export.Log.log;

/**
 * Utility methods for combining and re-splitting arbitrary streams of data into a single
 * stream, with length prefixes for each piece.
 */
public class Streams
{
    /**
     * Return the next InputStream, or null if we've reached the end of the stream.
     * This adds a level of safety in that the returned Stream does not need to actually be
     * read from at all in order to get the next stream from the source. This is accomplished
     * by pre-reading all the bytes and returning a wrapper around the byte[].
     */
    public static InputStream input (InputStream source)
        throws IOException
    {
        long length = readVarLong(source);
        if (length == -1) {
            return null;

        } else if (length > Integer.MAX_VALUE) {
            throw new IOException("Next stream is too long! [length=" + length + "]");
        }
        byte[] bytes = new byte[(int)length];
        ByteStreams.readFully(source, bytes); // may throw EOF, IOE
        return new ByteArrayInputStream(bytes);
    }

    /**
     * Return a new OutputStream that will be appended to 'dest' when it is closed,
     * with a varlong length prefix.
     */
    public static OutputStream output (final OutputStream dest)
    {
        return new ByteArrayOutputStream() {
            @Override
            public void close ()
                throws IOException
            {
                writeVarLong(dest, size());
                writeTo(dest);
                dest.flush();
                reset();
            }
        };
    }

    /**
     * Write a positive long to the specified stream, encoded little-endian, variable length.
     *
     * Each byte is used to encode 7 bits of data and a continuation bit if more is coming,
     * which will use between 1 and 9 bytes to write out any value between 0 and Long.MAX_VALUE.
     *
     * @throws IllegalArgumentException if value is negative.
     */
    public static void writeVarLong (OutputStream out, long value)
        throws IOException, IllegalArgumentException
    {
        Preconditions.checkArgument(value >= 0);
        while (true) {
            int bite = (int)(value & 0x7f);
            value >>= 7;
            if (value == 0) {
                out.write(bite); // write the byte and exit
                return;
            }
            out.write(bite | 0x80); // write the byte with the continuation flag
        }
    }

    /**
     * Read a positive long (encoded little-endian, variable length) from the specified stream.
     *
     * @return the value read off the stream, or -1 if we're at the end of the stream.
     */
    public static long readVarLong (InputStream in)
        throws IOException
    {
        long ret = 0;
        for (int shift = 0; shift < 63; shift += 7) {
            int bite = in.read();
            if (bite == -1) {
                if (shift == 0) {
                    return -1; // expected: we're at the end of the stream
                }
                break; // throw StreamCorrupted
            }
            ret |= ((long)(bite & 0x7f)) << shift;
            if ((bite & 0x80) == 0) {
                if (shift > 0 && ((bite & 0x7f) == 0)) {
                    break; // detect invalid extra 0-padding; throw StreamCorrupted
                }
                return ret;
            }
        }
        throw new StreamCorruptedException();
    }

    /**
     * Write a positive int to the specified stream, encoded little-endian, variable length.
     *
     * This presently merely calls writeVarLong but perhaps an optimized 32-bit impl hahaah
     * that was pretty good for a bit there, I had you going.
     *
     * @throws IllegalArgumentException if value is negative.
     */
    public static void writeVarInt (OutputStream out, int value)
        throws IOException, IllegalArgumentException
    {
        writeVarLong(out, value);
    }

    /**
     * Read the next varlong off the stream, but freak out if it's bigger than Integer.MAX_VALUE.
     *
     * @throws IllegalArgumentException if the read value is larger than Integer.MAX_VALUE.
     */
    public static int readVarInt (InputStream in)
        throws IOException, IllegalArgumentException
    {
        return Ints.checkedCast(readVarLong(in));
    }

    /**
     * Write a String to the specified stream, UTF-8 encoded and prefixed by a varint length.
     */
    public static void writeVarString (OutputStream out, String s)
        throws IOException
    {
        byte[] bytes = s.getBytes(Charsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    /**
     * Read a String from the specified stream, UTF-8 encoded and prefixed by a varint length.
     */
    public static String readVarString (InputStream in)
        throws IOException
    {
        int length = readVarInt(in);
        byte[] buf = new byte[length];
        ByteStreams.readFully(in, buf);
        return new String(buf, Charsets.UTF_8);
    }
}
