//
// $Id$

package com.threerings.export;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Utility methods for combining and re-splitting arbitrary streams of data into a single
 * stream, with length prefixes for each piece.
 *
 * @deprecated Use Streams.
 */
@Deprecated
public class MetaStreams
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
        return Streams.input(source);
    }

    /**
     * Return a new OutputStream that will be appended to 'dest' when it is closed,
     * with a varlong length prefix.
     */
    public static OutputStream output (final OutputStream dest)
    {
        return Streams.output(dest);
    }

    /**
     * Write length as a little-endian "varlong" to the specified stream.
     * Each byte is used to encode 7 bits of data and a continuation bit if more is coming,
     * which will use between 1 and 9 bytes to write out any length between 0 and Long.MAX_VALUE.
     * If you're just writing a few bytes after this, you'll appreciate the 1-byte prefix,
     * but if you're hairballing a few KB or more, you can afford a few extra bytes of prefix.
     */
    public static void writeLength (OutputStream out, long length)
        throws IOException
    {
        Streams.writeVarLong(out, length);
    }

    /**
     * Read a length as a little-endian "varlong" from the specified stream.
     *
     * @return the length read off the stream, or -1 if we're at the end of the stream.
     */
    public static long readLength (InputStream in)
        throws IOException
    {
        return Streams.readVarLong(in);
    }
}
