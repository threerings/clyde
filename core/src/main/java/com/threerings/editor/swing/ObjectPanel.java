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

package com.threerings.editor.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.threerings.util.DeepUtil;
import com.threerings.util.ReflectionUtil;
import com.threerings.util.Validatable;

import com.threerings.editor.EditorMessageBundle;
import com.threerings.editor.Property;
import com.threerings.editor.util.EditorContext;

import static com.threerings.editor.Log.log;

/**
 * Allows editing an object of a known class.
 */
public class ObjectPanel extends BasePropertyEditor
    implements ActionListener, ChangeListener
{
    /**
     * Creates a new object panel.
     *
     * @param tlabel the translatable label to use for the type chooser.
     * @param types the selectable subtypes.
     * @param ancestors the ancestor properties from which constraints are inherited.
     * @param outer the outer object to use when instantiating inner classes.
     */
    public ObjectPanel (
        EditorContext ctx, String tlabel, Class<?>[] types, Property[] ancestors, Object outer)
    {
        this(ctx, tlabel, types, ancestors, outer, false);
    }

    /**
     * Creates a new object panel.
     *
     * @param tlabel the translatable label to use for the type chooser.
     * @param types the selectable subtypes.
     * @param ancestors the ancestor properties from which constraints are inherited.
     * @param outer the outer object to use when instantiating inner classes.
     * @param omitColumns if true, do not add editors for the properties flagged as columns.
     */
    public ObjectPanel (
        EditorContext ctx, String tlabel, Class<?>[] types,
        Property[] ancestors, Object outer, boolean omitColumns)
    {
        _ctx = ctx;
        _msgmgr = ctx.getMessageManager();
        _msgs = _msgmgr.getBundle(EditorMessageBundle.DEFAULT);
        _outer = outer;
        _types = types;

        //setBackground(getDarkerBackground(ancestors.length));
        setBackground(getBackgroundColor(ancestors));

        setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        if (_types.length > 1) {
            JPanel tpanel = new JPanel();
            tpanel.setBackground(null);
            add(tpanel);
            tpanel.add(new JLabel(getLabel(tlabel) + ":"));
            String[] labels = new String[_types.length];
            for (int ii = 0; ii < _types.length; ii++) {
                labels[ii] = getLabel(_types[ii]);
            }
            tpanel.add(_box = new JComboBox(labels));
            _box.addActionListener(this);
            _values = new Object[_types.length];
        }
        add(_panel = new EditorPanel(
            _ctx, EditorPanel.CategoryMode.PANELS, ancestors, omitColumns));
        _panel.addChangeListener(this);
    }

    /**
     * Sets the outer object to use when instantiating inner classes (does not affect the current
     * value).
     */
    public void setOuter (Object outer)
    {
        _outer = outer;
    }

    /**
     * Sets the value of the object being edited.
     */
    public void setValue (Object value)
    {
        int tidx = getTypeIndex(value);
        if (tidx == -1) {
            log.warning("Wrong type for object panel.", "value", value, "types", _types);
            return;
        }
        if (_box != null) {
            // clear out the old entries
            Arrays.fill(_values, null);

            // put in the new entry
            _values[tidx] = value;
            _box.removeActionListener(this);
            _box.setSelectedIndex(tidx);
            _box.addActionListener(this);
        }
        if (_panel.getObject() == (_lvalue = value)) {
            _panel.update();
        } else {
            _panel.setObject(value);
        }

        checkValid(value);
    }

    /**
     * Returns the current value of the object being edited.
     */
    public Object getValue ()
    {
        return _panel.getObject();
    }

    /**
     * Enables or disables tree mode.
     */
    public void setTreeModeEnabled (boolean enabled)
    {
        BaseEditorPanel opanel = _panel;
        remove(opanel);
        add(_panel = enabled ?
            new TreeEditorPanel(_ctx, opanel.getAncestors(), opanel.getOmitColumns()) :
            new EditorPanel(_ctx, EditorPanel.CategoryMode.PANELS,
                opanel.getAncestors(), opanel.getOmitColumns()));
        _panel.addChangeListener(this);
        _panel.setObject(opanel.getObject());
        revalidate();
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        // switch to a different type
        int idx = _box.getSelectedIndex();
        Object value = null;
        Class<?> type = _types[idx];
        if (type != null) {
            value = _values[idx];
            if (value == null) {
                try {
                    _values[idx] = value = newInstance(type);
                } catch (Exception e) {
                    log.warning("Failed to create instance [type=" + type + "].", e);
                }
            }
            if (_lvalue != null && value != null) {
                // transfer state from shared ancestry
                DeepUtil.transfer(_lvalue, value);
            }
        }
        _panel.setObject(value);
        if (value != null) {
            _lvalue = value;
        }
        fireStateChanged();
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        fireStateChanged();
    }

    @Override
    public String getComponentPath (Component comp, boolean mouse)
    {
        return _panel.getComponentPath(comp, mouse);
    }

    @Override
    protected void fireStateChanged ()
    {
        checkValid(getValue());
        super.fireStateChanged();
    }

    /**
     * Returns the index of the specified value's type, or -1 if it doesn't match any of the
     * types.
     */
    protected int getTypeIndex (Object value)
    {
        Class<?> type = (value == null) ? null : value.getClass();
        for (int ii = 0; ii < _types.length; ii++) {
            if (_types[ii] == type) {
                return ii;
            }
        }
        return -1;
    }

    /**
     * Update our border based on whether the edited value is currently valid.
     */
    protected void checkValid (Object value)
    {
        boolean invalid = (value instanceof Validatable) && !((Validatable)value).isValid();
        if (invalid != _invalid) {
            _invalid = invalid;
            updateBorder();
        }
    }

    /**
     * Creates a new instance of the specified type.
     */
    protected Object newInstance (Class<?> type)
        throws Exception
    {
        // find the most specific constructor that can take the last value
        if (_lvalue != null) {
            boolean inner = ReflectionUtil.isInner(type);
            _lvalue.getClass();
            Constructor<?> cctor = null;
            Class<?> cptype = null;
            for (Constructor<?> ctor : type.getConstructors()) {
                Class<?>[] ptypes = ctor.getParameterTypes();
                if (inner ? (ptypes.length != 2 || !ptypes[0].isInstance(_outer)) :
                        (ptypes.length != 1)) {
                    continue;
                }
                Class<?> ptype = ptypes[ptypes.length - 1];
                if (ptype.isInstance(_lvalue) &&
                        (cctor == null || cptype.isAssignableFrom(ptype))) {
                    cctor = ctor;
                    cptype = ptype;
                }
            }
            if (cctor != null) {
                return inner ? cctor.newInstance(_outer, _lvalue) : cctor.newInstance(_lvalue);
            }
        }
        // fall back on default constructor
        return ReflectionUtil.newInstance(type, _outer);
    }

    /** Provides access to common services. */
    protected EditorContext _ctx;

    /** The type box. */
    protected JComboBox _box;

    /** The editor panel. */
    protected BaseEditorPanel _panel;

    /** The outer object reference. */
    protected Object _outer;

    /** The list of available types. */
    protected Class<?>[] _types;

    /** Stored values for each type. */
    protected Object[] _values;

    /** The last non-null value selected. */
    protected Object _lvalue;
}
