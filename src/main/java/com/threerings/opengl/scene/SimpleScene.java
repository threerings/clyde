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

import java.util.ArrayList;
import java.util.Collection;

import com.google.common.base.Predicate;

import com.threerings.math.Box;
import com.threerings.math.Ray3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.util.GlContext;

/**
 * A simple, "flat" scene implementation.
 */
public class SimpleScene extends Scene
{
    /**
     * Creates a new simple scene.
     */
    public SimpleScene (GlContext ctx)
    {
        super(ctx);
    }

    /**
     * Creates a new simple scene.
     *
     * @param sources the number of simultaneous sound sources to allow.
     */
    public SimpleScene (GlContext ctx, int sources)
    {
        super(ctx, sources);
    }

    // documentation inherited from interface Compositable
    public void composite ()
    {
        composite(_elements, _ctx.getCompositor().getCamera().getWorldVolume());
    }

    @Override
    public SceneElement getIntersection (
        Ray3D ray, Vector3f location, Predicate<? super SceneElement> filter)
    {
        return getIntersection(_elements, ray, location, filter);
    }

    @Override
    public void getElements (Box bounds, Collection<SceneElement> results)
    {
        getIntersecting(_elements, bounds, results);
    }

    @Override
    public void getInfluences (Box bounds, Collection<SceneInfluence> results)
    {
        getIntersecting(_influences, bounds, results);
    }

    @Override
    public void getEffects (Box bounds, Collection<ViewerEffect> results)
    {
        getIntersecting(_effects, bounds, results);
    }

    @Override
    protected void addToSpatial (SceneElement element)
    {
        _elements.add(element);
    }

    @Override
    protected void removeFromSpatial (SceneElement element)
    {
        _elements.remove(element);
    }

    @Override
    protected void addToSpatial (SceneInfluence influence)
    {
        _influences.add(influence);
    }

    @Override
    protected void removeFromSpatial (SceneInfluence influence)
    {
        _influences.remove(influence);
    }

    @Override
    protected void addToSpatial (ViewerEffect effect)
    {
        _effects.add(effect);
    }

    @Override
    protected void removeFromSpatial (ViewerEffect effect)
    {
        _effects.remove(effect);
    }

    /** The list of all scene elements. */
    protected ArrayList<SceneElement> _elements = new ArrayList<SceneElement>();

    /** The list of all scene influences. */
    protected ArrayList<SceneInfluence> _influences = new ArrayList<SceneInfluence>();

    /** The list of all viewer effects. */
    protected ArrayList<ViewerEffect> _effects = new ArrayList<ViewerEffect>();
}
