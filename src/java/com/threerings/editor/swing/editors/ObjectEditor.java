//
// $Id$

package com.threerings.editor.swing.editors;

import javax.swing.BorderFactory;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.editor.swing.ObjectPanel;
import com.threerings.editor.swing.PropertyEditor;

/**
 * An editor for objects with editable properties.
 */
public class ObjectEditor extends PropertyEditor
    implements ChangeListener
{
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        _property.set(_object, _panel.getValue());
        fireStateChanged();
    }

    @Override // documentation inherited
    public void update ()
    {
        _panel.setValue(_property.get(_object));
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        setBorder(BorderFactory.createTitledBorder(getPropertyLabel()));
        add(_panel = new ObjectPanel(
            _ctx, _property.getTypeLabel(), _property.getSubtypes(), _lineage));
        _panel.addChangeListener(this);
    }

    /** The object panel. */
    protected ObjectPanel _panel;
}
