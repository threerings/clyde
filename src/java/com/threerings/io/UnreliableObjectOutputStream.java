//
// $Id$

package com.threerings.io;

import java.io.IOException;
import java.io.OutputStream;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.threerings.ClydeLog.*;

/**
 * Extends {@link ObjectOutputStream} for use in unreliable channels, where we must transmit class
 * metadata with every object until we are explicitly notified that the receiver has cached the
 * metadata.
 */
public class UnreliableObjectOutputStream extends ObjectOutputStream
{
    /**
     * Constructs an object output stream which will write its data to the supplied target stream.
     */
    public UnreliableObjectOutputStream (OutputStream target)
    {
        super(target);
    }

    /**
     * Sets the reference to the set that will hold the classes for which metadata has been
     * written.
     */
    public void setMetadataClasses (Set<Class> metadataClasses)
    {
        _metadataClasses = metadataClasses;
    }

    /**
     * Returns a reference to the set of classes for which metadata has been written.
     */
    public Set<Class> getMetadataClasses ()
    {
        return _metadataClasses;
    }

    /**
     * Notes that the receiver has cached the metadata for a group of classes, and thus that from
     * now on, only the class code need be sent.
     */
    public void noteMetadataCached (Collection<Class> sclasses)
    {
        // sanity check
        if (_classmap == null) {
            throw new RuntimeException("Missing class map");
        }

        // make each class's code positive to signify that we no longer need to send metadata
        for (Class sclass : sclasses) {
            ClassMapping cmap = _classmap.get(sclass);
            if (cmap == null) {
                throw new RuntimeException("No class mapping for " + sclass.getName());
            }
            cmap.code = (short)Math.abs(cmap.code);
        }
    }

    @Override // documentation inherited
    public void writeObject (Object object)
        throws IOException
    {
        // if the object to be written is null, simply write a zero
        if (object == null) {
            writeShort(0);
            return;
        }

        // create our classmap if necessary
        if (_classmap == null) {
            _classmap = new HashMap<Class,ClassMapping>();
        }

        // otherwise, look up the class mapping record
        Class sclass = Streamer.getStreamerClass(object);
        ClassMapping cmap = _classmap.get(sclass);

        // create a class mapping for this class if we've not got one
        if (cmap == null) {
            // create a streamer instance and assign a code to this class
            Streamer streamer = Streamer.getStreamer(sclass);
            // we specifically do not inline the getStreamer() call into the ClassMapping
            // constructor because we want to be sure not to call _nextCode++ if getStreamer()
            // throws an exception
            if (ObjectInputStream.STREAM_DEBUG) {
                log.info(hashCode() + ": Creating class mapping [code=" + _nextCode +
                         ", class=" + sclass.getName() + "].");
            }
            // the negative class code indicates that we must rewrite the metadata for the first
            // instance in each go-round; when we are notified that the other side has cached the
            // mapping, we can simply write the (positive) code
            short code = _nextCode++;
            cmap = new ClassMapping((short)(-code), sclass, streamer);
            _classmap.put(sclass, cmap);

            // make sure we didn't blow past our maximum class count
            if (_nextCode <= 0) {
                throw new RuntimeException("Too many unique classes written to ObjectOutputStream");
            }
            writeClassMetadata(cmap);

        } else if (cmap.code < 0) {
            // rewrite the metadata if we haven't written it in this go-round
            if (_metadataClasses.contains(sclass)) {
                writeShort(-cmap.code);
            } else {
                writeClassMetadata(cmap);
            }

        } else {
            writeShort(cmap.code);
        }

        writeBareObject(object, cmap.streamer, true);
    }

    /**
     * Writes out the metadata for the specified class mapping.
     */
    protected void writeClassMetadata (ClassMapping cmap)
        throws IOException
    {
        // note that we're writing the metadata
        _metadataClasses.add(cmap.sclass);

        writeShort(cmap.code);
        String cname = cmap.sclass.getName();
        if (_translations != null) {
            String tname = _translations.get(cname);
            if (tname != null) {
                cname = tname;
            }
        }
        writeUTF(cname);
    }

    /** The set of classes for which we have written metadata. */
    protected Set<Class> _metadataClasses = new HashSet<Class>();
}
