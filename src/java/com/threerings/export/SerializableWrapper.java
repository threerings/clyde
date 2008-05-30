//
// $Id$

package com.threerings.export;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;

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
