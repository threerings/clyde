//
// $Id$

package com.threerings.opengl.mod;

/**
 * Notifies observers when animations complete or are cancelled.
 */
public interface AnimationObserver
{
    /**
     * Notes that an animation has started.
     *
     * @return true to keep the observer in the list, false to remove it.
     */
    public boolean animationStarted (Animation animation);

    /**
     * Notifies the observer that the animation has stopped.
     *
     * @param completed whether or not the animation ran to completion (as opposed to being
     * cancelled).
     * @return true to keep the observer in the list, false to remove it.
     */
    public boolean animationStopped (Animation animation, boolean completed);
}
