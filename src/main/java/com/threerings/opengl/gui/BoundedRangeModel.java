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

import java.util.ArrayList;

import com.threerings.opengl.gui.event.ChangeEvent;
import com.threerings.opengl.gui.event.ChangeListener;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.event.MouseWheelListener;

/**
 * Defines the model used by the {@link ScrollBar} to communicate with other components and
 * external entities that wish to be manipulated by a scroll bar.
 *
 * <p> A bounded range model has a minimum and maximum value, a current value and an extent. These
 * are easily visualized by showing how they control a scroll bar:
 *
 * <pre>
 * +-------------------------------------------------------------------+
 * |        +---------------------------------------+                  |
 * |        |                                       |                  |
 * |        +---------------------------------------+                  |
 * +-------------------------------------------------------------------+
 * min      value                        value+extent                max
 * </pre>
 */
public class BoundedRangeModel
{
    /**
     * Creates a bounded range model with the specified minimum value, current value, extent and
     * maximum value.
     */
    public BoundedRangeModel (int min, int value, int extent, int max)
    {
        _min = min;
        _value = value;
        _extent = extent;
        _max = max;
    }

    /**
     * Adds a listener to this model.
     */
    public void addChangeListener (ChangeListener listener)
    {
        _listeners.add(listener);
    }

    /**
     * Removes the specified listener from the model.
     */
    public void removeChangeListener (ChangeListener listener)
    {
        _listeners.remove(listener);
    }

    /**
     * Returns the minimum value this model will allow for its value.
     */
    public int getMinimum ()
    {
        return _min;
    }

    /**
     * Returns the maximum value this model will allow for <code>value + extent</code>.
     */
    public int getMaximum ()
    {
        return _max;
    }

    /**
     * Returns the range of this model (the maximum minus the minimum).
     */
    public int getRange ()
    {
        return _max - _min;
    }

    /**
     * Returns the current value of the model.
     */
    public int getValue ()
    {
        return _value;
    }

    /**
     * Returns the current extent of the model.
     */
    public int getExtent ()
    {
        return _extent;
    }

    /**
     * Returns the value of the model mapped into the range [0-1]: (value - minumum) / range.
     */
    public float getRatio ()
    {
        return (getValue() - getMinimum()) / (float)getRange();
    }

    /**
     * Returns the increment by which this model should be scrolled when the user presses one of
     * the buttons at the end of the scrollbar.
     */
    public int getScrollIncrement ()
    {
        return Math.max(1, getExtent() / 2);
    }

    /**
     * Configures the minimum value of this model, adjusting the value, extent and maximum as
     * necessary to maintain the consistency of the model.
     */
    public void setMinimum (int minimum)
    {
        int max = Math.max(minimum, _max);
        int val = Math.max(minimum, _value);
        int ext = Math.max(max - val, _extent);
        setRange(minimum, val, ext, max);
    }

    /**
     * Configures the maximum value of this model, adjusting the value, extent and minimum as
     * necessary to maintain the consistency of the model.
     */
    public void setMaximum (int maximum)
    {
        int min = Math.min(maximum, _min);
        int ext = Math.min(maximum - min, _extent);
        int val = Math.min(maximum - ext, _value);
        setRange(min, val, ext, maximum);
    }

    /**
     * Configures the value of this model. The new value will be adjusted if it does not fall
     * within the range of <code>min <= value <= max - extent<code>.
     */
    public void setValue (int value)
    {
        int val = Math.min(_max - _extent, Math.max(_min, value));
        setRange(_min, val, _extent, _max);
    }

    /**
     * Configures the extent of this model. The new value will be adjusted if it does not fall
     * within the range of <code>0 <= extent <= max - value<code>.
     */
    public void setExtent (int extent)
    {
        int ext = Math.min(_max - _value, Math.max(0, extent));
        setRange(_min, _value, ext, _max);
    }

    /**
     * Configures this model with a new minimum, maximum, current value and extent.
     *
     * @return true if the range was modified, false if the values were already set to the
     * requested values.
     */
    public boolean setRange (int min, int value, int extent, int max)
    {
        min = Math.min(min, max);
        max = Math.max(max, value);
        min = Math.min(min, value);
        extent = Math.max(Math.min(extent, max - value), 0);

        // if anything has changed
        if (min != _min || _value != value ||
            _extent != extent || _max != max) {
            // update our values
            _min = min;
            _value = value;
            _extent = extent;
            _max = max;

            // and notify our listeners
            for (int ii = 0, ll = _listeners.size(); ii < ll; ii++) {
                _listeners.get(ii).stateChanged(_event);
            }

            return true;
        }
        return false;
    }

    /**
     * Creates a mouse wheel listener that will respond to wheel events by adjusting this model up
     * or down accordingly.
     */
    public MouseWheelListener createWheelListener ()
    {
        return new MouseWheelListener() {
            public void mouseWheeled (MouseEvent event) {
                int delta = ((event.getDelta() > 0) ? -1 : 1) * getScrollIncrement();
                if ((event.getModifiers() & MouseEvent.CTRL_DOWN_MASK) != 0) {
                    delta *= 2;
                }
                setValue(getValue() + delta);
                event.consume();
            }
        };
    }

    @Override
    public String toString ()
    {
        return "Bounded Range (min:" + _min + ", max:" + _max + ", value:" + _value +
            ", extent:" + _extent + ")";
    }

    protected int _min, _max;
    protected int _value, _extent;
    protected ArrayList<ChangeListener> _listeners = new ArrayList<ChangeListener>();
    protected ChangeEvent _event = new ChangeEvent(this);
}
