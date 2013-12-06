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

import javax.annotation.Generated;

import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.ObjectAccessException;

import static com.threerings.ClydeLog.log;

/**
 * Contains the complete delta between the original set of configs and the current set.
 */
public class DConfigObject extends DObject
{
    /**
     * Extends {@link EntryAddedEvent} to include the client oid.
     */
    public static class ClientEntryAddedEvent<T extends DSet.Entry> extends EntryAddedEvent<T>
    {
        /**
         * Default constructor.
         */
        public ClientEntryAddedEvent (int toid, String name, T entry, int clientOid)
        {
            super(toid, name, entry);
            _clientOid = clientOid;
        }

        /**
         * Returns the oid of the client that caused the event.
         */
        public int getClientOid ()
        {
            return _clientOid;
        }

        @Override
        public boolean applyToObject (DObject target)
            throws ObjectAccessException
        {
            boolean result = super.applyToObject(target);
            _alreadyApplied = true;
            return result;
        }

        /** The oid of the client that caused the event. */
        protected int _clientOid;
    }

    /**
     * Extends {@link EntryRemovedEvent} to include the client oid.
     */
    public static class ClientEntryRemovedEvent<T extends DSet.Entry> extends EntryRemovedEvent<T>
    {
        /**
         * Default constructor.
         */
        public ClientEntryRemovedEvent (int toid, String name, Comparable<?> key, int clientOid)
        {
            super(toid, name, key);
            _clientOid = clientOid;
        }

        /**
         * Returns the oid of the client that caused the event.
         */
        public int getClientOid ()
        {
            return _clientOid;
        }

        /** The oid of the client that caused the event. */
        protected int _clientOid;
    }

    /**
     * Extends {@link EntryUpdatedEvent} to include the client oid.
     */
    public static class ClientEntryUpdatedEvent<T extends DSet.Entry> extends EntryUpdatedEvent<T>
    {
        /**
         * Default constructor.
         */
        public ClientEntryUpdatedEvent (int toid, String name, T entry, int clientOid)
        {
            super(toid, name, entry);
            _clientOid = clientOid;
        }

        /**
         * Returns the oid of the client that caused the event.
         */
        public int getClientOid ()
        {
            return _clientOid;
        }

        /** The oid of the client that caused the event. */
        protected int _clientOid;
    }

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>dconfigService</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String DCONFIG_SERVICE = "dconfigService";

    /** The field name of the <code>added</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String ADDED = "added";

    /** The field name of the <code>updated</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String UPDATED = "updated";

    /** The field name of the <code>removed</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String REMOVED = "removed";
    // AUTO-GENERATED: FIELDS END

    /** The config service. */
    public DConfigMarshaller dconfigService;

    /** The set of configs added to the manager. */
    public DSet<ConfigEntry> added = DSet.newDSet();

    /** The set of configs updated within the manager. */
    public DSet<ConfigEntry> updated = DSet.newDSet();

    /** The keys of all configs removed from the manager. */
    public DSet<ConfigKey> removed = DSet.newDSet();

    /**
     * Requests to add an entry to a set, including a source oid in the event.
     */
    public <T extends DSet.Entry> void requestEntryAdd (
        String name, DSet<T> set, T entry, int clientOid)
    {
        applyAndPostEvent(new ClientEntryAddedEvent<T>(_oid, name, entry, clientOid));
    }

    /**
     * Requests to remove an entry from a set, including a source oid in the event.
     */
    public <T extends DSet.Entry> void requestEntryRemove (
        String name, DSet<T> set, Comparable<?> key, int clientOid)
    {
        applyAndPostEvent(new ClientEntryRemovedEvent<T>(_oid, name, key, clientOid));
    }

    /**
     * Requests to update an entry within a set, including a source oid in the event.
     */
    public <T extends DSet.Entry> void requestEntryUpdate (
        String name, DSet<T> set, T entry, int clientOid)
    {
        applyAndPostEvent(new ClientEntryUpdatedEvent<T>(_oid, name, entry, clientOid));
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>dconfigService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setDconfigService (DConfigMarshaller value)
    {
        DConfigMarshaller ovalue = this.dconfigService;
        requestAttributeChange(
            DCONFIG_SERVICE, value, ovalue);
        this.dconfigService = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>added</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToAdded (ConfigEntry elem)
    {
        requestEntryAdd(ADDED, added, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>added</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromAdded (Comparable<?> key)
    {
        requestEntryRemove(ADDED, added, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>added</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void updateAdded (ConfigEntry elem)
    {
        requestEntryUpdate(ADDED, added, elem);
    }

    /**
     * Requests that the <code>added</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setAdded (DSet<ConfigEntry> value)
    {
        requestAttributeChange(ADDED, value, this.added);
        DSet<ConfigEntry> clone = (value == null) ? null : value.clone();
        this.added = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>updated</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToUpdated (ConfigEntry elem)
    {
        requestEntryAdd(UPDATED, updated, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>updated</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromUpdated (Comparable<?> key)
    {
        requestEntryRemove(UPDATED, updated, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>updated</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void updateUpdated (ConfigEntry elem)
    {
        requestEntryUpdate(UPDATED, updated, elem);
    }

    /**
     * Requests that the <code>updated</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setUpdated (DSet<ConfigEntry> value)
    {
        requestAttributeChange(UPDATED, value, this.updated);
        DSet<ConfigEntry> clone = (value == null) ? null : value.clone();
        this.updated = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>removed</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToRemoved (ConfigKey elem)
    {
        requestEntryAdd(REMOVED, removed, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>removed</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromRemoved (Comparable<?> key)
    {
        requestEntryRemove(REMOVED, removed, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>removed</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void updateRemoved (ConfigKey elem)
    {
        requestEntryUpdate(REMOVED, removed, elem);
    }

    /**
     * Requests that the <code>removed</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setRemoved (DSet<ConfigKey> value)
    {
        requestAttributeChange(REMOVED, value, this.removed);
        DSet<ConfigKey> clone = (value == null) ? null : value.clone();
        this.removed = clone;
    }
    // AUTO-GENERATED: METHODS END

    /**
     * Applies the specified event to this object and posts it.
     */
    protected void applyAndPostEvent (DEvent event)
    {
        try {
            event.applyToObject(this);
        } catch (ObjectAccessException e) {
            log.warning("Failed to apply event.", e);
        }
        postEvent(event);
    }
}
