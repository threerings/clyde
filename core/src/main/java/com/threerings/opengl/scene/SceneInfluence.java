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

package com.threerings.opengl.scene;

import com.samskivert.util.Tuple;

import com.threerings.math.Box;
import com.threerings.util.ShallowObject;

import com.threerings.opengl.material.Projection;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.state.FogState;

/**
 * Base class for things that influence scene elements.
 */
public abstract class SceneInfluence extends ShallowObject
    implements SceneObject
{
    /**
     * Returns the ambient light color associated with this influence, or <code>null</code> for
     * none.
     */
    public Color4f getAmbientLight ()
    {
        return null;
    }

    /**
     * Returns the fog state associated with this influence, or <code>null</code> for none.
     */
    public FogState getFogState ()
    {
        return null;
    }

    /**
     * Returns the light associated with this influence, or <code>null</code> for none.
     */
    public Light getLight ()
    {
        return null;
    }

    /**
     * Returns the projection associated with this influence, or <code>null</code> for none.
     */
    public Projection getProjection ()
    {
        return null;
    }

    /**
     * Returns the definitions associated with this influence, or <code>null</code> for none.
     */
    public Tuple<String, Object>[] getDefinitions ()
    {
        return null;
    }

    /**
     * Resets the influence.
     */
    public void reset ()
    {
        // nothing by default
    }

    // documentation inherited from interface SceneObject
    public Box getBounds ()
    {
        return _bounds;
    }

    // documentation inherited from interface SceneObject
    public boolean updateLastVisit (int visit)
    {
        if (_lastVisit == visit) {
            return false;
        }
        _lastVisit = visit;
        return true;
    }

    /** The bounds of the influence. */
    protected Box _bounds = new Box();

    /** The visitation id of the last visit. */
    protected int _lastVisit;
}
