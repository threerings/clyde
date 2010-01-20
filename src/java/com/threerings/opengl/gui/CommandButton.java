//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

package com.threerings.opengl.gui;

import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.event.CommandEvent;
import com.threerings.opengl.gui.icon.Icon;

/**
 * A button that fires a {@link CommandEvent} when clicked.
 */
public class CommandButton extends Button
{
    /**
     * Creates a command button with the specified textual label.
     */
    public CommandButton (GlContext ctx, String text)
    {
        this(ctx, text, "", null);
    }

    /**
     * Creates a button with the specified label, action, and argument. The action will be
     * dispatched via a {@link CommandEvent} when the button is clicked.
     */
    public CommandButton (GlContext ctx, String text, String action, Object argument)
    {
        this(ctx, text, null, action, null);
    }

    /**
     * Creates a button with the specified label, action, and argument. The action will be
     * dispatched via a {@link CommandEvent} to the specified {@link ActionListener} when the
     * button is clicked.
     */
    public CommandButton (
        GlContext ctx, String text, ActionListener listener, String action, Object argument)
    {
        super(ctx, text, listener, action);
        _argument = argument;
    }

    /**
     * Creates a button with the specified icon, action, and argument. The action will be
     * dispatched via a {@link CommandEvent} when the button is clicked.
     */
    public CommandButton (
        GlContext ctx, Icon icon, String action, Object argument)
    {
        this(ctx, icon, null, action, null);
    }

    /**
     * Creates a button with the specified icon, action, and argument. The action will be
     * dispatched via a {@link CommandEvent} to the specified {@link ActionListener} when the
     * button is clicked.
     */
    public CommandButton (
        GlContext ctx, Icon icon, ActionListener listener, String action, Object argument)
    {
        super(ctx, icon, listener, action);
        _argument = argument;
    }

    /**
     * Configures the argument to be generated when this button is clicked.
     */
    public void setArgument (Object argument)
    {
        _argument = argument;
    }

    /**
     * Returns the argument generated when this button is clicked.
     */
    public Object getArgument ()
    {
        return _argument;
    }

    @Override // documentation inherited
    protected void fireAction (long when, int modifiers)
    {
        emitEvent(new CommandEvent(this, when, modifiers, _action, _argument));
    }

    /** The argument generated when the button is clicked. */
    protected Object _argument;
}
