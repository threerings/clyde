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

/**
 * Creates a {@link FileOutputStream} lazily, when data is actually written to the stream.  For XML
 * exports, this allows us to avoid overwriting any existing file when an exception is thrown in
 * the process of building the object model before writing it out to the stream.
 */
public class LazyFileOutputStream extends OutputStream
{
    /**
     * Creates a new lazy stream to write to the specified file.
     */
    public LazyFileOutputStream (File file)
    {
        _file = file;
    }

    /**
     * Creates a new lazy stream to write to the named file.
     */
    public LazyFileOutputStream (String file)
    {
        _file = new File(file);
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
            _out = new FileOutputStream(_file);
        }
    }

    /** The file to which we will write. */
    protected File _file;

    /** The underlying file output stream, if created. */
    protected FileOutputStream _out;
}
