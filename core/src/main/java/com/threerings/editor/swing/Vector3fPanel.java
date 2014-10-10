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
import com.threerings.math.Vector3f;

/**
 * Allows editing a vector.
 */
public class Vector3fPanel extends BasePropertyEditor
    implements ChangeListener
{
    /** The available editing modes: Cartesian coordinates, spherical coordinates, normalized
     * spherical coordinates, Euler angles. */
    public enum Mode { CARTESIAN, SPHERICAL, NORMALIZED, ANGLES };

    /**
     * Creates a new vector panel with the specified editing mode.
     */
    public Vector3fPanel (MessageBundle msgs, Mode mode, float step, float scale)
    {
        _msgs = msgs;
        _mode = mode;
        _scale = scale;

        setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        setBackground(null);
        _spinners = new JSpinner[_mode == Mode.NORMALIZED ? 2 : 3];
        if (_mode == Mode.CARTESIAN || _mode == Mode.ANGLES) {
            float extent = Float.MAX_VALUE;
            if (_mode == Mode.ANGLES) {
                extent = 180f;
                _scale = FloatMath.PI/180f;
            }
            _spinners[0] = addSpinnerPanel("x", -extent, +extent, step);
            _spinners[1] = addSpinnerPanel("y", -extent, +extent, step);
            _spinners[2] = addSpinnerPanel("z", -extent, +extent, step);
        } else {
            _spinners[0] = addSpinnerPanel("azimuth", -180f, +180f, 1f);
            _spinners[1] = addSpinnerPanel("elevation", -90f, +90f, 1f);
            if (_mode != Mode.NORMALIZED) {
                _spinners[2] = addSpinnerPanel("length", 0f, Float.MAX_VALUE, step);
            }
        }
    }

    /**
     * Sets the value of the vector being edited.
     */
    public void setValue (Vector3f value)
    {
        float v1, v2, v3;
        if (_mode == Mode.CARTESIAN || _mode == Mode.ANGLES) {
            v1 = value.x / _scale;
            v2 = value.y / _scale;
            v3 = value.z;
        } else {
            v3 = value.length();
            if (v3 > 0.0001f) {
                v1 = (FloatMath.hypot(value.x, value.y) > 0.0001f) ?
                    FloatMath.toDegrees(FloatMath.atan2(-value.x, value.y)) : 0f;
                v2 = FloatMath.toDegrees(FloatMath.asin(value.z / v3));
            } else {
                v1 = v2 = 0f;
            }
        }
        _spinners[0].setValue(v1);
        _spinners[1].setValue(v2);
        if (_spinners.length >= 3) {
            _spinners[2].setValue(v3 / _scale);
        }
    }

    /**
     * Returns the current value of the vector being edited.
     */
    public Vector3f getValue ()
    {
        float v1 = ((Number)_spinners[0].getValue()).floatValue();
        float v2 = ((Number)_spinners[1].getValue()).floatValue();
        float v3 = (_spinners.length < 3) ?
            1f : (((Number)_spinners[2].getValue()).floatValue() * _scale);
        if (_mode == Mode.CARTESIAN || _mode == Mode.ANGLES) {
            return new Vector3f(v1 * _scale, v2 * _scale, v3);
        }
        float az = FloatMath.toRadians(v1), el = FloatMath.toRadians(v2);
        float cose = FloatMath.cos(el);
        return new Vector3f(
            -FloatMath.sin(az) * cose * v3,
            FloatMath.cos(az) * cose * v3,
            FloatMath.sin(el) * v3);
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
