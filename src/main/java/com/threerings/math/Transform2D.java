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

import java.io.IOException;

import com.threerings.io.Streamable;

import com.threerings.export.Exportable;
import com.threerings.export.Exporter;
import com.threerings.export.Importer;

/**
 * Represents a 2D transformation in such a way as to accelerate operations such as composition
 * and inversion by keeping track of the nature of the transform.
 */
public final class Transform2D
    implements Exportable, Streamable
{
    /** An identity transformation. */
    public static final int IDENTITY = 0;

    /** A rigid transformation represented by a translation vector and a rotation value. */
    public static final int RIGID = 1;

    /** A transformation represented by translation, rotation, and a uniform scale value. */
    public static final int UNIFORM = 2;

    /** An affine transformation represented by the upper two rows of a 3x3 matrix. */
    public static final int AFFINE = 3;

    /** A general transformation represented by a 3x3 matrix. */
    public static final int GENERAL = 4;

    /**
     * Creates an identity transformation.
     */
    public Transform2D ()
    {
    }

    /**
     * Creates an identity transformation of the specified type.
     */
    public Transform2D (int type)
    {
        setType(type);
    }

    /**
     * Creates a transformation from the values in the supplied objects.
     */
    public Transform2D (Vector2f translation, float rotation)
    {
        set(translation, rotation);
    }

    /**
     * Creates a transformation from the values in the supplied objects.
     */
    public Transform2D (Vector2f translation, float rotation, float scale)
    {
        set(translation, rotation, scale);
    }

    /**
     * Creates a transformation from the values in the supplied objects.
     */
    public Transform2D (Vector2f translation, float rotation, Vector2f scale)
    {
        set(translation, rotation, scale);
    }

    /**
     * Creates a transformation from the values in the supplied matrix.
     */
    public Transform2D (Matrix3f matrix)
    {
        set(matrix);
    }

    /**
     * Creates a transformation from the values in the supplied matrix.
     *
     * @param affine whether or not the provided matrix is known to be affine.
     */
    public Transform2D (Matrix3f matrix, boolean affine)
    {
        set(matrix, affine);
    }

    /**
     * Creates a flattened version of the supplied transform.
     */
    public Transform2D (Transform3D transform)
    {
        set(transform);
    }

    /**
     * Copy constructor.
     */
    public Transform2D (Transform2D transform)
    {
        set(transform);
    }

    /**
     * Returns the type of this transformation.
     */
    public int getType ()
    {
        return _type;
    }

    /**
     * Sets the type of the transformation.  This doesn't actually change any of the values,
     * but it does initialize the fields required for the type if they are <code>null</code>.
     * To promote to a more general transform type, see {@link #promote}.
     */
    public void setType (int type)
    {
        if (((_type = type) == AFFINE || _type == GENERAL) && _matrix == null) {
            _matrix = new Matrix3f();
        } else if ((_type == RIGID || _type == UNIFORM) && _translation == null) {
            _translation = new Vector2f();
        }
    }

    /**
     * Returns a reference to the translation vector, which is only definitive when the type is
     * {@link #RIGID} or {@link #UNIFORM}.
     */
    public Vector2f getTranslation ()
    {
        return _translation;
    }

    /**
     * Returns the rotation value, which is only definitive when the type is {@link #RIGID} or
     * {@link #UNIFORM}.
     */
    public float getRotation ()
    {
        return _rotation;
    }

    /**
     * Sets the rotation.
     *
     * @return the rotation value set, for chaining.
     */
    public float setRotation (float rotation)
    {
        return (_rotation = rotation);
    }

    /**
     * Returns the uniform scale, which is only definitive when the type is {@link #UNIFORM}.
     */
    public float getScale ()
    {
        return _scale;
    }

    /**
     * Sets the uniform scale.
     *
     * @return the scale value set, for chaining.
     */
    public float setScale (float scale)
    {
        return (_scale = scale);
    }

    /**
     * Returns a reference to the transformation matrix, which is only definitive when the type is
     * {@link #AFFINE} or {@link #GENERAL}.
     */
    public Matrix3f getMatrix ()
    {
        return _matrix;
    }

    /**
     * Inverts this transform in-place.
     *
     * @return a reference to this transform, for chaining.
     */
    public Transform2D invertLocal ()
    {
        return invert(this);
    }

    /**
     * Inverts this transform.
     *
     * @return a new transform containing the result.
     */
    public Transform2D invert ()
    {
        return invert(new Transform2D());
    }

    /**
     * Inverts this transform, storing the result in the provided object.
     *
     * @return a reference to the result transform, for chaining.
     */
    public Transform2D invert (Transform2D result)
    {
        // the method of inversion depends on the type
        switch (_type) {
            default:
            case IDENTITY:
                return result.setToIdentity();

            case RIGID:
                result.setType(RIGID);
                _translation.negate(result.getTranslation()).rotateLocal(
                    result.setRotation(-_rotation));
                return result;

            case UNIFORM:
                result.setType(UNIFORM);
                _translation.negate(result.getTranslation()).rotateLocal(
                    result.setRotation(-_rotation)).multLocal(
                        result.setScale(1f / _scale));
                return result;

            case AFFINE:
                result.setType(AFFINE);
                _matrix.invertAffine(result.getMatrix());
                return result;

            case GENERAL:
                result.setType(GENERAL);
                _matrix.invert(result.getMatrix());
                return result;
        }
    }

    /**
     * Composes this transform in-place with another.
     *
     * @return a reference to this transform, for chaining.
     */
    public Transform2D composeLocal (Transform2D other)
    {
        return compose(other, this);
    }

    /**
     * Composes this transform with another.
     *
     * @return a new transform containing the result.
     */
    public Transform2D compose (Transform2D other)
    {
        return compose(other, new Transform2D());
    }

    /**
     * Composes this transform with another, storing the result in the object provided.
     */
    public Transform2D compose (Transform2D other, Transform2D result)
    {
        // the common type is the greater of the two
        int ctype = Math.max(getType(), other.getType());
        update(ctype);
        other.update(ctype);

        // the method of composition depends on the common type
        switch (ctype) {
            default:
            case IDENTITY:
                return result.setToIdentity();

            case RIGID:
                result.setType(RIGID);
                other.getTranslation().rotateAndAdd(
                    _rotation, _translation, result.getTranslation());
                result.setRotation(FloatMath.normalizeAngle(_rotation + other.getRotation()));
                return result;

            case UNIFORM:
                result.setType(UNIFORM);
                other.getTranslation().rotateScaleAndAdd(
                    _rotation, _scale, _translation, result.getTranslation());
                result.setRotation(FloatMath.normalizeAngle(_rotation + other.getRotation()));
                result.setScale(_scale * other.getScale());
                return result;

            case AFFINE:
                result.setType(AFFINE);
                _matrix.multAffine(other.getMatrix(), result.getMatrix());
                return result;

            case GENERAL:
                result.setType(GENERAL);
                _matrix.mult(other.getMatrix(), result.getMatrix());
                return result;
        }
    }

    /**
     * Linearly interpolates between this and the specified other transform, placing the
     * result in this transform.
     *
     * @return a reference to this transform, for chaining.
     */
    public Transform2D lerpLocal (Transform2D other, float t)
    {
        return lerp(other, t, this);
    }

    /**
     * Linearly interpolates between this and the specified other transform.
     *
     * @return a new transform containing the result.
     */
    public Transform2D lerp (Transform2D other, float t)
    {
        return lerp(other, t, new Transform2D());
    }

    /**
     * Linearly interpolates between this and the specified other transform, placing the result
     * in the transform provided.
     *
     * @return a reference to the result transform, for chaining.
     */
    public Transform2D lerp (Transform2D other, float t, Transform2D result)
    {
        // the common type is the greater of the two
        int ctype = Math.max(getType(), other.getType());
        update(ctype);
        other.update(ctype);

        // the method of interpolation depends on the common type
        switch (ctype) {
            default:
            case IDENTITY:
                return result.setToIdentity();

            case RIGID:
                result.setType(RIGID);
                _translation.lerp(other.getTranslation(), t, result.getTranslation());
                result.setRotation(FloatMath.lerpa(_rotation, other.getRotation(), t));
                return result;

            case UNIFORM:
                result.setType(UNIFORM);
                _translation.lerp(other.getTranslation(), t, result.getTranslation());
                result.setRotation(FloatMath.lerpa(_rotation, other.getRotation(), t));
                result.setScale(FloatMath.lerp(_scale, other.getScale(), t));
                return result;

            case AFFINE:
                result.setType(AFFINE);
                _matrix.lerpAffine(other.getMatrix(), t, result.getMatrix());
                return result;

            case GENERAL:
                result.setType(GENERAL);
                _matrix.lerp(other.getMatrix(), t, result.getMatrix());
                return result;
        }
    }

    /**
     * Copies the values contained in another transform.
     *
     * @return a reference to this transform, for chaining.
     */
    public Transform2D set (Transform2D transform)
    {
        switch (transform.getType()) {
            default:
            case IDENTITY:
                return setToIdentity();
            case RIGID:
                return set(transform.getTranslation(), transform.getRotation());
            case UNIFORM:
                return set(transform.getTranslation(), transform.getRotation(),
                    transform.getScale());
            case AFFINE:
                return set(transform.getMatrix(), true);
            case GENERAL:
                return set(transform.getMatrix(), false);
        }
    }

    /**
     * Sets the transform to a flattened version of a 3D transform.
     *
     * @return a reference to this transform, for chaining.
     */
    public Transform2D set (Transform3D transform)
    {
        int type = transform.getType();
        switch (type) {
            default:
            case IDENTITY:
                return setToIdentity();
            case RIGID:
            case UNIFORM:
                setType(type);
                Vector3f translation = transform.getTranslation();
                _translation.set(translation.x, translation.y);
                _rotation = transform.getRotation().getRotationZ();
                if (type == UNIFORM) {
                    _scale = transform.getScale();
                }
                return this;
            case AFFINE:
            case GENERAL:
                setType(type);
                Matrix4f matrix = transform.getMatrix();
                _matrix.set(
                    matrix.m00, matrix.m10, matrix.m30,
                    matrix.m01, matrix.m11, matrix.m31,
                    matrix.m03, matrix.m13, matrix.m33);
                return this;
        }
    }

    /**
     * Sets the transform to the identity transform.
     *
     * @return a reference to this transform, for chaining.
     */
    public Transform2D setToIdentity ()
    {
        setType(IDENTITY);
        return this;
    }

    /**
     * Sets the transform using the supplied values.
     *
     * @return a reference to this transform, for chaining.
     */
    public Transform2D set (Vector2f translation, float rotation)
    {
        setType(RIGID);
        _translation.set(translation);
        _rotation = rotation;
        return this;
    }

    /**
     * Sets the transform using the supplied values.
     *
     * @return a reference to this transform, for chaining.
     */
    public Transform2D set (Vector2f translation, float rotation, float scale)
    {
        setType(UNIFORM);
        _translation.set(translation);
        _rotation = rotation;
        _scale = scale;
        return this;
    }

    /**
     * Sets the transform using the supplied values.
     *
     * @return a reference to this transform, for chaining.
     */
    public Transform2D set (Vector2f translation, float rotation, Vector2f scale)
    {
        setType(AFFINE);
        _matrix.setToTransform(translation, rotation, scale);
        return this;
    }

    /**
     * Sets the transform using the supplied matrix.
     *
     * @return a reference to this transform, for chaining.
     */
    public Transform2D set (Matrix3f matrix)
    {
        return set(matrix, false);
    }

    /**
     * Sets the transform using the supplied matrix.
     *
     * @param affine whether or not the provided matrix is affine.
     * @return a reference to this transform, for chaining.
     */
    public Transform2D set (Matrix3f matrix, boolean affine)
    {
        setType(affine ? AFFINE : GENERAL);
        _matrix.set(matrix);
        return this;
    }

    /**
     * Promotes this transform to the specified type, which must be greater than or equal to
     * its current type.
     *
     * @return a reference to this transform, for chaining.
     */
    public Transform2D promote (int type)
    {
        update(type);
        setType(type);
        return this;
    }

    /**
     * Updates the transform fields corresponding to the specified type.  For example, if this
     * matrix is {@link #IDENTITY} and <code>type</code> is {@link #RIGID}, then the translation
     * and rotation fields are set to zero.
     *
     * @param utype the desired type, which must be greater than or equal to the type of this
     * transform.
     * @return a reference to this transform, for chaining.
     */
    public Transform2D update (int utype)
    {
        if (_type == IDENTITY) {
            if (utype >= AFFINE) {
                _matrix = (_matrix == null) ? new Matrix3f() : _matrix;
            } else if (utype >= RIGID) {
                _translation = (_translation == null) ? new Vector2f() : _translation;
                _rotation = 0f;
                _scale = 1f;
            }
        } else if (_type == RIGID) {
            if (utype >= AFFINE) {
                (_matrix == null ? (_matrix = new Matrix3f()) : _matrix).setToTransform(
                    _translation, _rotation);
            } else if (utype == UNIFORM) {
                _scale = 1f;
            }
        } else if (_type == UNIFORM && utype >= AFFINE) {
            (_matrix == null ? (_matrix = new Matrix3f()) : _matrix).setToTransform(
                _translation, _rotation, _scale);
        }
        return this;
    }

    /**
     * Transforms a point in-place by this transform.
     *
     * @return a reference to the point, for chaining.
     */
    public Vector2f transformPointLocal (Vector2f pt)
    {
        return transformPoint(pt, pt);
    }

    /**
     * Transforms a point by this transform.
     *
     * @return a new vector containing the result.
     */
    public Vector2f transformPoint (Vector2f pt)
    {
        return transformPoint(pt, new Vector2f());
    }

    /**
     * Transforms a point by this transform and places the result in the object provided.
     *
     * @return a reference to the result object, for chaining.
     */
    public Vector2f transformPoint (Vector2f pt, Vector2f result)
    {
        switch (_type) {
            default:
            case IDENTITY:
                return result.set(pt);
            case RIGID:
                return pt.rotate(_rotation, result).addLocal(_translation);
            case UNIFORM:
                return pt.mult(_scale, result).rotateLocal(_rotation).addLocal(_translation);
            case AFFINE:
            case GENERAL:
                return _matrix.transformPoint(pt, result);
        }
    }

    /**
     * Transforms a vector in-place by this transform.
     *
     * @return a reference to the transformed vector, for chaining.
     */
    public Vector2f transformVectorLocal (Vector2f vec)
    {
        return transformVector(vec, vec);
    }

    /**
     * Transforms a vector by this transform.
     *
     * @return a new vector containing the result.
     */
    public Vector2f transformVector (Vector2f vec)
    {
        return transformVector(vec, new Vector2f());
    }

    /**
     * Transforms a vector by this transform and places the result in the object provided.
     *
     * @return a reference to the result vector, for chaining.
     */
    public Vector2f transformVector (Vector2f vec, Vector2f result)
    {
        switch (_type) {
            default:
            case IDENTITY:
                return result.set(vec);
            case RIGID:
                return vec.rotate(_rotation, result);
            case UNIFORM:
                return vec.mult(_scale, result).rotateLocal(_rotation);
            case AFFINE:
            case GENERAL:
                return _matrix.transformVector(vec, result);
        }
    }

    /**
     * Extracts the translation component of the transform.
     *
     * @return a new vector containing the result.
     */
    public Vector2f extractTranslation ()
    {
        return extractTranslation(new Vector2f());
    }

    /**
     * Extracts the translation component of the transform and places it in the provided result
     * vector.
     *
     * @return a reference to the result vector, for chaining.
     */
    public Vector2f extractTranslation (Vector2f result)
    {
        switch (_type) {
            default:
            case IDENTITY:
                return result.set(Vector2f.ZERO);
            case RIGID:
            case UNIFORM:
                return result.set(_translation);
            case AFFINE:
            case GENERAL:
                return result.set(_matrix.m20, _matrix.m21);
        }
    }

    /**
     * Extracts the rotation component of the transform.
     */
    public float extractRotation ()
    {
        switch (_type) {
            default:
            case IDENTITY:
                return 0f;
            case RIGID:
            case UNIFORM:
                return _rotation;
            case AFFINE:
            case GENERAL:
                return _matrix.extractRotation();
        }
    }

    /**
     * Extracts an approximation of the uniform scale from this transform.
     */
    public float approximateUniformScale ()
    {
        switch (_type) {
            default:
            case IDENTITY:
            case RIGID:
                return 1f;
            case UNIFORM:
                return _scale;
            case AFFINE:
            case GENERAL:
                return _matrix.approximateUniformScale();
        }
    }

    /**
     * Custom field write method.
     */
    public void writeFields (Exporter out)
        throws IOException
    {
        if (_type == IDENTITY) {
            return;
        } else if (_type == AFFINE || _type == GENERAL) {
            out.write("matrix", _matrix, Matrix3f.IDENTITY);
        } else { // _type == RIGID || _type == UNIFORM
            if (_type == UNIFORM) {
                out.write("scale", _scale, 1f);
            }
            out.write("translation", _translation, Vector2f.ZERO);
            out.write("rotation", _rotation, 0f);
        }
    }

    /**
     * Custom field read method.
     */
    public void readFields (Importer in)
        throws IOException
    {
        _translation = in.read("translation", (Vector2f)null);
        _rotation = in.read("rotation", 0f);
        _scale = in.read("scale", 1f);
        _matrix = in.read("matrix", (Matrix3f)null);
        if (_matrix != null) {
            _type = _matrix.isAffine() ? AFFINE : GENERAL;
        } else if (_translation != null || _rotation != 0f || _scale != 1f) {
            _translation = (_translation == null) ? new Vector2f() : _translation;
            _type = (_scale == 1f) ? RIGID : UNIFORM;
        } else {
            _type = IDENTITY;
        }
    }

    @Override
    public String toString ()
    {
        switch (_type) {
            default:
            case IDENTITY:
                return "[]";
            case RIGID:
                return "[" + _translation + ", " + _rotation + "]";
            case UNIFORM:
                return "[" + _translation + ", " + _rotation + ", " + _scale + "]";
            case AFFINE:
            case GENERAL:
                return _matrix.toString();
        }
    }

    @Override
    public int hashCode ()
    {
        switch (_type) {
            default:
            case IDENTITY:
                return _type;
            case RIGID:
            case UNIFORM:
                int hash = 31*_type + _translation.hashCode();
                hash = 31*hash + Float.floatToIntBits(_rotation);
                return (_type == UNIFORM) ? (31*hash + Float.floatToIntBits(_scale)) : hash;
            case AFFINE:
            case GENERAL:
                return 31*_type + _matrix.hashCode();
        }
    }

    @Override
    public boolean equals (Object other)
    {
        Transform2D otrans = (Transform2D)other;
        if (!(other instanceof Transform2D && _type == otrans.getType())) {
            return false;
        }
        switch (_type) {
            default:
            case IDENTITY:
                return true;
            case RIGID:
                return _translation.equals(otrans.getTranslation()) &&
                    _rotation == otrans.getRotation();
            case UNIFORM:
                return _translation.equals(otrans.getTranslation()) &&
                    _rotation == otrans.getRotation() &&
                    _scale == otrans.getScale();
            case AFFINE:
            case GENERAL:
                return _matrix.equals(otrans.getMatrix());
        }
    }

    /** The type of this transform. */
    protected int _type;

    /** For rigid and uniform transforms, the translation vector. */
    protected Vector2f _translation;

    /** For rigid and uniform transforms, the rotation value. */
    protected float _rotation;

    /** For uniform transforms, the uniform scale. */
    protected float _scale = 1f;

    /** For affine and general transforms, the transformation matrix. */
    protected Matrix3f _matrix;
}
