//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// https://github.com/threerings/nenya
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.geom;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import static com.threerings.geom.Log.log;

/**
 * General geometry utilities.
 */
public class GeomUtil
{
    /**
     * Computes and returns the dot product of the two vectors.
     *
     * @param v1s the starting point of the first vector.
     * @param v1e the ending point of the first vector.
     * @param v2s the starting point of the second vector.
     * @param v2e the ending point of the second vector.
     */
    public static int dot (Point v1s, Point v1e, Point v2s, Point v2e)
    {
        return ((v1e.x - v1s.x) * (v2e.x - v2s.x) + (v1e.y - v1s.y) * (v2e.y - v2s.y));
    }

    /**
     * Computes and returns the dot product of the two vectors.  See {@link
     * #dot(Point,Point,Point,Point)} for an explanation of the arguments
     */
    public static int dot (int v1sx, int v1sy, int v1ex, int v1ey,
                           int v2sx, int v2sy, int v2ex, int v2ey)
    {
        return ((v1ex - v1sx) * (v2ex - v2sx) + (v1ey - v1sy) * (v2ey - v2sy));
    }

    /**
     * Computes and returns the dot product of the two vectors. The vectors are assumed to start
     * with the same coordinate and end with different coordinates.
     *
     * @param vs the starting point of both vectors.
     * @param v1e the ending point of the first vector.
     * @param v2e the ending point of the second vector.
     */
    public static int dot (Point vs, Point v1e, Point v2e)
    {
        return ((v1e.x - vs.x) * (v2e.x - vs.x) + (v1e.y - vs.y) * (v2e.y - vs.y));
    }

    /**
     * Computes and returns the dot product of the two vectors.  See {@link
     * #dot(Point,Point,Point)} for an explanation of the arguments
     */
    public static int dot (int vsx, int vsy, int v1ex, int v1ey, int v2ex, int v2ey)
    {
        return ((v1ex - vsx) * (v2ex - vsx) + (v1ey - vsy) * (v2ey - vsy));
    }

    /**
     * Computes the point nearest to the specified point <code>p3</code> on the line defined by the
     * two points <code>p1</code> and <code>p2</code>. The computed point is stored into
     * <code>n</code>.  <em>Note:</em> <code>p1</code> and <code>p2</code> must not be coincident.
     *
     * @param p1 one point on the line.
     * @param p2 another point on the line (not equal to <code>p1</code>).
     * @param p3 the point to which we wish to be most near.
     * @param n the point on the line defined by <code>p1</code> and <code>p2</code> that is
     * nearest to <code>p</code>.
     *
     * @return the point object supplied via <code>n</code>.
     */
    public static Point nearestToLine (Point p1, Point p2, Point p3, Point n)
    {
        // see http://astronomy.swin.edu.au/~pbourke/geometry/pointline/ for a (not very good)
        // explanation of the math
        int Ax = p2.x - p1.x, Ay = p2.y - p1.y;
        float u = (p3.x - p1.x) * Ax + (p3.y - p1.y) * Ay;
        u /= (Ax * Ax + Ay * Ay);
        n.x = p1.x + Math.round(Ax * u);
        n.y = p1.y + Math.round(Ay * u);
        return n;
    }

    /**
     * Calculate the intersection of two lines. Either line may be considered as a line segment,
     * and the intersecting point is only considered valid if it lies upon the segment.  Note that
     * Point extends Point2D.
     *
     * @param p1 and p2 the coordinates of the first line.
     * @param seg1 if the first line should be considered a segment.
     * @param p3 and p4 the coordinates of the second line.
     * @param seg2 if the second line should be considered a segment.
     * @param result the point that will be filled in with the intersecting point.
     *
     * @return true if result was filled in, or false if the lines are parallel or the point of
     * intersection lies outside of a segment.
     */
    public static boolean lineIntersection (Point2D p1, Point2D p2, boolean seg1,
                                            Point2D p3, Point2D p4, boolean seg2, Point2D result)
    {
        // see http://astronomy.swin.edu.au/~pbourke/geometry/lineline2d/
        double y43 = p4.getY() - p3.getY();
        double x21 = p2.getX() - p1.getX();
        double x43 = p4.getX() - p3.getX();
        double y21 = p2.getY() - p1.getY();
        double denom = y43 * x21 - x43 * y21;
        if (denom == 0) {
            return false;
        }

        double y13 = p1.getY() - p3.getY();
        double x13 = p1.getX() - p3.getX();
        double ua = (x43 * y13 - y43 * x13) / denom;
        if (seg1 && ((ua < 0) || (ua > 1))) {
            return false;
        }

        if (seg2) {
            double ub = (x21 * y13 - y21 * x13) / denom;
            if ((ub < 0) || (ub > 1)) {
                return false;
            }
        }

        double x = p1.getX() + ua * x21;
        double y = p1.getY() + ua * y21;
        result.setLocation(x, y);
        return true;
    }

    /**
     * Returns less than zero if <code>p2</code> is on the left hand side of the line created by
     * <code>p1</code> and <code>theta</code> and greater than zero if it is on the right hand
     * side. In theory, it will return zero if the point is on the line, but due to rounding errors
     * it almost always decides that it's not exactly on the line.
     *
     * @param p1 the point on the line whose side we're checking.
     * @param theta the (logical) angle defining the line.
     * @param p2 the point that lies on one side or the other of the line.
     */
    public static int whichSide (Point p1, double theta, Point p2)
    {
        // obtain the point defining the right hand normal (N)
        theta += Math.PI/2;
        int x = p1.x + (int)Math.round(1000*Math.cos(theta)),
            y = p1.y + (int)Math.round(1000*Math.sin(theta));

        // now dot the vector from p1->p2 with the vector from p1->N, if it's positive, we're on
        // the right hand side, if it's negative we're on the left hand side and if it's zero,
        // we're on the line
        return dot(p1.x, p1.y, p2.x, p2.y, x, y);
    }

    /**
     * Shifts the position of the <code>tainer</code> rectangle to ensure that it contains the
     * <code>tained</code> rectangle. The <code>tainer</code> rectangle must be larger than or
     * equal to the size of the <code>tained</code> rectangle.
     */
    public static void shiftToContain (Rectangle tainer, Rectangle tained)
    {
        if (tained.x < tainer.x) {
            tainer.x = tained.x;
        }
        if (tained.y < tainer.y) {
            tainer.y = tained.y;
        }
        if (tained.x + tained.width > tainer.x + tainer.width) {
            tainer.x = tained.x - (tainer.width - tained.width);
        }
        if (tained.y + tained.height > tainer.y + tainer.height) {
            tainer.y = tained.y - (tainer.height - tained.height);
        }
    }

    /**
     * Adds the target rectangle to the bounds of the source rectangle. If the source rectangle is
     * null, a new rectangle is created that is the size of the target rectangle.
     *
     * @return the source rectangle.
     */
    public static Rectangle grow (Rectangle source, Rectangle target)
    {
        if (target == null) {
            log.warning("Can't grow with null rectangle [src=" + source + ", tgt=" + target + "].",
                        new Exception());
        } else if (source == null) {
            source = new Rectangle(target);
        } else {
            source.add(target);
        }
        return source;
    }

    /**
     * Returns the rectangle containing the specified tile in the supplied larger rectangle. Tiles
     * go from left to right, top to bottom.
     */
    public static Rectangle getTile (
        int width, int height, int tileWidth, int tileHeight, int tileIndex)
    {
        Rectangle bounds = new Rectangle();
        getTile(width, height, tileWidth, tileHeight, tileIndex, bounds);
        return bounds;
    }

    /**
     * Fills in the bounds of the specified tile in the supplied larger rectangle. Tiles go from
     * left to right, top to bottom.
     */
    public static void getTile (int width, int height, int tileWidth, int tileHeight, int tileIndex,
                                Rectangle bounds)
    {
        // figure out from whence to crop the tile
        int tilesPerRow = width / tileWidth;

        // if we got a bogus region, return bogus tile bounds
        if (tilesPerRow == 0) {
            bounds.setBounds(0, 0, width, height);

        } else {
            int row = tileIndex / tilesPerRow;
            int col = tileIndex % tilesPerRow;
            // crop the tile-sized image chunk from the full image
            bounds.setBounds(tileWidth*col, tileHeight*row, tileWidth, tileHeight);
        }
    }
}
