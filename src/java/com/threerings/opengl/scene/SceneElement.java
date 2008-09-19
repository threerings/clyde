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
     * Returns this element's user object reference.
     */
    public Object getUserObject ();

    /**
     * Notes that the element was added to the specified scene.
     */
    public void wasAdded (Scene scene);

    /**
     * Notes that the element will be removed from the scene.
     */
    public void willBeRemoved ();

    /**
     * Sets the influences affecting this element.
     */
    public void setInfluences (SceneInfluenceSet influences);
}
