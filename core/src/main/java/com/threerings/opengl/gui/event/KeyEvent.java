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

import org.lwjgl.input.Keyboard;

/**
 * Encapsulates the information associated with a keyboard event.
 */
public class KeyEvent extends InputEvent
{
    /** Indicates that an event represents a key pressing. */
    public static final int KEY_PRESSED = 0;

    /** Indicates that an event represents a key release. */
    public static final int KEY_RELEASED = 1;

    public KeyEvent (Object source, long when, int modifiers,
                     int type, char keyChar, int keyCode, boolean repeat)
    {
        super(source, when, modifiers);
        _type = type;
        _keyChar = keyChar;
        _keyCode = keyCode;
        _repeat = repeat;
    }

    /**
     * Indicates whether this was a {@link #KEY_PRESSED} or {@link
     * #KEY_RELEASED} event.
     */
    public int getType ()
    {
        return _type;
    }

    /**
     * Returns the character associated with the key. <em>Note:</em> this
     * is only valid for {@link #KEY_PRESSED} events, however {@link
     * #getKeyCode} works in all cases.
     */
    public char getKeyChar ()
    {
        // TEMP: This is a hack to get around a bug in lwjgl's handling of
        // numpad keys in windows
        if (_keyChar == 0) {
            switch (_keyCode) {
            case Keyboard.KEY_NUMPAD1: return '1';
            case Keyboard.KEY_NUMPAD2: return '2';
            case Keyboard.KEY_NUMPAD3: return '3';
            case Keyboard.KEY_NUMPAD4: return '4';
            case Keyboard.KEY_NUMPAD5: return '5';
            case Keyboard.KEY_NUMPAD6: return '6';
            case Keyboard.KEY_NUMPAD7: return '7';
            case Keyboard.KEY_NUMPAD8: return '8';
            case Keyboard.KEY_NUMPAD9: return '9';
            case Keyboard.KEY_NUMPAD0: return '0';
            default: return _keyChar;
            }
        }
        // END TEMP
        return _keyChar;
    }

    /**
     * Returns the numeric identifier associated with the key.
     *
     * @see Keyboard
     */
    public int getKeyCode ()
    {
        return _keyCode;
    }

    /**
     * Checks whether this is a repeat (press) event.
     */
    public boolean isRepeat ()
    {
        return _repeat;
    }

    // documentation inherited
    public void dispatch (ComponentListener listener)
    {
        super.dispatch(listener);
        switch (_type) {
        case KEY_PRESSED:
            if (listener instanceof KeyListener) {
                ((KeyListener)listener).keyPressed(this);
            }
            break;

        case KEY_RELEASED:
            if (listener instanceof KeyListener) {
                ((KeyListener)listener).keyReleased(this);
            }
            break;
        }
    }

    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", type=").append(_type);
        buf.append(", char=").append(_keyChar);
        buf.append(", code=").append(_keyCode);
        buf.append(", repeat=").append(_repeat);
    }

    protected int _type;
    protected char _keyChar;
    protected int _keyCode;
    protected boolean _repeat;
}
