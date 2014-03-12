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

import com.samskivert.util.Randoms;

/**
 * Utility methods and constants for single-precision floating point math.
 */
public class FloatMath
{
    /** The ratio of a circle's circumference to its diameter. */
    public static final float PI = (float)Math.PI;

    /** The circle constant, tau (&#964;) http://tauday.com/ */
    public static final float TWO_PI = (float)(Math.PI * 2);

    /** Pi times one half. */
    public static final float HALF_PI = PI * 0.5f;

    /** Pi times one quarter. */
    public static final float QUARTER_PI = PI * 0.25f;

    /** The base value of the natural logarithm. */
    public static final float E = (float)Math.E;

    /** A small number. */
    public static final float EPSILON = 0.000001f;

    /**
     * Computes and returns the sine of the given angle.
     *
     * @see Math#sin
     */
    public static float sin (float a)
    {
        return (float)Math.sin(a);
    }

    /**
     * Computes and returns the cosine of the given angle.
     *
     * @see Math#cos
     */
    public static float cos (float a)
    {
        return (float)Math.cos(a);
    }

    /**
     * Computes and returns the tangent of the given angle.
     *
     * @see Math#tan
     */
    public static float tan (float a)
    {
        return (float)Math.tan(a);
    }

    /**
     * Computes and returns the arc sine of the given value.
     *
     * @see Math#asin
     */
    public static float asin (float a)
    {
        return (float)Math.asin(a);
    }

    /**
     * Computes and returns the arc cosine of the given value.
     *
     * @see Math#acos
     */
    public static float acos (float a)
    {
        return (float)Math.acos(a);
    }

    /**
     * Computes and returns the arc tangent of the given value.
     *
     * @see Math#atan
     */
    public static float atan (float a)
    {
        return (float)Math.atan(a);
    }

    /**
     * Computes and returns the arc tangent of the given values.
     *
     * @see Math#atan2
     */
    public static float atan2 (float y, float x)
    {
        return (float)Math.atan2(y, x);
    }

    /**
     * Converts from radians to degrees.
     *
     * @see Math#toDegrees
     */
    public static float toDegrees (float a)
    {
        return a * (180f / PI);
    }

    /**
     * Converts from degrees to radians.
     *
     * @see Math#toRadians
     */
    public static float toRadians (float a)
    {
        return a * (PI / 180f);
    }

    /**
     * Returns the square root of the supplied value.
     *
     * @see Math#sqrt
     */
    public static float sqrt (float v)
    {
        return (float)Math.sqrt(v);
    }

    /**
     * Returns the cube root of the supplied value.
     *
     * @see Math#cbrt
     */
    public static float cbrt (float v)
    {
        return (float)Math.cbrt(v);
    }

    /**
     * Computes and returns sqrt(x*x + y*y).
     *
     * @see Math#hypot
     */
    public static float hypot (float x, float y)
    {
        return (float)Math.hypot(x, y);
    }

    /**
     * Returns e to the power of the supplied value.
     *
     * @see Math#exp
     */
    public static float exp (float v)
    {
        return (float)Math.exp(v);
    }

    /**
     * Returns the natural logarithm of the supplied value.
     *
     * @see Math#log
     */
    public static float log (float v)
    {
        return (float)Math.log(v);
    }

    /**
     * Returns the base 10 logarithm of the supplied value.
     *
     * @see Math#log10
     */
    public static float log10 (float v)
    {
        return (float)Math.log10(v);
    }

    /**
     * Returns v to the power of e.
     *
     * @see Math#pow
     */
    public static float pow (float v, float e)
    {
        return (float)Math.pow(v, e);
    }

    /**
     * Returns the floor of v.
     *
     * @see Math#floor
     */
    public static float floor (float v)
    {
        return (float)Math.floor(v);
    }

    /**
     * A cheaper version of {@link Math#round} that doesn't handle the special cases.
     */
    public static int round (float v)
    {
        return (v < 0f) ? (int)(v - 0.5f) : (int)(v + 0.5f);
    }

    /**
     * Returns the floor of v as an integer without calling the relatively expensive
     * {@link Math#floor}.
     */
    public static int ifloor (float v)
    {
        int iv = (int)v;
        return (v < 0f) ? ((iv == v || iv == Integer.MIN_VALUE) ? iv : (iv - 1)) : iv;
    }

    /**
     * Returns the ceiling of v.
     *
     * @see Math#ceil
     */
    public static float ceil (float v)
    {
        return (float)Math.ceil(v);
    }

    /**
     * Returns the ceiling of v as an integer without calling the relatively expensive
     * {@link Math#ceil}.
     */
    public static int iceil (float v)
    {
        int iv = (int)v;
        return (v > 0f) ? ((iv == v || iv == Integer.MAX_VALUE) ? iv : (iv + 1)) : iv;
    }

    /**
     * Returns the remainder when f1 is divided by f2.
     *
     * @see Math#IEEEremainder
     */
    public static float IEEEremainder (float f1, float f2)
    {
        return (float)Math.IEEEremainder(f1, f2);
    }

    /**
     * Clamps a value to the range [lower, upper].
     */
    public static float clamp (float v, float lower, float upper)
    {
        return Math.min(Math.max(v, lower), upper);
    }

    /**
     * Rounds a value to the nearest multiple of a target.
     */
    public static float roundNearest (float v, float target)
    {
        target = Math.abs(target);
        if (v >= 0) {
            return target * floor((v + 0.5f * target) / target);
        } else {
            return target * ceil((v - 0.5f * target) / target);
        }
    }

    /**
     * Checks whether the value supplied is in [lower, upper].
     */
    public static boolean isWithin (float v, float lower, float upper)
    {
        return v >= lower && v <= upper;
    }

    /**
     * Returns a uniformly distributed random floating point value in [lower, upper).
     */
    public static float random (float lower, float upper)
    {
        return Randoms.threadLocal().getInRange(lower, upper);
        //return lerp(lower, upper, random());
    }

    /**
     * Returns a uniformly distributed random floating point value in [0, 1).
     */
    public static float random ()
    {
        // TODO: support for this in Randoms?
        return Randoms.threadLocal().getFloat(1f);
    }

    /**
     * Returns a random value according to the normal distribution with the provided mean and
     * standard deviation.
     */
    public static float normal (float mean, float stddev)
    {
        return Randoms.threadLocal().getNormal(mean, stddev);
    }

    /**
     * Returns a random value according to the standard normal distribution.
     */
    public static float normal ()
    {
        // TODO: support for this in Randoms?
        return Randoms.threadLocal().getNormal(0f, 1f);
    }

    /**
     * Returns a random value according to the exponential distribution with the provided mean.
     */
    public static float exponential (float mean)
    {
        return -log(1f - random()) * mean;
    }

    /**
     * Linearly interpolates between two angles, taking the shortest path around the circle.
     * This assumes that both angles are in [-pi, +pi].
     */
    public static float lerpa (float a1, float a2, float t)
    {
        float ma1 = mirrorAngle(a1), ma2 = mirrorAngle(a2);
        float d = Math.abs(a2 - a1), md = Math.abs(ma1 - ma2);
        return (d < md) ? lerp(a1, a2, t) : mirrorAngle(lerp(ma1, ma2, t));
    }

    /**
     * Linearly interpolates between v1 and v2 by the parameter t.
     */
    public static float lerp (float v1, float v2, float t)
    {
        return v1 + t*(v2 - v1);
    }

    /**
     * Determines whether two values are "close enough" to equal.
     */
    public static boolean epsilonEquals (float v1, float v2)
    {
        return Math.abs(v1 - v2) < EPSILON;
    }

    /**
     * Returns the (shortest) distance between two angles, assuming that both angles are in
     * [-pi, +pi].
     */
    public static float getAngularDistance (float a1, float a2)
    {
        float ma1 = mirrorAngle(a1), ma2 = mirrorAngle(a2);
        return Math.min(Math.abs(a1 - a2), Math.abs(ma1 - ma2));
    }

    /**
     * Returns the (shortest) difference between two angles, assuming that both angles are in
     * [-pi, +pi].
     */
    public static float getAngularDifference (float a1, float a2)
    {
        float ma1 = mirrorAngle(a1), ma2 = mirrorAngle(a2);
        float diff = a1 - a2, mdiff = ma2 - ma1;
        return (Math.abs(diff) < Math.abs(mdiff)) ? diff : mdiff;
    }

    /**
     * Returns an angle in the range [-pi, pi].
     */
    public static float normalizeAngle (float a)
    {
        while (a < -PI) {
            a += TWO_PI;
        }
        while (a > PI) {
            a -= TWO_PI;
        }
        return a;
    }

    /**
     * Returns an angle in the range [0, 2pi].
     */
    public static float normalizeAnglePositive (float a)
    {
        while (a < 0f) {
            a += TWO_PI;
        }
        while (a > TWO_PI) {
            a -= TWO_PI;
        }
        return a;
    }

    /**
     * Returns the mirror angle of the specified angle (assumed to be in [-pi, +pi]).
     */
    public static float mirrorAngle (float a)
    {
        return (a > 0f ? PI : -PI) - a;
    }

    /**
     * Computes the reflection of a vector.  The formula comes from the GLSL specification.
     *
     * @return a new vector containing the result.
     */
    public static Vector3f reflect (Vector3f i, Vector3f n)
    {
        return reflect(i, n, new Vector3f());
    }

    /**
     * Computes the reflection of a vector and stores it in the provided vector.
     *
     * @return a reference to the result, for chaining.
     */
    public static Vector3f reflect (Vector3f i, Vector3f n, Vector3f result)
    {
        return result.set(n).multLocal(-2f * n.dot(i)).addLocal(i);
    }

    /**
     * Computes the refraction of a vector.  The formula comes from the GLSL specification.
     *
     * @return a new vector containing the result.
     */
    public static Vector3f refract (Vector3f i, Vector3f n, float eta)
    {
        return refract(i, n, eta, new Vector3f());
    }

    /**
     * Computes the refraction of a vector, placing the result in the provided vector.
     *
     * @return a reference to the result, for chaining.
     */
    public static Vector3f refract (Vector3f i, Vector3f n, float eta, Vector3f result)
    {
        float ndoti = n.dot(i);
        float k = 1f - eta * eta * (1f - ndoti * ndoti);
        return (k < 0f) ? result.set(Vector3f.ZERO) :
            result.set(n).multLocal(-eta * ndoti - sqrt(k)).addScaledLocal(i, eta);
    }

    /**
     * Updates the value of the closest point and returns a new result vector reference.
     * This is used to minimize garbage creation when searching for the closet point using, for
     * example, the following pattern:
     *
     * <p><pre>
     * Vector3f closest = result;
     * for (Model model : models) {
     *     if (model.getIntersection(ray, result)) {
     *         result = FloatMath.updateClosest(ray.getOrigin(), result, closest);
     *     }
     * }
     * // if result != closest, then we hit something
     * </pre>
     */
    public static Vector3f updateClosest (Vector3f origin, Vector3f result, Vector3f closest)
    {
        if (result == closest) {
            return new Vector3f();
        }
        if (origin.distanceSquared(result) < origin.distanceSquared(closest)) {
            closest.set(result);
        }
        return result;
    }
}
