//
// $Id$

package com.threerings.opengl.model;

import com.threerings.opengl.model.ArticulatedModel.AnimationTrack;

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
    public boolean animationCancelled (AnimationTrack track);

    /**
     * Notifies the observer that the animation has completed.
     *
     * @return true to keep the observer in the list, false to remove it.
     */
    public boolean animationCompleted (AnimationTrack track);
}
