//
// $Id$

package com.threerings.math;

/**
 * A set of spherical coordinates.
 */
public class SphereCoords
{
    /** The azimuth about the Z axis (in radians CCW from Y+). */
    public float azimuth;

    /** The elevation above the XY plane, in radians. */
    public float elevation;

    /** The distance from the origin. */
    public float distance;

    /**
     * Creates a set of coordinates from three components.
     */
    public SphereCoords (float azimuth, float elevation, float distance)
    {
        set(azimuth, elevation, distance);
    }

    /**
     * Copy constructor.
     */
    public SphereCoords (SphereCoords other)
    {
        set(other);
    }

    /**
     * Creates a set of zero coordinates.
     */
    public SphereCoords ()
    {
    }

    /**
     * Copies the elements of another set of coordinates.
     *
     * @return a reference to these coordinates, for chaining.
     */
    public SphereCoords set (SphereCoords other)
    {
        return set(other.azimuth, other.elevation, other.distance);
    }

    /**
     * Sets all of the elements of the coordinates.
     *
     * @return a reference to these coordinates, for chaining.
     */
    public SphereCoords set (float azimuth, float elevation, float distance)
    {
        this.azimuth = azimuth;
        this.elevation = elevation;
        this.distance = distance;
        return this;
    }

    @Override // documentation inherited
    public String toString ()
    {
        return "[" + azimuth + ", " + elevation + ", " + distance + "]";
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        SphereCoords ocoords = (SphereCoords)other;
        return (azimuth == ocoords.azimuth && elevation == ocoords.elevation &&
            distance == ocoords.distance);
    }
}
