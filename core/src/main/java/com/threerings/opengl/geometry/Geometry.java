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

package com.threerings.opengl.geometry;

import com.threerings.math.Matrix4f;
import com.threerings.math.Vector3f;

import com.threerings.opengl.renderer.SimpleBatch.DrawCommand;
import com.threerings.opengl.renderer.config.CoordSpace;
import com.threerings.opengl.renderer.state.ArrayState;

/**
 * Represents a geometry instance.
 */
public abstract class Geometry
{
    /** A "geometry" object that draws nothing. */
    public static final Geometry EMPTY = new SimpleGeometry() {
        protected void draw () {
            // no-op
        }
    };

    /**
     * Returns the geometry's bone matrices, if any.
     */
    public Matrix4f[] getBoneMatrices ()
    {
        return null;
    }

    /**
     * Returns the coordinate space in which the specified pass is given.
     */
    public CoordSpace getCoordSpace (int pass)
    {
        return CoordSpace.OBJECT;
    }

    /**
     * Returns a reference to the location of the center of the geometry (used for depth sorting).
     */
    public Vector3f getCenter ()
    {
        return Vector3f.ZERO;
    }

    /**
     * Returns the array state for the specified pass.
     */
    public abstract ArrayState getArrayState (int pass);

    /**
     * Returns the draw command for the specified pass.
     */
    public abstract DrawCommand getDrawCommand (int pass);

    /**
     * Checks whether this geometry requires a call to its {@link #update} method before rendering.
     */
    public boolean requiresUpdate ()
    {
        return false;
    }

    /**
     * Updates the state of the geometry.
     */
    public void update ()
    {
        // nothing by default
    }
}
