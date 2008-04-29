//
// $Id$

package com.threerings.opengl.gui.event;

/**
 * Dispatches mouse events to listeners on a component.
 */
public interface MouseListener extends ComponentListener
{
    /**
     * Dispatched when a button is pressed within the bounds of the target component.
     */
    public void mousePressed (MouseEvent event);

    /**
     * Dispatched when a button is released after having been pressed within the bounds of the
     * target component.
     */
    public void mouseReleased (MouseEvent event);

    /**
     * Dispatched when a button is clicked within the bounds of the target component.
     */
    public void mouseClicked (MouseEvent event);

    /**
     * Dispatched when the mouse enters the bounds of the target component.
     */
    public void mouseEntered (MouseEvent event);

    /**
     * Dispatched when the mouse exits the bounds of the target component.
     */
    public void mouseExited (MouseEvent event);
}
