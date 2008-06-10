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

import com.threerings.math.Transform;

import com.threerings.editor.Editable;
import com.threerings.editor.swing.PropertyEditor;
import com.threerings.editor.swing.TransformPanel;

/**
 * Editor for transform properties.
 */
public class TransformEditor extends PropertyEditor
    implements ChangeListener
{
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        Transform value = _panel.getValue();
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
        TransformPanel.Mode mode = TransformPanel.Mode.UNIFORM;
        try {
            mode = Enum.valueOf(TransformPanel.Mode.class, StringUtil.toUSUpperCase(mstr));
        } catch (IllegalArgumentException e) { }
        add(_panel = new TransformPanel(_msgs, mode, (float)getStep(), (float)getScale()));
        _panel.setBackground(getDarkerBackground(_lineage.length));
        Color ddarker = getDarkerBackground(_lineage.length + 1);
        _panel.getTranslationPanel().setBackground(ddarker);
        _panel.getRotationPanel().setBackground(ddarker);
        _panel.addChangeListener(this);
    }

    @Override // documentation inherited
    protected void update ()
    {
        _panel.setValue((Transform)_property.get(_object));
    }

    /** The transform panel. */
    protected TransformPanel _panel;
}
