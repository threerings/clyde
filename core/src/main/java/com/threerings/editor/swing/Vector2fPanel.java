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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;

/**
 * Allows editing a vector.
 */
public class Vector2fPanel extends BasePropertyEditor
    implements ChangeListener
{
    /** The available editing modes: Cartesian coordinates, polar coordinates, normalized
     * polar coordinates. */
    public enum Mode { CARTESIAN, POLAR, NORMALIZED };

    /**
     * Creates a new vector panel with the specified editing mode.
     */
    public Vector2fPanel (MessageBundle msgs, Mode mode, float step, float scale)
    {
        _msgs = msgs;
        _mode = mode;
        _scale = scale;

        setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        setBackground(null);
        _spinners = new JSpinner[_mode == Mode.NORMALIZED ? 1 : 2];
        if (_mode == Mode.CARTESIAN) {
            _spinners[0] = addSpinnerPanel("x", -Float.MAX_VALUE, +Float.MAX_VALUE, step);
            _spinners[1] = addSpinnerPanel("y", -Float.MAX_VALUE, +Float.MAX_VALUE, step);
        } else {
            _spinners[0] = addSpinnerPanel("angle", -180f, +180f, 1f);
            if (_mode != Mode.NORMALIZED) {
                _spinners[1] = addSpinnerPanel("length", 0f, Float.MAX_VALUE, step);
            }
        }
    }

    /**
     * Sets the value of the vector being edited.
     */
    public void setValue (Vector2f value)
    {
        float v1, v2;
        if (_mode == Mode.CARTESIAN) {
            v1 = value.x / _scale;
            v2 = value.y;
        } else {
            v2 = value.length();
            v1 = (v2 > 0.0001f) ? FloatMath.toDegrees(FloatMath.atan2(value.y, value.x)) : 0f;
        }
        _spinners[0].setValue(v1);
        if (_spinners.length >= 2) {
            _spinners[1].setValue(v2 / _scale);
        }
    }

    /**
     * Returns the current value of the vector being edited.
     */
    public Vector2f getValue ()
    {
        float v1 = ((Number)_spinners[0].getValue()).floatValue();
        float v2 = (_spinners.length < 2) ?
            1f : (((Number)_spinners[1].getValue()).floatValue() * _scale);
        if (_mode == Mode.CARTESIAN) {
            return new Vector2f(v1 * _scale, v2);
        }
        float angle = FloatMath.toRadians(v1);
        return new Vector2f(FloatMath.cos(angle) * v2, FloatMath.sin(angle) * v2);
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        fireStateChanged();
    }

    /**
     * Adds a spinner panel for the named component and returns the spinner.
     */
    protected JSpinner addSpinnerPanel (String name, float min, float max, float step)
    {
        JPanel panel = new JPanel();
        panel.setBackground(null);
        add(panel);
        panel.add(new JLabel(getLabel(name) + ":"));
        JSpinner spinner = new DraggableSpinner(0f,
            (min == -Float.MAX_VALUE) ? null : (Comparable<Float>)min,
            (max == +Float.MAX_VALUE) ? null : (Comparable<Float>)max, step);
        panel.add(spinner);
        spinner.addChangeListener(this);
        return spinner;
    }

    /** The editing mode. */
    protected Mode _mode;

    /** The scale to apply. */
    protected float _scale;

    /** The coordinate spinners. */
    protected JSpinner[] _spinners;
}
