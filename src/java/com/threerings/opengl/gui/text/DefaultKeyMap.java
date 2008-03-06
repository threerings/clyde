//
// $Id$

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

        addMapping(ANY_MODIFIER, Keyboard.KEY_HOME, EditCommands.START_OF_LINE);
        addMapping(ANY_MODIFIER, Keyboard.KEY_END, EditCommands.END_OF_LINE);

        addMapping(
                ANY_MODIFIER, Keyboard.KEY_ESCAPE, EditCommands.RELEASE_FOCUS);

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
