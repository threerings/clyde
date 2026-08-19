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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

import java.util.Objects;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.ListCellRenderer;

import com.samskivert.util.StringUtil;

import com.threerings.editor.swing.PropertyEditor;

import static com.threerings.editor.Log.log;

/**
 * Provides a means of selecting between several different objects.
 */
public class ChoiceEditor extends PropertyEditor
  implements ActionListener
{
  // documentation inherited from interface ActionListener
  public void actionPerformed (ActionEvent event)
  {
    Object selected = _box.getSelectedItem();
    if (!Objects.equals(_property.get(_object), selected)) {
      _property.set(_object, selected);
      fireStateChanged();
    }
  }

  @Override
  public void update ()
  {
    Object[] options = getOptions();
    _box.setRenderer(getPropertyRenderer(options.getClass().getComponentType()));
    _box.setModel(new DefaultComboBoxModel<Object>(options));
    _box.setSelectedItem(_property.get(_object));
  }

  @Override
  protected void didInit ()
  {
    add(new JLabel(getPropertyLabel() + ":"));
    add(_box = new JComboBox<>());
    _renderer = _box.getRenderer();
    _box.addActionListener(this);
  }

  /**
   * Returns the array of options available for selection.
   */
  protected Object[] getOptions ()
  {
    Object mobj = _property.getMemberObject(_object);
    if (mobj == null) {
      return new Object[0];
    }
    Class<?> mclass = mobj.getClass();
    Member member = _property.getMember();
    String mname = member.getName();
    mname = (member instanceof Method) ? mname.substring(3) : StringUtil.capitalize(mname);
    try {
      return (Object[])mclass.getMethod("get" + mname + "Options").invoke(mobj);
    } catch (NoSuchMethodException nsme) {
      // fall through
    } catch (Exception e) {
      log.warning("Error retrieving options.", "class", mclass, "member", mname, e);
    }
    return new Object[0];
  }

  /**
   * Get the combobox renderer to use with the property we've got.
   */
  protected ListCellRenderer<Object> getPropertyRenderer (Class<?> componentType)
  {
    Object mobj = _property.getMemberObject(_object);
    if (mobj != null) {
      Class<?> mclass = mobj.getClass();
      Member member = _property.getMember();
      String mname = member.getName();
      mname = (member instanceof Method) ? mname.substring(3) : StringUtil.capitalize(mname);

      for (; componentType != null; componentType = componentType.getSuperclass()) {
        try {
          var method = mclass.getMethod("format" + mname + "Option", componentType);
          if (method != null) return (list, value, index, isSelected, cellHasFocus) -> {
            try {
              value = method.invoke(mobj, value);
            } catch (Exception e) {
              log.warning("Oh no: " + e);
            }
            return _renderer.getListCellRendererComponent(
              list, value, index, isSelected, cellHasFocus);
          };
        } catch (NoSuchMethodException nsme) {
          // fall through
        } catch (Exception e) {
          log.warning("Error retrieving formatter?.", "class", mclass, "member", mname, e);
        }
      }
    }
    // just use the default
    return _renderer;
  }

  /** The combo box. */
  protected JComboBox<Object> _box;

  /** The default combobox cell renderer. */
  protected ListCellRenderer<Object> _renderer;
}
