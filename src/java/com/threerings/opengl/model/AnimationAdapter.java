//
// $Id: AnimationAdapter.java 270 2008-08-22 20:52:23Z andrzej $

package com.threerings.opengl.model;

/**
 * An adapter class for {@link AnimationObserver}.
 */
public class AnimationAdapter
    implements AnimationObserver
{
    // documentation inherited from interface AnimationObserver
    public boolean animationStarted (Animation animation)
    {
        return true;
    }

    // documentation inherited from interface AnimationObserver
    public boolean animationStopped (Animation animation, boolean completed)
    {
        return true;
    }
}
