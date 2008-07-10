//
// $Id$

package com.threerings.editor.swing.editors;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BorderFactory;
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
        if (event.getSource() == _class) {
            populateColor(((ClassItem)_class.getSelectedItem()).record);
        }
        Integer print = ((ColorItem)_color.getSelectedItem()).record.getColorPrint();
        if (!_property.get(_object).equals(print)) {
            _property.set(_object, print);
            fireStateChanged();
        }
    }

    @Override // documentation inherited
    public void update ()
    {
        int print = (Integer)_property.get(_object);
        ColorRecord color = _ctx.getColorPository().getColorRecord(print >> 8, print & 0xFF);
        if (color == null) {
            if (_class != null) {
                _class.setSelectedIndex(0);
            }
            _color.setSelectedIndex(0);

        } else {
            if (_class != null) {
                _class.setSelectedItem(new ClassItem(color.cclass));
            }
            _color.setSelectedItem(new ColorItem(color));
        }
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        String mode = getMode();
        if (mode.length() > 0) {
            add(new JLabel(getPropertyLabel() + ":"));
            add(_color = new JComboBox());
            populateColor(_ctx.getColorPository().getClassRecord(mode));
            _color.addActionListener(this);
        } else {
            setBorder(BorderFactory.createTitledBorder(getPropertyLabel()));
            setLayout(new HGroupLayout(
                GroupLayout.STRETCH, GroupLayout.NONE, 5, GroupLayout.CENTER));
            JPanel p1 = new JPanel();
            p1.setBackground(null);
            add(p1);
            p1.add(new JLabel(_msgs.get("m.class") + ":"));
            ArrayList<ClassItem> classes = new ArrayList<ClassItem>();
            for (Iterator it = _ctx.getColorPository().enumerateClasses(); it.hasNext(); ) {
                classes.add(new ClassItem((ClassRecord)it.next()));
            }
            QuickSort.sort(classes);
            p1.add(_class = new JComboBox(classes.toArray(new ClassItem[classes.size()])));
            _class.addActionListener(this);
            JPanel p2 = new JPanel();
            p2.setBackground(null);
            add(p2);
            p2.add(new JLabel(_msgs.get("m.color") + ":"));
            p2.add(_color = new JComboBox());
            _color.addActionListener(this);
        }
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

        @Override // documentation inherited
        public String toString ()
        {
            return record.name;
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return record == ((ClassItem)other).record;
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

        @Override // documentation inherited
        public String toString ()
        {
            return record.name;
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return record == ((ColorItem)other).record;
        }
    }

    /** The combo boxes. */
    protected JComboBox _class, _color;
}
