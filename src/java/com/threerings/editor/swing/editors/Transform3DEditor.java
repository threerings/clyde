//
// $Id$

package com.threerings.editor.swing.editors;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.util.StringUtil;

import com.threerings.math.Transform3D;

import com.threerings.editor.Editable;
import com.threerings.editor.swing.PropertyEditor;
import com.threerings.editor.swing.Transform3DPanel;

/**
 * Editor for transform properties.
 */
public class Transform3DEditor extends PropertyEditor
    implements ChangeListener
{
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        Transform3D value = _panel.getValue();
        if (!_property.get(_object).equals(value)) {
            _property.set(_object, value);
            fireStateChanged();
        }
    }

    @Override // documentation inherited
    public void update ()
    {
        _panel.setValue((Transform3D)_property.get(_object));
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        setBorder(BorderFactory.createTitledBorder(getPropertyLabel()));
        Editable annotation = _property.getAnnotation();
        String mstr = getMode();
        Transform3DPanel.Mode mode = Transform3DPanel.Mode.UNIFORM;
        try {
            mode = Enum.valueOf(Transform3DPanel.Mode.class, StringUtil.toUSUpperCase(mstr));
        } catch (IllegalArgumentException e) { }
        add(_panel = new Transform3DPanel(_msgs, mode, (float)getStep(), (float)getScale()));
        _panel.setBackground(getDarkerBackground(_lineage.length));
        Color ddarker = getDarkerBackground(_lineage.length + 1);
        _panel.getTranslationPanel().setBackground(ddarker);
        _panel.getRotationPanel().setBackground(ddarker);
        _panel.addChangeListener(this);
    }

    /** The transform panel. */
    protected Transform3DPanel _panel;
}
