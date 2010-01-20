//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

import javax.swing.JComboBox;
import javax.swing.JLabel;

import com.samskivert.util.ListUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.editor.Introspector;
import com.threerings.editor.swing.PropertyEditor;

/**
 * Editor for enumerated type properties.
 */
public class EnumEditor extends PropertyEditor
    implements ActionListener
{
    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        Object value = _property.getType().getEnumConstants()[_box.getSelectedIndex()];
        if (!_property.get(_object).equals(value)) {
            _property.set(_object, value);
            fireStateChanged();
        }
    }

    @Override // documentation inherited
    public void update ()
    {
        _box.setSelectedIndex(ListUtil.indexOf(
            _property.getType().getEnumConstants(), _property.get(_object)));
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        add(new JLabel(getPropertyLabel() + ":"));
        Class type = _property.getType();
        MessageBundle msgs = _msgmgr.getBundle(Introspector.getMessageBundle(type));
        Enum[] constants = (Enum[])type.getEnumConstants();
        String[] names = new String[constants.length];
        for (int ii = 0; ii < constants.length; ii++) {
            names[ii] = getLabel(StringUtil.toUSLowerCase(constants[ii].name()), msgs);
        }
        add(_box = new JComboBox(names));
        _box.addActionListener(this);
    }

    /** The combo box. */
    protected JComboBox _box;
}
