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

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;

/**
 * Manages a group of {@link ToggleButton}s, ensuring that only one is selected at any given time.
 */
public class ButtonGroup
    implements ActionListener, Selectable<ToggleButton>, Iterable<ToggleButton>
{
    /**
     * Creates a new button group in which one button is always selected.
     *
     * @param buttons the buttons in the group.
     */
    public ButtonGroup (ToggleButton... buttons)
    {
        this(true, buttons);
    }

    /**
     * Creates a new button group.
     *
     * @param alwaysSelect if true and the group contains at least one button, one button must
     * always be selected.
     * @param buttons the buttons in the group.
     */
    public ButtonGroup (boolean alwaysSelect, ToggleButton... buttons)
    {
        _alwaysSelect = alwaysSelect;
        for (ToggleButton button : buttons) {
            add(button);
        }
    }

    /**
     * Adds a listener for selection changes.
     */
    public void addListener (ActionListener listener)
    {
        _listeners.add(listener);
    }

    /**
     * Removes a selection change listener.
     */
    public void removeListener (ActionListener listener)
    {
        _listeners.remove(listener);
    }

    /**
     * Adds a button to the group.
     */
    public void add (ToggleButton button)
    {
        if (getSelectedIndex() != -1) {
            button.setSelected(false);
        } else if (_alwaysSelect) {
            button.setSelected(true);
        }
        _buttons.add(button);
        button.addListener(this);
    }

    /**
     * Removes a button from the group.
     */
    public void remove (ToggleButton button)
    {
        _buttons.remove(button);
        button.removeListener(this);
        if (_alwaysSelect && button.isSelected() && !_buttons.isEmpty()) {
            _buttons.get(0).setSelected(true);
        }
    }

    /**
     * Removes all of the buttons in the group.
     */
    public void removeAll ()
    {
        for (int ii = 0, nn = _buttons.size(); ii < nn; ii++) {
            _buttons.get(ii).removeListener(this);
        }
        _buttons.clear();
    }

    /**
     * Returns the number of buttons in the group.
     */
    public int getButtonCount ()
    {
        return _buttons.size();
    }

    /**
     * Sets the always select state. If set to true and no button is selected, it will
     * automatically select the first button in the group.
     */
    public void setAlwaysSelect (boolean alwaysSelect)
    {
        _alwaysSelect = alwaysSelect;
        if (_alwaysSelect && getSelectedIndex() == -1 && getButtonCount() > 0) {
            setSelected(_buttons.get(0));
        }
    }

    // from Selectable<ToggleButton>
    public ToggleButton getSelected ()
    {
        int idx = getSelectedIndex();
        return (idx == -1) ? null : _buttons.get(idx);
    }

    // from Selectable<ToggleButton>
    public void setSelected (ToggleButton button)
    {
        if (!button.isSelected()) {
            button.setSelected(true);
            selectionChanged(button, 0L, 0);
        }
    }

    // from Iterable<ToggleButton>
    public Iterator<ToggleButton> iterator ()
    {
        return Iterators.unmodifiableIterator(_buttons.iterator());
    }

    /**
     * Returns the button at the specified index.
     */
    public ToggleButton getButton (int idx)
    {
        return _buttons.get(idx);
    }

    // from Selectable<ToggleButton>
    public void setSelectedIndex (int idx)
    {
        setSelected(_buttons.get(idx));
    }

    // from Selectable<ToggleButton>
    public int getSelectedIndex ()
    {
        for (int ii = 0, nn = _buttons.size(); ii < nn; ii++) {
            if (_buttons.get(ii).isSelected()) {
                return ii;
            }
        }
        return -1;
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        ToggleButton button = (ToggleButton)event.getSource();
        if (button.isSelected()) {
            selectionChanged(button, event.getWhen(), event.getModifiers());

        } else if (_alwaysSelect) {
            // can only unselect by selecting another button
            button.setSelected(true);
        }
    }

    /**
     * Updates the states of the unselected buttons and fires a selection event.
     */
    protected void selectionChanged (ToggleButton selected, long when, int modifiers)
    {
        for (int ii = 0, nn = _buttons.size(); ii < nn; ii++) {
            ToggleButton button = _buttons.get(ii);
            if (button != selected) {
                button.setSelected(false);
            }
        }
        ActionEvent event = new ActionEvent(this, when, modifiers, SELECT, selected);
        for (int ii = 0, nn = _listeners.size(); ii < nn; ii++) {
            event.dispatch(_listeners.get(ii));
        }
    }

    /** If true and we have at least one button, one must always be selected. */
    protected boolean _alwaysSelect;

    /** The buttons in the group. */
    protected List<ToggleButton> _buttons = Lists.newArrayList();

    /** Listeners for action events. */
    protected List<ActionListener> _listeners = Lists.newArrayList();
}
