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

import java.util.Arrays;
import java.util.List;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import com.samskivert.swing.CollapsiblePanel;
import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.editor.Introspector;

import static com.threerings.editor.Log.log;

/**
 * An editor for arrays of lists of enums.
 */
public class EnumPanelArrayListEditor extends PanelArrayListEditor
{
    @Override
    protected void updatePanel (EntryPanel panel, Object value)
    {
        JComboBox box = ((EnumEntryPanel)panel).getBox();
        box.setSelectedIndex(getValues().indexOf(value));
    }

    @Override
    protected void addPanel (Object value)
    {
        _panels.add(new EnumEntryPanel(value));
    }

    /**
     * Get the valid values for this enum property, which may or may not include null.
     */
    protected List<Enum<?>> getValues ()
    {
        return Arrays.asList((Enum<?>[])getEnumType().getEnumConstants());
    }

    protected Class<?> getEnumType ()
    {
        Class<?> ctype = _property.getComponentType();
        if (ctype.isArray()) {
            ctype = ctype.getComponentType();
        }
        return ctype;
    }

    /**
     * Called when a enum is updated.
     */
    protected void boxUpdated (JComboBox box)
    {
        int idx = ((EntryPanel)box.getParent().getParent()).getIndex();
        setValue(idx, getValues().get(box.getSelectedIndex()));
        fireStateChanged(true);
    }

    /**
     * A panel for an enum entry.
     */
    protected class EnumEntryPanel extends EntryPanel
    {
        public EnumEntryPanel (Object value)
        {
            super(value);
        }

        public JComboBox getBox ()
        {
            return _box;
        }

        @Override
        public String getComponentPath (Component comp, boolean mouse)
        {
            return "";
        }

        @Override
        protected JPanel createPanel (Object value)
        {
            JPanel panel = new JPanel(new VGroupLayout(
                        GroupLayout.NONE, GroupLayout.CONSTRAIN, 5, GroupLayout.TOP));
            final MessageBundle msgs =
                _msgmgr.getBundle(Introspector.getMessageBundle(getEnumType()));
            List<Enum<?>> values = getValues();
            Object[] labels = Lists.transform(values, new Function<Enum<?>, String>() {
                public String apply (Enum<?> value) {
                    return getLabel(value, msgs);
                }
            }).toArray();
            panel.add(_box = new JComboBox(labels));
            _box.setSelectedIndex(values.indexOf(value));
            _box.addActionListener(new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    boxUpdated((JComboBox)event.getSource());
                }
            });

            return panel;
        }

        protected JComboBox _box;
    }
}
