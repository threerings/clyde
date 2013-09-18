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

/**
 * Dispatched by a component when some sort of component-specific action
 * has occurred.
 */
public class ActionEvent extends InputEvent
{
    public ActionEvent (Object source, long when, int modifiers, String action)
    {
        this(source, when, modifiers, action, null);
    }

    public ActionEvent (Object source, long when, int modifiers, String action, Object argument)
    {
        super(source, when, modifiers);
        _action = action;
        _argument = argument;
    }

    /**
     * Returns the action associated with this event.
     */
    public String getAction ()
    {
        return _action;
    }

    /**
     * Returns the argument associated with this event.
     */
    public Object getArgument ()
    {
        return _argument;
    }

    // documentation inherited
    public void dispatch (ComponentListener listener)
    {
        super.dispatch(listener);
        if (listener instanceof ActionListener) {
            ((ActionListener)listener).actionPerformed(this);
        }
    }

    // documentation inherited
    public boolean propagateUpHierarchy ()
    {
        return false;
    }

    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", action=").append(_action);
        buf.append(", argument=").append(_argument);
    }

    protected String _action;
    protected Object _argument;
}
