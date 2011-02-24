//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2011 Three Rings Design, Inc.
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

import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.crowd.chat.data.TellFeedbackMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.event.ChangeEvent;
import com.threerings.opengl.gui.event.ChangeListener;
import com.threerings.opengl.gui.layout.GroupLayout;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.util.GlContext;

/**
 * A chat display for use as a HUD element.
 */
public class ChatOverlay extends Container
    implements ChatDisplay
{
    /**
     * Creates a new chat overlay.
     */
    public ChatOverlay (GlContext ctx)
    {
        super(ctx, GroupLayout.makeHStretch());
        setBundle(MessageManager.GLOBAL_BUNDLE);

        String prefix = "Default/ChatOverlay";
        _area = new TextArea(_ctx);
        _area.setStyleConfig(prefix + "Text");
        final BoundedRangeModel model = _area.getScrollModel();
        model.addChangeListener(new ChangeListener() {
            public void stateChanged (ChangeEvent event) {
                updateButtons();
            }
        });

        Container bcont = GroupLayout.makeVBox(ctx, GroupLayout.BOTTOM);
        add(bcont, GroupLayout.FIXED);
        add(_area);

        // create the action listener for the buttons
        ActionListener al = new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                Object source = event.getSource();
                if (source == _less) {
                    model.setValue(model.getValue() - 1);
                } else if (source == _more) {
                    model.setValue(model.getValue() + 1);
                } else if (source == _end) {
                    model.setValue(model.getMaximum() - model.getExtent());
                }
            }
        };

        _less = new Button(_ctx, "", al, "less");
        _less.setStyleConfig(prefix + "Less");
        _less.setEnabled(false);
        bcont.add(_less);

        _more = new Button(_ctx, "", al, "more");
        _more.setStyleConfig(prefix + "More");
        _more.setEnabled(false);
        bcont.add(_more);

        _end = new Button(_ctx, "", al, "end");
        _end.setStyleConfig(prefix + "End");
        _end.setEnabled(false);
        bcont.add(_end);
    }

    /**
     * Sets the bundle to use to translate chat messsages.
     */
    public void setBundle (String bundle)
    {
        _msgs = _ctx.getMessageManager().getBundle(bundle);
    }

    /**
     * Sets the preferred width for the text area.
     */
    public void setPreferredWidth (int width)
    {
        _area.setPreferredWidth(width);
    }

    /**
     * Sets the colors to use for system chat messages.
     */
    public void setSystemColors (Color4f info, Color4f feedback, Color4f attention)
    {
        _systemColors[SystemMessage.INFO] = info;
        _systemColors[SystemMessage.FEEDBACK] = feedback;
        _systemColors[SystemMessage.ATTENTION] = attention;
    }

    // documentation inherited from interface ChatDisplay
    public void clear ()
    {
        _area.clearText();
    }

    // documentation inherited from interface ChatDisplay
    public boolean displayMessage (ChatMessage msg, boolean alreadyDisplayed)
    {
        String format = msg.getFormat();
        String text = msg.message;
        if (format != null && msg instanceof UserMessage) {
            UserMessage umsg = (UserMessage)msg;
            text = _msgs.get(format, umsg.getSpeakerDisplayName(), text);
        }
        BoundedRangeModel model = _area.getScrollModel();
        boolean end = (model.getValue() == model.getMaximum() - model.getExtent());
        appendMessage(msg, text + "\n", getColor(msg));
        if (end) {
            _area.validate();
            model.setValue(model.getMaximum() - model.getExtent());
        }
        return true;
    }

    @Override // documentation inherited
    public void setEnabled (boolean enabled)
    {
        super.setEnabled(enabled);
        updateButtons();
    }

    @Override // documentation inherited
    public Component getHitComponent (int mx, int my)
    {
        Component comp = super.getHitComponent(mx, my);
        return ((comp == _less || comp == _more || comp == _end) && comp.isEnabled()) ?
            comp : null;
    }

    @Override // documentation inherited
    public void validate ()
    {
        // If the overlay is resized, ensure we maintain our end state
        BoundedRangeModel model = _area.getScrollModel();
        boolean end = (model.getValue() == model.getMaximum() - model.getExtent());
        super.validate();
        if (end) {
            model.setValue(model.getMaximum() - model.getExtent());
        }
    }

    /**
     * Returns the color to use for the supplied message, or <code>null</code> for the default.
     */
    protected Color4f getColor (ChatMessage msg)
    {
        if (msg instanceof SystemMessage) {
            return _systemColors[((SystemMessage)msg).attentionLevel];
        } else if (msg instanceof TellFeedbackMessage) {
            return _systemColors[SystemMessage.FEEDBACK];
        } else {
            return null;
        }
    }

    /**
     * Appends a message to the text area.
     *
     * @param text the formatted, newline-terminated text of the message.
     * @param color the color to use for the message, or null for the default.
     */
    protected void appendMessage (ChatMessage msg, String text, Color4f color)
    {
        _area.appendText(text, color);
    }

    /**
     * Updates the enabled status of the buttons.
     */
    protected void updateButtons ()
    {
        if (!isEnabled()) {
            _less.setEnabled(false);
            _more.setEnabled(false);
            _end.setEnabled(false);
            return;
        }
        BoundedRangeModel model = _area.getScrollModel();
        int value = model.getValue();
        _less.setEnabled(value > model.getMinimum());
        boolean more = value + model.getExtent() < model.getMaximum();
        _more.setEnabled(more);
        _end.setEnabled(more);
    }

    @Override
    protected void wasAdded ()
    {
        super.wasAdded();

        // there was a bug where chat would not scroll into view when we weren't on screen.
        // This seems to fix it.
        _area.invalidate();
    }

    /** The bundle we use to translate messages. */
    protected MessageBundle _msgs;

    /** The colors to use for each system message level. */
    protected Color4f[] _systemColors = new Color4f[3];

    /** The less, more, and end buttons. */
    protected Button _less, _more, _end;

    /** The chat entry text area. */
    protected TextArea _area;
}
