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

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.google.common.primitives.Primitives;

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

    @Override
    public void update ()
    {
        double value = ((Number)_property.get(_object)).doubleValue() / _scale;
        if (_property.getAnnotation().constant()) {
            _value.setText(String.valueOf(fromDouble(value)));
            return;
        }
        _spinner.setValue(value);
        if (_slider != null) {
            _slider.setValue((int)Math.round(value / _step));
        }
    }

    @Override
    protected void didInit ()
    {
        add(new JLabel(getPropertyLabel() + ":"));
        double min = getMinimum();
        double max = getMaximum();
        _step = getStep();
        _scale = getScale();
        if (_property.getAnnotation().constant()) {
            add(_value = new JLabel(" "));

        } else {
            if (getMode().equals("wide") && min != -Double.MAX_VALUE &&
                    max != +Double.MAX_VALUE) {
                add(_slider = new JSlider(
                    (int)Math.round(min / _step),
                    (int)Math.round(max / _step)));
                _slider.setBackground(null);
                _slider.addChangeListener(this);
            }
            add(_spinner = new DraggableSpinner(min, min, max, _step));
            int width = _property.getWidth(-1);
            if (width != -1) {
                ((DraggableSpinner.NumberEditor)_spinner.getEditor()).getTextField().setColumns(
                    width);
                _spinner.setPreferredSize(null);
            }
            _spinner.addChangeListener(this);
        }
        addUnits(this);
    }

    /**
     * Converts a double value to a value of the property's type.
     */
    protected Number fromDouble (double value)
    {
        Class<?> type = Primitives.unwrap(_property.getType());
        if (type == Byte.TYPE) {
            return (byte)value;
        } else if (type == Double.TYPE) {
            return value;
        } else if (type == Float.TYPE) {
            return (float)value;
        } else if (type == Integer.TYPE) {
            return (int)value;
        } else if (type == Long.TYPE) {
            return (long)value;
        } else { // type == Short.TYPE
            return (short)value;
        }
    }

    @Override
    protected double getMinimum ()
    {
        return Math.max(super.getMinimum(), getTypeMinimum());
    }

    @Override
    protected double getMaximum ()
    {
        return Math.min(super.getMaximum(), getTypeMaximum());
    }

    /**
     * Get an overridden minimum based on the type of the property we're editing.
     */
    protected double getTypeMinimum ()
    {
        Class<?> type = Primitives.unwrap(_property.getType());
        if (type == Byte.TYPE) {
            return Byte.MIN_VALUE;
        } else if (type == Double.TYPE) {
            return -Double.MAX_VALUE;
        } else if (type == Float.TYPE) {
            return -Float.MAX_VALUE;
        } else if (type == Integer.TYPE) {
            return Integer.MIN_VALUE;
        } else if (type == Long.TYPE) {
            return Long.MIN_VALUE;
        } else { // type == Short.TYPE
            return Short.MIN_VALUE;
        }
    }

    /**
     * Get an overridden maximum based on the type of the property we're editing.
     */
    protected double getTypeMaximum ()
    {
        Class<?> type = Primitives.unwrap(_property.getType());
        if (type == Byte.TYPE) {
            return Byte.MAX_VALUE;
        } else if (type == Double.TYPE) {
            return Double.MAX_VALUE;
        } else if (type == Float.TYPE) {
            return Float.MAX_VALUE;
        } else if (type == Integer.TYPE) {
            return Integer.MAX_VALUE;
        } else if (type == Long.TYPE) {
            return Long.MAX_VALUE;
        } else { // type == Short.TYPE
            return Short.MAX_VALUE;
        }
    }

    /** The slider. */
    protected JSlider _slider;

    /** The spinner. */
    protected DraggableSpinner _spinner;

    /** The label when in constant mode. */
    protected JLabel _value;

    /** The step size as retrieved from the annotation. */
    protected double _step;

    /** The scale as retrieved from the annotation. */
    protected double _scale;
}
