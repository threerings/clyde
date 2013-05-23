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

package com.threerings.editor;

import com.threerings.config.ConfigManager;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

/**
 * A property that
 */
public class TranslatedPathProperty extends PathProperty
{
    /**
     * Creates a new path property.
     *
     * @param cfgmgr the config manager to use when resolving references.
     * @param name the name of the property.
     * @param reference the reference object from which we derive our property chains.
     * @param paths the list of paths.
     * @throws InvalidPathsException if none of the supplied paths are valid.
     */
    public TranslatedPathProperty (
        ConfigManager cfgmgr, String name, String bundle, Object reference, String... paths)
            throws InvalidPathsException
    {
        super(cfgmgr, name, reference, paths);
        MessageManager msgmgr = cfgmgr.getMessageManager();
        _msgs = msgmgr == null ? new MessageBundle() : msgmgr.getBundle(bundle);
    }

    @Override
    protected void setProperty (Object obj, Object value, Property prop, boolean coerce)
    {
        if (value instanceof String && _msgs.exists((String)value)) {
            value = _msgs.get((String)value);
        }
        super.setProperty(obj, value, prop, coerce);
    }

    /** Our message bundle. */
    protected MessageBundle _msgs;

}
