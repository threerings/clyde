//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

package com.threerings.config.dist.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.crowd.data.BodyObject;

import com.threerings.config.ConfigManager;
import com.threerings.config.dist.data.ConfigEntry;
import com.threerings.config.dist.data.ConfigKey;
import com.threerings.config.dist.data.DConfigObject;
import com.threerings.config.dist.util.ConfigUpdater;

import static com.threerings.ClydeLog.*;

/**
 * Handles the server side of the distributed config system.
 */
@Singleton @EventThread
public class DConfigManager
    implements DConfigProvider
{
    /**
     * Creates a new config manager.
     */
    @Inject public DConfigManager (
        ConfigManager cfgmgr, PresentsDObjectMgr omgr, InvocationManager invmgr)
    {
        omgr.registerObject(_cfgobj = new DConfigObject());
        _cfgobj.dconfigService = invmgr.registerDispatcher(new DConfigDispatcher(this));
        new ConfigUpdater(cfgmgr).init(_cfgobj);
    }

    /**
     * Returns a reference to the config object.
     */
    public DConfigObject getConfigObject ()
    {
        return _cfgobj;
    }

    // documentation inherited from interface DConfigProvider
    public void addConfig (ClientObject caller, ConfigEntry entry)
    {
        if (!verifyAdmin(caller)) {
            return;
        }
        ConfigKey key = (ConfigKey)entry.getKey();
        if (_cfgobj.removed.containsKey(key)) {
            _cfgobj.startTransaction();
            try {
                _cfgobj.requestEntryRemove(
                    DConfigObject.REMOVED, _cfgobj.removed, key, caller.getOid());
                _cfgobj.requestEntryAdd(
                    DConfigObject.UPDATED, _cfgobj.updated, entry, caller.getOid());
            } finally {
                _cfgobj.commitTransaction();
            }
        } else {
            _cfgobj.requestEntryAdd(
                DConfigObject.ADDED, _cfgobj.added, entry, caller.getOid());
        }
    }

    // documentation inherited from interface DConfigProvider
    public void removeConfig (ClientObject caller, ConfigKey key)
    {
        if (!verifyAdmin(caller)) {
            return;
        }
        if (_cfgobj.added.containsKey(key)) {
            _cfgobj.requestEntryRemove(
                DConfigObject.ADDED, _cfgobj.added, key, caller.getOid());
        } else {
            _cfgobj.startTransaction();
            try {
                if (_cfgobj.updated.containsKey(key)) {
                    _cfgobj.requestEntryRemove(
                        DConfigObject.UPDATED, _cfgobj.updated, key, caller.getOid());
                }
                _cfgobj.requestEntryAdd(
                    DConfigObject.REMOVED, _cfgobj.removed, key, caller.getOid());
            } finally {
                _cfgobj.commitTransaction();
            }
        }
    }

    // documentation inherited from interface DConfigProvider
    public void updateConfig (ClientObject caller, ConfigEntry entry)
    {
        if (!verifyAdmin(caller)) {
            return;
        }
        ConfigKey key = (ConfigKey)entry.getKey();
        if (_cfgobj.added.containsKey(key)) {
            _cfgobj.requestEntryUpdate(
                DConfigObject.ADDED, _cfgobj.added, entry, caller.getOid());
        } else if (_cfgobj.updated.containsKey(key)) {
            _cfgobj.requestEntryUpdate(
                DConfigObject.UPDATED, _cfgobj.updated, entry, caller.getOid());
        } else {
            _cfgobj.requestEntryAdd(
                DConfigObject.UPDATED, _cfgobj.updated, entry, caller.getOid());
        }
    }

    /**
     * Makes sure the specified caller is an admin, logging a warning and returning
     * <code>false</code> if not.
     */
    protected boolean verifyAdmin (ClientObject caller)
    {
        if (((BodyObject)caller).getTokens().isAdmin()) {
            return true;
        }
        log.warning("Non-admin tried to manipulate configs.", "who", caller.who());
        return false;
    }

    /** The config object. */
    protected DConfigObject _cfgobj;
}
