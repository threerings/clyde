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

package com.threerings.opengl.gui.text;

/**
 * Defines the various commands handled by our text editing components.
 */
public interface EditCommands
{
    /** A text editing command. */
    public static final int ACTION = 0;

    /** A text editing command. */
    public static final int BACKSPACE = 1;

    /** A text editing command. */
    public static final int DELETE = 2;

    /** A text editing command. */
    public static final int CURSOR_LEFT = 3;

    /** A text editing command. */
    public static final int CURSOR_RIGHT = 4;

    /** A text editing command. */
    public static final int CURSOR_UP = 5;

    /** A text editing command. */
    public static final int CURSOR_DOWN = 6;

    /** A text editing command. */
    public static final int WORD_LEFT = 7;

    /** A text editing command. */
    public static final int WORD_RIGHT = 8;

    /** A text editing command. */
    public static final int START_OF_LINE = 9;

    /** A text editing command. */
    public static final int END_OF_LINE = 10;

    /** A text editing command. */
    public static final int RELEASE_FOCUS = 11;

    /** A text editing command. */
    public static final int CLEAR = 12;

    /** A text editing command. */
    public static final int CUT = 13;

    /** A text editing command. */
    public static final int COPY = 14;

    /** A text editing command. */
    public static final int PASTE = 15;

    /** A text editing command. */
    public static final int UNDO = 16;

    /** A text editing command. */
    public static final int REDO = 17;
}
