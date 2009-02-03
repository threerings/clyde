//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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
