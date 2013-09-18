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
import com.samskivert.swing.HGroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.math.FloatMath;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

/**
 * Allows editing a 2D transform.
 */
public class Transform2DPanel extends BasePropertyEditor
    implements ChangeListener
{
    /** The available editing modes: rigid or uniform. */
    public enum Mode { RIGID, UNIFORM };

    /**
     * Creates a new transform panel with the specified editing mode.
     */
    public Transform2DPanel (MessageBundle msgs, Mode mode, float step, float scale)
    {
        _msgs = msgs;
        _mode = mode;

        setLayout(new HGroupLayout(GroupLayout.NONE, GroupLayout.NONE, 5, GroupLayout.CENTER));
        setBackground(null);
        JPanel tcont = GroupLayout.makeVBox(
            GroupLayout.NONE, GroupLayout.CENTER, GroupLayout.NONE);
        tcont.setBackground(null);
        tcont.setBorder(BorderFactory.createTitledBorder(getLabel("translation")));
        add(tcont);
        tcont.add(_tpanel = new Vector2fPanel(msgs, Vector2fPanel.Mode.CARTESIAN, step, scale));
        _tpanel.addChangeListener(this);
        JPanel rscont = GroupLayout.makeVBox(
            GroupLayout.NONE, GroupLayout.CENTER, GroupLayout.NONE);
        ((GroupLayout)rscont.getLayout()).setOffAxisJustification(GroupLayout.RIGHT);
        rscont.setBackground(null);
        add(rscont);
        JPanel rpanel = new JPanel();
        rpanel.setBackground(null);
        rscont.add(rpanel);
        rpanel.add(new JLabel(getLabel("rotation") + ":"));
        rpanel.add(_rspinner = new DraggableSpinner(0f, -180f, +180f, 1f));
        _rspinner.addChangeListener(this);
        if (_mode == Mode.RIGID) {
            return;
        }
        JPanel spanel = new JPanel();
        spanel.setBackground(null);
        rscont.add(spanel);
        spanel.add(new JLabel(getLabel("scale") + ":"));
        spanel.add(_sspinner = new DraggableSpinner(0f, 0f, null, step));
        _sspinner.addChangeListener(this);
    }

    /**
     * Returns a reference to the translation panel.
     */
    public Vector2fPanel getTranslationPanel ()
    {
        return _tpanel;
    }

    /**
     * Sets the value of the transform being edited.
     */
    public void setValue (Transform2D value)
    {
        value.update(Transform2D.UNIFORM);
        _tpanel.setValue(value.getTranslation());
        _rspinner.setValue(FloatMath.toDegrees(value.getRotation()));
        if (_mode == Mode.UNIFORM) {
            _sspinner.setValue(value.getScale());
        }
    }

    /**
     * Returns the current value of the transform being edited.
     */
    public Transform2D getValue ()
    {
        Vector2f translation = _tpanel.getValue();
        float rotation = FloatMath.toRadians(((Number)_rspinner.getValue()).floatValue());
        float scale = (_sspinner == null) ? 1f : ((Number)_sspinner.getValue()).floatValue();
        if (scale == 1f) {
            return (translation.equals(Vector2f.ZERO) && rotation == 0f) ?
                new Transform2D() : new Transform2D(translation, rotation);
        } else {
            return new Transform2D(translation, rotation, scale);
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
    protected Vector2fPanel _tpanel;

    /** The rotation spinner. */
    protected JSpinner _rspinner;

    /** The scale spinner. */
    protected JSpinner _sspinner;
}
