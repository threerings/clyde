//
// $Id$

package com.threerings.opengl.mod;

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
