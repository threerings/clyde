//
// $Id$

package com.threerings.math;

import com.samskivert.util.RandomUtil;

/**
 * Utility methods and constants for single-precision floating point math.
 */
public class FloatMath
{
    /** The ratio of a circle's circumference to its diameter. */
    public static final float PI = (float)Math.PI;

    /** Pi times two. */
    public static final float TWO_PI = PI * 2f;

    /** Pi times one half. */
    public static final float HALF_PI = PI * 0.5f;

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
     * Returns v to the power of e.
     *
     * @see Math#pow
     */
    public static float pow (float v, float e)
    {
        return (float)Math.pow(v, e);
    }

    /**
     * Returns the absolute value of v.
     *
     * @see Math#abs
     */
    public static float abs (float v)
    {
        return (float)Math.abs(v);
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
     * Returns the ceiling of v.
     *
     * @see Math#ceil
     */
    public static float ceil (float v)
    {
        return (float)Math.ceil(v);
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
        target = abs(target);
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
        return lerp(lower, upper, random());
    }

    /**
     * Returns a uniformly distributed random floating point value in [0, 1).
     */
    public static float random ()
    {
        return RandomUtil.rand.nextFloat();
    }

    /**
     * Returns a random value according to the normal distribution with the provided mean and
     * standard deviation.
     */
    public static float normal (float mean, float stddev)
    {
        return stddev*normal() + mean;
    }

    /**
     * Returns a random value according to the standard normal distribution.
     */
    public static float normal ()
    {
        return (float)RandomUtil.rand.nextGaussian();
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
     * Returns an angle in the range [-pi, pi].
     */
    public static float normalizeAngle (float a)
    {
        while (a < -FloatMath.PI) a += FloatMath.TWO_PI;
        while (a > FloatMath.PI) a -= FloatMath.TWO_PI;
        return a;
    }

    /**
     * Returns the mirror angle of the specified angle (assumed to be in [-pi, +pi]).
     */
    protected static float mirrorAngle (float a)
    {
        return (a > 0f ? PI : -PI) - a;
    }
}
