//
// $Id$

package com.threerings.tudey.tools;

import org.lwjgl.input.Keyboard;

import com.threerings.opengl.gui.event.KeyEvent;

import com.threerings.tudey.client.TudeySceneController;

/**
 * Scene controller for tools.
 */
public class ToolSceneController extends TudeySceneController
{
    @Override // documentation inherited
    public void keyPressed (KeyEvent event)
    {
        super.keyPressed(event);

        // go back to editor mode if escape is pressed
        if (event.getKeyCode() == Keyboard.KEY_ESCAPE) {
            _ctx.getLocationDirector().leavePlace();
        }
    }
}
