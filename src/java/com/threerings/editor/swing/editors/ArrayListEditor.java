//
// $Id$

package com.threerings.editor.swing.editors;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.lang.reflect.Array;

import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.editor.EditorMessageBundle;
import com.threerings.editor.swing.PropertyEditor;

import static com.threerings.editor.Log.*;

/**
 * Superclass of the array/list editors.
 */
public abstract class ArrayListEditor extends PropertyEditor
    implements ActionListener
{
    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if (event.getSource() == _add) {
            Class[] types = _property.getComponentSubtypes();
            Class type = (types[0] == null) ? types[1] : types[0];
            addValue(getDefaultInstance(type, _object));
        }
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        _min = getMinSize();
        _max = getMaxSize();

        setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        setBorder(BorderFactory.createTitledBorder(getPropertyLabel()));
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
        return values.getClass().isArray() ? Array.get(values, idx) : ((List)values).get(idx);
    }

    /**
     * Sets the element at the specified index of the array or list.
     */
    protected void setValue (int idx, Object value)
    {
        Object values = _property.get(_object);
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
        Class type = _property.getType();
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
            List values = (List)_property.get(_object);
            if (values == null) {
                try {
                    _property.set(_object, values = (List)type.newInstance());
                } catch (Exception e) {
                    log.warning("Failed to instantiate list [class=" + type + "].", e);
                    return;
                }
            }
            @SuppressWarnings("unchecked") List<Object> list = values;
            list.add(value);
        }
        _add.setEnabled(getLength() < _max);
        fireStateChanged();
    }

    /**
     * Removes the element at the specified index.
     */
    protected void removeValue (int idx)
    {
        Class type = _property.getType();
        if (type.isArray()) {
            Object ovalues = _property.get(_object);
            int olength = Array.getLength(ovalues);
            Object nvalues = Array.newInstance(type.getComponentType(), olength - 1);
            System.arraycopy(ovalues, 0, nvalues, 0, idx);
            System.arraycopy(ovalues, idx + 1, nvalues, idx, olength - idx - 1);
            _property.set(_object, nvalues);

        } else {
            List values = (List)_property.get(_object);
            values.remove(idx);
        }
        _add.setEnabled(getLength() < _max);
        fireStateChanged();
    }

    /** The minimum and maximum sizes of the list. */
    protected int _min, _max;

    /** The add value button. */
    protected JButton _add;
}
