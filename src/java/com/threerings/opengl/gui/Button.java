//
// $Id$

package com.threerings.opengl.gui;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.background.Background;
import com.threerings.opengl.gui.config.StyleConfig;
import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.icon.Icon;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;

/**
 * Displays a simple button that can be depressed and which generates an action
 * event when pressed and released.
 */
public class Button extends Label
    implements UIConstants
{
    /** Indicates that this button is in the down state. */
    public static final int DOWN = Component.STATE_COUNT + 0;

    /**
     * Creates a button with the specified textual label.
     */
    public Button (GlContext ctx, String text)
    {
        this(ctx, text, "");
    }

    /**
     * Creates a button with the specified label and action. The action
     * will be dispatched via an {@link ActionEvent} when the button is
     * clicked.
     */
    public Button (GlContext ctx, String text, String action)
    {
        this(ctx, text, null, action);
    }

    /**
     * Creates a button with the specified label and action. The action will be
     * dispatched via an {@link ActionEvent} to the specified {@link
     * ActionListener} when the button is clicked.
     */
    public Button (GlContext ctx, String text, ActionListener listener, String action)
    {
        super(ctx, text);
        _action = action;
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
        this(ctx, icon, null, action);
    }

    /**
     * Creates a button with the specified icon and action. The action will be
     * dispatched via an {@link ActionEvent} to the specified {@link
     * ActionListener} when the button is clicked.
     */
    public Button (GlContext ctx, Icon icon, ActionListener listener, String action)
    {
        super(ctx, icon);
        _action = action;
        if (listener != null) {
            addListener(listener);
        }
    }

    /**
     * Configures the action to be generated when this button is clicked.
     */
    public void setAction (String action)
    {
        _action = action;
    }

    /**
     * Returns the action generated when this button is clicked.
     */
    public String getAction ()
    {
        return _action;
    }

    // documentation inherited
    public int getState ()
    {
        int state = super.getState();
        if (state == DISABLED) {
            return state;
        }

        if (_armed && _pressed) {
            return DOWN;
        } else {
            return state; // most likely HOVER
        }
    }

    // documentation inherited
    public boolean dispatchEvent (Event event)
    {
        if (isEnabled() && event instanceof MouseEvent) {
            int ostate = getState();
            MouseEvent mev = (MouseEvent)event;
            switch (mev.getType()) {
            case MouseEvent.MOUSE_ENTERED:
                _armed = _pressed;
                // let the normal component hovered processing take place
                return super.dispatchEvent(event);

            case MouseEvent.MOUSE_EXITED:
                _armed = false;
                // let the normal component hovered processing take place
                return super.dispatchEvent(event);

            case MouseEvent.MOUSE_PRESSED:
                if (mev.getButton() == 0) {
                    _pressed = true;
                    _armed = true;
                } else if (mev.getButton() == 1) {
                    // clicking the right mouse button after arming the
                    // button disarms it
                    _armed = false;
                }
                break;

            case MouseEvent.MOUSE_RELEASED:
                if (_armed && _pressed) {
                    // create and dispatch an action event
                    fireAction(mev.getWhen(), mev.getModifiers());
                    _armed = false;
                }
                _pressed = false;
                break;

            default:
                return super.dispatchEvent(event);
            }

            // update our background image if necessary
            int state = getState();
            if (state != ostate) {
                stateDidChange();
            }

            return true;
        }

        return super.dispatchEvent(event);
    }

    @Override // documentation inherited
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

    @Override // documentation inherited
    protected void updateFromStyleConfig (int state, StyleConfig.Original config)
    {
        super.updateFromStyleConfig(state, config);

        // check to see if our stylesheet provides us with an icon
        if (state == DEFAULT && config.icon != null) {
            _label.setIcon(config.icon.getIcon(_ctx));
        }
    }

    /**
     * Called when the button is "clicked" which may due to the mouse being
     * pressed and released while over the button or due to keyboard
     * manipulation while the button has focus.
     */
    protected void fireAction (long when, int modifiers)
    {
        emitEvent(new ActionEvent(this, when, modifiers, _action));
    }

    protected boolean _armed, _pressed;
    protected String _action;

    protected static final int STATE_COUNT = Component.STATE_COUNT + 1;
    protected static final String[] STATE_PCLASSES = { "Down" };
}
