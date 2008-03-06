//
// $Id$

package com.threerings.opengl.gui.event;

/**
 * Generated when a model is changed.
 */
public class ChangeEvent extends Event
{
    public ChangeEvent (Object source)
    {
        super(source, -1L);
    }

    // documentation inherited
    public boolean propagateUpHierarchy ()
    {
        return false;
    }
}
