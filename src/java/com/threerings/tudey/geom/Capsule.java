//
// $Id$

package com.threerings.tudey.geom;

import com.threerings.math.FloatMath;

/**
 * A capsule shape.
 */
public final class Capsule extends Shape
{
    /**
     * Creates a capsule with the specified parameters.
     */
    public Capsule (float x1, float y1, float x2, float y2, float radius)
    {
        set(x1, y1, x2, y2, radius);
    }

    /**
     * Copy constructor.
     */
    public Capsule (Capsule other)
    {
        set(other);
    }

    /**
     * Creates an uninitialized capsule.
     */
    public Capsule ()
    {
    }

    /**
     * Sets the location of the capsule's vertices.
     */
    public void setVertices (float x1, float y1, float x2, float y2)
    {
        set(x1, y1, x2, y2, _radius);
    }

    /**
     * Sets the radius of the capsule.
     */
    public void setRadius (float radius)
    {
        set(_x1, _y1, _x2, _y2, radius);
    }

    /**
     * Copies the parameters of another capsule.
     */
    public void set (Capsule other)
    {
        set(other.getX1(), other.getY1(), other.getX2(), other.getY2(), other.getRadius());
    }

    /**
     * Sets the parameters of the capsule.
     */
    public void set (float x1, float y1, float x2, float y2, float radius)
    {
        if (_x1 == x1 && _y1 == y1 && _x2 == x2 && _y2 == y2 && _radius == radius) {
            return;
        }
        if (radius < 0f) {
            throw new IllegalArgumentException("Radius cannot be negative.");
        }
        willMove();
        _x1 = x1;
        _y1 = y1;
        _x2 = x2;
        _y2 = y2;
        _radius = radius;
        updateBounds();
        didMove();
    }

    /**
     * Returns the x coordinate of the first vertex.
     */
    public float getX1 ()
    {
        return _x1;
    }

    /**
     * Returns the y coordinate of the first vertex.
     */
    public float getY1 ()
    {
        return _y1;
    }

    /**
     * Returns the x coordinate of the second vertex.
     */
    public float getX2 ()
    {
        return _x2;
    }

    /**
     * Returns the y coordinate of the second vertex.
     */
    public float getY2 ()
    {
        return _y2;
    }

    /**
     * Returns the radius of the capsule.
     */
    public float getRadius ()
    {
        return _radius;
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        _bounds.set(
            Math.min(_x1, _x2) - _radius, Math.min(_y1, _y2) - _radius,
            Math.max(_x1, _x2) + _radius, Math.max(_y1, _y2) + _radius);
    }

    @Override // documentation inherited
    public boolean intersects (Shape other)
    {
        return other.checkIntersects(this);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        Capsule ocapsule;
        return super.equals(other) &&
            _x1 == (ocapsule = (Capsule)other)._x1 && _y1 == ocapsule._y1 &&
            _x2 == ocapsule._x2 && _y2 == ocapsule._y2 &&
            _radius == ocapsule._radius;
    }

    @Override // documentation inherited
    public String toString ()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Capsule[");
        builder.append("x1=").append(_x1).append(", ");
        builder.append("y1=").append(_y1).append(", ");
        builder.append("x2=").append(_x2).append(", ");
        builder.append("y2=").append(_y2).append(", ");
        builder.append("radius=").append(_radius);
        builder.append("]");
        return builder.toString();
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Point point)
    {
        return getMinimumDistanceSquared(point.getX(), point.getY()) <= _radius*_radius;
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Line line)
    {
        return checkIntersects(line.getX1(), line.getY1(), line.getX2(), line.getY2());
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Circle circle)
    {
        float rsum = _radius + circle.getRadius();
        return getMinimumDistanceSquared(circle.getX(), circle.getY()) <= rsum*rsum;
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Rectangle rectangle)
    {
        // check if the segment starts or ends inside the rectangle
        if (rectangle.checkIntersects(_x1, _y1) ||
                rectangle.checkIntersects(_x2, _y2)) {
            return true;
        }
        // check if the capsule intersects any side of the rectangle
        return checkIntersects(rectangle.getMinimumX(), rectangle.getMinimumY(), rectangle.getMaximumX(), rectangle.getMinimumY()) ||
                checkIntersects(rectangle.getMaximumX(), rectangle.getMinimumY(), rectangle.getMaximumX(), rectangle.getMaximumY()) ||
                checkIntersects(rectangle.getMaximumX(), rectangle.getMaximumY(), rectangle.getMinimumX(), rectangle.getMaximumY()) ||
                checkIntersects(rectangle.getMinimumX(), rectangle.getMaximumY(), rectangle.getMinimumX(), rectangle.getMinimumY());
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Capsule capsule)
    {
        float rsum = _radius + capsule.getRadius();
        return getMinimumDistanceSquared(capsule.getX1(), capsule.getY1(), capsule.getX2(), capsule.getY2()) <= rsum*rsum;
    }

    /**
     * Returns the square of the minimum distance from the line to the specified point.
     */
    protected float getMinimumDistanceSquared (float x, float y)
    {
        return GeomUtil.getMinimumDistanceSquared(_x1, _y1, _x2, _y2, x, y);
    }

    /**
     * Returns whether the capsule intersects the specified line segment.
     */
    protected boolean checkIntersects (
        float x1, float y1, float x2, float y2)
    {
        return getMinimumDistanceSquared(x1, y1, x2, y2) <= _radius*_radius;
    }

    /**
     * Returns the minimum distance between the capsule segment and the specified line segment.
     */
    protected float getMinimumDistanceSquared (
        float x1, float y1, float x2, float y2)
    {
        /* NOTE: adapted from www.geometrictools.com/Documentation/DistanceLine3Line3.pdf
         * Apparently this is the least complex way to do it.
         * Two line segments parameterized by s (the capsule segment) and t (the other segment).
         * Squared distance function Q(s, t) = as^2 + 2bst + ct^2 + 2ds + 2et + f, where the
         * letters are defined as below. 
         */
        float vx = _x2 - _x1, vy = _y2 - _y1;
        float ox = x2 - x1, oy = y2 - y1;
        float dx = _x1 - x1, dy = _y1 - y1;

        float a = vx*vx + vy*vy;
        float b = -vx*ox + -vy*oy;
        float c = ox*ox + oy*oy;
        float d = vx*dx + vy*dy;
        float e = -ox*dx + -oy*dy;
        float f = dx*dx + dy*dy;
        float det = a*c - b*b;

        float s, t, tmp;

        if (!FloatMath.epsilonEquals(0f, det)) { // segments are not parallel
            s = b*e - c*d;
            t = b*d - a*e;
            if (s >= 0f) {
                if (s <= det) {
                    if (t >= 0f) {
                        if (t <= det) { // region 0
                            float inv = 1f / det;
                            s *= inv;
                            t *= inv;
                            return a*s*s + 2*b*s*t + c*t*t + 2*d*s + 2*e*t + f;
                        } else { // region 3 (t=1)
                            tmp = b + d;
                            if (tmp >= 0f) { // s=0
                                return c + 2*e + f;
                            } else if (-tmp >= a) { // s=1
                                return a + c + f + 2*(e+tmp);
                            } else {
                                s = -tmp/a;
                                return tmp*s + c + 2*e + f;
                            }
                        }
                    } else { // region 7 (t=0)
                        if (d >= 0f) { // s=0
                            return f;
                        } else if (-d >= a) { // s=1
                            return a + 2*d + f;
                        } else {
                            s = -d/a;
                            return d*s + f;
                        }
                    }
                } else {
                    if (t >= 0f) {
                        if (t <= det) { // region 1 (s=1)
                            tmp = b + e;
                            if (tmp >= 0f) { // t=0
                                return a + 2*d + f;
                            } else if (-tmp >= c) { // t=1
                                return a + c + f + 2*(d+tmp);
                            } else {
                                t = -tmp/c;
                                return tmp*t + a + 2*d + f;
                            }
                        } else { // region 2
                            tmp = b + d;
                            if (-tmp <= a) { // t=1
                                if (tmp >= 0f) { // s=0
                                    return c + 2*e + f;
                                } else {
                                    s = -tmp/a;
                                    return tmp*s + c + 2*e + f;
                                }
                            } else { // s=1
                                tmp = b + e;
                                if (tmp >= 0f) { // t=0
                                    return a + 2*d + f;
                                } else if (-tmp >= c) { // t=1
                                    return a + c + f + 2*(d+tmp);
                                } else {
                                    t = -tmp/c;
                                    return tmp*t + a + 2*d + f;
                                }
                            }
                        }
                    } else { // region 8
                        if (-d < a) { // t=0
                            if (d >= 0f) { // s=0
                                return f;
                            } else {
                                s = -d/a;
                                return d*s + f;
                            }
                        } else { // s=1
                            tmp = b + e;
                            if (tmp >= 0f) { // t=0
                                return a + 2*d + f;
                            } else if (-tmp >= c) { // t=1
                                return a + c + f + 2*(d+tmp);
                            } else {
                                t = -tmp/c;
                                return tmp*t + a + 2*d + f;
                            }
                        }
                    }
                }
            } else {
                if (t >= 0f) {
                    if (t <= det) { // region 5 (s=0)
                        if (e >= 0f) { // t=0
                            return f;
                        } else if (-e >= c) { // t=1
                            return c + 2*e + f;
                        } else {
                            t = -e/c;
                            return e*t + f;
                        }
                    } else { // region 4
                        tmp = b + d;
                        if (tmp < 0f) { // t=1
                            if (-tmp >= a) { // s=1
                                return a + c + f + 2*(e+tmp);
                            } else {
                                s = -tmp/a;
                                return tmp*s + c + 2*e + f;
                            }
                        } else { // s=0
                            if (e >= 0f) { // t=0
                                return f;
                            } else if (-e >= c) {
                                return c + 2*e + f;
                            } else {
                                t = -e/c;
                                return e*t + f;
                            }
                        }
                    }
                } else { // region 6
                    if (d < 0f) { // t=0
                        if (-d >= a) { // s=1
                            return a + 2*d + f;
                        } else {
                            s = -d/a;
                            return d*s + f;
                        }
                    } else { // s=0
                        if (e >= 0f) { // t=0
                            return f;
                        } else if (-e >= c) { // t=1
                            return c + 2*e + f;
                        } else {
                            t = -e/c;
                            return e*t + f;
                        }
                    }
                }
            }
        } else { // segments are parallel
            if (b > 0f) { // dir vecs obtuse angle
                if (d >= 0f) { // s=0,t=0
                    return f;
                } else if (-d <= a) { // t=0
                    s = -d/a;
                    return d*s + f;
                } else { // s=1
                    tmp = a + d;
                    if (-tmp >= b) { // t=1
                        return a + c + f + 2*(b+d+e);
                    } else {
                        t = -tmp/b;
                        return a + 2*d + f + t*(c*t + 2*(b+e));
                    }
                }
            } else { // dir vecs acute angle
                if (-d >= a) { // s=1,t=0
                    return a + 2*d + f;
                } else if (d <= 0f) { // t=0
                    s = -d/a;
                    return d*s + f;
                } else { // s=0
                    if (d >= -b) { // t=1
                        return c + 2*e + f;
                    } else {
                        t = -d/b;
                        return f + t*(2*e + c*t);
                    }
                }
            }
            
        }
    }

    /** The first vertex. */
    protected float _x1, _y1;

    /** The second vertex. */
    protected float _x2, _y2;

    /** The radius. */
    protected float _radius;
}
