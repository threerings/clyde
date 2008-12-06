//
// $Id$

package com.threerings.opengl.model;

/**
 * Notifies observers of model events (including animation events).
 */
public interface ModelObserver extends AnimationObserver
{
    /**
     * Notes that a model (such as a transient effect) has completed.
     *
     * @return true to keep the observer in the list, false to remove it.
     */
    public boolean modelCompleted (Model model);
}
