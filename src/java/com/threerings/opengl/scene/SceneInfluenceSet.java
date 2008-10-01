//
// $Id$

package com.threerings.opengl.scene;

import java.util.ArrayList;
import java.util.HashSet;

import com.threerings.math.Box;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;

/**
 * A set of scene influences.
 */
public class SceneInfluenceSet extends HashSet<SceneInfluence>
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
            state.getGlobalAmbient().set(closestAmbient);
            return state;
        }
        return new LightState(lights.toArray(new Light[lights.size()]), closestAmbient);
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

    /** The maximum number of lights we allow in a set. */
    protected static final int MAX_LIGHTS = 3;
}
