//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.math;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.samskivert.util.StringUtil;

import com.threerings.io.Streamable;

import com.threerings.editor.Editable;
import com.threerings.export.Encodable;

/**
 * A set of spherical coordinates.
 */
public class SphereCoords
    implements Encodable, Streamable
{
    /** The azimuth about the Z axis (in radians CCW from Y+). */
    @Editable(min=-180.0, max=180.0, scale=Math.PI/180.0, hgroup="c")
    public float azimuth;

    /** The elevation above the XY plane, in radians. */
    @Editable(min=-90.0, max=90.0, scale=Math.PI/180.0, hgroup="c")
    public float elevation;

    /** The distance from the origin. */
    @Editable(min=0.0, step=0.01, hgroup="c")
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
     * Interpolates between this and the specified other set of coordinates, storing the
     * result in this object.
     *
     * @return a reference to these coords, for chaining.
     */
    public SphereCoords lerpLocal (SphereCoords other, float t)
    {
        return lerp(other, t, this);
    }

    /**
     * Interpolates between this and the specified other set of coordinates.
     *
     * @return a new set of coordinates containing the result.
     */
    public SphereCoords lerp (SphereCoords other, float t)
    {
        return lerp(other, t, new SphereCoords());
    }

    /**
     * Interpolates between this and the specified other set of coordinates, storing the result in
     * the object provided.
     *
     * @return a reference to the result coords, for chaining.
     */
    public SphereCoords lerp (SphereCoords other, float t, SphereCoords result)
    {
        return result.set(
            FloatMath.lerpa(azimuth, other.azimuth, t),
            elevation + t*(other.elevation - elevation),
            distance + t*(other.distance - distance));
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

    @Override
    public String toString ()
    {
        return "[" + azimuth + ", " + elevation + ", " + distance + "]";
    }

    @Override
    public int hashCode ()
    {
        return Float.floatToIntBits(azimuth) ^ Float.floatToIntBits(elevation) ^
            Float.floatToIntBits(distance);
    }

    @Override
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
