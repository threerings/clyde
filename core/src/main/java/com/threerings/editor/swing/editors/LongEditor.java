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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.google.common.primitives.Primitives;

import com.threerings.editor.Editable;
import com.threerings.editor.swing.DraggableSpinner;
import com.threerings.editor.swing.PropertyEditor;

/**
 * An editor for long values.
 */
public class LongEditor extends PropertyEditor
    implements ChangeListener
{
    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        Long nvalue = (Long)_spinner.getValue();
        if (!_property.get(_object).equals(nvalue)) {
            _property.set(_object, nvalue);
            fireStateChanged();
        }
    }

    @Override
    public void update ()
    {
        Long value = (Long)_property.get(_object);
        if (_property.getAnnotation().constant()) {
            _value.setText(String.valueOf(value));
            return;
        }
        _spinner.setValue(value);
    }

    @Override
    protected void didInit ()
    {
        add(new JLabel(getPropertyLabel() + ":"));
        long min = getLongMinimum();
        long max = getLongMaximum();

        if (_property.getAnnotation().constant()) {
            add(_value = new JLabel(" "));

        } else {
            // I have got NO idea why these extra casts are necessary to get it to use
            // the Number/Comparable overload, but they absolutely are. What the fuck.
            add(_spinner = new DraggableSpinner((Long)min, (Long)min, (Long)max, (Long)(long)getStep()));
            //add(_spinner = new DraggableSpinner(min, min, max, (long)getStep()));
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

    protected long getLongMinimum ()
    {
        return Math.max((long)getMinimum(), Long.MIN_VALUE);
    }

    protected long getLongMaximum ()
    {
        return Math.min((long)getMaximum(), Long.MAX_VALUE);
    }

    /** The spinner. */
    protected DraggableSpinner _spinner;

    /** The label when in constant mode. */
    protected JLabel _value;
}
