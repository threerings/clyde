//
// $Id$

package com.threerings.editor.swing.editors;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

import com.threerings.editor.swing.PropertyEditor;

/**
 * Editor for boolean properties.
 */
public class BooleanEditor extends PropertyEditor
    implements ActionListener
{
    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        Boolean selected = _box.isSelected();
        if (!_property.get(_object).equals(selected)) {
            _property.set(_object, selected);
            fireStateChanged();
        }
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        add(_box = new JCheckBox(getPropertyLabel()));
        _box.setBackground(null);
        Dimension size = _box.getPreferredSize();
        _box.setPreferredSize(new Dimension(size.width, 16));
        _box.addActionListener(this);
    }

    @Override // documentation inherited
    protected void update ()
    {
        _box.setSelected((Boolean)_property.get(_object));
    }

    /** The check box. */
    protected JCheckBox _box;
}
