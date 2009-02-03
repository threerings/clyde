//
// $Id$

package com.threerings.opengl.scene;

import com.threerings.opengl.material.Projection;

/**
 * Represents the influence of a projector.
 */
public class ProjectorInfluence extends SceneInfluence
{
    /**
     * Creates a new projector influence.
     */
    public ProjectorInfluence (Projection projection)
    {
        _projection = projection;
    }

    @Override // documentation inherited
    public Projection getProjection ()
    {
        return _projection;
    }

    /** The projection . */
    protected Projection _projection;
}
