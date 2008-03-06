//
// $Id$

package com.threerings.opengl.gui.event;

/**
 * Used to dispatch notifications of text changes in text components.
 */
public class TextEvent extends Event
{
    public TextEvent (Object source, long when)
    {
        super(source, when);
    }

    // documentation inherited
    public void dispatch (ComponentListener listener)
    {
        super.dispatch(listener);

        if (listener instanceof TextListener) {
            ((TextListener)listener).textChanged(this);
        }
    }

    // documentation inherited
    public boolean propagateUpHierarchy ()
    {
        return false;
    }
}
