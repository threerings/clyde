//
// $Id$

package com.threerings.opengl.mod;

/**
 * Notifies observers when animations complete or are cancelled.
 */
public interface AnimationObserver
{
    /**
     * Notifies the observer that the animation has been cancelled.
     *
     * @return true to keep the observer in the list, false to remove it.
     */
    public boolean animationCancelled (Animation animation);

    /**
     * Notifies the observer that the animation has completed.
     *
     * @return true to keep the observer in the list, false to remove it.
     */
    public boolean animationCompleted (Animation animation);
}
