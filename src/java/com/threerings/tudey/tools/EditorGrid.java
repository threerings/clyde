//
// $Id$

package com.threerings.tudey.tools;

import com.threerings.math.Plane;
import com.threerings.math.Ray3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.util.Grid;
import com.threerings.opengl.util.Tickable;

import com.threerings.tudey.util.TudeySceneMetrics;

/**
 * A special grid that follows the camera around (while staying aligned to the tile grid).
 */
public class EditorGrid extends Grid
    implements Tickable
{
    /**
     * Creates a new grid.
     */
    public EditorGrid (SceneEditor editor)
    {
        super(editor, LINE_COUNT, 1f);
    }

    /**
     * Sets the elevation of the grid in tile units.
     */
    public void setElevation (int elevation)
    {
        _elevation = elevation;
        _plane.constant = -getZ();
    }

    /**
     * Returns the elevation of the tile grid.
     */
    public int getElevation ()
    {
        return _elevation;
    }

    /**
     * Returns the z coordinate of the tile grid.
     */
    public float getZ ()
    {
        return TudeySceneMetrics.getZ(_elevation);
    }

    /**
     * Returns a reference to the grid plane.
     */
    public Plane getPlane ()
    {
        return _plane;
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        // find out where the camera is looking on the grid plane
        _ctx.getCompositor().getCamera().getCenterRay(_target);
        _plane.getIntersection(_target, _isect);

        // center the snapped grid
        int gx = (int)_isect.x;
        int gy = (int)_isect.y;

        // update our transform
        _transform.getTranslation().set(gx, gy, getZ());
    }

    /** The elevation of the grid in tile units. */
    protected int _elevation;

    /** The grid plane. */
    protected Plane _plane = new Plane(Plane.XY_PLANE);

    /** A working ray for intersection testing. */
    protected Ray3D _target = new Ray3D();

    /** A working vector for intersection testing. */
    protected Vector3f _isect = new Vector3f();

    /** The number of grid lines in each direction (must be odd so that the center is a line
     * intersection). */
    protected static final int LINE_COUNT = 65;
}
