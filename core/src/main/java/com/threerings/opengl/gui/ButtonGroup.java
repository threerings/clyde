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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;

import static com.threerings.opengl.gui.Log.log;

/**
 * Manages a group of {@link ToggleButton}s, ensuring that only one is selected at any given time.
 */
public class ButtonGroup
  implements ActionListener, Selectable<ToggleButton>, Iterable<ToggleButton>
{
  /**
   * Creates a new button group in which one button is always selected.
   */
  public ButtonGroup ()
  {
  }

  /**
   * Creates a new button group in which one button is always selected.
   * @param buttons the buttons in the group.
   */
  public ButtonGroup (ToggleButton... buttons)
  {
    addAll(Arrays.asList(buttons));
  }

  /**
   * Sets the always select state. If set to true and no button is selected, it will
   * automatically select the first button in the group.
   */
  public ButtonGroup setAlwaysSelect (boolean alwaysSelect)
  {
    _alwaysSelect = alwaysSelect;
    if (_alwaysSelect && _selected == null && !_buttons.isEmpty()) {
      setSelected(_buttons.get(0));
    }
    return this;
  }

  /**
   * Adds all the specified buttons.
   */
  public ButtonGroup addAll (Iterable<ToggleButton> buttons)
  {
    for (ToggleButton but : buttons) add(but);
    return this;
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
    _buttons.add(button);
    if (_selected != null) button.setSelected(false);
    else if (_alwaysSelect || button.isSelected()) {
      button.setSelected(true);
      selectionChanged(button, 0L, 0);
    }
    button.addListener(this);
  }

  /**
   * Removes a button from the group.
   */
  public void remove (ToggleButton button)
  {
    _buttons.remove(button);
    button.removeListener(this);
    if (_selected == button) {
      ToggleButton newSelect = _alwaysSelect && !_buttons.isEmpty() ? _buttons.get(0) : null;
      setSelected(newSelect);
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
    if (_selected != null) selectionChanged(null, 0L, 0);
  }

  /**
   * Returns the number of buttons in the group.
   */
  public int getButtonCount ()
  {
    return _buttons.size();
  }

  // from Selectable<ToggleButton>
  public ToggleButton getSelected ()
  {
    return _selected;
  }

  // from Selectable<ToggleButton>
  public void setSelected (ToggleButton button)
  {
    if (button != _selected) selectionChanged(button, 0L, 0);
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
    return _selected == null ? -1 : _buttons.indexOf(_selected);
  }

  // documentation inherited from interface ActionListener
  public void actionPerformed (ActionEvent event)
  {
    ToggleButton button = (ToggleButton)event.getSource();
    if (!button.isSelected()) {
      if (_alwaysSelect) {
        // can only unselect by selecting another button
        button.setSelected(true);
        return;
      }
      button = null; // inform the group we've unselected..
    }
    selectionChanged(button, event.getWhen(), event.getModifiers());
  }

  /**
   * Updates the states of the unselected buttons and fires a selection event.
   */
  protected void selectionChanged (ToggleButton selected, long when, int modifiers)
  {
    _selected = selected;
    for (int ii = 0, nn = _buttons.size(); ii < nn; ii++) {
      ToggleButton button = _buttons.get(ii);
      button.setSelected(button == selected);
    }
    int nlist = _listeners.size();
    if (nlist > 0) {
      ActionEvent event = new ActionEvent(this, when, modifiers, SELECT, selected);
      for (int ii = 0; ii < nlist; ii++) {
        event.dispatch(_listeners.get(ii));
      }
    }
  }

  /** If true and we have at least one button, one must always be selected. */
  protected boolean _alwaysSelect = true;

  /** The buttons in the group. */
  protected List<ToggleButton> _buttons = Lists.newArrayList();

  /** Listeners for action events. */
  protected List<ActionListener> _listeners = Lists.newArrayList();

  /** The currently selected button. */
  protected ToggleButton _selected;
}
