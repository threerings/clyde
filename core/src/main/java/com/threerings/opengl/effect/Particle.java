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

package com.threerings.opengl.effect;

import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.probs.ColorFunctionVariable;
import com.threerings.probs.FloatFunctionVariable;

import com.threerings.opengl.renderer.Color4f;

/**
 * Contains the state of a single particle.
 */
public final class Particle
{
    /** The depth of the particle (used for depth sorting). */
    public float depth;

    /**
     * Returns a reference to the particle's position.
     */
    public Vector3f getPosition ()
    {
        return _position;
    }

    /**
     * Computes the particle's position at some point in its past (where 0 is the oldest position
     * and 1 is the newest).
     */
    public Vector3f getPosition (float t, Vector3f result)
    {
        return (_lengthfunc == null) ? result.set(_position) : _history.get(t, result);
    }

    /**
     * Computes the particle's size at some point in its past.
     */
    public float getSize (float t)
    {
        return _sizefunc.getValue(Math.max(_age - t*_length*_lifescale, 0f));
    }

    /**
     * Returns a reference to the particle's (linear) velocity.
     */
    public Vector3f getVelocity ()
    {
        return _velocity;
    }

    /**
     * Returns a reference to the particle's orientation.
     */
    public Quaternion getOrientation ()
    {
        return _orientation;
    }

    /**
     * Returns a reference to the particle's angular velocity.
     */
    public Vector3f getAngularVelocity ()
    {
        return _angularVelocity;
    }

    /**
     * Returns a reference to the particle's color.
     */
    public Color4f getColor ()
    {
        return _color;
    }

    /**
     * Sets the particle's size.
     */
    public void setSize (float size)
    {
        _size = size;
    }

    /**
     * Returns the particle's size.
     */
    public float getSize ()
    {
        return _size;
    }

    /**
     * Returns the particle's texture frame.
     */
    public float getFrame ()
    {
        return _frame;
    }

    /**
     * Initializes the particle.
     */
    public void init (
        float lifespan, AlphaMode alphaMode, ColorFunctionVariable color,
        FloatFunctionVariable size, FloatFunctionVariable length,
        FloatFunctionVariable frame, Transform3D historyTransform)
    {
        _age = 0f;
        _lifescale = 1f / lifespan;
        _history.init(_position, historyTransform);
        _alphaMode = alphaMode;
        _colorfunc = color.getValue(_colorfunc);
        _alphaMode.apply(_colorfunc.getValue(0f, _color));
        _sizefunc = size.getValue(_sizefunc);
        _size = _sizefunc.getValue(0f);
        if (length == null) {
            _lengthfunc = null;
            _length = 0f;
        } else {
            _lengthfunc = length.getValue(_lengthfunc);
            _length = _lengthfunc.getValue(0f);
        }
        if (frame == null) {
            _framefunc = null;
            _frame = 0f;
        } else {
            _framefunc = frame.getValue(_framefunc);
            _frame = _framefunc.getValue(0f);
        }
    }

    /**
     * Updates the particle state based on the elapsed time in seconds.
     *
     * @return true if the particle is still alive, false if it is dead.
     */
    public boolean tick (float elapsed)
    {
        if ((_age += elapsed * _lifescale) >= 1f) {
            return false;
        }
        // take an Euler step
        _position.addScaledLocal(_velocity, elapsed);
        _orientation.integrateLocal(_angularVelocity, elapsed);

        // update color, size
        _alphaMode.apply(_colorfunc.getValue(_age, _color));
        _size = _sizefunc.getValue(_age);

        // update length and record the new position if we have a tail
        if (_lengthfunc != null) {
            _length = _lengthfunc.getValue(_age);
            _history.record(_position, elapsed, _length);
        }

        // update texture frame
        if (_framefunc != null) {
            _frame = _framefunc.getValue(_age);
        }
        return true;
    }

    /** The particle's proportional age (0 to 1). */
    protected float _age;

    /** The reciprocal of the particle's lifespan. */
    protected float _lifescale;

    /** The particle's position. */
    protected Vector3f _position = new Vector3f();

    /** The particle's position history. */
    protected PositionHistory _history = new PositionHistory();

    /** The particle's (linear) velocity. */
    protected Vector3f _velocity = new Vector3f();

    /** The particle's orientation. */
    protected Quaternion _orientation = new Quaternion();

    /** The particle's angular velocity. */
    protected Vector3f _angularVelocity = new Vector3f();

    /** The particle's alpha mode. */
    protected AlphaMode _alphaMode = AlphaMode.TRANSLUCENT;

    /** The particle's color as a function of its proportional age. */
    protected ColorFunction _colorfunc;

    /** The particle's current color. */
    protected Color4f _color = new Color4f(0f, 0f, 0f, 0f);

    /** The particle's size as a function of its proportional age. */
    protected FloatFunction _sizefunc;

    /** The particle's current size. */
    protected float _size;

    /** The particle's length as a function of its proportional age. */
    protected FloatFunction _lengthfunc;

    /** The particle's current length. */
    protected float _length;

    /** The particle's texture frame as a function of its proportional age. */
    protected FloatFunction _framefunc;

    /** The particle's current texture frame. */
    protected float _frame;
}
