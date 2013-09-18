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

package com.threerings.opengl.renderer.state;

import com.threerings.math.Transform3D;

import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the modelview transform state.
 */
public class TransformState extends RenderState
{
    /** The identity transform. */
    public static final TransformState IDENTITY = new TransformState();

    /**
     * Creates a new transform state with the values in the supplied transform.
     */
    public TransformState (Transform3D modelview)
    {
        _modelview.set(modelview);
    }

    /**
     * Creates a new transform state with the specified transform type
     * ({@link Transform3D#GENERAL}, {@link Transform3D#AFFINE}, etc).
     */
    public TransformState (int type)
    {
        _modelview.setType(type);
    }

    /**
     * Creates a new transform state with an identity transform.
     */
    public TransformState ()
    {
    }

    /**
     * Returns a reference to the modelview transformation.
     */
    public Transform3D getModelview ()
    {
        return _modelview;
    }

    @Override
    public int getType ()
    {
        return TRANSFORM_STATE;
    }

    @Override
    public void apply (Renderer renderer)
    {
        renderer.setTransformState(_modelview);
    }

    /** The modelview transformation. */
    protected Transform3D _modelview = new Transform3D();
}
