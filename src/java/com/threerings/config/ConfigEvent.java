//
// $Id$

package com.threerings.config;

import java.util.EventObject;

/**
 * Contains information about a configuration added, removed, or updated.
 */
public class ConfigEvent<T extends ManagedConfig> extends EventObject
{
    /** The types of configuration events. */
    public enum Type { ADDED, REMOVED, UPDATED };

    /**
     * Creates a new config event.
     */
    public ConfigEvent (ConfigGroup<T> group, Type type, T config)
    {
        super(group);
        _group = group;
        _type = type;
        _config = config;
    }

    /**
     * Returns a reference to the group that generated the event.
     */
    public ConfigGroup<T> getGroup ()
    {
        return _group;
    }

    /**
     * Returns the type of this event.
     */
    public Type getType ()
    {
        return _type;
    }

    /**
     * Returns a reference to the affected configuration.
     */
    public T getConfig ()
    {
        return _config;
    }

    /** The group that generated the event. */
    protected ConfigGroup<T> _group;

    /** The event type. */
    protected Type _type;

    /** The affected configuration. */
    protected T _config;
}
