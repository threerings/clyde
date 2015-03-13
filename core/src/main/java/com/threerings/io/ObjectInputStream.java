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

import java.util.List;
import java.util.Map;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.util.StringUtil;

import static com.threerings.NaryaLog.log;

/**
 * Used to read {@link Streamable} objects from an {@link InputStream}.  Other common object types
 * are supported as well (@see {@link ObjectOutputStream}).
 *
 * @see Streamable
 */
public class ObjectInputStream extends DataInputStream
{
    /**
     * Constructs an object input stream which will read its data from the supplied source stream.
     */
    public ObjectInputStream (InputStream source)
    {
        super(source);
    }

    /**
     * Customizes the class loader used to instantiate objects read from the input stream.
     */
    public void setClassLoader (ClassLoader loader)
    {
        _loader = loader;
    }

    /**
     * Configures this object input stream with a mapping from an old class name to a new
     * one. Serialized instances of the old class name will use the new class name when
     * unserializing.
     */
    public void addTranslation (String oldname, String newname)
    {
        if (_translations == null) {
            _translations = Maps.newHashMap();
        }
        _translations.put(oldname, newname);
    }

    /**
     * Reads a {@link Streamable} instance or one of the supported object types from the input
     * stream.
     */
    public Object readObject ()
        throws IOException, ClassNotFoundException
    {
        try {
            // read the class mapping
            ClassMapping cmap = readClassMapping();
            if (cmap == null) {
                if (STREAM_DEBUG) {
                    log.info(hashCode() + ": Read null.");
                }
                return null;
            }

            if (STREAM_DEBUG) {
                log.info(hashCode() + ": Reading with " + cmap.streamer + ".");
            }

            // create an instance of the appropriate object
            Object target = cmap.streamer.createObject(this);
            readBareObject(target, cmap.streamer, true);
            return target;

        } catch (OutOfMemoryError oome) {
            throw (IOException)new IOException("Malformed object data").initCause(oome);
        }
    }

    /**
     * Reads a pooled string value from the input stream.
     */
    public String readIntern ()
        throws IOException
    {
        // create our intern map if necessary
        if (_internmap == null) {
            _internmap = Lists.newArrayList();
            // insert a zeroth element
            _internmap.add(null);
        }

        // read in the intern code for this instance
        short code = readShort();

        // a zero code indicates a null value
        if (code == 0) {
            return null;

        // if the code is negative, that means that we've never seen if before and value follows
        } else if (code < 0) {
            // first swap the code into positive-land
            code *= -1;

            // read in the value
            String value = readUTF().intern();

            // create the mapping and return the value
            mapIntern(code, value);
            return value;

        } else {
            String value = (code < _internmap.size()) ? _internmap.get(code) : null;

            // sanity check
            if (value == null) {
                // this will help with debugging
                log.warning("Internal stream error, no intern value", "code", code,
                            "ois", this, new Exception());
                log.warning("ObjectInputStream mappings", "map", _internmap);
                String errmsg = "Read intern code for which we have no registered value " +
                    "metadata [code=" + code + "]";
                throw new RuntimeException(errmsg);
            }
            return value;
        }
    }

    /**
     * Adds the intern mapping for the specified code and value.
     */
    protected void mapIntern (short code, String value)
        throws IOException
    {
        _internmap.add(code, value);
    }

    /**
     * Reads a class mapping from the stream.
     *
     * @return the class mapping, or <code>null</code> to represent a null value.
     */
    protected ClassMapping readClassMapping ()
        throws IOException, ClassNotFoundException
    {
        // create our classmap if necessary
        if (_classmap == null) {
            _classmap = Lists.newArrayList();
            // insert a zeroth element
            _classmap.add(null);
        }

        // read in the class code for this instance
        short code = readShort();

        // a zero code indicates a null value
        if (code == 0) {
            return null;

        // if the code is negative, that means that we've never seen it before and class
        // metadata follows
        } else if (code < 0) {
            // first swap the code into positive-land
            code *= -1;

            // read in the class metadata
            String cname = readUTF();
            // if we have a translation (used to cope when serialized classes are renamed) use
            // it
            if (_translations != null) {
                String tname = _translations.get(cname);
                if (tname != null) {
                    cname = tname;
                }
            }

            // create the class mapping
            return mapClass(code, cname);

        } else {
            ClassMapping cmap = (code < _classmap.size()) ? _classmap.get(code) : null;

            // sanity check
            if (cmap == null) {
                // this will help with debugging
                log.warning("Internal stream error, no class metadata", "code", code,
                            "ois", this, new Exception());
                log.warning("ObjectInputStream mappings", "map", _classmap);
                String errmsg = "Read object code for which we have no registered class " +
                    "metadata [code=" + code + "]";
                throw new RuntimeException(errmsg);
            }
            return cmap;
        }
    }

    /**
     * Creates, adds, and returns the class mapping for the specified code and class name.
     */
    protected ClassMapping mapClass (short code, String cname)
        throws IOException, ClassNotFoundException
    {
        // create a class mapping record, and cache it
        ClassMapping cmap = createClassMapping(code, cname);
        _classmap.add(code, cmap);
        return cmap;
    }

    /**
     * Creates and returns a class mapping for the specified code and class name.
     */
    protected ClassMapping createClassMapping (short code, String cname)
        throws IOException, ClassNotFoundException
    {
        // resolve the class and streamer
        ClassLoader loader = (_loader != null) ? _loader :
            Thread.currentThread().getContextClassLoader();
        Class<?> sclass = Class.forName(cname, true, loader);
        Streamer streamer = Streamer.getStreamer(sclass);
        if (STREAM_DEBUG) {
            log.info(hashCode() + ": New class '" + cname + "'", "code", code);
        }

        // sanity check
        if (streamer == null) {
            String errmsg = "Aiya! Unable to create streamer for newly seen class " +
                "[code=" + code + ", class=" + cname + "]";
            throw new RuntimeException(errmsg);
        }

        return new ClassMapping(code, sclass, streamer);
    }

    /**
     * Reads an object from the input stream that was previously written with {@link
     * ObjectOutputStream#writeBareObject(Object)}.
     *
     * @param object the object to be populated from data on the stream.  It cannot be
     * <code>null</code>.
     */
    public void readBareObject (Object object)
        throws IOException, ClassNotFoundException
    {
        readBareObject(object, Streamer.getStreamer(object.getClass()), true);
    }

    /**
     * Reads an object from the input stream that was previously written with {@link
     * ObjectOutputStream#writeBareObject(Object,Streamer,boolean)}.
     */
    protected void readBareObject (Object object, Streamer streamer, boolean useReader)
        throws IOException, ClassNotFoundException
    {
        _current = object;
        _streamer = streamer;
        try {
            _streamer.readObject(object, this, useReader);
        } finally {
            // clear out our current object references
            _current = null;
            _streamer = null;
        }
    }

    /**
     * Reads the fields of the specified {@link Streamable} instance from the input stream using
     * the default object streaming mechanisms (a call is not made to <code>readObject()</code>,
     * even if such a method exists).
     */
    public void defaultReadObject ()
        throws IOException, ClassNotFoundException
    {
        // sanity check
        if (_current == null) {
            throw new RuntimeException("defaultReadObject() called illegally.");
        }

        // read the instance data
        _streamer.readObject(_current, this, false);
    }

    /**
     * Read a string encoded as real UTF-8 (rather than the modified format handled by
     * {link #readUTF}).
     */
    public String readUnmodifiedUTF ()
        throws IOException
    {
        // find out how many raw bytes of UTF8 data there is
        int utflen = readUnsignedShort();
        // read precisely that many into a buffer
        byte[] bbuf = new byte[utflen];
        in.read(bbuf);
        return new String(bbuf, "UTF-8");
        //return new String(bbuf, Charsets.UTF_8); // TODO Java 6 (Charsets is from guava)
    }

    @Override
    public String toString ()
    {
        return "[hash=" + hashCode() + ", mappings=" + _classmap.size() +
            ", current=" + StringUtil.safeToString(_current) + ", streamer=" + _streamer + "]";
    }

    /** Used to map classes to numeric codes and the {@link Streamer} instance used to write
     * them. */
    protected List<ClassMapping> _classmap;

    /** Maps numeric codes to pooled strings. */
    protected List<String> _internmap;

    /** The object currently being read from the stream. */
    protected Object _current;

    /** The streamer being used currently. */
    protected Streamer _streamer;

    /** If set, an overridden class loader used to instantiate objects. */
    protected ClassLoader _loader;

    /** An optional set of class name translations to use when unserializing objects. */
    protected Map<String, String> _translations;

    /** Used to activate verbose debug logging. */
    protected static final boolean STREAM_DEBUG = false;
}
