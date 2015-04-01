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

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

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
        rcont.add(_rpanel = new QuaternionPanel(msgs, QuaternionPanel.Mode.XYZ));
        _rpanel.addChangeListener(this);
        if (_mode == Mode.RIGID) {
            return;
        }
        JPanel spanel = new JPanel();
        spanel.setBackground(null);
        add(spanel);
        spanel.add(new JLabel(getLabel("scale") + ":"));
        spanel.add(_sspinner = new DraggableSpinner(0f, 0f, null, step));
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
        Vector3f translation = _tpanel.getValue();
        Quaternion rotation = _rpanel.getValue();
        float scale = (_sspinner == null) ? 1f : ((Number)_sspinner.getValue()).floatValue();
        if (scale == 1f) {
            return (translation.equals(Vector3f.ZERO) && rotation.equals(Quaternion.IDENTITY)) ?
                new Transform3D() : new Transform3D(translation, rotation);
        } else {
            return new Transform3D(translation, rotation, scale);
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
