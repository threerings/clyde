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
    public void update ()
    {
        double value = ((Number)_property.get(_object)).doubleValue() / _scale;
        _spinner.setValue(value);
        if (_slider != null) {
            _slider.setValue((int)Math.round(value / _step));
        }
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        add(new JLabel(getPropertyLabel() + ":"));
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
        if (getMode().equals("sized")) {
            ((DraggableSpinner.NumberEditor)_spinner.getEditor()).getTextField().setColumns(
                _property.getAnnotation().width());
            _spinner.setPreferredSize(null);
        }
        _spinner.addChangeListener(this);
        addUnits();
    }

    /**
     * Converts a double value to a value of the property's type.
     */
    protected Number fromDouble (double value)
    {
        Class<?> type = _property.getType();
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
