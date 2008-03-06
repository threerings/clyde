//
// $Id$

package com.threerings.math;

import java.nio.FloatBuffer;

/**
 * A 3x3 column-major matrix.
 */
public final class Matrix3f
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
     * Copies the contents of another matrix.
     *
     * @reutrn a reference to this matrix, for chaining.
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

    @Override // documentation inherited
    public String toString ()
    {
        return "[[" + m00 + ", " + m10 + ", " + m20 + "], " +
            "[" + m01 + ", " + m11 + ", " + m21 + "], " +
            "[" + m02 + ", " + m12 + ", " + m22 + "]]";
    }

    @Override // documentation inherited
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
