//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

package com.threerings.opengl.camera;

import com.threerings.math.FloatMath;
import com.threerings.math.Quaternion;
import com.threerings.math.Vector3f;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.util.Rectangle;

/**
 * Controls the camera parameters.
 */
public abstract class CameraHandler
    implements Renderer.Observer
{
    /**
     * Creates a new camera handler for the compositor camera.
     */
    public CameraHandler (GlContext ctx)
    {
        this(ctx, ctx.getCompositor().getCamera(), true);
    }

    /**
     * Creates a new camera handler for the specified camera.
     *
     * @param matchRenderSurface if true, automatically adjust the camera viewport to match the
     * dimensions of the renderer surface.
     */
    public CameraHandler (GlContext ctx, Camera camera, boolean matchRenderSurface)
    {
        _ctx = ctx;
        _camera = camera;
        _matchRenderSurface = matchRenderSurface;
    }

    /**
     * Sets the camera's perspective parameters.
     */
    public void setPerspective (float fovy, float near, float far)
    {
        _fovy = fovy;
        _near = near;
        _far = far;
        updatePerspective();
    }

    /**
     * Returns the camera's field of view in radians.
     */
    public float getFieldOfView ()
    {
        return _fovy;
    }

    /**
     * Returns the distance to the near clip plane.
     */
    public float getNear ()
    {
        return _near;
    }

    /**
     * Returns the distance to the far clip plane.
     */
    public float getFar ()
    {
        return _far;
    }

    /**
     * Returns a reference to the translation to use for the sound listener.
     */
    public abstract Vector3f getListenerTranslation ();

    /**
     * Returns a reference to the rotation to use for the sound listener.
     */
    public abstract Quaternion getListenerRotation ();

    /**
     * Determines whether the camera handler is currently active.
     */
    public boolean isAdded ()
    {
        return _ctx.getCameraHandler() == this;
    }

    /**
     * Notifies the handler that it has been added.
     */
    public void wasAdded ()
    {
        if (_matchRenderSurface) {
            Renderer renderer = _ctx.getRenderer();
            sizeChanged(renderer.getWidth(), renderer.getHeight());
            renderer.addObserver(this);
        }
    }

    /**
     * Notifies the handler that it has been removed.
     */
    public void wasRemoved ()
    {
        if (_matchRenderSurface) {
            _ctx.getRenderer().removeObserver(this);
        }
    }

    /**
     * Updates the camera perspective parameters.
     */
    public void updatePerspective ()
    {
        Rectangle viewport = _camera.getViewport();
        _camera.setPerspective(_fovy, (float)viewport.width / viewport.height, _near, _far);
    }

    /**
     * Updates the camera position.
     */
    public abstract void updatePosition ();

    // documentation inherited from interface Renderer.Observer
    public void sizeChanged (int width, int height)
    {
        _camera.getViewport().set(0, 0, width, height);
        updatePerspective();
    }

    /** The renderer context. */
    protected GlContext _ctx;

    /** The camera that we're handling. */
    protected Camera _camera;

    /** Whether or not we're to match the size of the render surface. */
    protected boolean _matchRenderSurface;

    /** The vertical field of view (in radians). */
    protected float _fovy = FloatMath.PI / 3f;

    /** The distance to the near clip plane. */
    protected float _near = 1f;

    /** The distance to the far clip plane. */
    protected float _far = 100f;
}
