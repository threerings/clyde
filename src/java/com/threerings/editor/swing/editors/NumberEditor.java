//
// $Id$

package com.threerings.editor.swing.editors;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.threerings.editor.Editable;
import com.threerings.editor.swing.DraggableSpinner;
import com.threerings.editor.swing.PropertyEditor;

/**
 * An editor for numerical values.
 */
public class NumberEditor extends PropertyEditor
    implements ChangeListener
{
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        double value;
        if (event.getSource() == _slider) {
            value = _slider.getValue() * _step;
            _spinner.setValue(value);
        } else { // event.getSource() == _spinner
            value = (Double)_spinner.getValue();
            if (_slider != null) {
                _slider.setValue((int)Math.round(value / _step));
            }
        }
        Number nvalue = fromDouble(value * _scale);
        if (!_property.get(_object).equals(nvalue)) {
            _property.set(_object, nvalue);
            fireStateChanged();
        }
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        add(new JLabel(getPropertyLabel() + ":"));
        Editable annotation = _property.getAnnotation();
        double min = getMinimum();
        double max = getMaximum();
        _step = getStep();
        _scale = getScale();
        if (getMode().equals("wide") && min != -Double.MAX_VALUE &&
                max != +Double.MAX_VALUE) {
            add(_slider = new JSlider(
                (int)Math.round(min / _step),
                (int)Math.round(max / _step)));
            _slider.setBackground(null);
            _slider.addChangeListener(this);
        }
        add(_spinner = new DraggableSpinner(min, min, max, _step));
        _spinner.addChangeListener(this);
        String units = getUnits();
        if (units.length() > 0) {
            add(new JLabel(getLabel(units)));
        }
    }

    @Override // documentation inherited
    protected void update ()
    {
        double value = ((Number)_property.get(_object)).doubleValue() / _scale;
        _spinner.setValue(value);
        if (_slider != null) {
            _slider.setValue((int)Math.round(value / _step));
        }
    }

    /**
     * Converts a double value to a value of the property's type.
     */
    protected Number fromDouble (double value)
    {
        Class type = _property.getType();
        if (type == Byte.TYPE || type == Byte.class) {
            return (byte)value;
        } else if (type == Double.TYPE || type == Double.class) {
            return value;
        } else if (type == Float.TYPE || type == Float.class) {
            return (float)value;
        } else if (type == Integer.TYPE || type == Integer.class) {
            return (int)value;
        } else if (type == Long.TYPE || type == Long.class) {
            return (long)value;
        } else { // type == Short.TYPE || type == Short.class
            return (short)value;
        }
    }

    /** The slider. */
    protected JSlider _slider;

    /** The spinner. */
    protected DraggableSpinner _spinner;

    /** The step size as retrieved from the annotation. */
    protected double _step;

    /** The scale as retrieved from the annotation. */
    protected double _scale;
}
