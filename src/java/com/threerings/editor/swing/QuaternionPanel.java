//
// $Id$

package com.threerings.editor.swing;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
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
    public QuaternionPanel (MessageBundle msgs)
    {
        _msgs = msgs;

        setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        setBackground(null);
        _spinners = new JSpinner[] {
            addSpinnerPanel("x", -180f, +180f),
            addSpinnerPanel("y", -90f, +90f),
            addSpinnerPanel("z", -180f, +180f)
        };
    }

    /**
     * Sets the value of the quaternion being edited.
     */
    public void setValue (Quaternion value)
    {
        Vector3f angles = value.toAngles();
        _spinners[0].setValue(FloatMath.toDegrees(angles.x));
        _spinners[1].setValue(FloatMath.toDegrees(angles.y));
        _spinners[2].setValue(FloatMath.toDegrees(angles.z));
    }

    /**
     * Returns the current value of the quaternion being edited.
     */
    public Quaternion getValue ()
    {
        return new Quaternion().fromAngles(
            FloatMath.toRadians(((Number)_spinners[0].getValue()).floatValue()),
            FloatMath.toRadians(((Number)_spinners[1].getValue()).floatValue()),
            FloatMath.toRadians(((Number)_spinners[2].getValue()).floatValue()));
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
        JSpinner spinner = new DraggableSpinner(0f, (Comparable)min, (Comparable)max, 1f);
        panel.add(spinner);
        spinner.addChangeListener(this);
        return spinner;
    }

    /** The angle spinners. */
    protected JSpinner[] _spinners;
}
