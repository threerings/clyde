//
// $Id$

package com.threerings.math;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.samskivert.util.StringUtil;

import com.threerings.io.Streamable;

import com.threerings.export.Encodable;

/**
 * A set of spherical coordinates.
 */
public class SphereCoords
    implements Encodable, Streamable
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
     * Creates a set of coordinates from three components.
     */
    public SphereCoords (float[] values)
    {
        set(values);
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
    public SphereCoords set (float[] values)
    {
        return set(values[0], values[1], values[2]);
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

    // documentation inherited from interface Encodable
    public String encodeToString ()
    {
        return azimuth + ", " + elevation + ", " + distance;
    }

    // documentation inherited from interface Encodable
    public void decodeFromString (String string)
        throws Exception
    {
        set(StringUtil.parseFloatArray(string));
    }

    // documentation inherited from interface Encodable
    public void encodeToStream (DataOutputStream out)
        throws IOException
    {
        out.writeFloat(azimuth);
        out.writeFloat(elevation);
        out.writeFloat(distance);
    }

    // documentation inherited from interface Encodable
    public void decodeFromStream (DataInputStream in)
        throws IOException
    {
        set(in.readFloat(), in.readFloat(), in.readFloat());
    }

    @Override // documentation inherited
    public String toString ()
    {
        return "[" + azimuth + ", " + elevation + ", " + distance + "]";
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return Float.floatToIntBits(azimuth) ^ Float.floatToIntBits(elevation) ^
            Float.floatToIntBits(distance);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        if (!(other instanceof SphereCoords)) {
            return false;
        }
        SphereCoords ocoords = (SphereCoords)other;
        return (azimuth == ocoords.azimuth && elevation == ocoords.elevation &&
            distance == ocoords.distance);
    }
}
