//
// $Id$

package com.threerings.io;

import java.io.IOException;
import java.io.InputStream;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;

/**
 * Reads input from a {@link ByteBuffer}.
 */
public class ByteBufferInputStream extends InputStream
{
    /**
     * Creates a new input stream to read from the specified buffer.
     */
    public ByteBufferInputStream (ByteBuffer buffer)
    {
        _buffer = buffer;
    }

    /**
     * Returns a reference to the underlying buffer.
     */
    public ByteBuffer getBuffer ()
    {
        return _buffer;
    }

    @Override // documentation inherited
    public int read ()
    {
        try {
            return (_buffer.get() & 0xFF);
        } catch (BufferUnderflowException e) {
            return -1;
        }
    }

    @Override // documentation inherited
    public int read (byte[] b, int offset, int length)
        throws IOException
    {
        length = Math.min(length, _buffer.remaining());
        if (length <= 0) {
            return -1;
        }
        _buffer.get(b, offset, length);
        return length;
    }

    @Override // documentation inherited
    public long skip (long n)
        throws IOException
    {
        n = Math.min(n, _buffer.remaining());
        _buffer.position((int)(_buffer.position() + n));
        return n;
    }

    @Override // documentation inherited
    public int available ()
    {
        return _buffer.remaining();
    }

    @Override // documentation inherited
    public boolean markSupported ()
    {
        return true;
    }

    @Override // documentation inherited
    public void mark (int readLimit)
    {
        _buffer.mark();
    }

    @Override // documentation inherited
    public void reset ()
        throws IOException
    {
        try {
            _buffer.reset();
        } catch (InvalidMarkException e) {
            throw new IOException("No mark set.");
        }
    }

    /** The buffer from which we read. */
    protected ByteBuffer _buffer;
}

