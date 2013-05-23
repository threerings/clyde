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

import com.threerings.opengl.gui.config.CursorConfig;
import com.threerings.opengl.gui.config.StyleConfig;
import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.icon.Icon;

/**
 * Displays a simple button that can be depressed and which generates an action
 * event when pressed and released.
 */
public class Button extends AbstractButton
{
    /** Indicates that this button is in the down state. */
    public static final int DOWN = Component.STATE_COUNT + 0;

    /**
     * Creates a button with the specified textual label.
     */
    public Button (GlContext ctx, String text)
    {
        this(ctx, text, null, null, null);
    }

    /**
     * Creates a button with the specified label and action. The action
     * will be dispatched via an {@link ActionEvent} when the button is
     * clicked.
     */
    public Button (GlContext ctx, String text, String action)
    {
        this(ctx, text, null, action, null);
    }

    /**
     * Creates a button with the specified label, action, and argument.
     */
    public Button (GlContext ctx, String text, String action, Object argument)
    {
        this(ctx, text, null, action, argument);
    }

    /**
     * Creates a button with the specified label and action. The action will be
     * dispatched via an {@link ActionEvent} to the specified {@link
     * ActionListener} when the button is clicked.
     */
    public Button (GlContext ctx, String text, ActionListener listener, String action)
    {
        this(ctx, text, listener, action, null);
    }

    /**
     * Creates a button with the specified label, action, and argument.
     */
    public Button (
        GlContext ctx, String text, ActionListener listener, String action, Object argument)
    {
        super(ctx, null, text, action, argument);
        if (listener != null) {
            addListener(listener);
        }
    }

    /**
     * Creates a button with the specified icon and action. The action will be
     * dispatched via an {@link ActionEvent} when the button is clicked.
     */
    public Button (GlContext ctx, Icon icon, String action)
    {
        this(ctx, icon, null, action, null);
    }

    /**
     * Creates a button with the specified icon, action, and argument.
     */
    public Button (GlContext ctx, Icon icon, String action, Object argument)
    {
        this(ctx, icon, null, action, argument);
    }

    /**
     * Creates a button with the specified icon and action. The action will be
     * dispatched via an {@link ActionEvent} to the specified {@link
     * ActionListener} when the button is clicked.
     */
    public Button (GlContext ctx, Icon icon, ActionListener listener, String action)
    {
        this(ctx, icon, listener, action, null);
    }

    /**
     * Creates a button with the specified icon, action, and argument.
     */
    public Button (
        GlContext ctx, Icon icon, ActionListener listener, String action, Object argument)
    {
        super(ctx, icon, null, action, argument);
        if (listener != null) {
            addListener(listener);
        }
    }

    /**
     * Returns a reference to the feedback sound used by this component.
     */
    public String getFeedbackSound ()
    {
        String sound = _feedbackSounds[getState()];
        return (sound != null) ? sound : _feedbackSounds[DEFAULT];
    }

    /**
     * Programmatically activates the button.
     */
    public void doClick ()
    {
        if (isAdded()) {
            Root root = getWindow().getRoot();
            fireAction(root.getTickStamp(), root.getModifiers());
        }
    }

    // documentation inherited
    public int getState ()
    {
        int state = super.getState();
        return (_armed && (state != DISABLED))
            ? DOWN
            : state; // most likely HOVER
    }

    @Override
    protected String getDefaultStyleConfig ()
    {
        return "Default/Button";
    }

    // documentation inherited
    protected int getStateCount ()
    {
        return STATE_COUNT;
    }

    // documentation inherited
    protected String getStatePseudoClass (int state)
    {
        if (state >= Component.STATE_COUNT) {
            return STATE_PCLASSES[state-Component.STATE_COUNT];
        } else {
            return super.getStatePseudoClass(state);
        }
    }

    @Override
    protected void updateFromStyleConfig (int state, StyleConfig.Original config)
    {
        super.updateFromStyleConfig(state, config);

        // check to see if our stylesheet provides us with an icon
        if (state == DEFAULT && config.icon != null) {
            _label.setIcon(config.icon.getIcon(_ctx));
        }

        _feedbackSounds[state] = config.feedbackSound;

        // use the hand cursor if none other defined
        if (state != DISABLED && (_cursor == null)) {
            CursorConfig handCursor = _ctx.getConfigManager().getConfig(CursorConfig.class, "Hand");
            if (handCursor != null) {
                _cursor = handCursor.getCursor(_ctx);
            }
        }
    }

    @Override
    protected void fireAction (long when, int modifiers)
    {
        playFeedbackSound();
        emitEvent(new ActionEvent(this, when, modifiers, _action, _argument));
    }

    /**
     * Plays the feedback sound, if any.
     */
    protected void playFeedbackSound ()
    {
        String sound = getFeedbackSound();
        if (sound != null && isAdded()) {
            Window window = getWindow();
            if (window != null) {
                window.getRoot().playSound(sound);
            }
        }
    }

    protected String[] _feedbackSounds = new String[getStateCount()];

    protected static final int STATE_COUNT = Component.STATE_COUNT + 1;
    protected static final String[] STATE_PCLASSES = { "Down" };
}
