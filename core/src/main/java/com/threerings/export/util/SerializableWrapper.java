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

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;

import com.threerings.export.BinaryExporter;
import com.threerings.export.BinaryImporter;

/**
 * Wraps an exportable object so that it can be serialized (in an inefficient manner, by creating
 * a new {@link BinaryExporter}).
 */
public class SerializableWrapper
    implements Externalizable
{
    /**
     * Creates a new wrapper for the specified object.
     */
    public SerializableWrapper (Object object)
    {
        _object = object;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public SerializableWrapper ()
    {
    }

    /**
     * Returns a reference to the wrapped object.
     */
    public Object getObject ()
    {
        return _object;
    }

    // documentation inherited from interface Externalizable
    public void writeExternal (final ObjectOutput out)
        throws IOException
    {
        // gotta wrap the output because BinaryExporter expects an actual stream
        BinaryExporter exporter = new BinaryExporter(new OutputStream() {
            public void write (int b) throws IOException {
                out.write(b);
            }
            public void write (byte[] b, int off, int len) throws IOException {
                out.write(b, off, len);
            }
            public void flush () throws IOException {
                out.flush();
            }
        });
        try {
            exporter.writeObject(_object);
        } finally {
            exporter.finish();
        }
    }

    // documentation inherited from interface Externalizable
    public void readExternal (final ObjectInput in)
        throws IOException, ClassNotFoundException
    {
        BinaryImporter importer = new BinaryImporter(new InputStream() {
            public int read () throws IOException {
                return in.read();
            }
            public int read (byte[] b, int off, int len) throws IOException {
                return in.read(b, off, len);
            }
            public long skip (long n) throws IOException {
                return in.skip(n);
            }
            public int available () throws IOException {
                return in.available();
            }
        });
        _object = importer.readObject();
    }

    /** The wrapped object. */
    protected Object _object;
}
