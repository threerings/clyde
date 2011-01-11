//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2011 Three Rings Design, Inc.
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

package com.threerings.editor;

import java.util.Map;

import com.threerings.config.ConfigManager;
import com.threerings.util.DeepUtil;
import com.threerings.util.Shallow;

/**
 * Extends {@link PathProperty} to read values from and write values to a {@link Map}, using
 * the reference object passed to the constructor as a source of default values.
 */
public class ArgumentPathProperty extends PathProperty
{
    /**
     * Creates a new map property.
     *
     * @param cfgmgr the config manager to use when resolving references.
     * @param name the name of the property.
     * @param reference the reference object from which we derive our property chains and default
     * values.
     * @param path the path.
     * @throws InvalidPathsException if the path is invalid.
     */
    public ArgumentPathProperty (ConfigManager cfgmgr, String name, Object reference, String path)
        throws InvalidPathsException
    {
        super(cfgmgr, name, reference, path);
        _reference = reference;
    }

    @Override // documentation inherited
    public Object getMemberObject (Object object)
    {
        return super.getMemberObject(_reference);
    }

    @Override // documentation inherited
    public Object get (Object object)
    {
        @SuppressWarnings("unchecked") Map<Object, Object> map =
            (Map<Object, Object>)object;
        // If we don't have a value for this key, or it's invalid, use the default
        if (!map.containsKey(_name) || !isLegalValue(map.get(_name))) {
            Object value = DeepUtil.copy(super.get(_reference));
            map.put(_name, value);
            return value;
        }
        return map.get(_name);
    }

    @Override // documentation inherited
    public void set (Object object, Object value)
    {
        @SuppressWarnings("unchecked") Map<Object, Object> map =
            (Map<Object, Object>)object;
        map.put(_name, value);
    }

    /** The reference object from which we obtain the default values. */
    @Shallow
    protected Object _reference;
}