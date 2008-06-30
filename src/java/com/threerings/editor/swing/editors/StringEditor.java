//
// $Id$

package com.threerings.editor.swing.editors;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.threerings.editor.Editable;
import com.threerings.editor.swing.PropertyEditor;

/**
 * Editor for string properties.
 */
public class StringEditor extends PropertyEditor
    implements DocumentListener
{
    // documentation inherited from interface DocumentListener
    public void insertUpdate (DocumentEvent event)
    {
        changedUpdate(null);
    }

    // documentation inherited from interface DocumentListener
    public void removeUpdate (DocumentEvent event)
    {
        changedUpdate(null);
    }

    // documentation inherited from interface DocumentListener
    public void changedUpdate (DocumentEvent event)
    {
        String text = _field.getText();
        if (!_property.get(_object).equals(text)) {
            _property.set(_object, text);
            fireStateChanged();
        }
    }

    @Override // documentation inherited
    public void update ()
    {
        _field.setText((String)_property.get(_object));
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        add(new JLabel(getPropertyLabel() + ":"));
        Editable annotation = _property.getAnnotation();
        add(_field = new JTextField(annotation.width()));
        _field.getDocument().addDocumentListener(this);
    }

    /** The text field. */
    protected JTextField _field;
}
