//
// $Id$

package com.threerings.export.util;

import java.io.IOException;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.export.BinaryExporter;
import com.threerings.export.BinaryImporter;

/**
 * Wraps an exportable object so that it can be streamed (in an inefficient manner, by creating
 * a new {@link BinaryExporter}).
 */
public class StreamableWrapper extends SimpleStreamableObject
{
    /**
     * Creates a new wrapper for the specified object.
     */
    public StreamableWrapper (Object object)
    {
        _object = object;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public StreamableWrapper ()
    {
    }

    /**
     * Returns a reference to the wrapped object.
     */
    public Object getObject ()
    {
        return _object;
    }

    /**
     * Writes the object to the specified stream.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        BinaryExporter exporter = new BinaryExporter(out);
        try {
            exporter.writeObject(_object);
        } finally {
            exporter.finish();
        }
    }

    /**
     * Reads the object state from the specified stream.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        BinaryImporter importer = new BinaryImporter(in);
        _object = importer.readObject();
    }

    /** The wrapped object. */
    protected Object _object;
}
