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

import com.threerings.export.Encodable;

/**
 * A unit quaternion.  Many of the formulas come from the
 * <a href="http://www.j3d.org/matrix_faq/matrfaq_latest.html">Matrix and Quaternion FAQ</a>.
 */
public final class Quaternion
    implements Encodable, Streamable
{
    /** The identity quaternion. */
    public static final Quaternion IDENTITY = new Quaternion(0f, 0f, 0f, 1f);

    /** The components of the quaternion. */
    public float x, y, z, w;

    /**
     * Creates a quaternion from four components.
     */
    public Quaternion (float x, float y, float z, float w)
    {
        set(x, y, z, w);
    }

    /**
     * Creates a quaternion from an array of values.
     */
    public Quaternion (float[] values)
    {
        set(values);
    }

    /**
     * Copy constructor.
     */
    public Quaternion (Quaternion other)
    {
        set(other);
    }

    /**
     * Creates an identity quaternion.
     */
    public Quaternion ()
    {
        set(0f, 0f, 0f, 1f);
    }

    /**
     * Sets this quaternion to the rotation of the first normalized vector onto the second.
     *
     * @return a reference to this quaternion, for chaining.
     */
    public Quaternion fromVectors (Vector3f from, Vector3f to)
    {
        float angle = from.angle(to);
        if (angle < FloatMath.EPSILON) {
            return set(IDENTITY);
        }
        if (angle <= FloatMath.PI - FloatMath.EPSILON) {
            return fromAngleAxis(angle, from.cross(to).normalizeLocal());
        }
        // it's a 180 degree rotation; any axis orthogonal to the from vector will do
        Vector3f axis = new Vector3f(0f, from.z, -from.y);
        float length = axis.length();
        return fromAngleAxis(FloatMath.PI, length < FloatMath.EPSILON ?
            axis.set(-from.z, 0f, from.x).normalizeLocal() : axis.multLocal(1f / length));
    }

    /**
     * Sets this quaternion to the rotation of (0, 0, -1) onto the supplied normalized vector.
     *
     * @return a reference to the quaternion, for chaining.
     */
    public Quaternion fromVectorFromNegativeZ (Vector3f to)
    {
        return fromVectorFromNegativeZ(to.x, to.y, to.z);
    }

    /**
     * Sets this quaternion to the rotation of (0, 0, -1) onto the supplied normalized vector.
     *
     * @return a reference to the quaternion, for chaining.
     */
    public Quaternion fromVectorFromNegativeZ (float tx, float ty, float tz)
    {
        float angle = FloatMath.acos(-tz);
        if (angle < FloatMath.EPSILON) {
            return set(IDENTITY);
        }
        if (angle > FloatMath.PI - FloatMath.EPSILON) {
            return set(0f, 1f, 0f, 0f); // 180 degrees about y
        }
        float len = FloatMath.hypot(tx, ty);
        return fromAngleAxis(angle, ty/len, -tx/len, 0f);
    }

    /**
     * Sets this quaternion to one that rotates onto the given unit axes.
     *
     * @return a reference to this quaternion, for chaining.
     */
    public Quaternion fromAxes (Vector3f nx, Vector3f ny, Vector3f nz)
    {
        float x2 = (1f + nx.x - ny.y - nz.z)/4f;
        float y2 = (1f - nx.x + ny.y - nz.z)/4f;
        float z2 = (1f - nx.x - ny.y + nz.z)/4f;
        float w2 = (1f - x2 - y2 - z2);
        return set(
            FloatMath.sqrt(x2) * (ny.z >= nz.y ? +1f : -1f),
            FloatMath.sqrt(y2) * (nz.x >= nx.z ? +1f : -1f),
            FloatMath.sqrt(z2) * (nx.y >= ny.x ? +1f : -1f),
            FloatMath.sqrt(w2));
    }

    /**
     * Sets this quaternion to the rotation described by the given angle and normalized
     * axis.
     *
     * @return a reference to this quaternion, for chaining.
     */
    public Quaternion fromAngleAxis (float angle, Vector3f axis)
    {
        return fromAngleAxis(angle, axis.x, axis.y, axis.z);
    }

    /**
     * Sets this quaternion to the rotation described by the given angle and normalized
     * axis.
     *
     * @return a reference to this quaternion, for chaining.
     */
    public Quaternion fromAngleAxis (float angle, float x, float y, float z)
    {
        float sina = FloatMath.sin(angle / 2f);
        return set(x*sina, y*sina, z*sina, FloatMath.cos(angle / 2f));
    }

    /**
     * Sets this to a random rotation obtained from a completely uniform distribution.
     */
    public Quaternion randomize ()
    {
        // pick angles according to the surface area distribution
        return fromAngles(
            FloatMath.random(-FloatMath.PI, +FloatMath.PI),
            FloatMath.asin(FloatMath.random(-1f, +1f)),
            FloatMath.random(-FloatMath.PI, +FloatMath.PI));
    }

    /**
     * Sets this quaternion to one that first rotates about x by the specified number of radians,
     * then rotates about z by the specified number of radians.
     */
    public Quaternion fromAnglesXZ (float x, float z)
    {
        float hx = x * 0.5f, hz = z * 0.5f;
        float sx = FloatMath.sin(hx), cx = FloatMath.cos(hx);
        float sz = FloatMath.sin(hz), cz = FloatMath.cos(hz);
        return set(cz*sx, sz*sx, sz*cx, cz*cx);
    }

    /**
     * Sets this quaternion to one that first rotates about x by the specified number of radians,
     * then rotates about y by the specified number of radians.
     */
    public Quaternion fromAnglesXY (float x, float y)
    {
        float hx = x * 0.5f, hy = y * 0.5f;
        float sx = FloatMath.sin(hx), cx = FloatMath.cos(hx);
        float sy = FloatMath.sin(hy), cy = FloatMath.cos(hy);
        return set(cy*sx, sy*cx, -sy*sx, cy*cx);
    }

    /**
     * Sets this quaternion to one that first rotates about x by the specified number of radians,
     * then rotates about y, then about z.
     */
    public Quaternion fromAngles (Vector3f angles)
    {
        return fromAngles(angles.x, angles.y, angles.z);
    }

    /**
     * Sets this quaternion to one that first rotates about x by the specified number of radians,
     * then rotates about y, then about z.
     */
    public Quaternion fromAngles (float x, float y, float z)
    {
        // TODO: it may be more convenient to define the angles in the opposite order (first z,
        // then y, then x)
        float hx = x * 0.5f, hy = y * 0.5f, hz = z * 0.5f;
        float sz = FloatMath.sin(hz), cz = FloatMath.cos(hz);
        float sy = FloatMath.sin(hy), cy = FloatMath.cos(hy);
        float sx = FloatMath.sin(hx), cx = FloatMath.cos(hx);
        float szsy = sz*sy, czsy = cz*sy, szcy = sz*cy, czcy = cz*cy;
        return set(
            czcy*sx - szsy*cx,
            czsy*cx + szcy*sx,
            szcy*cx - czsy*sx,
            czcy*cx + szsy*sx);
    }

    /**
     * Sets this quaternion to one that first rotates about z by the specified number of radians,
     * then rotates about x, then about y.
     */
    public Quaternion fromAnglesZXY (float x, float y, float z)
    {
        float hx = x * 0.5f, hy = y * 0.5f, hz = z * 0.5f;
        float sz = FloatMath.sin(hz), cz = FloatMath.cos(hz);
        float sy = FloatMath.sin(hy), cy = FloatMath.cos(hy);
        float sx = FloatMath.sin(hx), cx = FloatMath.cos(hx);
        float sysx = sy*sx, cysx = cy*sx, sycx = sy*cx, cycx = cy*cx;
        return set(
            sycx*sz + cysx*cz,
            sycx*cz - cysx*sz,
            cycx*sz - sysx*cz,
            cycx*cz + sysx*sz);
    }

    /**
     * Computes the angles to pass to {@link #fromAngles} to reproduce this rotation, placing them
     * in the provided vector.  This uses the factorization method described in David Eberly's
     * <a href="http://www.geometrictools.com/Documentation/EulerAngles.pdf">Euler Angle
     * Formulas</a>.
     *
     * @return a reference to the result vector, for chaining.
     */
    public Vector3f toAngles (Vector3f result)
    {
        float sy = 2f*(y*w - x*z);
        if (sy < 1f - FloatMath.EPSILON) {
            if (sy > -1 + FloatMath.EPSILON) {
                return result.set(
                    FloatMath.atan2(y*z + x*w, 0.5f - (x*x + y*y)),
                    FloatMath.asin(sy),
                    FloatMath.atan2(x*y + z*w, 0.5f - (y*y + z*z)));
            } else {
                // not a unique solution; x + z = atan2(-m21, m11)
                return result.set(
                    0f,
                    -FloatMath.HALF_PI,
                    FloatMath.atan2(x*w - y*z, 0.5f - (x*x + z*z)));
            }
        } else {
            // not a unique solution; x - z = atan2(-m21, m11)
            return result.set(
                0f,
                FloatMath.HALF_PI,
                -FloatMath.atan2(x*w - y*z, 0.5f - (x*x + z*z)));
        }
    }

    /**
     * Computes the angles to pass to {@link #fromAnglesZXY} to reproduce this rotation, placing
     * them in the provided vector.
     *
     * @return a reference to the result vector, for chaining.
     */
    public Vector3f toAnglesZXY (Vector3f result)
    {
        float sx = 2f*(x*w - y*z);
        if (sx < 1f - FloatMath.EPSILON) {
            if (sx > -1 + FloatMath.EPSILON) {
                return result.set(
                    FloatMath.asin(sx),
                    FloatMath.atan2(x*z + y*w, 0.5f - (x*x + y*y)),
                    FloatMath.atan2(x*y + z*w, 0.5f - (x*x + z*z)));
            } else {
                // not a unique solution; y - z = atan2(m02, m00)
                return result.set(
                    -FloatMath.HALF_PI,
                    0f,
                    FloatMath.atan2(y*w - x*z, 0.5f - (y*y + z*z)));
            }
        } else {
            // not a unique solution; y + z = atan2(m02, m00)
            return result.set(
                FloatMath.HALF_PI,
                0f,
                -FloatMath.atan2(y*w - x*z, 0.5f - (y*y + z*z)));
        }
    }

    /**
     * Computes and returns the angles to pass to {@link #fromAngles} to reproduce this rotation.
     *
     * @return a new vector containing the resulting angles.
     */
    public Vector3f toAngles ()
    {
        return toAngles(new Vector3f());
    }

    /**
     * Computes and returns the angles to pass to {@link #fromAnglesZXY} to reproduce this rotation.
     *
     * @return a new vector containing the resulting angles.
     */
    public Vector3f toAnglesZXY ()
    {
        return toAnglesZXY(new Vector3f());
    }

    /**
     * Normalizes this quaternion in-place.
     *
     * @return a reference to this quaternion, for chaining.
     */
    public Quaternion normalizeLocal ()
    {
        return normalize(this);
    }

    /**
     * Normalizes this quaternion.
     *
     * @return a new quaternion containing the result.
     */
    public Quaternion normalize ()
    {
        return normalize(new Quaternion());
    }

    /**
     * Normalizes this quaternion, storing the result in the object provided.
     *
     * @return a reference to the result, for chaining.
     */
    public Quaternion normalize (Quaternion result)
    {
        float rlen = 1f / FloatMath.sqrt(x*x + y*y + z*z + w*w);
        return result.set(x*rlen, y*rlen, z*rlen, w*rlen);
    }

    /**
     * Inverts this quaternion in-place.
     *
     * @return a reference to this quaternion, for chaining.
     */
    public Quaternion invertLocal ()
    {
        return invert(this);
    }

    /**
     * Inverts this quaternion.
     *
     * @return a new quaternion containing the result.
     */
    public Quaternion invert ()
    {
        return invert(new Quaternion());
    }

    /**
     * Inverts this quaternion, storing the result in the object provided.
     *
     * @return a reference to the result, for chaining.
     */
    public Quaternion invert (Quaternion result)
    {
        return result.set(-x, -y, -z, w);
    }

    /**
     * Multiplies this quaternion in-place by another.
     *
     * @return a reference to this quaternion, for chaining.
     */
    public Quaternion multLocal (Quaternion other)
    {
        return mult(other, this);
    }

    /**
     * Multiplies this quaternion by another.
     *
     * @return a new quaternion containing the result.
     */
    public Quaternion mult (Quaternion other)
    {
        return mult(other, new Quaternion());
    }

    /**
     * Multiplies this quaternion by another and stores the result in the provided object.
     *
     * @return a reference to the result, for chaining.
     */
    public Quaternion mult (Quaternion other, Quaternion result)
    {
        return result.set(
            w*other.x + x*other.w + y*other.z - z*other.y,
            w*other.y + y*other.w + z*other.x - x*other.z,
            w*other.z + z*other.w + x*other.y - y*other.x,
            w*other.w - x*other.x - y*other.y - z*other.z);
    }

    /**
     * Interpolates in-place between this and the specified other quaternion.
     *
     * @return a reference to this quaternion, for chaining.
     */
    public Quaternion slerpLocal (Quaternion other, float t)
    {
        return slerp(other, t, this);
    }

    /**
     * Interpolates between this and the specified other quaternion.
     *
     * @return a new quaternion containing the result.
     */
    public Quaternion slerp (Quaternion other, float t)
    {
        return slerp(other, t, new Quaternion());
    }

    /**
     * Interpolates between this and the specified other quaternion, placing the result in the
     * object provided.  Based on the code in Nick Bobick's article,
     * <a href="http://www.gamasutra.com/features/19980703/quaternions_01.htm">Rotating Objects
     * Using Quaternions</a>.
     *
     * @return a reference to the result quaternion, for chaining.
     */
    public Quaternion slerp (Quaternion other, float t, Quaternion result)
    {
        float cosa = x*other.x + y*other.y + z*other.z + w*other.w;
        float ox = other.x, oy = other.y, oz = other.z, ow = other.w, s0, s1;

        // adjust signs if necessary
        if (cosa < 0f) {
            cosa = -cosa;
            ox = -ox;
            oy = -oy;
            oz = -oz;
            ow = -ow;
        }

        // calculate coefficients; if the angle is too close to zero, we must fall back
        // to linear interpolation
        if ((1f - cosa) > FloatMath.EPSILON) {
            float angle = FloatMath.acos(cosa), sina = FloatMath.sin(angle);
            s0 = FloatMath.sin((1f - t) * angle) / sina;
            s1 = FloatMath.sin(t * angle) / sina;
        } else {
            s0 = 1f - t;
            s1 = t;
        }

        return result.set(s0*x + s1*ox, s0*y + s1*oy, s0*z + s1*oz, s0*w + s1*ow);
    }

    /**
     * Transforms a vector in-place by this quaternion.
     *
     * @return a reference to the vector, for chaining.
     */
    public Vector3f transformLocal (Vector3f vector)
    {
        return transform(vector, vector);
    }

    /**
     * Transforms a vector by this quaternion.
     *
     * @return a new vector containing the result.
     */
    public Vector3f transform (Vector3f vector)
    {
        return transform(vector, new Vector3f());
    }

    /**
     * Transforms a vector by this quaternion and places the result in the provided object.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f transform (Vector3f vector, Vector3f result)
    {
        float xx = x*x, yy = y*y, zz = z*z;
        float xy = x*y, xz = x*z, xw = x*w;
        float yz = y*z, yw = y*w, zw = z*w;
        float vx2 = vector.x*2f, vy2 = vector.y*2f, vz2 = vector.z*2f;
        return result.set(
            vector.x + vy2*(xy - zw) + vz2*(xz + yw) - vx2*(yy + zz),
            vector.y + vx2*(xy + zw) + vz2*(yz - xw) - vy2*(xx + zz),
            vector.z + vx2*(xz - yw) + vy2*(yz + xw) - vz2*(xx + yy));
    }

    /**
     * Transforms the unit x vector by this quaternion, placing the result in the provided object.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f transformUnitX (Vector3f result)
    {
        return result.set(1f - 2f*(y*y + z*z), 2f*(x*y + z*w), 2f*(x*z - y*w));
    }

    /**
     * Transforms the unit y vector by this quaternion, placing the result in the provided object.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f transformUnitY (Vector3f result)
    {
        return result.set(2f*(x*y - z*w), 1f - 2f*(x*x + z*z), 2f*(y*z + x*w));
    }

    /**
     * Transforms the unit z vector by this quaternion, placing the result in the provided object.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f transformUnitZ (Vector3f result)
    {
        return result.set(2f*(x*z + y*w), 2f*(y*z - x*w), 1f - 2f*(x*x + y*y));
    }

    /**
     * Transforms a vector by this quaternion and adds another vector to it, placing the result
     * in the object provided.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f transformAndAdd (Vector3f vector, Vector3f add, Vector3f result)
    {
        float xx = x*x, yy = y*y, zz = z*z;
        float xy = x*y, xz = x*z, xw = x*w;
        float yz = y*z, yw = y*w, zw = z*w;
        float vx2 = vector.x*2f, vy2 = vector.y*2f, vz2 = vector.z*2f;
        return result.set(
            vector.x + add.x + vy2*(xy - zw) + vz2*(xz + yw) - vx2*(yy + zz),
            vector.y + add.y + vx2*(xy + zw) + vz2*(yz - xw) - vy2*(xx + zz),
            vector.z + add.z + vx2*(xz - yw) + vy2*(yz + xw) - vz2*(xx + yy));
    }

    /**
     * Transforms a vector by this quaternion, applies a uniform scale, and adds another vector to
     * it, placing the result in the object provided.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f transformScaleAndAdd (
        Vector3f vector, float scale, Vector3f add, Vector3f result)
    {
        float xx = x*x, yy = y*y, zz = z*z;
        float xy = x*y, xz = x*z, xw = x*w;
        float yz = y*z, yw = y*w, zw = z*w;
        float vx2 = vector.x*2f, vy2 = vector.y*2f, vz2 = vector.z*2f;
        return result.set(
            (vector.x + vy2*(xy - zw) + vz2*(xz + yw) - vx2*(yy + zz)) * scale + add.x,
            (vector.y + vx2*(xy + zw) + vz2*(yz - xw) - vy2*(xx + zz)) * scale + add.y,
            (vector.z + vx2*(xz - yw) + vy2*(yz + xw) - vz2*(xx + yy)) * scale + add.z);
    }

    /**
     * Transforms a vector by this quaternion and returns the z coordinate of the result.
     */
    public float transformZ (Vector3f vector)
    {
        return vector.z + vector.x*2f*(x*z - y*w) +
            vector.y*2f*(y*z + x*w) - vector.z*2f*(x*x + y*y);
    }

    /**
     * Returns the amount of rotation about the z axis (for the purpose of flattening the
     * rotation).
     */
    public float getRotationZ ()
    {
        return FloatMath.atan2(2f*(x*y + z*w), 1f - 2f*(y*y + z*z));
    }

    /**
     * Integrates in-place the provided angular velocity over the specified timestep.
     *
     * @return a reference to this quaternion, for chaining.
     */
    public Quaternion integrateLocal (Vector3f velocity, float t)
    {
        return integrate(velocity, t, this);
    }

    /**
     * Integrates the provided angular velocity over the specified timestep.
     *
     * @return a new quaternion containing the result.
     */
    public Quaternion integrate (Vector3f velocity, float t)
    {
        return integrate(velocity, t, new Quaternion());
    }

    /**
     * Integrates the provided angular velocity over the specified timestep, storing the result
     * in the object provided.
     *
     * @return a reference to the result object, for chaining.
     */
    public Quaternion integrate (Vector3f velocity, float t, Quaternion result)
    {
        // TODO: use Runge-Kutta integration?
        float qx = 0.5f * velocity.x;
        float qy = 0.5f * velocity.y;
        float qz = 0.5f * velocity.z;
        return result.set(
            x + t*(qx*w + qy*z - qz*y),
            y + t*(qy*w + qz*x - qx*z),
            z + t*(qz*w + qx*y - qy*x),
            w + t*(-qx*x - qy*y - qz*z)).normalizeLocal();
    }

    /**
     * Copies the elements of another quaternion.
     *
     * @return a reference to this quaternion, for chaining.
     */
    public Quaternion set (Quaternion other)
    {
        return set(other.x, other.y, other.z, other.w);
    }

    /**
     * Copies the elements of an array.
     *
     * @return a reference to this quaternion, for chaining.
     */
    public Quaternion set (float[] values)
    {
        return set(values[0], values[1], values[2], values[3]);
    }

    /**
     * Sets all of the elements of the quaternion.
     *
     * @return a reference to this quaternion, for chaining.
     */
    public Quaternion set (float x, float y, float z, float w)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    /**
     * Populates the supplied array with the contents of this quaternion.
     */
    public void get (float[] values)
    {
        values[0] = x;
        values[1] = y;
        values[2] = z;
        values[3] = w;
    }

    /**
     * Checks whether any of the components of this quaternion are not-numbers.
     */
    public boolean hasNaN ()
    {
        return Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z) || Float.isNaN(w);
    }

    // documentation inherited from interface Encodable
    public String encodeToString ()
    {
        return x + ", " + y + ", " + z + ", " + w;
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
        out.writeFloat(x);
        out.writeFloat(y);
        out.writeFloat(z);
        out.writeFloat(w);
    }

    // documentation inherited from interface Encodable
    public void decodeFromStream (DataInputStream in)
        throws IOException
    {
        set(in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat());
    }

    @Override
    public String toString ()
    {
        return "[" + x + ", " + y + ", " + z + ", " + w + "]";
    }

    @Override
    public int hashCode ()
    {
        return Float.floatToIntBits(x) ^ Float.floatToIntBits(y) ^
            Float.floatToIntBits(z) ^ Float.floatToIntBits(w);
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof Quaternion)) {
            return false;
        }
        Quaternion oquat = (Quaternion)other;
        return (x == oquat.x && y == oquat.y && z == oquat.z && w == oquat.w) ||
            (x == -oquat.x && y == -oquat.y && z == -oquat.z && w == -oquat.x);
    }
}
