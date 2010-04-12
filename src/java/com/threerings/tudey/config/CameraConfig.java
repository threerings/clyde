//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

package com.threerings.tudey.config;

import com.threerings.io.Streamable;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.math.SphereCoords;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;

import com.threerings.opengl.camera.OrbitCameraHandler;

/**
 * Contains the parameters of the camera (field of view, coordinates, etc.) and allows for
 * interpolation.
 */
public class CameraConfig extends DeepObject
    implements Exportable, Streamable
{
    /** The camera's vertical field of view, in radians. */
    @Editable(min=0.0, max=180.0, scale=Math.PI/180.0, hgroup="f")
    public float fov = FloatMath.PI/3f;

    /** The distances to the camera's near and far clip planes. */
    @Editable(min=0.0, step=0.01, hgroup="f")
    public float near = 1f, far = 100f;

    /** The camera's coordinates about the target. */
    @Editable
    public SphereCoords coords = new SphereCoords(0f, FloatMath.PI/4f, 10f);

    /** The camera's offset from the target. */
    @Editable
    public Vector3f offset = new Vector3f(0f, 0f, 1f);

    /**
     * Creates a new config with the supplied values.
     */
    public CameraConfig (float fov, float near, float far, SphereCoords coords, Vector3f offset)
    {
        this.fov = fov;
        this.near = near;
        this.far = far;
        this.coords.set(coords);
        this.offset.set(offset);
    }

    /**
     * Creates a new config with default values.
     */
    public CameraConfig ()
    {
    }

    /**
     * Applies this config (minus the offset) to the specified camera handler.
     */
    public void apply (OrbitCameraHandler camhand)
    {
        camhand.setPerspective(fov, near, far);
        camhand.getCoords().set(coords);
    }
}
