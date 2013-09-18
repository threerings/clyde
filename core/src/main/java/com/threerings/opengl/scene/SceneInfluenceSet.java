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
import java.util.Map;

import com.google.common.collect.Maps;

import com.samskivert.util.Tuple;

import com.threerings.math.Box;
import com.threerings.util.AbstractIdentityHashSet;

import com.threerings.opengl.material.Projection;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;

/**
 * A set of scene influences.
 */
public class SceneInfluenceSet extends AbstractIdentityHashSet<SceneInfluence>
{
    /**
     * Returns the fog state for this influence set.
     *
     * @param bounds the bounds used to resolve conflicts.
     * @param state an existing state to reuse, if possible.
     */
    public FogState getFogState (Box bounds, FogState state)
    {
        FogState closestState = null;
        float cdist = Float.MAX_VALUE;
        for (SceneInfluence influence : this) {
            state = influence.getFogState();
            if (state != null) {
                float distance = influence.getBounds().getExtentDistance(bounds);
                if (closestState == null || distance < cdist) {
                    closestState = state;
                    cdist = distance;
                }
            }
        }
        return (closestState == null) ? FogState.DISABLED : closestState;
    }

    /**
     * Returns the light state for this influence set.
     *
     * @param bounds the bounds used to resolve conflicts.
     * @param state an existing state to reuse, if possible.
     */
    public LightState getLightState (Box bounds, LightState state)
    {
        Color4f closestAmbient = null;
        float cdist = Float.MAX_VALUE;
        ArrayList<Light> lights = new ArrayList<Light>();
        for (SceneInfluence influence : this) {
            Color4f ambient = influence.getAmbientLight();
            if (ambient != null) {
                float distance = influence.getBounds().getExtentDistance(bounds);
                if (closestAmbient == null || distance <= cdist) {
                    closestAmbient = ambient;
                    cdist = distance;
                }
            }
            Light light = influence.getLight();
            if (light != null && lights.size() < MAX_LIGHTS) {
                lights.add(light);
            }
        }
        if (closestAmbient == null) {
            return LightState.DISABLED;
        }
        if (canReuse(state, lights)) {
            Light[] olights = state.getLights();
            for (int ii = 0; ii < olights.length; ii++) {
                olights[ii] = lights.get(ii);
            }
        } else {
            state = new LightState(lights.toArray(new Light[lights.size()]), Color4f.WHITE);
        }
        state.setGlobalAmbient(closestAmbient);
        return state;
    }

    /**
     * Returns the set of projections for this influence set.
     *
     * @param projections an existing array to reuse, if possible.
     */
    public Projection[] getProjections (Projection[] projections)
    {
        ArrayList<Projection> projs = new ArrayList<Projection>();
        for (SceneInfluence influence : this) {
            Projection projection = influence.getProjection();
            if (projection != null) {
                projs.add(projection);
            }
        }
        int size = projs.size();
        if (size == 0) {
            return NO_PROJECTIONS;
        }
        if (canReuse(projections, projs)) {
            return projections;
        }
        return projs.toArray(new Projection[size]);
    }

    /**
     * Returns the set of definitions for this influence set.
     *
     * @param definitions an existing map to reuse, if possible.
     */
    public Map<String, Object> getDefinitions (Map<String, Object> definitions)
    {
        Map<String, Object> defs = Maps.newHashMap();
        for (SceneInfluence influence : this) {
            Tuple<String, Object>[] idefs = influence.getDefinitions();
            if (idefs != null) {
                for (Tuple<String, Object> def : idefs) {
                    defs.put(def.left, def.right);
                }
            }
        }
        if (defs.isEmpty()) {
            return null;
        }
        return canReuse(definitions, defs) ? definitions : defs;
    }

    /**
     * Determines whether we can reuse the specified state, given the provided new list of
     * lights.
     */
    protected boolean canReuse (LightState state, ArrayList<Light> nlights)
    {
        if (state == null) {
            return false;
        }
        Light[] olights = state.getLights();
        if (olights == null || olights.length != nlights.size()) {
            return false;
        }
        for (int ii = 0; ii < olights.length; ii++) {
            if (!olights[ii].isCompatible(nlights.get(ii))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether we can reuse the specified projections.
     */
    protected boolean canReuse (Projection[] projections, ArrayList<Projection> nprojs)
    {
        if (projections == null || projections.length != nprojs.size()) {
            return false;
        }
        for (Projection proj : projections) {
            if (!nprojs.contains(proj)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether we can reuse the specified definitions.
     */
    protected boolean canReuse (Map<String, Object> definitions, Map<String, Object> ndefs)
    {
        if (definitions == null || definitions.size() != ndefs.size()) {
            return false;
        }
        for (Map.Entry<String, Object> entry : definitions.entrySet()) {
            if (ndefs.get(entry.getKey()) != entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    /** The maximum number of lights we allow in a set. */
    protected static final int MAX_LIGHTS = 4;

    /** Reusable empty projections array. */
    protected static final Projection[] NO_PROJECTIONS = new Projection[0];
}
