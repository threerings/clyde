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

package com.threerings.util;

import com.samskivert.util.ArrayUtil;

import com.threerings.math.FloatMath;

/**
 * Methods for generating Perlin noise.  The algorithm used is that described in Ken Perlin's
 * <a href="http://mrl.nyu.edu/~perlin/paper445.pdf">Improving Noise</a>.
 */
public class NoiseUtil
{
  /**
   * Returns the noise value at the specified coordinate.
   */
  public static float getNoise (float x)
  {
    int x0 = FloatMath.ifloor(x);
    x -= x0;
    x0 &= PERMUTATION_MASK;
    return FloatMath.lerp(
      grad(PERMUTATIONS[x0], x),
      grad(PERMUTATIONS[x0 + 1], x - 1f),
      ease(x));
  }

  /**
   * Returns the noise value at the specified coordinates.
   */
  public static float getNoise (float x, int y)
  {
    int x0 = FloatMath.ifloor(x);
    x -= x0;
    x0 &= PERMUTATION_MASK;
    y &= PERMUTATION_MASK;
    return FloatMath.lerp(
      grad(PERMUTATIONS[PERMUTATIONS[x0] + y], x),
      grad(PERMUTATIONS[PERMUTATIONS[x0 + 1] + y], x - 1f),
      ease(x));
  }

  /**
   * Returns the noise value at the specified coordinates.
   */
  public static float getNoise (float x, float y)
  {
    int x0 = FloatMath.ifloor(x);
    int y0 = FloatMath.ifloor(y);
    x -= x0;
    y -= y0;
    x0 &= PERMUTATION_MASK;
    y0 &= PERMUTATION_MASK;
    int y1 = y0 + 1;
    int p0 = PERMUTATIONS[x0];
    int p1 = PERMUTATIONS[x0 + 1];
    float xm1 = x - 1f, ym1 = y - 1f;
    float s = ease(x);
    return FloatMath.lerp(
      FloatMath.lerp(
        grad(PERMUTATIONS[p0 + y0], x, y),
        grad(PERMUTATIONS[p1 + y0], xm1, y), s),
      FloatMath.lerp(
        grad(PERMUTATIONS[p0 + y1], x, ym1),
        grad(PERMUTATIONS[p1 + y1], xm1, ym1), s),
      ease(y));
  }

  /**
   * Returns the noise value at the specified coordinates.
   */
  public static float getNoise (float x, float y, int z)
  {
    int x0 = FloatMath.ifloor(x);
    int y0 = FloatMath.ifloor(y);
    x -= x0;
    y -= y0;
    x0 &= PERMUTATION_MASK;
    y0 &= PERMUTATION_MASK;
    z &= PERMUTATION_MASK;
    int y1 = y0 + 1;
    int p0 = PERMUTATIONS[x0];
    int p1 = PERMUTATIONS[x0 + 1];
    float xm1 = x - 1f, ym1 = y - 1f;
    float s = ease(x);
    return FloatMath.lerp(
      FloatMath.lerp(
        grad(PERMUTATIONS[PERMUTATIONS[p0 + y0] + z], x, y),
        grad(PERMUTATIONS[PERMUTATIONS[p1 + y0] + z], xm1, y), s),
      FloatMath.lerp(
        grad(PERMUTATIONS[PERMUTATIONS[p0 + y1] + z], x, ym1),
        grad(PERMUTATIONS[PERMUTATIONS[p1 + y1] + z], xm1, ym1), s),
      ease(y));
  }

  /**
   * Returns the noise value at the specified coordinates.
   */
  public static float getNoise (float x, float y, float z)
  {
    int x0 = FloatMath.ifloor(x);
    int y0 = FloatMath.ifloor(y);
    int z0 = FloatMath.ifloor(z);
    x -= x0;
    y -= y0;
    z -= z0;
    x0 &= PERMUTATION_MASK;
    y0 &= PERMUTATION_MASK;
    z0 &= PERMUTATION_MASK;
    int y1 = y0 + 1, z1 = z0 + 1;
    int p0 = PERMUTATIONS[x0];
    int p1 = PERMUTATIONS[x0 + 1];
    int p00 = PERMUTATIONS[p0 + y0];
    int p10 = PERMUTATIONS[p1 + y0];
    int p01 = PERMUTATIONS[p0 + y1];
    int p11 = PERMUTATIONS[p1 + y1];
    float xm1 = x - 1f, ym1 = y - 1f, zm1 = z - 1f;
    float s = ease(x), t = ease(y);
    return FloatMath.lerp(
      FloatMath.lerp(
        FloatMath.lerp(
          grad(PERMUTATIONS[p00 + z0], x, y, z),
          grad(PERMUTATIONS[p10 + z0], xm1, y, z), s),
        FloatMath.lerp(
          grad(PERMUTATIONS[p01 + z0], x, ym1, z),
          grad(PERMUTATIONS[p11 + z0], xm1, ym1, z), s), t),
      FloatMath.lerp(
        FloatMath.lerp(
          grad(PERMUTATIONS[p00 + z1], x, y, zm1),
          grad(PERMUTATIONS[p10 + z1], xm1, y, zm1), s),
        FloatMath.lerp(
          grad(PERMUTATIONS[p01 + z1], x, ym1, zm1),
          grad(PERMUTATIONS[p11 + z1], xm1, ym1, zm1), s), t),
      ease(z));
  }

  /**
   * Returns the dot product of the provided value with the hashed gradient.
   */
  protected static float grad (int hash, float x)
  {
    // TODO: would a lookup table or the bit tests in Perlin's reference implementation
    // be more efficient?
    switch (hash & GRADIENT_MASK) {
      case 0: case 2: case 4: case 6: case 12: return x;
      case 1: case 3: case 5: case 7: case 13: return -x;
      default: return 0f;
    }
  }

  /**
   * Returns the dot product of the provided values with the hashed gradient.
   */
  protected static float grad (int hash, float x, float y)
  {
    switch (hash & GRADIENT_MASK) {
      case 0: case 12: return x + y;
      case 1: case 13: return y - x;
      case 2: return x - y;
      case 3: return -x - y;
      case 4: case 6: return x;
      case 5: case 7: return -x;
      case 8: case 10: return y;
      case 9: case 11: case 14: case 15: return -y;
      default: return 0f; // unreachable
    }
  }

  /**
   * Returns the dot product of the provided values with the hashed gradient.
   */
  protected static float grad (int hash, float x, float y, float z)
  {
    switch (hash & GRADIENT_MASK) {
      case 0: case 12: return x + y;
      case 1: case 13: return y - x;
      case 2: return x - y;
      case 3: return -x - y;
      case 4: return x + z;
      case 5: return z - x;
      case 6: return x - z;
      case 7: return -x - z;
      case 8: return y + z;
      case 9: case 14: return z - y;
      case 10: return y - z;
      case 11: case 15: return -y - z;
      default: return 0f; // unreachable
    }
  }

  /**
   * Computes the ease parameter for the given linear value.
   */
  protected static float ease (float t)
  {
    return t*t*t*(t*(6f*t - 15f) + 10f);
  }

  /** The number of permutation bits. */
  protected static final int PERMUTATION_BITS = 8;

  /** The number of permutations stored. */
  protected static final int PERMUTATION_COUNT = 1 << PERMUTATION_BITS;

  /** Mask for permutation table entries. */
  protected static final int PERMUTATION_MASK = PERMUTATION_COUNT - 1;

  /** Mask for gradient values. */
  protected static final int GRADIENT_MASK = 15;

  /** Permutation table (doubled so that we can offset values without having to remask). */
  protected static final int[] PERMUTATIONS = new int[PERMUTATION_COUNT * 2];
  static {
    for (int ii = 0; ii < PERMUTATION_COUNT; ii++) {
      PERMUTATIONS[ii] = ii;
    }
    ArrayUtil.shuffle(PERMUTATIONS, 0, PERMUTATION_COUNT);
    System.arraycopy(PERMUTATIONS, 0, PERMUTATIONS, PERMUTATION_COUNT, PERMUTATION_COUNT);
  }
}
