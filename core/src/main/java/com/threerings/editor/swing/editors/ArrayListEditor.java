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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.lang.reflect.Array;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.util.StringUtil;

import com.threerings.editor.EditorMessageBundle;
import com.threerings.editor.swing.PropertyEditor;
import com.threerings.util.DeepUtil;

import static com.threerings.editor.Log.log;

/**
 * Superclass of the array/list editors.
 */
public abstract class ArrayListEditor extends PropertyEditor
    implements ActionListener
{
    /**
     * Ensures that the specified index is visible.
     */
    public abstract void makeVisible (int idx);

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if (event.getSource() == _add) {
            Class<?>[] types = _property.getComponentSubtypes();
            Class<?> type = (types[0] == null) ? types[1] : types[0];
            addValue(getDefaultInstance(type, _object));
        } else {
            super.actionPerformed(event);
        }
    }

    @Override
    protected void didInit ()
    {
        _min = getMinSize();
        _max = getMaxSize();
        _fixed = isFixedSize();

        makeCollapsible(_ctx, getPropertyLabel(), false);
    }

    /**
     * Returns a label for the specified action.
     */
    protected String getActionLabel (String action)
    {
        return getActionLabel(action, null);
    }

    /**
     * Returns a label for the specified action.
     *
     * @param units an optional override for the units parameter.
     */
    protected String getActionLabel (String action, String units)
    {
        String bundle = EditorMessageBundle.DEFAULT;
        if (units == null) {
            units = _property.getAnnotation().units();
            bundle = _property.getMessageBundle();
        }
        return _msgs.get("m." + action + "_entry", (units.length() > 0) ?
            getLabel(units, bundle) : getLabel(_property.getComponentType()));
    }

    /**
     * Returns the length of the array or list property.
     */
    protected int getLength ()
    {
        Object values = _property.get(_object);
        return (values == null) ? 0 :
            (values.getClass().isArray() ? Array.getLength(values) : ((List)values).size());
    }

    /**
     * Returns the element at the specified index of the array or list.
     */
    protected Object getValue (int idx)
    {
        Object values = _property.get(_object);
        Object value = values.getClass().isArray() ?
            Array.get(values, idx) : ((List)values).get(idx);
        if (value instanceof String) {
            value = StringUtil.trim((String)value);
        }
        return value;
    }

    /**
     * Sets the element at the specified index of the array or list.
     */
    protected void setValue (int idx, Object value)
    {
        Object values = _property.get(_object);
        if (value instanceof String) {
            value = StringUtil.trim((String)value);
        }
        if (values.getClass().isArray()) {
            Array.set(values, idx, value);
        } else {
            @SuppressWarnings("unchecked") List<Object> list = (List<Object>)values;
            list.set(idx, value);
        }
    }

    /**
     * Adds an object to the end of the list.
     */
    protected void addValue (Object value)
    {
        Class<?> type = _property.getType();
        if (type.isArray()) {
            Object ovalues = _property.get(_object);
            int olength = (ovalues == null) ? 0 : Array.getLength(ovalues);
            Object nvalues = Array.newInstance(type.getComponentType(), olength + 1);
            if (olength > 0) {
                System.arraycopy(ovalues, 0, nvalues, 0, olength);
            }
            Array.set(nvalues, olength, value);
            _property.set(_object, nvalues);

        } else {
            List<?> values = (List<?>)_property.get(_object);
            if (values == null) {
                if (type == List.class) {
                    type = ArrayList.class;
                }
                try {
                    _property.set(_object, values = (List<?>)type.newInstance());
                } catch (Exception e) {
                    log.warning("Failed to instantiate list [class=" + type + "].", e);
                    return;
                }
            }
            @SuppressWarnings("unchecked") List<Object> list = (List<Object>)values;
            list.add(value);
            _property.set(_object, values);
        }
        _add.setEnabled(!_fixed && getLength() < _max);
        fireStateChanged();
    }

    /**
     * Copies the element at the specified index.
     */
    protected void copyValue (int idx)
    {
        Class<?> type = _property.getType();
        if (type.isArray()) {
            Object ovalues = _property.get(_object);
            int olength = Array.getLength(ovalues);
            Object nvalues = Array.newInstance(type.getComponentType(), olength + 1);
            System.arraycopy(ovalues, 0, nvalues, 0, idx + 1);
            Array.set(nvalues, idx + 1, DeepUtil.copy(Array.get(ovalues, idx)));
            System.arraycopy(ovalues, idx + 1, nvalues, idx + 2, olength - idx - 1);
            _property.set(_object, nvalues);

        } else {
            @SuppressWarnings("unchecked") List<Object> values =
                (List<Object>)_property.get(_object);
            values.add(idx + 1, DeepUtil.copy(values.get(idx)));
            _property.set(_object, values);
        }
        _add.setEnabled(!_fixed && getLength() < _max);
        fireStateChanged();
    }

    /**
     * Removes the element at the specified index.
     */
    protected void removeValue (int idx)
    {
        Class<?> type = _property.getType();
        if (type.isArray()) {
            Object ovalues = _property.get(_object);
            int olength = Array.getLength(ovalues);
            Object nvalues = Array.newInstance(type.getComponentType(), olength - 1);
            System.arraycopy(ovalues, 0, nvalues, 0, idx);
            System.arraycopy(ovalues, idx + 1, nvalues, idx, olength - idx - 1);
            _property.set(_object, nvalues);

        } else {
            List<?> values = (List<?>)_property.get(_object);
            values.remove(idx);
            _property.set(_object, values);
        }
        _add.setEnabled(!_fixed && getLength() < _max);
        fireStateChanged();
    }

    /**
     * Fires a state change, optionally resetting the property value.
     */
    protected void fireStateChanged (boolean reset)
    {
        if (reset) {
            _property.set(_object, _property.get(_object));
        }
        fireStateChanged();
    }

    /** The minimum and maximum sizes of the list. */
    protected int _min, _max;

    /** A fixed size list. */
    protected boolean _fixed;

    /** The add value button. */
    protected JButton _add;
}
