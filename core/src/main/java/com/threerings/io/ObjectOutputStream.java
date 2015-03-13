//
// $Id$
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/narya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.io;

import java.util.Map;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.google.common.collect.Maps;

import static com.threerings.NaryaLog.log;

/**
 * Used to write {@link Streamable} objects to an {@link OutputStream}.  Other common object types
 * are supported as well: <code>Boolean, Byte, Character, Short, Integer, Long, Float, Double,
 * boolean[], byte[], char[], short[], int[], long[], float[], double[], Object[]</code>.
 *
 * @see Streamable
 */
public class ObjectOutputStream extends DataOutputStream
{
    /**
     * Constructs an object output stream which will write its data to the supplied target stream.
     */
    public ObjectOutputStream (OutputStream target)
    {
        super(target);
    }

    /**
     * Configures this object output stream with a mapping from a classname to a streamed name.
     */
    public void addTranslation (String className, String streamedName)
    {
        if (_translations == null) {
            _translations = Maps.newHashMap();
        }
        _translations.put(className, streamedName);
    }

    /**
     * Writes a {@link Streamable} instance or one of the support object types to the output
     * stream.
     */
    public void writeObject (Object object)
        throws IOException
    {
        // if the object to be written is null, simply write a zero
        if (object == null) {
            writeShort(0);
            return;
        }

        // otherwise, write the class mapping, then the bare object
        Class<?> sclass = Streamer.getStreamerClass(object);
        ClassMapping cmap = writeClassMapping(sclass);
        writeBareObject(object, cmap.streamer, true);
    }

    /**
     * Writes a pooled string value to the output stream.
     */
    public void writeIntern (String value)
        throws IOException
    {
        // if the value to be written is null, simply write a zero
        if (value == null) {
            writeShort(0);
            return;
        }

        // create our intern map if necessary
        if (_internmap == null) {
            _internmap = Maps.newHashMap();
        }

        // look up the intern mapping record
        Short code = _internmap.get(value);

        // create a mapping for the value if we've not got one
        if (code == null) {
            if (ObjectInputStream.STREAM_DEBUG) {
                log.info(hashCode() + ": Creating intern mapping", "code", _nextInternCode,
                         "value", value);
            }
            code = createInternMapping(_nextInternCode++);
            _internmap.put(value.intern(), code);

            // make sure we didn't blow past our maximum intern count
            if (_nextInternCode <= 0) {
                throw new RuntimeException("Too many unique interns written to ObjectOutputStream");
            }
            writeNewInternMapping(code, value);

        } else {
            writeExistingInternMapping(code, value);
        }
    }

    /**
     * Creates and returns a new intern mapping.
     */
    protected Short createInternMapping (short code)
    {
        return code;
    }

    /**
     * Writes a new intern mapping to the stream.
     */
    protected void writeNewInternMapping (short code, String value)
        throws IOException
    {
        // writing a negative code indicates that the value will follow
        writeInternMapping(-code, value);
    }

    /**
     * Writes an existing intern mapping to the stream.
     */
    protected void writeExistingInternMapping (short code, String value)
        throws IOException
    {
        writeShort(code);
    }

    /**
     * Writes out the mapping for an intern.
     */
    protected void writeInternMapping (int code, String value)
        throws IOException
    {
        writeShort(code);
        writeUTF(value);
    }

    /**
     * Retrieves or creates the class mapping for the supplied class, writes it out to the stream,
     * and returns a reference to it.
     */
    protected ClassMapping writeClassMapping (Class<?> sclass)
        throws IOException
    {
        // create our classmap if necessary
        if (_classmap == null) {
            _classmap = Maps.newHashMap();
        }

        // look up the class mapping record
        ClassMapping cmap = _classmap.get(sclass);

        // create a class mapping for this class if we've not got one
        if (cmap == null) {
            // see if we just want to use an existing class mapping
            Class<?> collClass = Streamer.getCollectionClass(sclass);
            if (collClass != null && !collClass.equals(sclass)) {
                cmap = writeClassMapping(collClass);
                _classmap.put(sclass, cmap);
                return cmap;
            }

            // create a streamer instance and assign a code to this class
            Streamer streamer = Streamer.getStreamer(sclass);
            // we specifically do not inline the getStreamer() call into the ClassMapping
            // constructor because we want to be sure not to call _nextClassCode++ if getStreamer()
            // throws an exception
            if (ObjectInputStream.STREAM_DEBUG) {
                log.info(hashCode() + ": Creating class mapping", "code", _nextClassCode,
                         "class", sclass.getName());
            }
            cmap = createClassMapping(_nextClassCode++, sclass, streamer);
            _classmap.put(sclass, cmap);

            // make sure we didn't blow past our maximum class count
            if (_nextClassCode <= 0) {
                throw new RuntimeException("Too many unique classes written to ObjectOutputStream");
            }
            writeNewClassMapping(cmap);

        } else {
            writeExistingClassMapping(cmap);
        }
        return cmap;
    }

    /**
     * Creates and returns a new class mapping.
     */
    protected ClassMapping createClassMapping (short code, Class<?> sclass, Streamer streamer)
    {
        return new ClassMapping(code, sclass, streamer);
    }

    /**
     * Writes a new class mapping to the stream.
     */
    protected void writeNewClassMapping (ClassMapping cmap)
        throws IOException
    {
        // writing a negative class code indicates that the class name will follow
        writeClassMapping(-cmap.code, cmap.sclass);
    }

    /**
     * Writes an existing class mapping to the stream.
     */
    protected void writeExistingClassMapping (ClassMapping cmap)
        throws IOException
    {
        writeShort(cmap.code);
    }

    /**
     * Writes out the mapping for a class.
     */
    protected void writeClassMapping (int code, Class<?> sclass)
        throws IOException
    {
        writeShort(code);
        String cname = sclass.getName();
        if (_translations != null) {
            String tname = _translations.get(cname);
            if (tname != null) {
                cname = tname;
            }
        }
        writeUTF(cname);
    }

    /**
     * Writes a {@link Streamable} instance or one of the support object types <em>without
     * associated class metadata</em> to the output stream. The caller is responsible for knowing
     * the exact class of the written object, creating an instance of such and calling {@link
     * ObjectInputStream#readBareObject(Object)} to read its data from the stream.
     *
     * @param object the object to be written. It cannot be <code>null</code>.
     */
    public void writeBareObject (Object object)
        throws IOException
    {
        writeBareObject(object, Streamer.getStreamer(Streamer.getStreamerClass(object)), true);
    }

    /**
     * Write a {@link Streamable} instance without associated class metadata.
     */
    protected void writeBareObject (Object obj, Streamer streamer, boolean useWriter)
        throws IOException
    {
        _current = obj;
        _streamer = streamer;
        try {
            _streamer.writeObject(obj, this, useWriter);
        } finally {
            _current = null;
            _streamer = null;
        }
    }

    /**
     * Uses the default streamable mechanism to write the contents of the object currently being
     * streamed. This can only be called from within a <code>writeObject</code> implementation in a
     * {@link Streamable} object.
     */
    public void defaultWriteObject ()
        throws IOException
    {
        // sanity check
        if (_current == null) {
            throw new RuntimeException("defaultWriteObject() called illegally.");
        }

//         log.info("Writing default", "cmap", _streamer, "current", _current);

        // write the instance data
        _streamer.writeObject(_current, this, false);
    }

    /**
     * Write a string encoded as real UTF-8 (rather than the modified format handled by
     * {link #writeUTF}).
     */
    public void writeUnmodifiedUTF (String str)
        throws IOException
    {
        // byte[] bytes = str.getBytes(Charsets.UTF_8); // TODO Java 6 (Charsets is from guava)
        byte[] bytes = str.getBytes("UTF-8");
        writeShort(bytes.length);
        write(bytes);
    }

    /** Used to map classes to numeric codes and the {@link Streamer} instance used to write
     * them. */
    protected Map<Class<?>, ClassMapping> _classmap;

    /** Used to map pooled strings to numeric codes. */
    protected Map<String, Short> _internmap;

    /** A counter used to assign codes to streamed classes. */
    protected short _nextClassCode = 1;

    /** A counter used to assign codes to pooled strings. */
    protected short _nextInternCode = 1;

    /** The object currently being written to the stream. */
    protected Object _current;

    /** The streamer being used currently. */
    protected Streamer _streamer;

    /** An optional set of class name translations to use when serializing objects. */
    protected Map<String, String> _translations;
}
