//
// $Id$

package com.threerings.editor.swing.editors;

import javax.swing.BorderFactory;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.util.StringUtil;

import com.threerings.math.Vector3f;

import com.threerings.editor.Editable;
import com.threerings.editor.swing.PropertyEditor;
import com.threerings.editor.swing.Vector3fPanel;

/**
 * Editor for vector properties.
 */
public class Vector3fEditor extends PropertyEditor
    implements ChangeListener
{
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        Vector3f value = _panel.getValue();
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
        Editable annotation = _property.getAnnotation();
        String mstr = getMode();
        Vector3fPanel.Mode mode = Vector3fPanel.Mode.CARTESIAN;
        try {
            mode = Enum.valueOf(Vector3fPanel.Mode.class, StringUtil.toUSUpperCase(mstr));
        } catch (IllegalArgumentException e) { }
        add(_panel = new Vector3fPanel(_msgs, mode, (float)getStep(), (float)getScale()));
        _panel.setBackground(getDarkerBackground(_lineage.length));
        _panel.addChangeListener(this);
    }

    @Override // documentation inherited
    protected void update ()
    {
        _panel.setValue((Vector3f)_property.get(_object));
    }

    /** The vector panel. */
    protected Vector3fPanel _panel;
}
