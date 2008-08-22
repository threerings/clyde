//
// $Id$

package com.threerings.opengl.mod;

/**
 * An adapter class for {@link ModelObserver}.
 */
public class ModelAdapter extends AnimationAdapter
    implements ModelObserver
{
    // documentation inherited from interface ModelObserver
    public boolean modelCompleted (Model model)
    {
        return true;
    }
}
