//
// $Id$

package com.threerings.opengl.scene;

import com.threerings.math.Box;

import com.threerings.opengl.util.Intersectable;
import com.threerings.opengl.util.Renderable;
import com.threerings.opengl.util.Tickable;

/**
 * Interface for elements that can be embedded into scenes.
 */
public interface SceneElement extends SceneObject, Tickable, Intersectable, Renderable
{
    /** Determines when the {@link #tick} method must be called. */
    public enum TickPolicy { NEVER, WHEN_VISIBLE, ALWAYS };

    /**
     * Returns the policy that determines when the {@link #tick} method must be called.
     */
    public TickPolicy getTickPolicy ();

    /**
     * Notes that an influence has been added whose bounds intersect those of the element.
     */
    public void influenceAdded (SceneInfluence influence);

    /**
     * Notes that an intersecting influence has been removed.
     */
    public void influenceRemoved (SceneInfluence influence);
}
