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

package com.threerings.opengl.gui;

import javax.swing.SpinnerModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.google.common.base.Preconditions;

import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.event.InputEvent;

/**
 * Contains the logic for a spinner, allowing the hook-up of any buttons/label.
 */
public class SpinnerLogic
{
    /**
     * Create the logic of a spinner with other components.
     */
    public SpinnerLogic (TextComponent label, Button next, Button prev, SpinnerModel model)
    {
        _label = Preconditions.checkNotNull(label);
        _next = next;
        _prev = prev;

        _next.addListener(_buttonListener);
        _prev.addListener(_buttonListener);

        setModel(model);
    }

    /**
     * Set a new model.
     */
    public void setModel (SpinnerModel newModel)
    {
        if (!newModel.equals(_model)) { // will NPE if newModel is null
            if (_model != null) {
                _model.removeChangeListener(_modelListener);
            }
            _model = newModel;
            _model.addChangeListener(_modelListener);
            updateValue();
        }
    }

    /**
     * Get the current model.
     */
    public SpinnerModel getModel ()
    {
        return _model;
    }

    /**
     * Convenience to get the current value of this spinner.
     */
    public Object getValue ()
    {
        return _model.getValue();
    }

    /**
     * Set whether the logic of the spinner should be enabled.
     */
    public void setEnabled (boolean enabled)
    {
        _enabled = enabled;
        _label.setEnabled(_enabled);
        updateButtons();
    }

    /**
     * The state of the model has changed: update our subcomponents and fire an action
     * if applicable.
     */
    protected void updateValue ()
    {
        _label.setText(String.valueOf(getValue()));
        updateButtons();
    }

    /**
     * Update the enabled state of the buttons.
     */
    protected void updateButtons ()
    {
        _next.setEnabled(_enabled && (null != _model.getNextValue()));
        _prev.setEnabled(_enabled && (null != _model.getPreviousValue()));
    }

    /**
     * Get the number of times to rotate the spinner...
     */
    protected int getRotationCount (ActionEvent event)
    {
        // 10 rotations if shift/ctrl is pressed...
        return
            (0 == (event.getModifiers() & (InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)))
            ? 1
            : 10;
    }

    /** The next and previous buttons. */
    protected Button _next, _prev;

    /** Displays and allows direct editing of the value. */
    protected TextComponent _label;

    /** Our model. */
    protected SpinnerModel _model;

    /** Are we enabled? */
    protected boolean _enabled = true;

    /** Listens for changes to the model and updates our state. */
    protected ChangeListener _modelListener = new ChangeListener() {
        public void stateChanged (ChangeEvent e) {
            updateValue();
        }
    };

    /** Listens to our buttons and updates the model when they're pressed. */
    protected ActionListener _buttonListener = new ActionListener() {
        public void actionPerformed (ActionEvent e) {
            for (int ii = getRotationCount(e); ii > 0; ii--) {
                Object newValue =
                    (e.getSource() == _next) ? _model.getNextValue() : _model.getPreviousValue();
                if (newValue == null) {
                    break;
                }
                _model.setValue(newValue);
            }
        }
    };
}
