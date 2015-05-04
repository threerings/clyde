//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.export.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.concurrent.Callable;

import com.google.common.base.Preconditions;

/**
 * Creates an OutputStream that isn't actually opened until the first data is written.
 * For XML exports, this allows us to avoid overwriting any existing file when an
 * exception is thrown in the process of building the object model before writing
 * it out to the stream.
 * For writing a servlet response, if there's an error even coming up with the response
 * we might be able to avoid creating the stream.
 */
public class LazyOutputStream extends OutputStream
{
    /**
     * Creates a new lazy stream that will call to create its stream at the right time.
     */
    public LazyOutputStream (Callable<? extends OutputStream> creator)
    {
        _creator = Preconditions.checkNotNull(creator);
    }

    /**
     * Creates a new lazy stream that will write to the specified file.
     */
    public LazyOutputStream (final File file)
    {
        this(new Callable<FileOutputStream>() {
                    public FileOutputStream call ()
                        throws IOException
                    {
                        return new FileOutputStream(file);
                    }
                });
    }

    @Override
    public void write (int b)
        throws IOException
    {
        ensureInitialized();
        _out.write(b);
    }

    @Override
    public void write (byte[] b)
        throws IOException
    {
        ensureInitialized();
        _out.write(b);
    }

    @Override
    public void write (byte[] b, int off, int len)
        throws IOException
    {
        ensureInitialized();
        _out.write(b, off, len);
    }

    @Override
    public void flush ()
        throws IOException
    {
        if (_out != null) {
            _out.flush();
        }
    }

    @Override
    public void close ()
        throws IOException
    {
        if (_out != null) {
            _out.close();
        }
    }

    /**
     * Creates the underlying output stream if necessary.
     */
    protected void ensureInitialized ()
        throws IOException
    {
        if (_out == null) {
            try {
                _out = _creator.call();
            } catch (IOException ioe) {
                throw ioe;
            } catch (Exception e) {
                throw new IOException("Exception lazy-creating OutputStream: " + e.getMessage(), e);
            } finally {
                _creator = null;
            }
        }
    }

    /** Called to create the output stream. */
    protected Callable<? extends OutputStream> _creator;

    /** The underlying output stream, if created. */
    protected OutputStream _out;
}
