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
import com.threerings.math.Quaternion;
import com.threerings.math.Vector3f;

/**
 * Allows editing a quaternion orientation (as a set of Euler angles).
 */
public class QuaternionPanel extends BasePropertyEditor
    implements ChangeListener
{
    /** The available editing modes: right-handed, left-handed. */
    public enum Mode { XYZ, ZXY };

    public QuaternionPanel (MessageBundle msgs, Mode mode)
    {
        _msgs = msgs;
        _mode = mode;

        setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        setBackground(null);
        switch (_mode) {
        case XYZ:
            _spinners = new JSpinner[] {
                addSpinnerPanel("x", -180f, 180f),
                addSpinnerPanel("y", -90f, 90f),
                addSpinnerPanel("z", -180f, +180f)
            };
            break;
        case ZXY:
            _spinners = new JSpinner[] {
                addSpinnerPanel("x", -180f, 360f),
                addSpinnerPanel("y", -90f, 360f),
                addSpinnerPanel("z", -180f, 360f)
            };
            break;
        }
    }

    /**
     * Sets the value of the quaternion being edited.
     */
    public void setValue (Quaternion value)
    {
        Vector3f angles = _mode == Mode.XYZ ? value.toAngles() : value.toAnglesZXY();
        _spinners[0].setValue(FloatMath.toDegrees(angles.x));
        _spinners[1].setValue(FloatMath.toDegrees(angles.y));
        _spinners[2].setValue(FloatMath.toDegrees(angles.z));
    }

    /**
     * Returns the current value of the quaternion being edited.
     */
    public Quaternion getValue ()
    {
        float x = FloatMath.toRadians(((Number)_spinners[0].getValue()).floatValue());
        float y = FloatMath.toRadians(((Number)_spinners[1].getValue()).floatValue());
        float z = FloatMath.toRadians(((Number)_spinners[2].getValue()).floatValue());
        return _mode == Mode.XYZ
            ? new Quaternion().fromAngles(x, y, z)
            : new Quaternion().fromAnglesZXY(x, y, z);
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        fireStateChanged();
    }

    /**
     * Adds a spinner panel for the named component and returns the spinner.
     */
    protected JSpinner addSpinnerPanel (String name, float min, float max)
    {
        JPanel panel = new JPanel();
        panel.setBackground(null);
        add(panel);
        panel.add(new JLabel(getLabel(name) + ":"));
        JSpinner spinner = new DraggableSpinner(
                0f, (Comparable<Float>)min, (Comparable<Float>)max, 1f);
        panel.add(spinner);
        spinner.addChangeListener(this);
        return spinner;
    }

    /** The angle spinners. */
    protected JSpinner[] _spinners;

    /** The editing mode. */
    protected Mode _mode;
}
