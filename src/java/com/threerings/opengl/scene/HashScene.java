//
// $Id$

package com.threerings.opengl.scene;

import java.util.HashMap;

import com.threerings.math.Box;
import com.threerings.math.FloatMath;
import com.threerings.math.Frustum;
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
     * @param finest the size of the hash cells at the finest resolution.
     * @param levels the number of resolution levels in the hash space.
     */
    public HashScene (GlContext ctx, float finest, int levels)
    {
        super(ctx);

        // create the levels
        _levels = new Level[levels];
        float size = finest;
        for (int ii = levels - 1; ii >= 0; ii--) {
            _levels[ii] = new Level(size);
            size *= 2f;
        }
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        // start at the coarsest level and work downwards
        _levels[0].enqueue(_ctx.getCompositor().getCamera().getWorldVolume());
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

    /**
     * Represents a single level of the hash space.
     */
    protected class Level
    {
        /**
         * Creates a new level with cells of the specified size.
         */
        public Level (float size)
        {
            _size = size;
            _rsize = 1f / size;
        }

        /**
         * Enqueues the elements at this level.
         */
        public void enqueue (Frustum frustum)
        {
            Box bounds = frustum.getBounds();
            Vector3f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
            int minx = (int)FloatMath.floor(min.x * _rsize);
            int maxx = (int)FloatMath.ceil(max.x * _rsize);
            int miny = (int)FloatMath.floor(min.y * _rsize);
            int maxy = (int)FloatMath.ceil(max.y * _rsize);
            int minz = (int)FloatMath.floor(min.z * _rsize);
            int maxz = (int)FloatMath.ceil(max.z * _rsize);
            Coord coord = _coord;
            for (int zz = minz; zz <= maxz; zz++) {
                for (int yy = miny; yy <= maxy; yy++) {
                    for (int xx = minx; xx <= maxx; xx++) {
                        Cell cell = _cells.get(coord.set(xx, yy, zz));
                        if (cell == null) {
                            continue;
                        }
                        Frustum.IntersectionType type = frustum.getIntersectionType(cell.bounds);
                        if (type == Frustum.IntersectionType.NONE) {
                            continue;
                        }

                    }
                }
            }
        }

        /** The size (and size reciprocal) of the cells. */
        protected float _size, _rsize;

        /** The cells at this level. */
        protected HashMap<Coord, Cell> _cells = new HashMap<Coord, Cell>();

        /** A reusable coord object for queries. */
        protected Coord _coord = new Coord();
    }

    /**
     * The contents of a single hash cell.
     */
    protected static class Cell
    {
        /** The bounds of the cell. */
        public Box bounds = new Box();
    }

    /**
     * The coordinates of a hash cell.
     */
    protected static class Coord
    {
        /** The coordinates of the cell. */
        public int x, y, z;

        /**
         * Sets the fields of the coord.
         *
         * @return a reference to this coord, for chaining.
         */
        public Coord set (int x, int y, int z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return x + 31*(y + 31*z);
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            Coord ocoord = (Coord)other;
            return x == ocoord.x && y == ocoord.y && z == ocoord.z;
        }
    }

    /** The levels of the hash, from coarsest to finest. */
    protected Level[] _levels;
}
