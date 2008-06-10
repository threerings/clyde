//
// $Id$

package com.threerings.editor.swing.editors;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import com.samskivert.util.ListUtil;
import com.samskivert.util.StringUtil;

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
    protected void didInit ()
    {
        add(new JLabel(getPropertyLabel() + ":"));
        Enum[] constants = (Enum[])_property.getType().getEnumConstants();
        String[] names = new String[constants.length];
        for (int ii = 0; ii < constants.length; ii++) {
            names[ii] = getLabel(StringUtil.toUSLowerCase(constants[ii].name()));
        }
        add(_box = new JComboBox(names));
        _box.addActionListener(this);
    }

    @Override // documentation inherited
    protected void update ()
    {
        _box.setSelectedIndex(ListUtil.indexOf(
            _property.getType().getEnumConstants(), _property.get(_object)));
    }

    /** The combo box. */
    protected JComboBox _box;
}
