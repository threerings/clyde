//
// $Id$

package com.threerings.opengl.gui.event;

/**
 * An interface used to inform listeners when a model has changed.
 */
public interface ChangeListener
{
    /**
     * Indicates that the underlying model has changed.
     */
    public void stateChanged (ChangeEvent event);
}
