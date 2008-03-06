//
// $Id$

package com.threerings.opengl.gui.event;

/**
 * Dispatches mouse wheel events to listeners on a component.
 */
public interface MouseWheelListener extends ComponentListener
{
    /**
     * Dispatched when the mouse wheel is rotated within the bounds of the
     * target component.
     */
    public void mouseWheeled (MouseEvent event);
}
