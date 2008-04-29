//
// $Id$

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
    public static final int WORD_LEFT = 5;

    /** A text editing command. */
    public static final int WORD_RIGHT = 6;

    /** A text editing command. */
    public static final int START_OF_LINE = 7;

    /** A text editing command. */
    public static final int END_OF_LINE = 8;

    /** A text editing command. */
    public static final int RELEASE_FOCUS = 9;

    /** A text editing command. */
    public static final int CLEAR = 10;

    /** A text editing command. */
    public static final int CUT = 11;

    /** A text editing command. */
    public static final int COPY = 12;

    /** A text editing command. */
    public static final int PASTE = 13;
}
