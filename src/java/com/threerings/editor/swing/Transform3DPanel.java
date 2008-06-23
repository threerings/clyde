//
// $Id$

package com.threerings.editor.swing;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.math.Transform3D;

/**
 * Allows editing a 3D transform.
 */
public class Transform3DPanel extends BasePropertyEditor
    implements ChangeListener
{
    /** The available editing modes: rigid or uniform. */
    public enum Mode { RIGID, UNIFORM };

    /**
     * Creates a new transform panel with the specified editing mode.
     */
    public Transform3DPanel (MessageBundle msgs, Mode mode, float step, float scale)
    {
        _msgs = msgs;
        _mode = mode;

        setLayout(new VGroupLayout(GroupLayout.NONE, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        setBackground(null);
        JPanel trpanel = GroupLayout.makeHBox(
            GroupLayout.NONE, GroupLayout.CENTER, GroupLayout.NONE);
        trpanel.setBackground(null);
        add(trpanel);
        JPanel tcont = GroupLayout.makeVBox(
            GroupLayout.NONE, GroupLayout.CENTER, GroupLayout.NONE);
        tcont.setBackground(null);
        tcont.setBorder(BorderFactory.createTitledBorder(getLabel("translation")));
        trpanel.add(tcont);
        tcont.add(_tpanel = new Vector3fPanel(msgs, Vector3fPanel.Mode.CARTESIAN, step, scale));
        _tpanel.addChangeListener(this);
        JPanel rcont = GroupLayout.makeVBox(
            GroupLayout.NONE, GroupLayout.CENTER, GroupLayout.NONE);
        rcont.setBackground(null);
        rcont.setBorder(BorderFactory.createTitledBorder(getLabel("rotation")));
        trpanel.add(rcont);
        rcont.add(_rpanel = new QuaternionPanel(msgs));
        _rpanel.addChangeListener(this);
        if (_mode == Mode.RIGID) {
            return;
        }
        JPanel spanel = new JPanel();
        spanel.setBackground(null);
        add(spanel);
        spanel.add(new JLabel(getLabel("scale") + ":"));
        spanel.add(_sspinner = new DraggableSpinner(0f, (Comparable)0f, null, step));
        _sspinner.addChangeListener(this);
    }

    /**
     * Returns a reference to the translation panel.
     */
    public Vector3fPanel getTranslationPanel ()
    {
        return _tpanel;
    }

    /**
     * Returns a reference to the rotation panel.
     */
    public QuaternionPanel getRotationPanel ()
    {
        return _rpanel;
    }

    /**
     * Sets the value of the transform being edited.
     */
    public void setValue (Transform3D value)
    {
        value.update(Transform3D.UNIFORM);
        _tpanel.setValue(value.getTranslation());
        _rpanel.setValue(value.getRotation());
        if (_mode == Mode.UNIFORM) {
            _sspinner.setValue(value.getScale());
        }
    }

    /**
     * Returns the current value of the transform being edited.
     */
    public Transform3D getValue ()
    {
        if (_mode == Mode.RIGID) {
            return new Transform3D(_tpanel.getValue(), _rpanel.getValue());
        } else { // _mode == Mode.UNIFORM
            return new Transform3D(_tpanel.getValue(), _rpanel.getValue(),
                ((Number)_sspinner.getValue()).floatValue());
        }
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        fireStateChanged();
    }

    /** The editing mode. */
    protected Mode _mode;

    /** The translation panel. */
    protected Vector3fPanel _tpanel;

    /** The rotation panel. */
    protected QuaternionPanel _rpanel;

    /** The scale spinner. */
    protected JSpinner _sspinner;
}
