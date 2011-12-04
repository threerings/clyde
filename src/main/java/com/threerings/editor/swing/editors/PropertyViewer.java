//
// $Id$

package com.threerings.editor.swing.editors;

import java.text.DateFormat;

/**
 * A view-only property editor.
 */
public class PropertyViewer extends StringEditor
{
    @Override
    public void update ()
    {
        _field.setText(valueToString(_property.get(_object)));
    }

    @Override
    protected void didInit ()
    {
        super.didInit();
        _field.getDocument().removeDocumentListener(this); // undo editings stuff from super
        _field.setEditable(false);
    }

    /**
     * Turn the property value into a String.
     */
    protected String valueToString (Object value)
    {
        String mode = getMode();
        if ("date".equals(mode)) {
            return DateFormat.getInstance().format(value);
        }

        // unknown
        return String.valueOf(value);
    }
}
