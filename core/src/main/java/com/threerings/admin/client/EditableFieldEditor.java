//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
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

package com.threerings.admin.client;

import java.awt.event.ActionEvent;

import java.lang.reflect.Field;

import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.util.PresentsContext;
import com.threerings.util.DeepUtil;
import com.threerings.util.MessageBundle;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorMessageBundle;
import com.threerings.editor.Introspector;
import com.threerings.editor.Property;
import com.threerings.editor.swing.PropertyEditor;
import com.threerings.editor.util.EditorContext;

/**
 * A field editor for {@link Editable} properties.
 */
public class EditableFieldEditor extends FieldEditor
    implements ChangeListener
{
    /**
     * Creates a new field editor.
     */
    public EditableFieldEditor (PresentsContext ctx, Field field, DObject object)
    {
        super(ctx, field, object);
        removeAll();
        setLayout(new VGroupLayout(GroupLayout.STRETCH, GroupLayout.STRETCH, 5, GroupLayout.TOP));

        // create the dummy object that we'll actually edit
        try {
            _dummy = object.getClass().newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }

        // find the actual property corresponding to the field
        EditorContext ectx = (EditorContext)ctx;
        for (Property prop : Introspector.getProperties(object)) {
            if (prop.getMember().equals(field)) {
                _editor = PropertyEditor.createEditor(ectx, prop, null);
                break;
            }
        }
        if (_editor == null) {
            throw new AssertionError("Missing property for field " + field);
        }
        add(_editor);
        _editor.setObject(_dummy);
        _editor.addChangeListener(this);

        JPanel bpanel = new JPanel();
        add(bpanel, GroupLayout.FIXED);

        MessageBundle msgs = ectx.getMessageManager().getBundle(EditorMessageBundle.DEFAULT);
        bpanel.add(_revert = new JButton(new AbstractAction(msgs.get("m.revert")) {
            public void actionPerformed (ActionEvent event) {
                noteUpdatedExternally();
            }
        }));
        _revert.setEnabled(false);

        bpanel.add(_commit = new JButton(msgs.get("m.commit")));
        _commit.addActionListener(this);
        _commit.setEnabled(false);
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        boolean modified = !valueMatches(getDisplayValue());
        updateBorder(modified);
        _revert.setEnabled(modified);
        _commit.setEnabled(modified);
    }

    @Override
    public void noteUpdatedExternally ()
    {
        super.noteUpdatedExternally();
        _revert.setEnabled(false);
        _commit.setEnabled(false);
    }

    @Override
    protected Object getDisplayValue ()
    {
        try {
            return DeepUtil.copy(_field.get(_dummy));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected void displayValue (Object value)
    {
        try {
            _field.set(_dummy, DeepUtil.copy(value));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        _editor.update();
    }

    @Override
    protected boolean valueMatches (Object dvalue)
    {
        _a1[0] = dvalue;
        _a2[0] = getValue();
        return Arrays.deepEquals(_a1, _a2);
    }

    /** The dummy object that we actually edit. */
    protected Object _dummy;

    /** The contained property editor. */
    protected PropertyEditor _editor;

    /** The revert and commit buttons. */
    protected JButton _revert, _commit;

    /** Used for deep-comparing objects. */
    protected Object[] _a1 = new Object[1], _a2 = new Object[1];
}
