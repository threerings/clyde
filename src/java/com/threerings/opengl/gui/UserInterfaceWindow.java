//
// $Id$

package com.threerings.opengl.gui;

import com.threerings.config.ConfigReference;
import com.threerings.expr.DynamicScope;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.config.UserInterfaceConfig;
import com.threerings.opengl.gui.layout.BorderLayout;

/**
 * A window that contains a user interface.
 */
public class UserInterfaceWindow extends Window
    implements Renderer.Observer
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
        UserInterface ui = new UserInterface(ctx);
        ui.getScope().setParentScope(_scope);
        ui.setConfig(ref);
        add(ui, BorderLayout.CENTER);
        _stretch = stretch;
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
        return (UserInterface)getComponent(0);
    }

    // documentation inherited from interface Renderer.Observer
    public void sizeChanged (int width, int height)
    {
        setSize(width, height);
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // update size and register as observer
        if (_stretch) {
            Renderer renderer = _ctx.getRenderer();
            setSize(renderer.getWidth(), renderer.getHeight());
            renderer.addObserver(this);
        }
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        if (_stretch) {
            _ctx.getRenderer().removeObserver(this);
        }
    }

    /** The window scope. */
    protected DynamicScope _scope = new DynamicScope(this, "window");

    /** The contained user interface. */
    protected UserInterface _interface;

    /** Whether or not to stretch the window across the entire display. */
    protected boolean _stretch;
}
