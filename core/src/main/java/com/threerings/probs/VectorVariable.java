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

package com.threerings.probs;

import com.threerings.io.Streamable;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.math.FloatMath;
import com.threerings.math.Vector3f;

/**
 * A vector-valued random variable.
 */
@EditorTypes(value={
  VectorVariable.Constant.class,
  VectorVariable.Uniform.class }, label="distribution")
public abstract class VectorVariable extends DeepObject
  implements Exportable, Streamable
{
  /**
   * Always returns the same value.
   */
  public static class Constant extends VectorVariable
  {
    /** The value to return. */
    @Editable(step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
    public Vector3f value = new Vector3f();

    /**
     * Creates a constant variable from the parameters of the other variable.
     */
    public Constant (VectorVariable variable)
    {
      variable.getMean(value);
    }

    /**
     * No-arg constructor for deserialization, etc.
     */
    public Constant ()
    {
    }

    @Override
    public Vector3f getValue (Vector3f result)
    {
      return result.set(value);
    }

    @Override
    public Vector3f getMean (Vector3f result)
    {
      return result.set(value);
    }
  }

  /**
   * Returns a uniformly distributed value.
   */
  public static class Uniform extends VectorVariable
  {
    /** The minimum extent. */
    @Editable(hgroup="range", step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
    public Vector3f minimum = new Vector3f();

    /** The maximum extent. */
    @Editable(hgroup="range", step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
    public Vector3f maximum = new Vector3f();

    /**
     * Creates a uniform variable from the parameters of the other variable.
     */
    public Uniform (VectorVariable variable)
    {
      maximum.set(variable.getMean(minimum));
    }

    /**
     * No-arg constructor for deserialization, etc.
     */
    public Uniform ()
    {
    }

    @Override
    public Vector3f getValue (Vector3f result)
    {
      return result.set(
        FloatMath.random(minimum.x, maximum.x),
        FloatMath.random(minimum.y, maximum.y),
        FloatMath.random(minimum.z, maximum.z));
    }

    @Override
    public Vector3f getMean (Vector3f result)
    {
      return minimum.add(maximum, result).multLocal(0.5f);
    }
  }

  /**
   * Computes a sample value according to the variable's distribution.
   *
   * @return a reference to the result value, for chaining.
   */
  public abstract Vector3f getValue (Vector3f result);

  /**
   * Computes the mean value.
   *
   * @return a reference to the result value, for chaining.
   */
  public abstract Vector3f getMean (Vector3f result);
}
