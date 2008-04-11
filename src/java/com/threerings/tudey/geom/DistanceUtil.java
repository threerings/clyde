//
// $Id$

package com.threerings.tudey.geom;

import com.threerings.math.FloatMath;

/**
 * Utility methods for distances between geometry.
 */
public class DistanceUtil
{
    /**
     * Returns the square of the distance between the two points.
     */
    public static float getPointPoint2 (float x1, float y1, float x2, float y2)
    {
        return (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1);
    }

    /**
     * Returns the sqaured of the minimum distance from the line segment to the specified point.
     */
    public static float getLinePoint2 (
        float x1, float y1, float x2, float y2, float x, float y)
    {
        float vx = (x2 - x1), vy = (y2 - y1);
        float px = (x - x1), py = (y - y1);
        float t = px*vx + py*vy;
        if (t < 0f) { // point closest to start
            return px*px + py*py;
        }
        float d = vx*vx + vy*vy;
        if (t > d) { // point closest to end
            float wx = (x - x2), wy = (y - y2);
            return wx*wx + wy*wy;
        } else { // point closest to middle
            float u = px*px + py*py;
            return u - t*t/d;
        }
    }

    /**
     * Returns the square of the minimum distance between the two line segments.
     */
    public static float getLineLine2 (
        float ax1, float ay1, float ax2, float ay2,
        float bx1, float by1, float bx2, float by2)
    {
        /* NOTE: adapted from www.geometrictools.com/Documentation/DistanceLine3Line3.pdf
         * Apparently this is the least complex way to do it.
         * Two line segments parameterized by s (the capsule segment) and t (the other segment).
         * Squared distance function Q(s, t) = as^2 + 2bst + ct^2 + 2ds + 2et + f, where the
         * letters are defined as below. 
         */
        float vx = ax2 - ax1, vy = ay2 - ay1;
        float ox = bx2 - bx1, oy = by2 - by1;
        float dx = ax1 - bx1, dy = ay1 - by1;

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
}
