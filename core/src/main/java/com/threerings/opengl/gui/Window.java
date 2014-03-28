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

import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.layout.LayoutManager;
import com.threerings.opengl.gui.util.Dimension;

/**
 * A window defines the top-level of a component hierarchy. It must be created with a stylesheet
 * and layout manager.
 */
public class Window extends Container
    implements Comparable<Window>
{
    public Window (GlContext ctx, LayoutManager layout)
    {
        super(ctx);
        setLayoutManager(layout);
    }

    /**
     * Sizes this window to its preferred size. This method does not change the window's
     * coordinates.
     */
    public void pack ()
    {
        pack(-1, -1);
    }

    /**
     * Sizes this window to its preferred size, accounting for the specified width or height
     * hints. Specify -1 for a dimension to indicate that it is freely resizable and a non-negative
     * hint for a dimension that should be no larger than a particular size. This method does not
     * change the window's coordinates.
     */
    public void pack (int whint, int hhint)
    {
        Dimension ps = getPreferredSize(whint, hhint);
        setBounds(_x, _y, ps.width, ps.height);
    }

    /**
     * Positions this window in the center of the display. This should be called after configuring
     * the size of the window (using, for example, a call to {@link #pack}).
     */
    public void center ()
    {
        int width = _root.getDisplayWidth();
        int height = _root.getDisplayHeight();
        setLocation((width-getWidth())/2, (height-getHeight())/2);
    }

    /**
     * Positions this window in the top center of the display. This should be called after configuring
     * the size of the window (using, for example, a call to {@link #pack}).
     */
    public void topCenter ()
    {
        int width = _root.getDisplayWidth();
        int height = _root.getDisplayHeight();
        setLocation((width-getWidth())/2, (height-getHeight()));
    }

    /**
     * Configures this window to be modal which causes it to "steal" all mouse and keyboard input
     * while it is added to the interface hierarchy. Mouse movement and button press events that
     * would normally go to other windows or to the default mouse target will instead be sent to
     * the top-most modal window. Other events destined for other windows (key events and mouse
     * entry and exit events) will not be dispatched.
     */
    public void setModal (boolean modal)
    {
        _modal = modal;
    }

    /**
     * Returns whether or not this window is modal. See {@link #setModal} for more information on
     * modality.
     */
    public boolean isModal ()
    {
        return _modal;
    }

    /**
     * Returns whether this window should have shading applied beneath it.
     */
    public boolean shouldShadeBehind ()
    {
        return _modal;
    }

    /**
     * Returns whether or not this window is an overlay, in which case its children will not
     * receive input focus by default.
     */
    public boolean isOverlay ()
    {
        return false;
    }

    /**
     * Returns the root node that manages this window.
     */
    public Root getRoot ()
    {
        return _root;
    }

    /**
     * Returns the layer occupied by this window. See {@link #setLayer} for more details.
     */
    public int getLayer ()
    {
        return _layer;
    }

    /**
     * Configures the "layer" occupied by this window. Windows normally stack one atop another with
     * the most recently added window being highest in the stack. The layer configuration allows a
     * window to be added above windows in a lower layer regardless of when it or other windows are
     * added. All windows default to a layer of zero, windows with a higher layer will be "above"
     * those with a lower layer. Windows in the same layer stack according to the order in which
     * they are added.
     */
    public void setLayer (int layer)
    {
        _layer = layer;
        if (_root != null) {
            _root.resortWindows();
        }
    }

    /**
     * Detaches this window from the root node and removes it from the display.
     */
    public void dismiss ()
    {
        if (_root != null) {
            _root.removeWindow(this);
        } else {
            Log.log.warning("Unmanaged window dismissed: " + this + ".", new Exception());
        }
    }

    // documentation inherited from interface Comparable
    public int compareTo (Window other)
    {
        return _layer - other._layer;
    }

    @Override // from Component
    public void setParent (Container parent)
    {
        throw new RuntimeException(
            "A window may not be added as a child of any other component. Add it to a BRootNode.");
    }

    @Override // from Component
    public void invalidate ()
    {
        super.invalidate();

        if (_root != null) {
            // when an invalidation call reaches an attached top-level window, let the root node
            // know that we're invalid
            _root.rootInvalidated(this);
        }
    }

    @Override // from Component
    public void setBounds (int x, int y, int width, int height)
    {
        boolean relocated = (x != _x || y != _y);
        super.setBounds(x, y, width, height);

        // if this window was moved, we need to tell our root node to recomput the hover component
        if (relocated && _root != null) {
            _root.windowDidMove(this);
        }
    }

    @Override // from Component
    public boolean isAdded ()
    {
        return _root != null;
    }

    /**
     * Set the parent window.  If the parent window is removed from its root node, this window will
     * also be removed.
     */
    public void setParentWindow (Window parentWindow)
    {
        _parentWindow = parentWindow;
    }

    /**
     * Returne the parent window.
     */
    public Window getParentWindow ()
    {
        return _parentWindow;
    }

    /**
     * Return a new ActionListener that will dismiss this window upon any action.
     */
    public ActionListener dismissListener ()
    {
        return new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                dismiss();
            }
        };
    }

    @Override
    protected String getDefaultStyleConfig ()
    {
        return "Default/Window";
    }

    /**
     * Configures this window with its root node. Do not call this method, it is called
     * automatically when a window is added to the root node via a call to {@link
     * Root#addWindow}.
     */
    protected void setRoot (Root root)
    {
        if (_root != root) {
            _root = root;
            if (_root == null) {
                wasRemoved();
            } else {
                wasAdded();
                // if we've already been configured with dimensions, start the validation process,
                // otherwise wait for whoever created us to give us dimensions
                if (_width != 0 && _height != 0) {
                    validate();
                }
            }
        }
    }

    /**
     * Requests that the specified component be given the input focus.
     */
    protected void requestFocus (Component component)
    {
        if (_root == null) {
            _savedFocus = component;
        } else {
            _root.requestFocus(component);
        }
    }

    /**
     * Called when this window gets the focus after a window on the hierarchy has been removed.
     */
    protected void gotFocus ()
    {
        if (_savedFocus != null) {
            _root.setFocus(_savedFocus);
            _savedFocus = null;
        }
    }

    /** The root node that connects us into the JME system. */
    protected Root _root;

    /** Whether or not this window steals input from other windows further down the hierarchy. */
    protected boolean _modal;

    /** The "layer" in the window stack occupied by this window. */
    protected int _layer;

    /** Stores a reference to our focus when this window is no longer the top-most window. */
    protected Component _savedFocus;

    /** Used to associate this window with a parent window. */
    protected Window _parentWindow;
}
