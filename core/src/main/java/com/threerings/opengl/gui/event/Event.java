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
        StringBuilder buf = new StringBuilder("[ev:");
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

    protected void toString (StringBuilder buf)
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
