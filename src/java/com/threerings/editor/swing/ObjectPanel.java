//
// $Id$

package com.threerings.editor.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.lang.reflect.Constructor;

import java.util.Arrays;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.util.ListUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.editor.Property;

import static java.util.logging.Level.*;
import static com.threerings.editor.Log.*;

/**
 * Allows editing an object of a known class.
 */
public class ObjectPanel extends BasePropertyEditor
    implements ActionListener, ChangeListener
{
    public ObjectPanel (MessageBundle msgs, String tlabel, Class[] types, Property[] ancestors)
    {
        _msgs = msgs;
        _types = types;

        setBackground(getDarkerBackground(ancestors.length));

        setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        if (_types.length > 1) {
            JPanel tpanel = new JPanel();
            tpanel.setBackground(null);
            add(tpanel);
            tpanel.add(new JLabel(getLabel(tlabel) + ":"));
            String[] labels = new String[_types.length];
            for (int ii = 0; ii < _types.length; ii++) {
                Class type = _types[ii];
                String name = (type == null) ? "none" : type.getName();
                name = name.substring(
                    Math.max(name.lastIndexOf('$'), name.lastIndexOf('.')) + 1);
                name = StringUtil.toUSLowerCase(StringUtil.unStudlyName(name));
                labels[ii] = getLabel(name);
            }
            tpanel.add(_box = new JComboBox(labels));
            _box.addActionListener(this);
            _values = new Object[_types.length];
        }
        add(_panel = new EditorPanel(_msgs, EditorPanel.CategoryMode.PANELS, ancestors));
        _panel.addChangeListener(this);
    }

    /**
     * Sets the value of the object being edited.
     */
    public void setValue (Object value)
    {
        // make sure it's not the same object
        Object ovalue = _panel.getObject();
        if (value == ovalue) {
            return;
        }
        if (_box != null) {
            // clear out the old entries
            Arrays.fill(_values, null);

            // put in the new entry
            int nidx = (value == null) ? 0 : ListUtil.indexOfRef(_types, value.getClass());
            _values[nidx] = value;
            _box.setSelectedIndex(nidx);
        }
        _panel.setObject(_lvalue = value);
    }

    /**
     * Returns the current value of the object being edited.
     */
    public Object getValue ()
    {
        return _panel.getObject();
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        // switch to a different type
        int idx = _box.getSelectedIndex();
        Object value = null;
        Class type = _types[idx];
        if (type != null) {
            value = _values[idx];
            if (value == null) {
                try {
                    _values[idx] = value = newInstance(type);
                } catch (Exception e) {
                    log.log(WARNING, "Failed to create instance [type=" + type + "].", e);
                }
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

    /**
     * Creates a new instance of the specified type.
     */
    protected Object newInstance (Class type)
        throws Exception
    {
        // find the most specific constructor that can take the last value
        if (_lvalue != null) {
            Class ltype = _lvalue.getClass();
            Constructor cctor = null;
            Class<?> cptype = null;
            for (Constructor ctor : type.getConstructors()) {
                Class<?>[] ptypes = ctor.getParameterTypes();
                if (ptypes.length == 1 && ptypes[0].isInstance(_lvalue) &&
                        (cctor == null || cptype.isAssignableFrom(ptypes[0]))) {
                    cctor = ctor;
                    cptype = ptypes[0];
                }
            }
            if (cctor != null) {
                return cctor.newInstance(_lvalue);
            }
        }
        // fall back on default constructor
        return type.newInstance();
    }

    /** The type box. */
    protected JComboBox _box;

    /** The editor panel. */
    protected EditorPanel _panel;

    /** The list of available types. */
    protected Class[] _types;

    /** Stored values for each type. */
    protected Object[] _values;

    /** The last non-null value selected. */
    protected Object _lvalue;
}
