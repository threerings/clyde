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

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.HGroupLayout;
import com.samskivert.util.QuickSort;

import com.threerings.media.image.ColorPository.ClassRecord;
import com.threerings.media.image.ColorPository.ColorRecord;

import com.threerings.editor.swing.PropertyEditor;

/**
 * Edits colorization reference properties.
 */
public class ColorizationEditor extends PropertyEditor
    implements ActionListener
{
    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        Integer value;
        if (_color == null) {
            value = ((ClassItem)_class.getSelectedItem()).record.classId;
        } else {
            if (event.getSource() == _class) {
                populateColor(((ClassItem)_class.getSelectedItem()).record);
            }
            value = ((ColorItem)_color.getSelectedItem()).record.getColorPrint();
        }
        if (!_property.get(_object).equals(value)) {
            _property.set(_object, value);
            fireStateChanged();
        }
    }

    @Override
    public void update ()
    {
        int value = (Integer)_property.get(_object);
        if (_color == null) {
            ClassRecord clazz = _ctx.getColorPository().getClassRecord(value);
            if (clazz == null) {
                _class.setSelectedIndex(0);
            } else {
                _class.setSelectedItem(new ClassItem(clazz));
            }
            return;
        }
        ColorRecord color = _ctx.getColorPository().getColorRecord(value >> 8, value & 0xFF);
        if (color == null) {
            if (_class != null) {
                // because we're setting two things, we need to avoid
                // responding to the first (incomplete) change
                _class.removeActionListener(this);
                try {
                    _class.setSelectedIndex(0);
                } finally {
                    _class.addActionListener(this);
                }
                populateColor(((ClassItem)_class.getSelectedItem()).record);
            }
            _color.setSelectedIndex(0);

        } else {
            if (_class != null) {
                _class.removeActionListener(this);
                try {
                    _class.setSelectedItem(new ClassItem(color.cclass));
                } finally {
                    _class.addActionListener(this);
                }
                populateColor(color.cclass);
            }
            _color.setSelectedItem(new ColorItem(color));
        }
    }

    @Override
    protected void didInit ()
    {
        String mode = getMode();
        if (mode.equals("class")) {
            add(new JLabel(getPropertyLabel() + ":"));
            add(_class = createClassBox());

        } else if (mode.length() > 0) {
            add(new JLabel(getPropertyLabel() + ":"));
            add(_color = new JComboBox());
            populateColor(_ctx.getColorPository().getClassRecord(mode));
            _color.addActionListener(this);

        } else {
            setTitle(getPropertyLabel());
            setLayout(new HGroupLayout(
                GroupLayout.STRETCH, GroupLayout.NONE, 5, GroupLayout.CENTER));
            JPanel p1 = new JPanel();
            p1.setBackground(null);
            add(p1);
            p1.add(new JLabel(_msgs.get("m.class") + ":"));
            p1.add(_class = createClassBox());
            JPanel p2 = new JPanel();
            p2.setBackground(null);
            add(p2);
            p2.add(new JLabel(_msgs.get("m.color") + ":"));
            p2.add(_color = new JComboBox());
            _color.addActionListener(this);
        }
    }

    /**
     * Creates and returns the class combo box.
     */
    protected JComboBox createClassBox ()
    {
        ArrayList<ClassItem> classes = new ArrayList<ClassItem>();
        for (ClassRecord rec : _ctx.getColorPository().getClasses()) {
            classes.add(new ClassItem(rec));
        }
        QuickSort.sort(classes);
        JComboBox box = new JComboBox(classes.toArray(new ClassItem[classes.size()]));
        box.addActionListener(this);
        return box;
    }

    /**
     * Populates the color box with the colors in the identified class.
     */
    protected void populateColor (ClassRecord crec)
    {
        ArrayList<ColorItem> colors = new ArrayList<ColorItem>();
        for (Object color : crec.colors.values()) {
            colors.add(new ColorItem((ColorRecord)color));
        }
        QuickSort.sort(colors);
        _color.setModel(new DefaultComboBoxModel(colors.toArray(new ColorItem[colors.size()])));
    }

    /**
     * A colorization class for use in a combo box model.
     */
    protected static class ClassItem
        implements Comparable<ClassItem>
    {
        /** The contained record. */
        public ClassRecord record;

        public ClassItem (ClassRecord record)
        {
            this.record = record;
        }

        // documentation inherited from interface Comparable
        public int compareTo (ClassItem other)
        {
            return record.classId - other.record.classId;
        }

        @Override
        public String toString ()
        {
            return record.name;
        }

        @Override
        public boolean equals (Object other)
        {
            return record == ((ClassItem)other).record;
        }

        @Override
        public int hashCode ()
        {
            return record != null ? record.hashCode() : 0;
        }
    }

    /**
     * A colorization color for use in a combo box model.
     */
    protected static class ColorItem
        implements Comparable<ColorItem>
    {
        /** The contained record. */
        public ColorRecord record;

        public ColorItem (ColorRecord record)
        {
            this.record = record;
        }

        // documentation inherited from interface Comparable
        public int compareTo (ColorItem other)
        {
            return record.colorId - other.record.colorId;
        }

        @Override
        public String toString ()
        {
            return record.name;
        }

        @Override
        public boolean equals (Object other)
        {
            return record == ((ColorItem)other).record;
        }

        @Override
        public int hashCode ()
        {
            return record != null ? record.hashCode() : 0;
        }
    }

    /** The combo boxes. */
    protected JComboBox _class, _color;
}
