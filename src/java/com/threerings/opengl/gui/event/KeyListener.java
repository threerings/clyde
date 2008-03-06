//
// $Id$

package com.threerings.opengl.gui.event;

/**
 * Dispatches key events to listeners on a component.
 */
public interface KeyListener extends ComponentListener
{
    /**
     * Dispatched when a key is pressed within the bounds of the target
     * component.
     */
    public void keyPressed (KeyEvent event);

    /**
     * Dispatched when a key is released after having been pressed
     * within the bounds of the target component.
     */
    public void keyReleased (KeyEvent event);
}
