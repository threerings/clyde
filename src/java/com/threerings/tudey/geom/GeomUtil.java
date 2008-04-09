//
// $Id$

package com.threerings.tudey.geom;

import com.threerings.math.FloatMath;

/**
 * Utility methods for geometry.
 */
public class GeomUtil
{
    /**
     * Returns the square of the distance between the two points.
     */
    public static float getDistanceSquared (float x1, float y1, float x2, float y2)
    {
        return (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1);
    }

    /**
     * Returns the minimum distance from the line segment to the specified point.
     */
    public static float getMinimumDistanceSquared (
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
}
