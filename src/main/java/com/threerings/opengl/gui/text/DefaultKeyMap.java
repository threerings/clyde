//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

package com.threerings.opengl.gui.text;

import org.lwjgl.input.Keyboard;

import com.threerings.opengl.gui.event.InputEvent;

/**
 * Defines a default key mapping for our text editing components.
 */
public class DefaultKeyMap extends KeyMap
{
    public DefaultKeyMap ()
    {
        addMapping(ANY_MODIFIER, Keyboard.KEY_RETURN, EditCommands.ACTION);
        addMapping(ANY_MODIFIER, Keyboard.KEY_NUMPADENTER, EditCommands.ACTION);
        addMapping(ANY_MODIFIER, Keyboard.KEY_BACK, EditCommands.BACKSPACE);
        addMapping(ANY_MODIFIER, Keyboard.KEY_DELETE, EditCommands.DELETE);

        addMapping(ANY_MODIFIER, Keyboard.KEY_LEFT, EditCommands.CURSOR_LEFT);
        addMapping(ANY_MODIFIER, Keyboard.KEY_RIGHT, EditCommands.CURSOR_RIGHT);
        addMapping(ANY_MODIFIER, Keyboard.KEY_UP, EditCommands.CURSOR_UP);
        addMapping(ANY_MODIFIER, Keyboard.KEY_DOWN, EditCommands.CURSOR_DOWN);

        addMapping(InputEvent.CTRL_DOWN_MASK, Keyboard.KEY_LEFT, EditCommands.WORD_LEFT);
        addMapping(InputEvent.CTRL_DOWN_MASK, Keyboard.KEY_RIGHT, EditCommands.WORD_RIGHT);

        addMapping(ANY_MODIFIER, Keyboard.KEY_HOME, EditCommands.START_OF_LINE);
        addMapping(ANY_MODIFIER, Keyboard.KEY_END, EditCommands.END_OF_LINE);

        addMapping(InputEvent.CTRL_DOWN_MASK, Keyboard.KEY_X, EditCommands.CUT);
        addMapping(InputEvent.CTRL_DOWN_MASK, Keyboard.KEY_C, EditCommands.COPY);
        addMapping(InputEvent.CTRL_DOWN_MASK, Keyboard.KEY_V, EditCommands.PASTE);

        addMapping(InputEvent.CTRL_DOWN_MASK, Keyboard.KEY_Z, EditCommands.UNDO);
        addMapping(InputEvent.CTRL_DOWN_MASK, Keyboard.KEY_Y, EditCommands.REDO);

        // some emacs commands because I love them so
        addMapping(InputEvent.CTRL_DOWN_MASK, Keyboard.KEY_A,
                   EditCommands.START_OF_LINE);
        addMapping(InputEvent.CTRL_DOWN_MASK, Keyboard.KEY_E,
                   EditCommands.END_OF_LINE);
        addMapping(InputEvent.CTRL_DOWN_MASK, Keyboard.KEY_D,
                   EditCommands.DELETE);
        addMapping(InputEvent.CTRL_DOWN_MASK, Keyboard.KEY_K,
                   EditCommands.CLEAR);
    }
}
