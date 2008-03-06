//
// $Id$

package com.threerings.opengl.gui.event;

/**
 * Listens for all input events.
 */
public interface EventListener extends ComponentListener
{
    /**
     * Indicates that an event was dispatched on the target component.
     */
    public void eventDispatched (Event event);
}
