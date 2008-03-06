//
// $Id$

package com.threerings.opengl.gui.event;

/**
 * Dispatches action events to interested parties.
 */
public interface ActionListener extends ComponentListener
{
    /**
     * Dispatched when a component has generated an "action".
     */
    public void actionPerformed (ActionEvent event);
}
