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

package com.threerings.tudey.shape.config;

import com.threerings.editor.Editable;
import com.threerings.expr.Scope;

import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.shape.ShapeModel;

/**
 * A model implementation that depicts a 2D shape.
 */
public class ShapeModelConfig extends ModelConfig.Implementation
{
  /** Whether or not to render the shape as an outline. */
  @Editable
  public boolean outline;

  /** The shape to depict. */
  @Editable
  public ShapeConfig shape = new ShapeConfig.Point();

  @Override
  public void preload (GlContext ctx)
  {
    // Do nothing
  }

  @Override
  public Model.Implementation getModelImplementation (
    GlContext ctx, Scope scope, Model.Implementation impl)
  {
    if (impl instanceof ShapeModel) {
      ((ShapeModel)impl).setConfig(ctx, this);
    } else {
      impl = new ShapeModel(ctx, scope, this);
    }
    return impl;
  }

  @Override
  public void invalidate ()
  {
    shape.invalidate();
  }
}
