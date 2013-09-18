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

package com.threerings.tudey.client.util;

import org.lwjgl.opengl.GL11;

import com.threerings.math.Box;
import com.threerings.math.Ray3D;
import com.threerings.math.Triangle;
import com.threerings.math.Vector3f;

import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.AlphaState;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.scene.SimpleSceneElement;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.data.TudeySceneModel.Vertex;

/**
 * Displays a solid area.
 */
public class AreaElement extends SimpleSceneElement
{
    /**
     * Creates a new area element.
     */
    public AreaElement (GlContext ctx)
    {
        super(ctx, RenderQueue.TRANSPARENT);
    }

    /**
     * Sets the vertices of the area.
     */
    public void setVertices (Vertex[] vertices)
    {
        _vertices = vertices;
        updateBounds();
    }

    /**
     * Returns a reference to the outline color.
     */
    public Color4f getColor ()
    {
        ColorState cstate = (ColorState)_batch.getStates()[RenderState.COLOR_STATE];
        return cstate.getColor();
    }

    /**
     * Update the visibility.
     */
    public void setVisible (boolean visible)
    {
        _visible = visible;
    }

    @Override
    public boolean getIntersection (Ray3D ray, Vector3f result)
    {
        // make sure the ray intersects the bounds
        if (!_bounds.intersects(ray)) {
            return false;
        }
        // transform into model space and check against both sides of the triangles
        // (transforming back if we get a hit)
        ray = ray.transform(_transform.invert());
        Vertex v0 = _vertices[0];
        _triangle.getFirstVertex().set(v0.x, v0.y, v0.z);
        for (int ii = 2; ii < _vertices.length; ii++) {
            Vertex v1 = _vertices[ii - 1], v2 = _vertices[ii];
            _triangle.getSecondVertex().set(v1.x, v1.y, v1.z);
            _triangle.getThirdVertex().set(v2.x, v2.y, v2.z);
            if (_triangle.getIntersection(ray, result) ||
                    _triangle.flipLocal().getIntersection(ray, result)) {
                _transform.transformPointLocal(result);
                return true;
            }
        }
        return false;
    }

    @Override
    protected RenderState[] createStates ()
    {
        RenderState[] states = super.createStates();
        states[RenderState.ALPHA_STATE] = AlphaState.PREMULTIPLIED;
        states[RenderState.COLOR_STATE] = new ColorState();
        states[RenderState.DEPTH_STATE] = DepthState.TEST;
        return states;
    }

    @Override
    protected void computeBounds (Box result)
    {
        result.setToEmpty();
        if (_vertices == null) {
            return;
        }
        for (Vertex vertex : _vertices) {
            result.addLocal(new Vector3f(vertex.x, vertex.y, vertex.z));
        }
        result.getCenter(_center);
        result.transformLocal(_transform);
    }

    @Override
    protected void draw ()
    {
        if (_vertices == null || !_visible) {
            return;
        }
        GL11.glBegin(GL11.GL_POLYGON);
        for (Vertex vertex : _vertices) {
            GL11.glVertex3f(vertex.x, vertex.y, vertex.z);
        }
        GL11.glEnd();
    }

    @Override
    protected Vector3f getCenter ()
    {
        return _center;
    }

    /** The vertices of the area. */
    protected Vertex[] _vertices;

    /** The model space center. */
    protected Vector3f _center = new Vector3f();

    /** Triangle used for intersection testing. */
    protected Triangle _triangle = new Triangle();

    /** Are we visible? */
    protected boolean _visible = true;
}
