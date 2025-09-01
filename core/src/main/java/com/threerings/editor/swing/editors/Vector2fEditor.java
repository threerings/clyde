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

package com.threerings.editor.swing.editors;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.util.StringUtil;

import com.threerings.math.Vector2f;

import com.threerings.editor.Editable;
import com.threerings.editor.swing.PropertyEditor;
import com.threerings.editor.swing.Vector2fPanel;

/**
 * Editor for vector properties.
 */
public class Vector2fEditor extends PropertyEditor
  implements ChangeListener
{
  // documentation inherited from interface ChangeListener
  public void stateChanged (ChangeEvent event)
  {
    Vector2f value = _panel.getValue();
    if (!_property.get(_object).equals(value)) {
      _property.set(_object, value);
      fireStateChanged();
    }
  }

  @Override
  public void update ()
  {
    _panel.setValue((Vector2f)_property.get(_object));
  }

  @Override
  protected void didInit ()
  {
    setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
    setTitle(getPropertyLabel());
    String mstr = getMode();
    Vector2fPanel.Mode mode = Vector2fPanel.Mode.CARTESIAN;
    try {
      mode = Enum.valueOf(Vector2fPanel.Mode.class, StringUtil.toUSUpperCase(mstr));
    } catch (IllegalArgumentException e) { }
    add(_panel = new Vector2fPanel(_msgs, mode, (float)getStep(), (float)getScale()));
    _panel.setBackground(getDarkerBackground(_lineage.length));
    _panel.addChangeListener(this);
  }

  /** The vector panel. */
  protected Vector2fPanel _panel;
}
