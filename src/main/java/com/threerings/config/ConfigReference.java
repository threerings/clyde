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

package com.threerings.config;

import com.threerings.io.Intern;
import com.threerings.io.Streamable;

import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * A reference to a configuration that can be embedded in, for example, other configurations.
 */
public class ConfigReference<T extends ManagedConfig> extends DeepObject
    implements Exportable, Streamable
{
    /**
     * Creates a new reference to the named configuration.
     */
    public ConfigReference (String name)
    {
        _name = name;
    }

    /**
     * Creates a new reference to the named configuration with the specified arguments.
     */
    public ConfigReference (String name, String firstKey, Object firstValue, Object... otherArgs)
    {
        _name = name;
        _arguments.put(firstKey, firstValue);
        for (int ii = 0; ii < otherArgs.length; ii += 2) {
            _arguments.put((String)otherArgs[ii], otherArgs[ii + 1]);
        }
    }

    /**
     * No-arg constructor for deserialization.
     */
    public ConfigReference ()
    {
    }

    /**
     * Returns the name of the referenced config.
     */
    public String getName ()
    {
        return _name;
    }

    /**
     * Returns a reference to the argument map.
     */
    public ArgumentMap getArguments ()
    {
        return _arguments;
    }

    /**
     * Fluent method to get the config.
     */
    public T getConfig (ConfigManager cfgMgr, Class<T> token)
    {
        return cfgMgr.getConfig(token, this);
    }

    @Override
    public Object copy (Object dest)
    {
        ConfigReference<?> cref;
        if (dest instanceof ConfigReference) {
            cref = (ConfigReference<?>)dest;
            cref._name = _name;
        } else {
            cref = new ConfigReference<T>(_name);
        }
        _arguments.copy(cref.getArguments());
        return cref;
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof ConfigReference)) {
            return false;
        }
        ConfigReference<?> oref = (ConfigReference<?>)other;
        return _name.equals(oref.getName()) && _arguments.equals(oref.getArguments());
    }

    @Override
    public int hashCode ()
    {
        return 31*_name.hashCode() + _arguments.hashCode();
    }

    @Override
    public String toString ()
    {
        return "[name=" + _name + ", arguments=" + _arguments + "]";
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigReference<T> clone ()
    {
        return (ConfigReference<T>)super.clone();
    }

    /**
     * Clone this reference, and add the specified argument to the cloned instance.
     */
    public ConfigReference<T> clone (String arg, Object value)
    {
        ConfigReference<T> that = clone();
        that.getArguments().put(arg, value);
        return that;
    }

    /**
     * Clone this reference, and add the specified arguments to the cloned instance.
     */
    public ConfigReference<T> clone (String arg, Object value, Object... moreArgs)
    {
        ConfigReference<T> that = clone(arg, value);
        for (int ii = 0, nn = moreArgs.length; ii < nn; ii += 2) {
            that.getArguments().put((String)moreArgs[ii], moreArgs[ii + 1]);
        }
        return that;
    }

    /** The name of the referenced configuration. */
    @Intern
    protected String _name;

    /** The arguments of the reference, mapped by name. */
    protected ArgumentMap _arguments = new ArgumentMap();
}
