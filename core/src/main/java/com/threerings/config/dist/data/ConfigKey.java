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

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.config.ManagedConfig;

/**
 * Identifies a configuration.
 */
public class ConfigKey extends SimpleStreamableObject
    implements Comparable<ConfigKey>, DSet.Entry
{
    /**
     * Constructor for new keys.
     */
    public ConfigKey (Class<? extends ManagedConfig> cclass, String name)
    {
        _cclass = cclass;
        _name = name;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public ConfigKey ()
    {
    }

    /**
     * Returns the config class.
     */
    public Class<? extends ManagedConfig> getConfigClass ()
    {
        return _cclass;
    }

    /**
     * Returns the name of the config.
     */
    public String getName ()
    {
        return _name;
    }

    /**
     * Custom write method for streaming.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();
        out.writeIntern(_cclass.getName());
    }

    /**
     * Custom read method for streaming.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        @SuppressWarnings("unchecked") Class<? extends ManagedConfig> cclass =
            (Class<? extends ManagedConfig>)Class.forName(in.readIntern());
        _cclass = cclass;
    }

    // documentation inherited from interface Comparable
    public int compareTo (ConfigKey other)
    {
        int comp = _cclass.getName().compareTo(other._cclass.getName());
        return (comp == 0) ? _name.compareTo(other._name) : comp;
    }

    // documentation inherited from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return this;
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof ConfigKey)) {
            return false;
        }
        ConfigKey okey = (ConfigKey)other;
        return _cclass == okey._cclass && _name.equals(okey._name);
    }

    @Override
    public String toString ()
    {
        return "[cclass=" + _cclass.getName() + ", name=" + _name + "]";
    }

    /** The config class. */
    protected transient Class<? extends ManagedConfig> _cclass;

    /** The config name. */
    protected String _name;

    @Override
    public int hashCode ()
    {
        int result = _cclass != null ? _cclass.hashCode() : 0;
        result = 31 * result + (_name != null ? _name.hashCode() : 0);
        return result;
    }
}
