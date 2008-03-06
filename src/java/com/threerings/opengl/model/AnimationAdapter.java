//
// $Id$

package com.threerings.opengl.model;

import com.threerings.opengl.model.ArticulatedModel.AnimationTrack;

/**
 * Minimal implementation of {@link AnimationObserver}.
 */
public class AnimationAdapter
    implements AnimationObserver
{
    /**
     * Creates a new adapter.
     *
     * @param keep whether or not to keep the observer in the list by default.
     */
    public AnimationAdapter (boolean keep)
    {
        _keep = keep;
    }

    // documentation inherited from interface AnimationObserver
    public boolean animationCancelled (AnimationTrack track)
    {
        return _keep;
    }

    // documentation inherited from interface AnimationObserver
    public boolean animationCompleted (AnimationTrack track)
    {
        return _keep;
    }

    /** Whether or not to keep the observer in the list. */
    protected boolean _keep;
}
