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

import java.util.Arrays;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

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
        Object value = getValues().get(_box.getSelectedIndex());
        if (!Objects.equal(_property.get(_object), value)) {
            _property.set(_object, value);
            fireStateChanged();
        }
    }

    @Override
    public void update ()
    {
        _box.setSelectedIndex(getValues().indexOf(_property.get(_object)));
    }

    @Override
    protected void didInit ()
    {
        add(new JLabel(getPropertyLabel() + ":"));
        final MessageBundle msgs =
            _msgmgr.getBundle(Introspector.getMessageBundle(_property.getType()));
        Object[] labels = Lists.transform(getValues(), new Function<Enum<?>, String>() {
            public String apply (Enum<?> value) {
                return getLabel(value, msgs);
            }
        }).toArray();
        add(_box = new JComboBox(labels));
        _box.addActionListener(this);
    }

    /**
     * Get the valid values for this enum property, which may or may not include null.
     */
    protected List<Enum<?>> getValues ()
    {
        Enum<?>[] constants = (Enum<?>[])_property.getType().getEnumConstants();
        return _property.nullable()
            ? Lists.asList(null, constants)
            : Arrays.asList(constants);
    }

    /** The combo box. */
    protected JComboBox _box;
}
