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

package com.threerings.opengl.gui;

import com.threerings.config.ConfigReference;
import com.threerings.expr.DynamicScope;

import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.config.UserInterfaceConfig;
import com.threerings.opengl.gui.layout.BorderLayout;

/**
 * A window that contains a user interface.
 */
public class UserInterfaceWindow extends StretchWindow
{
    /**
     * Creates a new user interface window.
     *
     * @param stretch whether or not to stretch the window across the entire screen.
     */
    public UserInterfaceWindow (GlContext ctx, boolean stretch)
    {
        this(ctx, stretch, (ConfigReference<UserInterfaceConfig>)null);
    }

    /**
     * Creates a new interface window with the named configuration.
     *
     * @param stretch whether or not to stretch the window across the entire screen.
     */
    public UserInterfaceWindow (GlContext ctx, boolean stretch, String name)
    {
        this(ctx, stretch, new ConfigReference<UserInterfaceConfig>(name));
    }

    /**
     * Creates a new interface with the named configuration and arguments.
     *
     * @param stretch whether or not to stretch the window across the entire screen.
     */
    public UserInterfaceWindow (
        GlContext ctx, boolean stretch, String name, String firstKey,
        Object firstValue, Object... otherArgs)
    {
        this(ctx, stretch, new ConfigReference<UserInterfaceConfig>(
            name, firstKey, firstValue, otherArgs));
    }

    /**
     * Creates a new user interface window.
     *
     * @param stretch whether or not to stretch the window across the entire screen.
     */
    public UserInterfaceWindow (
        GlContext ctx, boolean stretch, ConfigReference<UserInterfaceConfig> ref)
    {
        super(ctx, new BorderLayout());
        _stretch = stretch;
        _scope.setParentScope(ctx.getScope());
        _interface = createInterface();
        _interface.getScope().setParentScope(_scope);
        _interface.setConfig(ref);
        add(_interface, BorderLayout.CENTER);
    }

    /**
     * Returns a reference to the window scope.
     */
    public DynamicScope getScope ()
    {
        return _scope;
    }

    /**
     * Returns a reference to the user interface component.
     */
    public UserInterface getInterface ()
    {
        return _interface;
    }

    /**
     * A shortcut method for retrieving a component registered by name from the interface.
     */
    public Component getComponent (String name)
    {
        return _interface.getComponent(name);
    }

    /**
     * Creates the user interface for the window.
     */
    protected UserInterface createInterface ()
    {
        return new UserInterface(_ctx);
    }

    @Override
    protected boolean isStretching ()
    {
        return _stretch;
    }

    /** The window scope. */
    protected DynamicScope _scope = new DynamicScope(this, "window");

    /** The contained user interface. */
    protected UserInterface _interface;

    /** Whether or not to stretch the window across the entire display. */
    protected boolean _stretch;
}
