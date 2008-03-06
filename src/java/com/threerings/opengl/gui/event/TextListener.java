//
// $Id$

package com.threerings.opengl.gui.event;

/**
 * Used to communicate text modification events for text components.
 */
public interface TextListener extends ComponentListener
{
    /**
     * Dispatched when the text value changes in a text component.
     */
    public void textChanged (TextEvent event);
}
