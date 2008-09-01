//
// $Id$

package com.threerings.opengl.scene;

import com.threerings.math.Ray;
import com.threerings.math.Vector3f;

import com.threerings.opengl.util.GlContext;

/**
 * A scene based on a multiresolution spatial hashing scheme.
 */
public class HashScene extends Scene
{
    /**
     * Creates a new hash scene.
     *
     * @param size the size of the hash cells at the finest resolution.
     * @param levels the number of resolution levels in the hash space.
     */
    public HashScene (GlContext ctx, float finest, int levels)
    {
        super(ctx);
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
    }

    @Override // documentation inherited
    public void add (SceneElement element)
    {
    }

    @Override // documentation inherited
    public void remove (SceneElement element)
    {
    }

    @Override // documentation inherited
    public SceneElement getIntersection (Ray ray, Vector3f location)
    {
        return null;
    }
}
