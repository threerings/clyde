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

package com.threerings.tudey.client.sprite;

import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;

import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.model.Model;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.util.TudeyContext;

/**
 * Represents a placeable object.
 */
public abstract class Sprite extends SimpleScope
{
    /**
     * Creates a new sprite.
     */
    public Sprite (TudeyContext ctx, TudeySceneView view)
    {
        super(view);
        _ctx = ctx;
        _view = view;
    }

    /**
     * Returns the sprite's floor flags.
     */
    public int getFloorFlags ()
    {
        return 0x0;
    }

    /**
     * Returns the sprite's floor mask.
     */
    public int getFloorMask ()
    {
        return 0x255;
    }

    /**
     * Determines whether the sprite is hoverable (for purposes of in-game user interaction).
     */
    public boolean isHoverable ()
    {
        return false;
    }

    /**
     * Determines whether the sprite is clickable.
     */
    public boolean isClickable ()
    {
        return false;
    }

    /**
     * Returns the sprite's tooltip text, or <code>null</code> for none.
     */
    public String getTooltipText ()
    {
        return null;
    }

    /**
     * Returns the sprite's tooltip timeout, or -1 to use the default.
     */
    public float getTooltipTimeout ()
    {
        return -1f;
    }

    /**
     * Returns the sprite's tooltip window style.
     */
    public String getTooltipWindowStyle ()
    {
        return "Default/TooltipWindow";
    }

    /**
     * Creates a tooltip component for the sprite (will only be called if {@link #getTooltipText}
     * returns true).
     */
    public Component createTooltipComponent (String tiptext)
    {
        return Component.createDefaultTooltipComponent(_ctx, tiptext);
    }

    /**
     * Returns the model associated with the sprite (if any).
     */
    public Model getModel ()
    {
        return null;
    }

    /**
     * Attempt to set the visibility of this sprite. No guarantees are made.
     */
    public void setVisible (boolean visible)
    {
        Model model = getModel();
        if (model != null) {
            model.setVisible(visible);
        }
    }

    /**
     * Dispatches an event on the sprite.
     *
     * @return true if the sprite handled the event, false if it should be handled elsewhere.
     */
    public boolean dispatchEvent (Event event)
    {
        return false;
    }

    @Override
    public String getScopeName ()
    {
        return "sprite";
    }

    /** The application context. */
    protected TudeyContext _ctx;

    /** The parent view. */
    @Scoped
    protected TudeySceneView _view;
}
