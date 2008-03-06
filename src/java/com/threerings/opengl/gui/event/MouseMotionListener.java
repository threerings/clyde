//
// $Id$

package com.threerings.opengl.gui.event;

/**
 * Dispatches mouse motion events to listeners on a component.
 */
public interface MouseMotionListener extends ComponentListener
{
    /**
     * Dispatched when the mouse is moved within the bounds of the target
     * component.
     */
    public void mouseMoved (MouseEvent event);

    /**
     * Dispatched when the mouse is moved after a button having been
     * pressed within the bounds of the target component.
     */
    public void mouseDragged (MouseEvent event);
}
