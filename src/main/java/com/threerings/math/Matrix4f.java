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
 * A 4x4 column-major matrix.
 */
public final class Matrix4f
    implements Encodable, Streamable
{
    /** The identity matrix. */
    public static final Matrix4f IDENTITY = new Matrix4f();

    /** An empty matrix array. */
    public static final Matrix4f[] EMPTY_ARRAY = new Matrix4f[0];

    /** The values of the matrix. */
    public float m00, m10, m20, m30;
    public float m01, m11, m21, m31;
    public float m02, m12, m22, m32;
    public float m03, m13, m23, m33;

    /**
     * Creates a matrix from its components.
     */
    public Matrix4f (
        float m00, float m10, float m20, float m30,
        float m01, float m11, float m21, float m31,
        float m02, float m12, float m22, float m32,
        float m03, float m13, float m23, float m33)
    {
        set(m00, m10, m20, m30,
            m01, m11, m21, m31,
            m02, m12, m22, m32,
            m03, m13, m23, m33);
    }

    /**
     * Creates a matrix from an array of values.
     */
    public Matrix4f (float[] values)
    {
        set(values);
    }

    /**
     * Creates a matrix from a float buffer.
     */
    public Matrix4f (FloatBuffer buf)
    {
        set(buf);
    }

    /**
     * Copy constructor.
     */
    public Matrix4f (Matrix4f other)
    {
        set(other);
    }

    /**
     * Creates an identity matrix.
     */
    public Matrix4f ()
    {
        setToIdentity();
    }

    /**
     * Sets this matrix to the identity matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToIdentity ()
    {
        return set(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f);
    }

    /**
     * Sets this to a matrix that first rotates, then translates.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToTransform (Vector3f translation, Quaternion rotation)
    {
        return setToRotation(rotation).setTranslation(translation);
    }

    /**
     * Sets this to a matrix that first scales, then rotates, then translates.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToTransform (Vector3f translation, Quaternion rotation, float scale)
    {
        return setToRotation(rotation).set(
            m00 * scale, m10 * scale, m20 * scale, translation.x,
            m01 * scale, m11 * scale, m21 * scale, translation.y,
            m02 * scale, m12 * scale, m22 * scale, translation.z,
            0f, 0f, 0f, 1f);
    }

    /**
     * Sets this to a matrix that first scales, then rotates, then translates.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToTransform (Vector3f translation, Quaternion rotation, Vector3f scale)
    {
        return setToRotation(rotation).set(
            m00 * scale.x, m10 * scale.y, m20 * scale.z, translation.x,
            m01 * scale.x, m11 * scale.y, m21 * scale.z, translation.y,
            m02 * scale.x, m12 * scale.y, m22 * scale.z, translation.z,
            0f, 0f, 0f, 1f);
    }

    /**
     * Sets this to a translation matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToTranslation (Vector3f translation)
    {
        return setToTranslation(translation.x, translation.y, translation.z);
    }

    /**
     * Sets this to a translation matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToTranslation (float x, float y, float z)
    {
        return set(
            1f, 0f, 0f, x,
            0f, 1f, 0f, y,
            0f, 0f, 1f, z,
            0f, 0f, 0f, 1f);
    }

    /**
     * Sets the translation component of this matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setTranslation (Vector3f translation)
    {
        return setTranslation(translation.x, translation.y, translation.z);
    }

    /**
     * Sets the translation component of this matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setTranslation (float x, float y, float z)
    {
        m30 = x;
        m31 = y;
        m32 = z;
        return this;
    }

    /**
     * Sets this to a rotation matrix that rotates one vector onto another.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToRotation (Vector3f from, Vector3f to)
    {
        float angle = from.angle(to);
        if (angle < FloatMath.EPSILON) {
            return setToIdentity();
        }
        if (angle <= FloatMath.PI - FloatMath.EPSILON) {
            return setToRotation(angle, from.cross(to).normalizeLocal());
        }
        // it's a 180 degree rotation; any axis orthogonal to the from vector will do
        Vector3f axis = new Vector3f(0f, from.z, -from.y);
        float length = axis.length();
        return setToRotation(FloatMath.PI, length < FloatMath.EPSILON ?
            axis.set(-from.z, 0f, from.x).normalizeLocal() : axis.multLocal(1f / length));
    }

    /**
     * Sets this to a rotation matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToRotation (float angle, Vector3f axis)
    {
        return setToRotation(angle, axis.x, axis.y, axis.z);
    }

    /**
     * Sets this to a rotation matrix.  The formula comes from the OpenGL documentation for the
     * glRotatef function.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToRotation (float angle, float x, float y, float z)
    {
        float c = FloatMath.cos(angle), s = FloatMath.sin(angle), omc = 1f - c;
        float xs = x*s, ys = y*s, zs = z*s, xy = x*y, xz = x*z, yz = y*z;
        return set(
            x*x*omc + c, xy*omc - zs, xz*omc + ys, 0f,
            xy*omc + zs, y*y*omc + c, yz*omc - xs, 0f,
            xz*omc - ys, yz*omc + xs, z*z*omc + c, 0f,
            0f, 0f, 0f, 1f);
    }

    /**
     * Sets this to a rotation matrix.  The formula comes from the
     * <a href="http://www.j3d.org/matrix_faq/matrfaq_latest.html">Matrix and Quaternion FAQ</a>.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToRotation (Quaternion quat)
    {
        float xx = quat.x*quat.x, yy = quat.y*quat.y, zz = quat.z*quat.z;
        float xy = quat.x*quat.y, xz = quat.x*quat.z, xw = quat.x*quat.w;
        float yz = quat.y*quat.z, yw = quat.y*quat.w, zw = quat.z*quat.w;
        return set(
            1f - 2f*(yy + zz), 2f*(xy - zw), 2f*(xz + yw), 0f,
            2f*(xy + zw), 1f - 2f*(xx + zz), 2f*(yz - xw), 0f,
            2f*(xz - yw), 2f*(yz + xw), 1f - 2f*(xx + yy), 0f,
            0f, 0f, 0f, 1f);
    }

    /**
     * Sets this to a scale matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToScale (Vector3f scale)
    {
        return setToScale(scale.x, scale.y, scale.z);
    }

    /**
     * Sets this to a uniform scale matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToScale (float s)
    {
        return setToScale(s, s, s);
    }

    /**
     * Sets this to a scale matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToScale (float x, float y, float z)
    {
        return set(
            x,  0f, 0f, 0f,
            0f, y,  0f, 0f,
            0f, 0f, z,  0f,
            0f, 0f, 0f, 1f);
    }

    /**
     * Sets this to a reflection across a plane intersecting the origin with the supplied normal.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToReflection (Vector3f normal)
    {
        return setToReflection(normal.x, normal.y, normal.z);
    }

    /**
     * Sets this to a reflection across a plane intersecting the origin with the supplied normal.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToReflection (float x, float y, float z)
    {
        float x2 = -2f*x, y2 = -2f*y, z2 = -2f*z;
        float xy2 = x2*y, xz2 = x2*z, yz2 = y2*z;
        return set(
            1f + x2*x, xy2, xz2, 0f,
            xy2, 1f + y2*y, yz2, 0f,
            xz2, yz2, 1f + z2*z, 0f,
            0f, 0f, 0f, 1f);
    }

    /**
     * Sets this to a reflection across the specified plane.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToReflection (Plane plane)
    {
        return setToReflection(plane.getNormal(), plane.constant);
    }

    /**
     * Sets this to a reflection across the specified plane.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToReflection (Vector3f normal, float constant)
    {
        return setToReflection(normal.x, normal.y, normal.z, constant);
    }

    /**
     * Sets this to a reflection across the specified plane.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToReflection (float x, float y, float z, float w)
    {
        float x2 = -2f*x, y2 = -2f*y, z2 = -2f*z;
        float xy2 = x2*y, xz2 = x2*z, yz2 = y2*z;
        float x2y2z2 = x*x + y*y + z*z;
        return set(
            1f + x2*x, xy2, xz2, x2*w*x2y2z2,
            xy2, 1f + y2*y, yz2, y2*w*x2y2z2,
            xz2, yz2, 1f + z2*z, z2*w*x2y2z2,
            0f, 0f, 0f, 1f);
    }

    /**
     * Sets this to a skew by the specified amount relative to the given plane.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToSkew (Plane plane, Vector3f amount)
    {
        return setToSkew(plane.getNormal(), plane.constant, amount);
    }

    /**
     * Sets this to a skew by the specified amount relative to the given plane.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToSkew (Vector3f normal, float constant, Vector3f amount)
    {
        return setToSkew(normal.x, normal.y, normal.z, constant, amount.x, amount.y, amount.z);
    }

    /**
     * Sets this to a skew by the specified amount relative to the given plane.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToSkew (float a, float b, float c, float d, float x, float y, float z)
    {
        return set(
            1f + a*x, b*x, c*x, d*x,
            a*y, 1f + b*y, c*y, d*y,
            a*z, b*z, 1f + c*z, d*z,
            0f, 0f, 0f, 1f);
    }

    /**
     * Sets this to a perspective projection matrix.  The formula comes from the OpenGL
     * documentation for the gluPerspective function.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToPerspective (float fovy, float aspect, float near, float far)
    {
        float f = 1f / FloatMath.tan(fovy / 2f), dscale = 1f / (near - far);
        return set(
            f/aspect, 0f, 0f, 0f,
            0f, f, 0f, 0f,
            0f, 0f, (far+near) * dscale, 2f * far * near * dscale,
            0f, 0f, -1f, 0f);
    }

    /**
     * Sets this to a perspective projection matrix.  The formula comes from the OpenGL
     * documentation for the glFrustum function.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToFrustum (
        float left, float right, float bottom, float top, float near, float far)
    {
        return setToFrustum(left, right, bottom, top, near, far, Vector3f.UNIT_Z);
    }

    /**
     * Sets this to a perspective projection matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToFrustum (
        float left, float right, float bottom, float top,
        float near, float far, Vector3f nearFarNormal)
    {
        float rrl = 1f / (right - left);
        float rtb = 1f / (top - bottom);
        float rnf = 1f / (near - far);
        float n2 = 2f * near;
        float s = (far + near) / (near*nearFarNormal.z - far*nearFarNormal.z);
        return set(
            n2 * rrl, 0f, (right + left) * rrl, 0f,
            0f, n2 * rtb, (top + bottom) * rtb, 0f,
            s * nearFarNormal.x, s * nearFarNormal.y, (far + near) * rnf, n2 * far * rnf,
            0f, 0f, -1f, 0f);
    }

    /**
     * Sets this to an orthographic projection matrix.  The formula comes from the OpenGL
     * documentation for the glOrtho function.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToOrtho (
        float left, float right, float bottom, float top, float near, float far)
    {
        return setToOrtho(left, right, bottom, top, near, far, Vector3f.UNIT_Z);
    }

    /**
     * Sets this to an orthographic projection matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f setToOrtho (
        float left, float right, float bottom, float top,
        float near, float far, Vector3f nearFarNormal)
    {
        float rlr = 1f / (left - right);
        float rbt = 1f / (bottom - top);
        float rnf = 1f / (near - far);
        float s = 2f / (near*nearFarNormal.z - far*nearFarNormal.z);
        return set(
            -2f * rlr, 0f, 0f, (right + left) * rlr,
            0f, -2f * rbt, 0f, (top + bottom) * rbt,
            s * nearFarNormal.x, s * nearFarNormal.y, 2f * rnf, (far + near) * rnf,
            0f, 0f, 0f, 1f);
    }

    /**
     * Transposes this matrix in-place.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f transposeLocal ()
    {
        return transpose(this);
    }

    /**
     * Transposes this matrix.
     *
     * @return a new matrix containing the result.
     */
    public Matrix4f transpose ()
    {
        return transpose(new Matrix4f());
    }

    /**
     * Transposes this matrix, storing the result in the provided object.
     *
     * @return the result matrix, for chaining.
     */
    public Matrix4f transpose (Matrix4f result)
    {
        return result.set(
            m00, m01, m02, m03,
            m10, m11, m12, m13,
            m20, m21, m22, m23,
            m30, m31, m32, m33);
    }

    /**
     * Multiplies this matrix in-place by another.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f multLocal (Matrix4f other)
    {
        return mult(other, this);
    }

    /**
     * Multiplies this matrix by another.
     *
     * @return a new matrix containing the result.
     */
    public Matrix4f mult (Matrix4f other)
    {
        return mult(other, new Matrix4f());
    }

    /**
     * Multiplies this matrix by another and stores the result in the object provided.
     *
     * @return a reference to the result matrix, for chaining.
     */
    public Matrix4f mult (Matrix4f other, Matrix4f result)
    {
        return result.set(
            m00*other.m00 + m10*other.m01 + m20*other.m02 + m30*other.m03,
            m00*other.m10 + m10*other.m11 + m20*other.m12 + m30*other.m13,
            m00*other.m20 + m10*other.m21 + m20*other.m22 + m30*other.m23,
            m00*other.m30 + m10*other.m31 + m20*other.m32 + m30*other.m33,

            m01*other.m00 + m11*other.m01 + m21*other.m02 + m31*other.m03,
            m01*other.m10 + m11*other.m11 + m21*other.m12 + m31*other.m13,
            m01*other.m20 + m11*other.m21 + m21*other.m22 + m31*other.m23,
            m01*other.m30 + m11*other.m31 + m21*other.m32 + m31*other.m33,

            m02*other.m00 + m12*other.m01 + m22*other.m02 + m32*other.m03,
            m02*other.m10 + m12*other.m11 + m22*other.m12 + m32*other.m13,
            m02*other.m20 + m12*other.m21 + m22*other.m22 + m32*other.m23,
            m02*other.m30 + m12*other.m31 + m22*other.m32 + m32*other.m33,

            m03*other.m00 + m13*other.m01 + m23*other.m02 + m33*other.m03,
            m03*other.m10 + m13*other.m11 + m23*other.m12 + m33*other.m13,
            m03*other.m20 + m13*other.m21 + m23*other.m22 + m33*other.m23,
            m03*other.m30 + m13*other.m31 + m23*other.m32 + m33*other.m33);
    }

    /**
     * Determines whether this matrix represents an affine transformation.
     */
    public boolean isAffine ()
    {
        return (m03 == 0f && m13 == 0f && m23 == 0f && m33 == 1f);
    }

    /**
     * Determines whether the matrix is mirrored.
     */
    public boolean isMirrored ()
    {
        return m00*(m11*m22 - m12*m21) + m01*(m12*m20 - m10*m22) + m02*(m10*m21 - m11*m20) < 0f;
    }

    /**
     * Multiplies this matrix in-place by another, treating the matricees as affine.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f multAffineLocal (Matrix4f other)
    {
        return multAffine(other, this);
    }

    /**
     * Multiplies this matrix by another, treating the matrices as affine.
     *
     * @return a new matrix containing the result.
     */
    public Matrix4f multAffine (Matrix4f other)
    {
        return multAffine(other, new Matrix4f());
    }

    /**
     * Multiplies this matrix by another, treating the matrices as affine, and stores the result
     * in the object provided.
     *
     * @return a reference to the result matrix, for chaining.
     */
    public Matrix4f multAffine (Matrix4f other, Matrix4f result)
    {
        return result.set(
            m00*other.m00 + m10*other.m01 + m20*other.m02,
            m00*other.m10 + m10*other.m11 + m20*other.m12,
            m00*other.m20 + m10*other.m21 + m20*other.m22,
            m00*other.m30 + m10*other.m31 + m20*other.m32 + m30,

            m01*other.m00 + m11*other.m01 + m21*other.m02,
            m01*other.m10 + m11*other.m11 + m21*other.m12,
            m01*other.m20 + m11*other.m21 + m21*other.m22,
            m01*other.m30 + m11*other.m31 + m21*other.m32 + m31,

            m02*other.m00 + m12*other.m01 + m22*other.m02,
            m02*other.m10 + m12*other.m11 + m22*other.m12,
            m02*other.m20 + m12*other.m21 + m22*other.m22,
            m02*other.m30 + m12*other.m31 + m22*other.m32 + m32,

            0f, 0f, 0f, 1f);
    }

    /**
     * Inverts this matrix in-place.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f invertLocal ()
    {
        return invert(this);
    }

    /**
     * Inverts this matrix.
     *
     * @return a new matrix containing the result.
     */
    public Matrix4f invert ()
    {
        return invert(new Matrix4f());
    }

    /**
     * Inverts this matrix and places the result in the given object.  This code is based on the
     * examples in the <a href="http://www.j3d.org/matrix_faq/matrfaq_latest.html">Matrix and
     * Quaternion FAQ</a>.
     *
     * @return a reference to the result matrix, for chaining.
     */
    public Matrix4f invert (Matrix4f result)
        throws SingularMatrixException
    {
        // compute the determinant, storing the subdeterminants for later use
        float sd00 = m11*(m22*m33 - m23*m32) + m21*(m13*m32 - m12*m33) + m31*(m12*m23 - m13*m22);
        float sd10 = m01*(m22*m33 - m23*m32) + m21*(m03*m32 - m02*m33) + m31*(m02*m23 - m03*m22);
        float sd20 = m01*(m12*m33 - m13*m32) + m11*(m03*m32 - m02*m33) + m31*(m02*m13 - m03*m12);
        float sd30 = m01*(m12*m23 - m13*m22) + m11*(m03*m22 - m02*m23) + m21*(m02*m13 - m03*m12);
        float det = m00*sd00 + m20*sd20 - m10*sd10 - m30*sd30;
        if (Math.abs(det) == 0f) {
            // determinant is zero; matrix is not invertible
            throw new SingularMatrixException(this.toString());
        }
        float rdet = 1f / det;
        return result.set(
            +sd00 * rdet,
            -(m10*(m22*m33 - m23*m32) + m20*(m13*m32 - m12*m33) + m30*(m12*m23 - m13*m22)) * rdet,
            +(m10*(m21*m33 - m23*m31) + m20*(m13*m31 - m11*m33) + m30*(m11*m23 - m13*m21)) * rdet,
            -(m10*(m21*m32 - m22*m31) + m20*(m12*m31 - m11*m32) + m30*(m11*m22 - m12*m21)) * rdet,

            -sd10 * rdet,
            +(m00*(m22*m33 - m23*m32) + m20*(m03*m32 - m02*m33) + m30*(m02*m23 - m03*m22)) * rdet,
            -(m00*(m21*m33 - m23*m31) + m20*(m03*m31 - m01*m33) + m30*(m01*m23 - m03*m21)) * rdet,
            +(m00*(m21*m32 - m22*m31) + m20*(m02*m31 - m01*m32) + m30*(m01*m22 - m02*m21)) * rdet,

            +sd20 * rdet,
            -(m00*(m12*m33 - m13*m32) + m10*(m03*m32 - m02*m33) + m30*(m02*m13 - m03*m12)) * rdet,
            +(m00*(m11*m33 - m13*m31) + m10*(m03*m31 - m01*m33) + m30*(m01*m13 - m03*m11)) * rdet,
            -(m00*(m11*m32 - m12*m31) + m10*(m02*m31 - m01*m32) + m30*(m01*m12 - m02*m11)) * rdet,

            -sd30 * rdet,
            +(m00*(m12*m23 - m13*m22) + m10*(m03*m22 - m02*m23) + m20*(m02*m13 - m03*m12)) * rdet,
            -(m00*(m11*m23 - m13*m21) + m10*(m03*m21 - m01*m23) + m20*(m01*m13 - m03*m11)) * rdet,
            +(m00*(m11*m22 - m12*m21) + m10*(m02*m21 - m01*m22) + m20*(m01*m12 - m02*m11)) * rdet);
    }

    /**
     * Inverts this matrix in-place as an affine matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f invertAffineLocal ()
    {
        return invertAffine(this);
    }

    /**
     * Inverts this matrix as an affine matrix.
     *
     * @return a new matrix containing the result.
     */
    public Matrix4f invertAffine ()
    {
        return invertAffine(new Matrix4f());
    }

    /**
     * Inverts this matrix as an affine matrix and places the result in the given object.
     *
     * @return a reference to the result matrix, for chaining.
     */
    public Matrix4f invertAffine (Matrix4f result)
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
            -(m10*(m21*m32 - m22*m31) + m20*(m12*m31 - m11*m32) + m30*sd00) * rdet,

            -sd10 * rdet,
            +(m00*m22 - m20*m02) * rdet,
            -(m00*m21 - m20*m01) * rdet,
            +(m00*(m21*m32 - m22*m31) + m20*(m02*m31 - m01*m32) + m30*sd10) * rdet,

            +sd20 * rdet,
            -(m00*m12 - m10*m02) * rdet,
            +(m00*m11 - m10*m01) * rdet,
            -(m00*(m11*m32 - m12*m31) + m10*(m02*m31 - m01*m32) + m30*sd20) * rdet,

            0f, 0f, 0f, 1f);
    }

    /**
     * Linearly interpolates between the this and the specified other matrix, placing the result in
     * this matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f lerpLocal (Matrix4f other, float t)
    {
        return lerp(other, t, this);
    }

    /**
     * Linearly interpolates between this and the specified other matrix.
     *
     * @return a new matrix containing the result.
     */
    public Matrix4f lerp (Matrix4f other, float t)
    {
        return lerp(other, t, new Matrix4f());
    }

    /**
     * Linearly interpolates between this and the specified other matrix, placing the result in
     * the object provided.
     *
     * @return a reference to the result object, for chaining.
     */
    public Matrix4f lerp (Matrix4f other, float t, Matrix4f result)
    {
        return result.set(
            m00 + t*(other.m00 - m00),
            m10 + t*(other.m10 - m10),
            m20 + t*(other.m20 - m20),
            m30 + t*(other.m30 - m30),

            m01 + t*(other.m01 - m01),
            m11 + t*(other.m11 - m11),
            m21 + t*(other.m21 - m21),
            m31 + t*(other.m31 - m31),

            m02 + t*(other.m02 - m02),
            m12 + t*(other.m12 - m12),
            m22 + t*(other.m22 - m22),
            m32 + t*(other.m32 - m32),

            m03 + t*(other.m03 - m03),
            m13 + t*(other.m13 - m13),
            m23 + t*(other.m23 - m23),
            m33 + t*(other.m33 - m33));
    }

    /**
     * Linearly interpolates between this and the specified other matrix (treating the matrices as
     * affine), placing the result in this matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f lerpAffineLocal (Matrix4f other, float t)
    {
        return lerpAffine(other, t, this);
    }

    /**
     * Linearly interpolates between this and the specified other matrix, treating the matrices as
     * affine.
     *
     * @return a new matrix containing the result.
     */
    public Matrix4f lerpAffine (Matrix4f other, float t)
    {
        return lerpAffine(other, t, new Matrix4f());
    }

    /**
     * Linearly interpolates between this and the specified other matrix (treating the matrices as
     * affine), placing the result in the object provided.
     *
     * @return a reference to the result object, for chaining.
     */
    public Matrix4f lerpAffine (Matrix4f other, float t, Matrix4f result)
    {
        return result.set(
            m00 + t*(other.m00 - m00),
            m10 + t*(other.m10 - m10),
            m20 + t*(other.m20 - m20),
            m30 + t*(other.m30 - m30),

            m01 + t*(other.m01 - m01),
            m11 + t*(other.m11 - m11),
            m21 + t*(other.m21 - m21),
            m31 + t*(other.m31 - m31),

            m02 + t*(other.m02 - m02),
            m12 + t*(other.m12 - m12),
            m22 + t*(other.m22 - m22),
            m32 + t*(other.m32 - m32),

            0f, 0f, 0f, 1f);
    }

    /**
     * Copies the contents of another matrix.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f set (Matrix4f other)
    {
        return set(
            other.m00, other.m10, other.m20, other.m30,
            other.m01, other.m11, other.m21, other.m31,
            other.m02, other.m12, other.m22, other.m32,
            other.m03, other.m13, other.m23, other.m33);
    }

    /**
     * Copies the elements of an array.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f set (float[] values)
    {
        return set(
            values[0], values[1], values[2], values[3],
            values[4], values[5], values[6], values[7],
            values[8], values[9], values[10], values[11],
            values[12], values[13], values[14], values[15]);
    }

    /**
     * Sets the contents of this matrix from the supplied buffer.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f set (FloatBuffer buf)
    {
        m00 = buf.get(); m01 = buf.get(); m02 = buf.get(); m03 = buf.get();
        m10 = buf.get(); m11 = buf.get(); m12 = buf.get(); m13 = buf.get();
        m20 = buf.get(); m21 = buf.get(); m22 = buf.get(); m23 = buf.get();
        m30 = buf.get(); m31 = buf.get(); m32 = buf.get(); m33 = buf.get();
        return this;
    }

    /**
     * Sets all of the matrix's components at once.
     *
     * @return a reference to this matrix, for chaining.
     */
    public Matrix4f set (
        float m00, float m10, float m20, float m30,
        float m01, float m11, float m21, float m31,
        float m02, float m12, float m22, float m32,
        float m03, float m13, float m23, float m33)
    {
        this.m00 = m00; this.m01 = m01; this.m02 = m02; this.m03 = m03;
        this.m10 = m10; this.m11 = m11; this.m12 = m12; this.m13 = m13;
        this.m20 = m20; this.m21 = m21; this.m22 = m22; this.m23 = m23;
        this.m30 = m30; this.m31 = m31; this.m32 = m32; this.m33 = m33;
        return this;
    }

    /**
     * Places the contents of this matrix into the given buffer in the standard OpenGL order.
     *
     * @return a reference to the buffer, for chaining.
     */
    public FloatBuffer get (FloatBuffer buf)
    {
        buf.put(m00).put(m01).put(m02).put(m03);
        buf.put(m10).put(m11).put(m12).put(m13);
        buf.put(m20).put(m21).put(m22).put(m23);
        buf.put(m30).put(m31).put(m32).put(m33);
        return buf;
    }

    /**
     * Projects the supplied point in-place using this matrix.
     *
     * @return a reference to the point, for chaining.
     */
    public Vector3f projectPointLocal (Vector3f point)
    {
        return projectPoint(point, point);
    }

    /**
     * Projects the supplied point using this matrix.
     *
     * @return a new vector containing the result.
     */
    public Vector3f projectPoint (Vector3f point)
    {
        return projectPoint(point, new Vector3f());
    }

    /**
     * Projects the supplied point using this matrix and places the result in the object supplied.
     *
     * @return a reference to the result vector, for chaining.
     */
    public Vector3f projectPoint (Vector3f point, Vector3f result)
    {
        float rw = 1f / (m03*point.x + m13*point.y + m23*point.z + m33);
        return result.set(
            (m00*point.x + m10*point.y + m20*point.z + m30) * rw,
            (m01*point.x + m11*point.y + m21*point.z + m31) * rw,
            (m02*point.x + m12*point.y + m22*point.z + m32) * rw);
    }

    /**
     * Transforms a point in-place by this matrix.
     *
     * @return a reference to the point, for chaining.
     */
    public Vector3f transformPointLocal (Vector3f point)
    {
        return transformPoint(point, point);
    }

    /**
     * Transforms a point by this matrix.
     *
     * @return a new vector containing the result.
     */
    public Vector3f transformPoint (Vector3f point)
    {
        return transformPoint(point, new Vector3f());
    }

    /**
     * Transforms a point by this matrix and places the result in the object provided.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f transformPoint (Vector3f point, Vector3f result)
    {
        return result.set(
            m00*point.x + m10*point.y + m20*point.z + m30,
            m01*point.x + m11*point.y + m21*point.z + m31,
            m02*point.x + m12*point.y + m22*point.z + m32);
    }

    /**
     * Transforms a point by this matrix and returns the resulting z coordinate.
     */
    public float transformPointZ (Vector3f point)
    {
        return m02*point.x + m12*point.y + m22*point.z + m32;
    }

    /**
     * Transforms a vector in-place by the inner 3x3 part of this matrix.
     *
     * @return a reference to the vector, for chaining.
     */
    public Vector3f transformVectorLocal (Vector3f vector)
    {
        return transformVector(vector, vector);
    }

    /**
     * Transforms a vector by this inner 3x3 part of this matrix.
     *
     * @return a new vector containing the result.
     */
    public Vector3f transformVector (Vector3f vector)
    {
        return transformVector(vector, new Vector3f());
    }

    /**
     * Transforms a vector by the inner 3x3 part of this matrix and places the result in the object
     * provided.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f transformVector (Vector3f vector, Vector3f result)
    {
        return result.set(
            m00*vector.x + m10*vector.y + m20*vector.z,
            m01*vector.x + m11*vector.y + m21*vector.z,
            m02*vector.x + m12*vector.y + m22*vector.z);
    }

    /**
     * Extracts the rotation component of the matrix.
     *
     * @return a new quaternion containing the result.
     */
    public Quaternion extractRotation ()
    {
        return extractRotation(new Quaternion());
    }

    /**
     * Extracts the rotation component of the matrix and places it in the provided result
     * quaternion.  This uses the iterative polar decomposition algorithm described by
     * <a href="http://www.cs.wisc.edu/graphics/Courses/838-s2002/Papers/polar-decomp.pdf">Ken
     * Shoemake</a>.
     *
     * @return a reference to the result quaternion, for chaining.
     */
    public Quaternion extractRotation (Quaternion result)
    {
        // start with the contents of the upper 3x3 portion of the matrix
        float n00 = m00, n10 = m10, n20 = m20;
        float n01 = m01, n11 = m11, n21 = m21;
        float n02 = m02, n12 = m12, n22 = m22;
        for (int ii = 0; ii < 10; ii++) {
            // store the results of the previous iteration
            float o00 = n00, o10 = n10, o20 = n20;
            float o01 = n01, o11 = n11, o21 = n21;
            float o02 = n02, o12 = n12, o22 = n22;

            // compute average of the matrix with its inverse transpose
            float sd00 = o11*o22 - o21*o12;
            float sd10 = o01*o22 - o21*o02;
            float sd20 = o01*o12 - o11*o02;
            float det = o00*sd00 + o20*sd20 - o10*sd10;
            if (Math.abs(det) == 0f) {
                // determinant is zero; matrix is not invertible
                throw new SingularMatrixException(this.toString());
            }
            float hrdet = 0.5f / det;
            n00 = +sd00 * hrdet + o00*0.5f;
            n10 = -sd10 * hrdet + o10*0.5f;
            n20 = +sd20 * hrdet + o20*0.5f;

            n01 = -(o10*o22 - o20*o12) * hrdet + o01*0.5f;
            n11 = +(o00*o22 - o20*o02) * hrdet + o11*0.5f;
            n21 = -(o00*o12 - o10*o02) * hrdet + o21*0.5f;

            n02 = +(o10*o21 - o20*o11) * hrdet + o02*0.5f;
            n12 = -(o00*o21 - o20*o01) * hrdet + o12*0.5f;
            n22 = +(o00*o11 - o10*o01) * hrdet + o22*0.5f;

            // compute the difference; if it's small enough, we're done
            float d00 = n00 - o00, d10 = n10 - o10, d20 = n20 - o20;
            float d01 = n01 - o01, d11 = n11 - o11, d21 = n21 - o21;
            float d02 = n02 - o02, d12 = n12 - o12, d22 = n22 - o22;
            if (d00*d00 + d10*d10 + d20*d20 + d01*d01 + d11*d11 + d21*d21 +
                    d02*d02 + d12*d12 + d22*d22 < FloatMath.EPSILON) {
                break;
            }
        }
        // now that we have a nice orthogonal matrix, we can extract the rotation quaternion
        // using the method described in http://en.wikipedia.org/wiki/Rotation_matrix#Conversions
        float x2 = Math.abs(1f + n00 - n11 - n22);
        float y2 = Math.abs(1f - n00 + n11 - n22);
        float z2 = Math.abs(1f - n00 - n11 + n22);
        float w2 = Math.abs(1f + n00 + n11 + n22);
        result.set(
            0.5f * FloatMath.sqrt(x2) * (n12 >= n21 ? +1f : -1f),
            0.5f * FloatMath.sqrt(y2) * (n20 >= n02 ? +1f : -1f),
            0.5f * FloatMath.sqrt(z2) * (n01 >= n10 ? +1f : -1f),
            0.5f * FloatMath.sqrt(w2));
        return result;
    }

    /**
     * Extracts the scale component of the matrix.
     *
     * @return a new vector containing the result.
     */
    public Vector3f extractScale ()
    {
        return extractScale(new Vector3f());
    }

    /**
     * Extracts the scale component of the matrix and places it in the provided result vector.
     *
     * @return a reference to the result vector, for chaining.
     */
    public Vector3f extractScale (Vector3f result)
    {
        return result.set(
            FloatMath.sqrt(m00*m00 + m01*m01 + m02*m02),
            FloatMath.sqrt(m10*m10 + m11*m11 + m12*m12),
            FloatMath.sqrt(m20*m20 + m21*m21 + m22*m22));
    }

    /**
     * Returns an approximation of the uniform scale for this matrix (the cube root of the
     * signed volume of the parallelepiped spanned by the axis vectors).
     */
    public float approximateUniformScale ()
    {
        return FloatMath.cbrt(
            m00*(m11*m22 - m12*m21) +
            m01*(m12*m20 - m10*m22) +
            m02*(m10*m21 - m11*m20));
    }

    /**
     * Compares this matrix to another with the provided epsilon.
     */
    public boolean epsilonEquals (Matrix4f other, float epsilon)
    {
        return
            Math.abs(m00 - other.m00) < epsilon &&
            Math.abs(m10 - other.m10) < epsilon &&
            Math.abs(m20 - other.m20) < epsilon &&
            Math.abs(m30 - other.m30) < epsilon &&

            Math.abs(m01 - other.m01) < epsilon &&
            Math.abs(m11 - other.m11) < epsilon &&
            Math.abs(m21 - other.m21) < epsilon &&
            Math.abs(m31 - other.m31) < epsilon &&

            Math.abs(m02 - other.m02) < epsilon &&
            Math.abs(m12 - other.m12) < epsilon &&
            Math.abs(m22 - other.m22) < epsilon &&
            Math.abs(m32 - other.m32) < epsilon &&

            Math.abs(m03 - other.m03) < epsilon &&
            Math.abs(m13 - other.m13) < epsilon &&
            Math.abs(m23 - other.m23) < epsilon &&
            Math.abs(m33 - other.m33) < epsilon;
    }

    // documentation inherited from interface Encodable
    public String encodeToString ()
    {
        return m00 + ", " + m10 + ", " + m20 + ", " + m30 + ", " +
            m01 + ", " + m11 + ", " + m21 + ", " + m31 + ", " +
            m02 + ", " + m12 + ", " + m22 + ", " + m32 + ", " +
            m03 + ", " + m13 + ", " + m23 + ", " + m33;
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
        out.writeFloat(m30);

        out.writeFloat(m01);
        out.writeFloat(m11);
        out.writeFloat(m21);
        out.writeFloat(m31);

        out.writeFloat(m02);
        out.writeFloat(m12);
        out.writeFloat(m22);
        out.writeFloat(m32);

        out.writeFloat(m03);
        out.writeFloat(m13);
        out.writeFloat(m23);
        out.writeFloat(m33);
    }

    // documentation inherited from interface Encodable
    public void decodeFromStream (DataInputStream in)
        throws IOException
    {
        set(in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat(),
            in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat(),
            in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat(),
            in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat());
    }

    @Override
    public String toString ()
    {
        return "[[" + m00 + ", " + m10 + ", " + m20 + ", " + m30 + "], " +
            "[" + m01 + ", " + m11 + ", " + m21 + ", " + m31 + "], " +
            "[" + m02 + ", " + m12 + ", " + m22 + ", " + m32 + "], " +
            "[" + m03 + ", " + m13 + ", " + m23 + ", " + m33 + "]]";
    }

    @Override
    public int hashCode ()
    {
        return Float.floatToIntBits(m00) ^ Float.floatToIntBits(m10) ^
                Float.floatToIntBits(m20) ^ Float.floatToIntBits(m30) ^
            Float.floatToIntBits(m01) ^ Float.floatToIntBits(m11) ^
                Float.floatToIntBits(m21) ^ Float.floatToIntBits(m31) ^
            Float.floatToIntBits(m02) ^ Float.floatToIntBits(m12) ^
                Float.floatToIntBits(m22) ^ Float.floatToIntBits(m32) ^
            Float.floatToIntBits(m03) ^ Float.floatToIntBits(m13) ^
                Float.floatToIntBits(m23) ^ Float.floatToIntBits(m33);
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof Matrix4f)) {
            return false;
        }
        Matrix4f omat = (Matrix4f)other;
        return
            m00 == omat.m00 && m10 == omat.m10 && m20 == omat.m20 && m30 == omat.m30 &&
            m01 == omat.m01 && m11 == omat.m11 && m21 == omat.m21 && m31 == omat.m31 &&
            m02 == omat.m02 && m12 == omat.m12 && m22 == omat.m22 && m32 == omat.m32 &&
            m03 == omat.m03 && m13 == omat.m13 && m23 == omat.m23 && m33 == omat.m33;
    }
}
