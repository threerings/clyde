//
// $Id$

package com.threerings.editor.swing.editors;

import javax.swing.BorderFactory;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.math.Quaternion;

import com.threerings.editor.swing.PropertyEditor;
import com.threerings.editor.swing.QuaternionPanel;

/**
 * Editor for quaternion properties.
 */
public class QuaternionEditor extends PropertyEditor
    implements ChangeListener
{
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        Quaternion value = _panel.getValue();
        if (!_property.get(_object).equals(value)) {
            _property.set(_object, value);
            fireStateChanged();
        }
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        setBorder(BorderFactory.createTitledBorder(getPropertyLabel()));
        add(_panel = new QuaternionPanel(_msgs));
        _panel.setBackground(getDarkerBackground(_lineage.length));
        _panel.addChangeListener(this);
    }

    @Override // documentation inherited
    protected void update ()
    {
        _panel.setValue((Quaternion)_property.get(_object));
    }

    /** The quaternion panel. */
    protected QuaternionPanel _panel;
}
