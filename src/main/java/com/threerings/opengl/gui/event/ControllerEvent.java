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

package com.threerings.opengl.gui.event;

import org.lwjgl.input.Controller;

/**
 * Encapsulates the information associated with a controller (joystick, gamepad) event.
 */
public class ControllerEvent extends InputEvent
{
    /** An event generated when a controller button is pressed. */
    public static final int CONTROLLER_PRESSED = 0;

    /** An event generated when a controller button is released. */
    public static final int CONTROLLER_RELEASED = 1;

    /** An event generated when the controller is moved on an axis. */
    public static final int CONTROLLER_MOVED = 2;

    /** An event generated when the controller's pov is moved on the x axis. */
    public static final int CONTROLLER_POV_X_MOVED = 3;

    /** An event generated when the controller's pov is moved on the y axis. */
    public static final int CONTROLLER_POV_Y_MOVED = 4;

    /**
     * Creates a new controller button event.
     */
    public ControllerEvent (
        Controller source, long when, int modifiers, int type, int controlIndex)
    {
        this(source, when, modifiers, type, controlIndex, false, false, 0f);
    }

    /**
     * Creates a new controller pov event.
     */
    public ControllerEvent (
        Controller source, long when, int modifiers, int type, float value)
    {
        this(source, when, modifiers, type, -1, false, false, value);
    }

    /**
     * Creates a new controller event.
     */
    public ControllerEvent (
        Controller source, long when, int modifiers, int type,
        int controlIndex, boolean xAxis, boolean yAxis, float value)
    {
        super(source, when, modifiers);
        _type = type;
        _controlIndex = controlIndex;
        _xAxis = xAxis;
        _yAxis = yAxis;
        _value = value;
    }

    /**
     * Returns a reference to the controller that caused the event.
     */
    public Controller getController ()
    {
        return (Controller)getSource();
    }

    /**
     * Returns the type of this event, one of {@link #CONTROLLER_PRESSED},
     * {#link CONTROLLER_RELEASED}, etc.
     */
    public int getType ()
    {
        return _type;
    }

    /**
     * Returns the index of the control (button or axis) that caused the event, or -1 if not
     * caused by a button or axis.
     */
    public int getControlIndex ()
    {
        return _controlIndex;
    }

    /**
     * Checks whether the event pertains to the x axis.
     */
    public boolean isXAxis ()
    {
        return _xAxis;
    }

    /**
     * Checks whether the event pertains to the y axis.
     */
    public boolean isYAxis ()
    {
        return _yAxis;
    }

    /**
     * Returns the value corresponding to the event.
     */
    public float getValue ()
    {
        return _value;
    }

    @Override
    public void dispatch (ComponentListener listener)
    {
        super.dispatch(listener);
        switch (_type) {
        case CONTROLLER_PRESSED:
            if (listener instanceof ControllerListener) {
                ((ControllerListener)listener).controllerPressed(this);
            }
            break;

        case CONTROLLER_RELEASED:
            if (listener instanceof ControllerListener) {
                ((ControllerListener)listener).controllerReleased(this);
            }
            break;

        case CONTROLLER_MOVED:
            if (listener instanceof ControllerListener) {
                ((ControllerListener)listener).controllerMoved(this);
            }
            break;

        case CONTROLLER_POV_X_MOVED:
            if (listener instanceof ControllerListener) {
                ((ControllerListener)listener).controllerPovXMoved(this);
            }
            break;

        case CONTROLLER_POV_Y_MOVED:
            if (listener instanceof ControllerListener) {
                ((ControllerListener)listener).controllerPovYMoved(this);
            }
            break;
        }
    }

    @Override
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", type=").append(_type);
        buf.append(", controlIndex=").append(_controlIndex);
        buf.append(", xAxis=").append(_xAxis);
        buf.append(", yAxis=").append(_yAxis);
        buf.append(", value=").append(_value);
    }

    /** The type of the event. */
    protected int _type;

    /** The index of the control (button or axis) that caused the event. */
    protected int _controlIndex;

    /** Whether or not the axis is the x axis. */
    protected boolean _xAxis;

    /** Whether or not the axis is the y axis. */
    protected boolean _yAxis;

    /** The value of the axis. */
    protected float _value;
}
