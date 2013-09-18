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

import java.nio.FloatBuffer;

import com.samskivert.util.StringUtil;

import com.threerings.io.Streamable;

import com.threerings.export.Encodable;

/**
 * A 3x3 column-major matrix.
 */
public final class Matrix3f
    implements Encodable, Streamable
{
    /** The identity matrix. */
    public static final Matrix3f IDENTITY = new Matrix3f();

    /** The values of the matrix. */
    public float m00, m10, m20;
    public float m01, m11, m21;
    public float m02, m12, m22;

    /**
     * Creates a matrix from its components.
     */
    public Matrix3f (
        float m00, float m10, float m20,
        float m01, float m11, float m21,
        float m02, float m12, float m22)
    {
        set(m00, m10, m20,
            m01, m11, m21,
            m02, m12, m22);
    }

    /**
     * Creates a matrix from an array of values.
     */
    public Matrix3f (float[] values)
    {
        set(values);
    }

    /**
     * Copy constructor.
     */
    public Matrix3f (Matrix3f other)
    {
        set(other);
    }

    /**
     * Creates an identity matrix.
     */
    public Matrix3f ()
    {
        setToIdentity();
    }

    /**
     * Sets this matrix to the identity matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f setToIdentity ()
    {
        return set(
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f);
    }

    /**
     * Sets this to a rotation matrix that rotates one vector onto another.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f setToRotation (Vector3f from, Vector3f to)
    {
        float angle = from.angle(to);
        return (angle < 0.0001f) ?
            setToIdentity() : setToRotation(angle, from.cross(to).normalizeLocal());
    }

    /**
     * Sets this to a rotation matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f setToRotation (float angle, Vector3f axis)
    {
        return setToRotation(angle, axis.x, axis.y, axis.z);
    }

    /**
     * Sets this to a rotation matrix.  The formula comes from the OpenGL documentation for the
     * glRotatef function.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f setToRotation (float angle, float x, float y, float z)
    {
        float c = FloatMath.cos(angle), s = FloatMath.sin(angle), omc = 1f - c;
        float xs = x*s, ys = y*s, zs = z*s, xy = x*y, xz = x*z, yz = y*z;
        return set(
            x*x*omc + c, xy*omc - zs, xz*omc + ys,
            xy*omc + zs, y*y*omc + c, yz*omc - xs,
            xz*omc - ys, yz*omc + xs, z*z*omc + c);
    }

    /**
     * Sets this to a rotation matrix.  The formula comes from the
     * <a href="http://www.j3d.org/matrix_faq/matrfaq_latest.html">Matrix and Quaternion FAQ</a>.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f setToRotation (Quaternion quat)
    {
        float xx = quat.x*quat.x, yy = quat.y*quat.y, zz = quat.z*quat.z;
        float xy = quat.x*quat.y, xz = quat.x*quat.z, xw = quat.x*quat.w;
        float yz = quat.y*quat.z, yw = quat.y*quat.w, zw = quat.z*quat.w;
        return set(
            1f - 2f*(yy + zz), 2f*(xy - zw), 2f*(xz + yw),
            2f*(xy + zw), 1f - 2f*(xx + zz), 2f*(yz - xw),
            2f*(xz - yw), 2f*(yz + xw), 1f - 2f*(xx + yy));
    }

    /**
     * Sets this to a scale matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f setToScale (Vector3f scale)
    {
        return setToScale(scale.x, scale.y, scale.z);
    }

    /**
     * Sets this to a uniform scale matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f setToScale (float s)
    {
        return setToScale(s, s, s);
    }

    /**
     * Sets this to a scale matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f setToScale (float x, float y, float z)
    {
        return set(
            x,  0f, 0f,
            0f, y,  0f,
            0f, 0f, z);
    }

    /**
     * Sets this to a reflection across a plane intersecting the origin with the supplied normal.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f setToReflection (Vector3f normal)
    {
        return setToReflection(normal.x, normal.y, normal.z);
    }

    /**
     * Sets this to a reflection across a plane intersecting the origin with the supplied normal.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f setToReflection (float x, float y, float z)
    {
        float x2 = -2f*x, y2 = -2f*y, z2 = -2f*z;
        float xy2 = x2*y, xz2 = x2*z, yz2 = y2*z;
        return set(
            1f + x2*x, xy2, xz2,
            xy2, 1f + y2*y, yz2,
            xz2, yz2, 1f + z2*z);
    }

    /**
     * Sets this to a matrix that first rotates, then translates.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f setToTransform (Vector2f translation, float rotation)
    {
        return setToRotation(rotation).setTranslation(translation);
    }

    /**
     * Sets this to a matrix that first scales, then rotates, then translates.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f setToTransform (Vector2f translation, float rotation, float scale)
    {
        return setToRotation(rotation).set(
            m00 * scale, m10 * scale, translation.x,
            m01 * scale, m11 * scale, translation.y,
            0f, 0f, 1f);
    }

    /**
     * Sets this to a matrix that first scales, then rotates, then translates.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f setToTransform (Vector2f translation, float rotation, Vector2f scale)
    {
        return setToRotation(rotation).set(
            m00 * scale.x, m10 * scale.y, translation.x,
            m01 * scale.x, m11 * scale.y, translation.y,
            0f, 0f, 1f);
    }

    /**
     * Sets this to a translation matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f setToTranslation (Vector2f translation)
    {
        return setToTranslation(translation.x, translation.y);
    }

    /**
     * Sets this to a translation matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f setToTranslation (float x, float y)
    {
        return set(
            1f, 0f, x,
            0f, 1f, y,
            0f, 0f, 1f);
    }

    /**
     * Sets the translation component of this matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f setTranslation (Vector2f translation)
    {
        return setTranslation(translation.x, translation.y);
    }

    /**
     * Sets the translation component of this matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f setTranslation (float x, float y)
    {
        m20 = x;
        m21 = y;
        return this;
    }

    /**
     * Sets this to a rotation matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f setToRotation (float angle)
    {
        float sina = FloatMath.sin(angle), cosa = FloatMath.cos(angle);
        return set(
            cosa, -sina, 0f,
            sina, cosa, 0f,
            0f, 0f, 1f);
    }

    /**
     * Transposes this matrix in-place.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f transposeLocal ()
    {
        return transpose(this);
    }

    /**
     * Transposes this matrix.
     *
     * @return a new matrix containing the result.
     */
    public Matrix3f transpose ()
    {
        return transpose(new Matrix3f());
    }

    /**
     * Transposes this matrix, storing the result in the provided object.
     *
     * @return the result matrix, for chaining.
     */
    public Matrix3f transpose (Matrix3f result)
    {
        return result.set(
            m00, m01, m02,
            m10, m11, m12,
            m20, m21, m22);
    }

    /**
     * Multiplies this matrix in-place by another.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f multLocal (Matrix3f other)
    {
        return mult(other, this);
    }

    /**
     * Multiplies this matrix by another.
     *
     * @return a new matrix containing the result.
     */
    public Matrix3f mult (Matrix3f other)
    {
        return mult(other, new Matrix3f());
    }

    /**
     * Multiplies this matrix by another and stores the result in the object provided.
     *
     * @return a reference to the result matrix, for chaining.
     */
    public Matrix3f mult (Matrix3f other, Matrix3f result)
    {
        return result.set(
            m00*other.m00 + m10*other.m01 + m20*other.m02,
            m00*other.m10 + m10*other.m11 + m20*other.m12,
            m00*other.m20 + m10*other.m21 + m20*other.m22,

            m01*other.m00 + m11*other.m01 + m21*other.m02,
            m01*other.m10 + m11*other.m11 + m21*other.m12,
            m01*other.m20 + m11*other.m21 + m21*other.m22,

            m02*other.m00 + m12*other.m01 + m22*other.m02,
            m02*other.m10 + m12*other.m11 + m22*other.m12,
            m02*other.m20 + m12*other.m21 + m22*other.m22);
    }

    /**
     * Determines whether this matrix represents an affine transformation.
     */
    public boolean isAffine ()
    {
        return (m02 == 0f && m12 == 0f && m22 == 1f);
    }

    /**
     * Multiplies this matrix in-place by another, treating the matricees as affine.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f multAffineLocal (Matrix3f other)
    {
        return multAffine(other, this);
    }

    /**
     * Multiplies this matrix by another, treating the matrices as affine.
     *
     * @return a new matrix containing the result.
     */
    public Matrix3f multAffine (Matrix3f other)
    {
        return multAffine(other, new Matrix3f());
    }

    /**
     * Multiplies this matrix by another, treating the matrices as affine, and stores the result
     * in the object provided.
     *
     * @return a reference to the result matrix, for chaining.
     */
    public Matrix3f multAffine (Matrix3f other, Matrix3f result)
    {
        return result.set(
            m00*other.m00 + m10*other.m01,
            m00*other.m10 + m10*other.m11,
            m00*other.m20 + m10*other.m21 + m20,

            m01*other.m00 + m11*other.m01,
            m01*other.m10 + m11*other.m11,
            m01*other.m20 + m11*other.m21 + m21,

            0f, 0f, 1f);
    }

    /**
     * Inverts this matrix in-place.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f invertLocal ()
    {
        return invert(this);
    }

    /**
     * Inverts this matrix.
     *
     * @return a new matrix containing the result.
     */
    public Matrix3f invert ()
    {
        return invert(new Matrix3f());
    }

    /**
     * Inverts this matrix and places the result in the given object.  This code is based on the
     * examples in the <a href="http://www.j3d.org/matrix_faq/matrfaq_latest.html">Matrix and
     * Quaternion FAQ</a>.
     *
     * @return a reference to the result matrix, for chaining.
     */
    public Matrix3f invert (Matrix3f result)
        throws SingularMatrixException
    {
        // compute the determinant, storing the subdeterminants for later use
        float sd00 = m11*m22 - m21*m12;
        float sd10 = m01*m22 - m21*m02;
        float sd20 = m01*m12 - m11*m02;
        float det = m00*sd00 + m20*sd20 - m10*sd10;
        if (Math.abs(det) == 0f) {
            // determinant is zero; matrix is not invertible
            throw new SingularMatrixException(this.toString());
        }
        float rdet = 1f / det;
        return result.set(
            +sd00 * rdet,
            -(m10*m22 - m20*m12) * rdet,
            +(m10*m21 - m20*m11) * rdet,

            -sd10 * rdet,
            +(m00*m22 - m20*m02) * rdet,
            -(m00*m21 - m20*m01) * rdet,

            +sd20 * rdet,
            -(m00*m12 - m10*m02) * rdet,
            +(m00*m11 - m10*m01) * rdet);
    }

    /**
     * Inverts this matrix in-place as an affine matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f invertAffineLocal ()
    {
        return invertAffine(this);
    }

    /**
     * Inverts this matrix as an affine matrix.
     *
     * @return a new matrix containing the result.
     */
    public Matrix3f invertAffine ()
    {
        return invertAffine(new Matrix3f());
    }

    /**
     * Inverts this matrix as an affine matrix and places the result in the given object.
     *
     * @return a reference to the result matrix, for chaining.
     */
    public Matrix3f invertAffine (Matrix3f result)
        throws SingularMatrixException
    {
        // compute the determinant, storing the subdeterminants for later use
        float det = m00*m11 - m10*m01;
        if (Math.abs(det) == 0f) {
            // determinant is zero; matrix is not invertible
            throw new SingularMatrixException(this.toString());
        }
        float rdet = 1f / det;
        return result.set(
            +m11 * rdet,
            -m10 * rdet,
            +(m10*m21 - m20*m11) * rdet,

            -m01 * rdet,
            +m00 * rdet,
            -(m00*m21 - m20*m01) * rdet,

            0f, 0f, 1f);
    }

    /**
     * Linearly interpolates between the this and the specified other matrix, placing the result in
     * this matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f lerpLocal (Matrix3f other, float t)
    {
        return lerp(other, t, this);
    }

    /**
     * Linearly interpolates between this and the specified other matrix.
     *
     * @return a new matrix containing the result.
     */
    public Matrix3f lerp (Matrix3f other, float t)
    {
        return lerp(other, t, new Matrix3f());
    }

    /**
     * Linearly interpolates between this and the specified other matrix, placing the result in
     * the object provided.
     *
     * @return a reference to the result object, for chaining.
     */
    public Matrix3f lerp (Matrix3f other, float t, Matrix3f result)
    {
        return result.set(
            m00 + t*(other.m00 - m00),
            m10 + t*(other.m10 - m10),
            m20 + t*(other.m20 - m20),

            m01 + t*(other.m01 - m01),
            m11 + t*(other.m11 - m11),
            m21 + t*(other.m21 - m21),

            m02 + t*(other.m02 - m02),
            m12 + t*(other.m12 - m12),
            m22 + t*(other.m22 - m22));
    }

    /**
     * Linearly interpolates between this and the specified other matrix (treating the matrices as
     * affine), placing the result in this matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f lerpAffineLocal (Matrix3f other, float t)
    {
        return lerpAffine(other, t, this);
    }

    /**
     * Linearly interpolates between this and the specified other matrix, treating the matrices as
     * affine.
     *
     * @return a new matrix containing the result.
     */
    public Matrix3f lerpAffine (Matrix3f other, float t)
    {
        return lerpAffine(other, t, new Matrix3f());
    }

    /**
     * Linearly interpolates between this and the specified other matrix (treating the matrices as
     * affine), placing the result in the object provided.
     *
     * @return a reference to the result object, for chaining.
     */
    public Matrix3f lerpAffine (Matrix3f other, float t, Matrix3f result)
    {
        return result.set(
            m00 + t*(other.m00 - m00),
            m10 + t*(other.m10 - m10),
            m20 + t*(other.m20 - m20),

            m01 + t*(other.m01 - m01),
            m11 + t*(other.m11 - m11),
            m21 + t*(other.m21 - m21),

            0f, 0f, 1f);
    }

    /**
     * Copies the contents of another matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f set (Matrix3f other)
    {
        return set(
            other.m00, other.m10, other.m20,
            other.m01, other.m11, other.m21,
            other.m02, other.m12, other.m22);
    }

    /**
     * Copies the elements of an array.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f set (float[] values)
    {
        return set(
            values[0], values[1], values[2],
            values[3], values[4], values[5],
            values[6], values[7], values[8]);
    }

    /**
     * Sets all of the matrix's components at once.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix3f set (
        float m00, float m10, float m20,
        float m01, float m11, float m21,
        float m02, float m12, float m22)
    {
        this.m00 = m00; this.m01 = m01; this.m02 = m02;
        this.m10 = m10; this.m11 = m11; this.m12 = m12;
        this.m20 = m20; this.m21 = m21; this.m22 = m22;
        return this;
    }

    /**
     * Places the contents of this matrix into the given buffer in the standard OpenGL order.
     *
     * @return a reference to the buffer, for chaining.
     */
    public FloatBuffer get (FloatBuffer buf)
    {
        buf.put(m00).put(m01).put(m02);
        buf.put(m10).put(m11).put(m12);
        buf.put(m20).put(m21).put(m22);
        return buf;
    }

    /**
     * Transforms a vector in-place by the inner 3x3 part of this matrix.
     *
     * @return a reference to the vector, for chaining.
     */
    public Vector3f transformLocal (Vector3f vector)
    {
        return transform(vector, vector);
    }

    /**
     * Transforms a vector by this matrix.
     *
     * @return a new vector containing the result.
     */
    public Vector3f transform (Vector3f vector)
    {
        return transform(vector, new Vector3f());
    }

    /**
     * Transforms a vector by this matrix and places the result in the object provided.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f transform (Vector3f vector, Vector3f result)
    {
        return result.set(
            m00*vector.x + m10*vector.y + m20*vector.z,
            m01*vector.x + m11*vector.y + m21*vector.z,
            m02*vector.x + m12*vector.y + m22*vector.z);
    }

    /**
     * Transforms a point in-place by this matrix.
     *
     * @return a reference to the point, for chaining.
     */
    public Vector2f transformPointLocal (Vector2f point)
    {
        return transformPoint(point, point);
    }

    /**
     * Transforms a point by this matrix.
     *
     * @return a new vector containing the result.
     */
    public Vector2f transformPoint (Vector2f point)
    {
        return transformPoint(point, new Vector2f());
    }

    /**
     * Transforms a point by this matrix and places the result in the object provided.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector2f transformPoint (Vector2f point, Vector2f result)
    {
        return result.set(
            m00*point.x + m10*point.y + m20,
            m01*point.x + m11*point.y + m21);
    }

    /**
     * Transforms a vector in-place by the inner 2x2 part of this matrix.
     *
     * @return a reference to the vector, for chaining.
     */
    public Vector2f transformVectorLocal (Vector2f vector)
    {
        return transformVector(vector, vector);
    }

    /**
     * Transforms a vector by this inner 2x2 part of this matrix.
     *
     * @return a new vector containing the result.
     */
    public Vector2f transformVector (Vector2f vector)
    {
        return transformVector(vector, new Vector2f());
    }

    /**
     * Transforms a vector by the inner 2x2 part of this matrix and places the result in the object
     * provided.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector2f transformVector (Vector2f vector, Vector2f result)
    {
        return result.set(
            m00*vector.x + m10*vector.y,
            m01*vector.x + m11*vector.y);
    }

    /**
     * Extracts the rotation component of the matrix.  This uses the iterative polar decomposition
     * algorithm described by
     * <a href="http://www.cs.wisc.edu/graphics/Courses/838-s2002/Papers/polar-decomp.pdf">Ken
     * Shoemake</a>.
     */
    public float extractRotation ()
    {
        // start with the contents of the upper 2x2 portion of the matrix
        float n00 = m00, n10 = m10;
        float n01 = m01, n11 = m11;
        for (int ii = 0; ii < 10; ii++) {
            // store the results of the previous iteration
            float o00 = n00, o10 = n10;
            float o01 = n01, o11 = n11;

            // compute average of the matrix with its inverse transpose
            float det = o00*o11 - o10*o01;
            if (Math.abs(det) == 0f) {
                // determinant is zero; matrix is not invertible
                throw new SingularMatrixException(this.toString());
            }
            float hrdet = 0.5f / det;
            n00 = +o11 * hrdet + o00*0.5f;
            n10 = -o01 * hrdet + o10*0.5f;

            n01 = -o10 * hrdet + o01*0.5f;
            n11 = +o00 * hrdet + o11*0.5f;

            // compute the difference; if it's small enough, we're done
            float d00 = n00 - o00, d10 = n10 - o10;
            float d01 = n01 - o01, d11 = n11 - o11;
            if (d00*d00 + d10*d10 + d01*d01 + d11*d11 < FloatMath.EPSILON) {
                break;
            }
        }
        // now that we have a nice orthogonal matrix, we can extract the rotation
        return FloatMath.atan2(n01, n00);
    }

    /**
     * Extracts the scale component of the matrix.
     *
     * @return a new vector containing the result.
     */
    public Vector2f extractScale ()
    {
        return extractScale(new Vector2f());
    }

    /**
     * Extracts the scale component of the matrix and places it in the provided result vector.
     *
     * @return a reference to the result vector, for chaining.
     */
    public Vector2f extractScale (Vector2f result)
    {
        return result.set(
            FloatMath.sqrt(m00*m00 + m01*m01),
            FloatMath.sqrt(m10*m10 + m11*m11));
    }

    /**
     * Returns an approximation of the uniform scale for this matrix (the square root of the
     * signed area of the parallelogram spanned by the axis vectors).
     */
    public float approximateUniformScale ()
    {
        float cp = m00*m11 - m01*m10;
        return (cp < 0f) ? -FloatMath.sqrt(-cp) : FloatMath.sqrt(cp);
    }

    // documentation inherited from interface Encodable
    public String encodeToString ()
    {
        return m00 + ", " + m10 + ", " + m20 + ", " +
            m01 + ", " + m11 + ", " + m21 + ", " +
            m02 + ", " + m12 + ", " + m22;
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
        out.writeFloat(m00);
        out.writeFloat(m10);
        out.writeFloat(m20);

        out.writeFloat(m01);
        out.writeFloat(m11);
        out.writeFloat(m21);

        out.writeFloat(m02);
        out.writeFloat(m12);
        out.writeFloat(m22);
    }

    // documentation inherited from interface Encodable
    public void decodeFromStream (DataInputStream in)
        throws IOException
    {
        set(in.readFloat(), in.readFloat(), in.readFloat(),
            in.readFloat(), in.readFloat(), in.readFloat(),
            in.readFloat(), in.readFloat(), in.readFloat());
    }

    @Override
    public String toString ()
    {
        return "[[" + m00 + ", " + m10 + ", " + m20 + "], " +
            "[" + m01 + ", " + m11 + ", " + m21 + "], " +
            "[" + m02 + ", " + m12 + ", " + m22 + "]]";
    }

    @Override
    public int hashCode ()
    {
        return Float.floatToIntBits(m00) ^ Float.floatToIntBits(m10) ^ Float.floatToIntBits(m20) ^
            Float.floatToIntBits(m01) ^ Float.floatToIntBits(m11) ^ Float.floatToIntBits(m21) ^
            Float.floatToIntBits(m02) ^ Float.floatToIntBits(m12) ^ Float.floatToIntBits(m22);
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof Matrix3f)) {
            return false;
        }
        Matrix3f omat = (Matrix3f)other;
        return
            m00 == omat.m00 && m10 == omat.m10 && m20 == omat.m20 &&
            m01 == omat.m01 && m11 == omat.m11 && m21 == omat.m21 &&
            m02 == omat.m02 && m12 == omat.m12 && m22 == omat.m22;
    }
}
