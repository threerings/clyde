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

package com.threerings.config.dist.util;

import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ManagedConfig;
import com.threerings.config.dist.data.ConfigEntry;
import com.threerings.config.dist.data.ConfigKey;
import com.threerings.config.dist.data.DConfigObject;

import static com.threerings.ClydeLog.log;

/**
 * A utility class used on both the client and the server to apply events received on the config
 * object to the set of managed configs.
 */
public class ConfigUpdater
    implements SetListener<DSet.Entry>
{
    /**
     * Creates a new updater.
     */
    public ConfigUpdater (ConfigManager cfgmgr)
    {
        _cfgmgr = cfgmgr;
    }

    /**
     * Initializes the updater with a reference to the config object.
     */
    public void init (DConfigObject cfgobj)
    {
        // apply all changes made to date
        for (ConfigEntry entry : cfgobj.added) {
            addConfig(entry.getConfig());
        }
        for (ConfigEntry entry : cfgobj.updated) {
            updateConfig(entry.getConfig());
        }
        for (ConfigKey key : cfgobj.removed) {
            removeConfig(key);
        }

        // register as a listener for further updates
        cfgobj.addListener(this);
    }

    // documentation inherited from interface SetListener
    public void entryAdded (EntryAddedEvent<DSet.Entry> event)
    {
        String name = event.getName();
        if (name.equals(DConfigObject.ADDED)) {
            ConfigEntry entry = (ConfigEntry)event.getEntry();
            addConfig(entry.getConfig());

        } else if (name.equals(DConfigObject.UPDATED)) {
            ConfigEntry entry = (ConfigEntry)event.getEntry();
            updateConfig(entry.getConfig());

        } else if (name.equals(DConfigObject.REMOVED)) {
            removeConfig((ConfigKey)event.getEntry());
        }
    }

    // documentation inherited from interface SetListener
    public void entryRemoved (EntryRemovedEvent<DSet.Entry> event)
    {
        if (event.getName().equals(DConfigObject.ADDED)) {
            removeConfig((ConfigKey)event.getKey());
        }
    }

    // documentation inherited from interface SetListener
    public void entryUpdated (EntryUpdatedEvent<DSet.Entry> event)
    {
        String name = event.getName();
        if (name.equals(DConfigObject.ADDED) || name.equals(DConfigObject.UPDATED)) {
            ConfigEntry entry = (ConfigEntry)event.getEntry();
            updateConfig(entry.getConfig());
        }
    }

    /**
     * Attempts to add a config.
     */
    protected void addConfig (ManagedConfig config)
    {
        @SuppressWarnings("unchecked") ConfigGroup<ManagedConfig> group =
            (ConfigGroup<ManagedConfig>)_cfgmgr.getGroup(config.getClass());
        group.addConfig(config);
    }

    /**
     * Attempts to remove a config.
     */
    protected void removeConfig (ConfigKey key)
    {
        @SuppressWarnings("unchecked") Class<ManagedConfig> mclass =
            (Class<ManagedConfig>)key.getConfigClass();
        ConfigGroup<ManagedConfig> group = _cfgmgr.getGroup(mclass);
        ManagedConfig config = group.getRawConfig(key.getName());
        if (config != null) {
            group.removeConfig(config);
        } else {
            log.warning("Missing config to remove.", "key", key);
        }
    }

    /**
     * Attempts to update a config.
     */
    protected void updateConfig (ManagedConfig nconfig)
    {
        ManagedConfig oconfig = _cfgmgr.getConfig(nconfig.getClass(), nconfig.getName());
        if (oconfig != null) {
            nconfig.copy(oconfig);
            oconfig.wasUpdated();
        } else if (!_cfgmgr.isResourceClass(nconfig.getClass())) {
            addConfig(nconfig);
        } else {
            log.warning("Attempted to update unknown resource.", "name", nconfig.getName());
        }
    }

    /** The config manager. */
    protected ConfigManager _cfgmgr;
}
