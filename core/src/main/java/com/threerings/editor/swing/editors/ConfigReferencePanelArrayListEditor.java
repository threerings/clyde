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

package com.threerings.editor.swing.editors;

import java.lang.reflect.Member;
import java.lang.reflect.Type;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import com.samskivert.swing.CollapsiblePanel;
import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.editor.Introspector;
import com.threerings.editor.Property;
import com.threerings.editor.swing.PropertyEditor;

import com.threerings.config.ConfigReference;

import static com.threerings.editor.Log.log;

/**
 * An editor for arrays of lists of config references.
 */
public class ConfigReferencePanelArrayListEditor extends PanelArrayListEditor
{
    @Override
    protected void updatePanel (EntryPanel panel, Object value)
    {
        // TODO::::: ?
        PropertyEditor editor = ((WrappedEditorEntryPanel)panel).getEditor();
        System.err.println("Updating panel: " + value);
        editor.setObject(value);
        editor.update();
    }

    @Override
    protected void addPanel (Object value)
    {
        // create and add the panel first, THEN update so that the panel has an index...
        WrappedEditorEntryPanel pan = new WrappedEditorEntryPanel(value);
        _panels.add(pan);
        pan.getEditor().update();
    }

    /**
     * A panel for a config reference entry.
     */
    protected class WrappedEditorEntryPanel extends EntryPanel
    {
        public WrappedEditorEntryPanel (Object value)
        {
            super(value);
        }

        /**
         * Get the actual property editor we're wrapping.
         */
        public PropertyEditor getEditor ()
        {
            return _editor;
        }

        @Override
        public String getComponentPath (Component comp, boolean mouse)
        {
            PropertyEditor pe = getNextChildComponent(PropertyEditor.class, comp);
            return (pe == null)
                ? ""
                : "[\"" + pe.getProperty().getName().replace("\"", "\\\"") + "\"]" +
                        pe.getComponentPath(comp, mouse);
        }

        @Override
        protected JPanel createPanel (Object ignoredValue)
        {
            Object val = _property.get(_object);
            // this object should never be null: our superclass ArrayListEditor will create it

            @SuppressWarnings("unchecked")
            final List<Object> fvalue = (List<Object>)val;
            Property prop = new Property() {
                {
                    _name = ""; //_property.getName() + "[" + _idx + "]";
                }
                @Override
                public boolean shouldTranslateName ()
                {
                    return false;
                }
                @Override
                public Member getMember () {
                    return _property.getMember();
                }
                @Override
                public Class<?> getType () {
                    return _property.getComponentType();
                }
                @Override
                public Type getGenericType () {
                    return _property.getGenericComponentType();
                }
                @Override
                public Object get (Object object) {
                    int idx = getIndex();
                    return (idx == -1) // happens when this panel is removed...?
                        ? null
                        : fvalue.get(idx);
                }
                @Override
                public void set (Object object, Object value) {
                    fvalue.set(getIndex(), value);
                }
            };
            _editor = PropertyEditor.createEditor(_ctx, prop, _lineage);
            return _editor.getContent();
        }

        /** Our wrapped property editor. */
        protected PropertyEditor _editor; 
    }
}
