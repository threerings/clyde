//
// $Id$

package com.threerings.tudey.space;

import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

/**
 * Shapes that can be used for intersection testing.
 */
public abstract class Intersector
{
    /** Intersection types indicating that the intersector does not intersect, intersects, or fully
     * contains, respectively, the parameter. */
    public enum IntersectionType { NONE, INTERSECTS, CONTAINS };

    /**
     * A point intersector.
     */
    public static class Point extends Intersector
    {
        /**
         * Creates a point at the specified location.
         */
        public Point (Vector2f location)
        {
            _location.set(location);
            updateBounds();
        }

        /**
         * Creates an uninitialized point.
         */
        public Point ()
        {
        }

        /**
         * Returns a reference to the location of the point.
         */
        public Vector2f getLocation ()
        {
            return _location;
        }

        @Override // documentation inherited
        public void updateBounds ()
        {
            _bounds.getMinimumExtent().set(_location);
            _bounds.getMaximumExtent().set(_location);
        }

        @Override // documentation inherited
        public Intersector transform (Transform2D transform, Intersector result)
        {
            Point presult = (result instanceof Point) ? ((Point)result) : new Point();
            transform.transformPoint(_location, presult._location);
            presult.updateBounds();
            return presult;
        }

        @Override // documentation inherited
        public IntersectionType getIntersectionType (Rect rect)
        {
            return rect.contains(_location) ? IntersectionType.INTERSECTS : IntersectionType.NONE;
        }

        @Override // documentation inherited
        protected boolean reallyIntersects (SpaceElement element)
        {
            return element.intersects(this);
        }

        /** The location of the point. */
        protected Vector2f _location = new Vector2f();
    }

    /**
     * A line segment intersector.
     */
    public static class Segment extends Intersector
    {
        /**
         * Creates a segment between the specified points.
         */
        public Segment (Vector2f start, Vector2f end)
        {
            _start.set(start);
            _end.set(end);
            updateBounds();
        }

        /**
         * Creates an uninitialized segment.
         */
        public Segment ()
        {
        }

        /**
         * Returns a reference to the start vertex.
         */
        public Vector2f getStart ()
        {
            return _start;
        }

        /**
         * Returns a reference to the end vertex.
         */
        public Vector2f getEnd ()
        {
            return _end;
        }

        @Override // documentation inherited
        public void updateBounds ()
        {
            _bounds.setToEmpty();
            _bounds.addLocal(_start);
            _bounds.addLocal(_end);
        }

        @Override // documentation inherited
        public Intersector transform (Transform2D transform, Intersector result)
        {
            Segment sresult = (result instanceof Segment) ? ((Segment)result) : new Segment();
            transform.transformPoint(_start, sresult._start);
            transform.transformPoint(_end, sresult._end);
            sresult.updateBounds();
            return sresult;
        }

        @Override // documentation inherited
        public IntersectionType getIntersectionType (Rect rect)
        {
            return IntersectionType.NONE;
        }

        @Override // documentation inherited
        protected boolean reallyIntersects (SpaceElement element)
        {
            return element.intersects(this);
        }

        /** The start and end vertices. */
        protected Vector2f _start = new Vector2f(), _end = new Vector2f();
    }

    /**
     * A quad intersector.
     */
    public static class Quad extends Intersector
    {
        /**
         * Creates a quad with the supplied vertices.
         */
        public Quad (Vector2f... vertices)
        {
            for (int ii = 0; ii < 4; ii++) {
                _vertices[ii].set(vertices[ii]);
            }
            updateBounds();
        }

        /**
         * Creates an uninitialized quad.
         */
        public Quad ()
        {
        }

        /**
         * Returns a reference to the indexed vertex.
         */
        public Vector2f getVertex (int idx)
        {
            return _vertices[idx];
        }

        @Override // documentation inherited
        public void updateBounds ()
        {
            _bounds.fromPoints(_vertices);
        }

        @Override // documentation inherited
        public Intersector transform (Transform2D transform, Intersector result)
        {
            Quad qresult = (result instanceof Quad) ? ((Quad)result) : new Quad();
            for (int ii = 0; ii < 4; ii++) {
                transform.transformPoint(_vertices[ii], qresult._vertices[ii]);
            }
            qresult.updateBounds();
            return qresult;
        }

        @Override // documentation inherited
        public IntersectionType getIntersectionType (Rect rect)
        {
            return IntersectionType.NONE;
        }

        @Override // documentation inherited
        protected boolean reallyIntersects (SpaceElement element)
        {
            return element.intersects(this);
        }

        /** The vertices of the quad. */
        protected Vector2f[] _vertices = new Vector2f[] {
            new Vector2f(), new Vector2f(), new Vector2f(), new Vector2f() };
    }

    /**
     * A circle intersector.
     */
    public static class Circle extends Intersector
    {
        /** The radius of the circle. */
        public float radius;

        /**
         * Creates a circle with the supplied center and radius.
         */
        public Circle (Vector2f center, float radius)
        {
            _center.set(center);
            this.radius = radius;
            updateBounds();
        }

        /**
         * Creates an uninitialized circle.
         */
        public Circle ()
        {
        }

        /**
         * Returns a reference to the center of the circle.
         */
        public Vector2f getCenter ()
        {
            return _center;
        }

        @Override // documentation inherited
        public void updateBounds ()
        {
            _bounds.getMinimumExtent().set(_center);
            _bounds.getMaximumExtent().set(_center);
            _bounds.expandLocal(radius, radius);
        }

        @Override // documentation inherited
        public Intersector transform (Transform2D transform, Intersector result)
        {
            Circle cresult = (result instanceof Circle) ? ((Circle)result) : new Circle();
            transform.transformPoint(_center, cresult._center);
            cresult.radius = radius * transform.approximateUniformScale();
            cresult.updateBounds();
            return cresult;
        }

        @Override // documentation inherited
        public IntersectionType getIntersectionType (Rect rect)
        {
            return IntersectionType.NONE;
        }

        @Override // documentation inherited
        protected boolean reallyIntersects (SpaceElement element)
        {
            return element.intersects(this);
        }

        /** The center of the circle. */
        protected Vector2f _center = new Vector2f();
    }

    /**
     * A capsule intersector.
     */
    public static class Capsule extends Intersector
    {
        /** The radius of the capsule. */
        public float radius;

        /**
         * Creates a capsule with the supplied center and radius.
         */
        public Capsule (Vector2f start, Vector2f end, float radius)
        {
            _start.set(start);
            _end.set(end);
            this.radius = radius;
            updateBounds();
        }

        /**
         * Creates an uninitialized capsule.
         */
        public Capsule ()
        {
        }

        /**
         * Returns a reference to the start vertex.
         */
        public Vector2f getStart ()
        {
            return _start;
        }

        /**
         * Returns a reference to the end vertex.
         */
        public Vector2f getEnd ()
        {
            return _end;
        }

        @Override // documentation inherited
        public void updateBounds ()
        {
            _bounds.setToEmpty();
            _bounds.addLocal(_start);
            _bounds.addLocal(_end);
            _bounds.expandLocal(radius, radius);
        }

        @Override // documentation inherited
        public Intersector transform (Transform2D transform, Intersector result)
        {
            Capsule cresult = (result instanceof Capsule) ? ((Capsule)result) : new Capsule();
            transform.transformPoint(_start, cresult._start);
            transform.transformPoint(_end, cresult._end);
            cresult.radius = radius * transform.approximateUniformScale();
            cresult.updateBounds();
            return cresult;
        }

        @Override // documentation inherited
        public IntersectionType getIntersectionType (Rect rect)
        {
            return IntersectionType.NONE;
        }

        @Override // documentation inherited
        protected boolean reallyIntersects (SpaceElement element)
        {
            return element.intersects(this);
        }

        /** The start and end vertices of the capsule. */
        protected Vector2f _start = new Vector2f(), _end = new Vector2f();
    }

    /**
     * A polygon intersector.
     */
    public static class Polygon extends Intersector
    {
        /**
         * Creates a polygon with the supplied vertices.
         */
        public Polygon (Vector2f... vertices)
        {
            _vertices = new Vector2f[vertices.length];
            for (int ii = 0; ii < vertices.length; ii++) {
                _vertices[ii] = new Vector2f(vertices[ii]);
            }
            updateBounds();
        }

        /**
         * Creates an uninitialized polygon with the specified number of vertices.
         */
        public Polygon (int vcount)
        {
            initVertices(vcount);
        }

        /**
         * Returns the number of vertices in this polygon.
         */
        public int getVertexCount ()
        {
            return _vertices.length;
        }

        /**
         * Returns a reference to the indexed vertex.
         */
        public Vector2f getVertex (int idx)
        {
            return _vertices[idx];
        }

        @Override // documentation inherited
        public void updateBounds ()
        {
            _bounds.fromPoints(_vertices);
        }

        @Override // documentation inherited
        public Intersector transform (Transform2D transform, Intersector result)
        {
            Polygon presult = (result instanceof Polygon) ?
                ((Polygon)result) : new Polygon(_vertices.length);
            if (presult.getVertexCount() != _vertices.length) {
                presult.initVertices(_vertices.length);
            }
            for (int ii = 0; ii < _vertices.length; ii++) {
                transform.transformPoint(_vertices[ii], presult._vertices[ii]);
            }
            presult.updateBounds();
            return presult;
        }

        @Override // documentation inherited
        public IntersectionType getIntersectionType (Rect rect)
        {
            return IntersectionType.NONE;
        }

        @Override // documentation inherited
        protected boolean reallyIntersects (SpaceElement element)
        {
            return element.intersects(this);
        }

        /**
         * (Re)initializes the array of vertices.
         */
        protected void initVertices (int vcount)
        {
            _vertices = new Vector2f[vcount];
            for (int ii = 0; ii < vcount; ii++) {
                _vertices[ii] = new Vector2f();
            }
        }

        /** The vertices of the polygon. */
        protected Vector2f[] _vertices;
    }

    /**
     * A compound intersector.
     */
    public static class Compound extends Intersector
    {
        /**
         * Creates a compound containing the supplied intersectors (which will be referenced,
         * not copied).
         */
        public Compound (Intersector... intersectors)
        {
            _intersectors = intersectors;
            updateBounds();
        }

        /**
         * Creates an uninitialized compound.
         */
        public Compound (int icount)
        {
            _intersectors = new Intersector[icount];
        }

        /**
         * Returns the number of intersectors in this compound.
         */
        public int getIntersectorCount ()
        {
            return _intersectors.length;
        }

        /**
         * Returns a reference to the indexed intersector.
         */
        public Intersector getIntersector (int idx)
        {
            return _intersectors[idx];
        }

        @Override // documentation inherited
        public void updateBounds ()
        {
            _bounds.setToEmpty();
            for (Intersector intersector : _intersectors) {
                intersector.updateBounds();
                _bounds.addLocal(intersector.getBounds());
            }
        }

        @Override // documentation inherited
        public Intersector transform (Transform2D transform, Intersector result)
        {
            Compound cresult = (result instanceof Compound) ?
                ((Compound)result) : new Compound(_intersectors.length);
            if (cresult.getIntersectorCount() != _intersectors.length) {
                cresult._intersectors = new Intersector[_intersectors.length];
            }
            for (int ii = 0; ii < _intersectors.length; ii++) {
                cresult._intersectors[ii] = _intersectors[ii].transform(
                    transform, cresult._intersectors[ii]);
            }
            cresult.updateBounds();
            return cresult;
        }

        @Override // documentation inherited
        public IntersectionType getIntersectionType (Rect rect)
        {
            return IntersectionType.NONE;
        }

        @Override // documentation inherited
        protected boolean reallyIntersects (SpaceElement element)
        {
            return element.intersects(this);
        }

        /** The intersectors of which this intersector is composed. */
        protected Intersector[] _intersectors;
    }

    /**
     * Returns a reference to the bounds of the intersector.
     */
    public Rect getBounds ()
    {
        return _bounds;
    }

    /**
     * Updates the bounds of the intersector.
     */
    public abstract void updateBounds ();

    /**
     * Transforms this intersector in-place.
     *
     * @return a reference to this intersector, for chaining.
     */
    public Intersector transformLocal (Transform2D transform)
    {
        return transform(transform, this);
    }

    /**
     * Transforms this intersector.
     *
     * @return a new intersector containing the result.
     */
    public Intersector transform (Transform2D transform)
    {
        return transform(transform, null);
    }

    /**
     * Transforms this intersector, placing the result in the provided object if possible.
     *
     * @return a reference to the result object, if it was reused; otherwise, a new object
     * containing the result.
     */
    public abstract Intersector transform (Transform2D transform, Intersector result);

    /**
     * Checks whether the intersector intersects the specified rect.
     */
    public abstract IntersectionType getIntersectionType (Rect rect);

    /**
     * Determines whether this intersector intersects the specified element.
     */
    public boolean intersects (SpaceElement element)
    {
        switch (getIntersectionType(element.getBounds())) {
            default: case NONE: return false;
            case INTERSECTS: return reallyIntersects(element);
            case CONTAINS: return true;
        }
    }

    /**
     * Determines whether this intersector intersects the specified element (uses double-dispatch
     * to call the appropriate method in {@link SpaceElement}).
     */
    protected abstract boolean reallyIntersects (SpaceElement element);

    /** The bounds of the intersector. */
    protected Rect _bounds = new Rect();
}
