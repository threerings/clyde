//
// $Id$

package com.threerings.opengl.gui.event;

import java.util.EventObject;

/**
 * The base event class for all events.
 */
public class Event extends EventObject
{
    /**
     * Returns the time at which this event was generated or -1 if this
     * event was not a result of a user action with an associated
     * timestamp.
     */
    public long getWhen ()
    {
        return _when;
    }

    /**
     * Checks whether the event has been flagged as consumed.
     */
    public boolean isConsumed ()
    {
        return _consumed;
    }

    /**
     * Flags this event as consumed.
     */
    public void consume ()
    {
        _consumed = true;
    }

    /**
     * Generates a string representation of this instance.
     */
    public String toString ()
    {
        StringBuffer buf = new StringBuffer("[ev:");
        toString(buf);
        buf.append("]");
        return buf.toString();
    }

    /**
     * Instructs this event to notify the supplied listener if they
     * implement an interface appropriate to this event.
     */
    public void dispatch (ComponentListener listener)
    {
        if (listener instanceof EventListener) {
            ((EventListener)listener).eventDispatched(this);
        }
    }

    /**
     * Returns true if this event should be propagated up the interface
     * hierarchy (input events) or false if it should be considered processed
     * once it is dispatched on its originating component (derivative events
     * like action or text events).
     */
    public boolean propagateUpHierarchy ()
    {
        return true;
    }

    protected Event (Object source, long when)
    {
        super(source);
        _when = when;
    }

    protected void toString (StringBuffer buf)
    {
        String name = getClass().getName();
        name = name.substring(name.lastIndexOf(".") + 1);
        buf.append("type=").append(name);
        buf.append(", source=").append(source);
        buf.append(", when=").append(_when);
    }

    protected long _when;

    /** Whether or not the event has been flagged as "consumed." */
    protected boolean _consumed;
}
