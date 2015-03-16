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

package com.threerings.config.dist.data;

import java.io.IOException;

import java.util.Arrays;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.config.ManagedConfig;
import com.threerings.export.util.ExportUtil;

/**
 * Represents an added or updated configuration.
 */
public class ConfigEntry extends SimpleStreamableObject
    implements DSet.Entry
{
    /**
     * Creates a new config entry.
     */
    public ConfigEntry (ManagedConfig config)
    {
        _key = new ConfigKey(config.getClass(), config.getName());
        _bytes = ExportUtil.toBytes(_config = config);
    }

    /**
     * No-arg constructor for deserialization.
     */
    public ConfigEntry ()
    {
    }

    /**
     * Returns the config class.
     */
    public Class<? extends ManagedConfig> getConfigClass ()
    {
        return _key.getConfigClass();
    }

    /**
     * Returns the name of the config.
     */
    public String getName ()
    {
        return _key.getName();
    }

    /**
     * Returns a reference to the config object.
     */
    public ManagedConfig getConfig ()
    {
        return _config;
    }

    /**
     * Custom read method for streaming.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        _config = (ManagedConfig)ExportUtil.fromBytes(_bytes);
    }

    // documentation inherited from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return _key;
    }

    @Override
    public String toString ()
    {
        return "[key=" + _key + ", config=" + _config + "]";
    }

    @Override
    public boolean equals (Object other)
    {
        ConfigEntry oentry;
        return other instanceof ConfigEntry && (oentry = (ConfigEntry)other)._key.equals(_key) &&
            Arrays.equals(oentry._bytes, _bytes);
    }

    @Override
    public int hashCode ()
    {
        int result = _key != null ? _key.hashCode() : 0;
        result = 31 * result + (_bytes != null ? Arrays.hashCode(_bytes) : 0);
        result = 31 * result + (_config != null ? _config.hashCode() : 0);
        return result;
    }

    /** The config key. */
    protected ConfigKey _key;

    /** The exported config. */
    protected byte[] _bytes;

    /** The config object. */
    protected transient ManagedConfig _config;
}
