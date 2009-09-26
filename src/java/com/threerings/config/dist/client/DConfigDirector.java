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

package com.threerings.config.dist.client;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.PresentsContext;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigGroupListener;
import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.config.ManagedConfig;
import com.threerings.config.dist.data.ConfigEntry;
import com.threerings.config.dist.data.ConfigKey;
import com.threerings.config.dist.data.DConfigBootstrapData;
import com.threerings.config.dist.data.DConfigObject;
import com.threerings.config.dist.util.ConfigUpdater;
import com.threerings.util.ChangeBlock;

import static com.threerings.ClydeLog.*;

/**
 * Handles the client side of the distributed config system.
 */
public class DConfigDirector extends BasicDirector
    implements Subscriber<DConfigObject>, ConfigGroupListener<ManagedConfig>,
        ConfigUpdateListener<ManagedConfig>
{
    /**
     * Creates a new distributed config director.
     */
    public DConfigDirector (PresentsContext ctx, ConfigManager cfgmgr)
    {
        super(ctx);
        _cfgmgr = cfgmgr;

        // listen to all groups and for all updates
        for (ConfigGroup group : cfgmgr.getGroups()) {
            @SuppressWarnings("unchecked") ConfigGroup<ManagedConfig> mgroup =
                (ConfigGroup<ManagedConfig>)group;
            mgroup.addListener(this);
        }
        _cfgmgr.addUpdateListener(this);
    }

    // documentation inherited from interface Subscriber
    public void objectAvailable (DConfigObject cfgobj)
    {
        // create an update to apply events that did not originate on this client
        new ConfigUpdater(_cfgmgr) {
            @Override public void entryAdded (EntryAddedEvent event) {
                if (event.getSourceOid() != _ctx.getClient().getClientOid() && _block.enter()) {
                    try {
                        super.entryAdded(event);
                    } finally {
                        _block.leave();
                    }
                }
            }
            @Override public void entryUpdated (EntryUpdatedEvent event) {
                if (event.getSourceOid() != _ctx.getClient().getClientOid() && _block.enter()) {
                    try {
                        super.entryUpdated(event);
                    } finally {
                        _block.leave();
                    }
                }
            }
            @Override public void entryRemoved (EntryRemovedEvent event) {
                if (event.getSourceOid() != _ctx.getClient().getClientOid() && _block.enter()) {
                    try {
                        super.entryRemoved(event);
                    } finally {
                        _block.leave();
                    }
                }
            }
        }.init(_cfgobj = cfgobj);
    }

    // documentation inherited from interface Subscriber
    public void requestFailed (int oid, ObjectAccessException cause)
    {
        log.warning("Failed to subscribe to config object.", "oid", oid, cause);
    }

    // documentation inherited from interface ConfigGroupListener
    public void configAdded (ConfigEvent<ManagedConfig> event)
    {
        if (_cfgobj == null || !_block.enter()) {
            return;
        }
        try {
            _cfgobj.dconfigService.addConfig(_ctx.getClient(), new ConfigEntry(event.getConfig()));
        } finally {
            _block.leave();
        }
    }

    // documentation inherited from interface ConfigGroupListener
    public void configRemoved (ConfigEvent<ManagedConfig> event)
    {
        if (_cfgobj == null || !_block.enter()) {
            return;
        }
        try {
            ManagedConfig config = event.getConfig();
            _cfgobj.dconfigService.removeConfig(
                _ctx.getClient(), new ConfigKey(config.getClass(), config.getName()));
        } finally {
            _block.leave();
        }
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<ManagedConfig> event)
    {
        if (_cfgobj == null || !_block.enter()) {
            return;
        }
        try {
            _cfgobj.dconfigService.updateConfig(
                _ctx.getClient(), new ConfigEntry(event.getConfig()));
        } finally {
            _block.leave();
        }
    }

    @Override // documentation inherited
    public void clientDidLogoff (Client client)
    {
        super.clientDidLogoff(client);
        _cfgobj = null;
    }

    @Override // documentation inherited
    protected void fetchServices (Client client)
    {
        int oid = ((DConfigBootstrapData)client.getBootstrapData()).dconfigOid;
        _ctx.getDObjectManager().subscribeToObject(oid, this);
    }

    /** The root config manager. */
    protected ConfigManager _cfgmgr;

    /** The config object. */
    protected DConfigObject _cfgobj;

    /** Indicates that we should ignore any changes, because we're the one effecting them. */
    protected ChangeBlock _block = new ChangeBlock();
}
