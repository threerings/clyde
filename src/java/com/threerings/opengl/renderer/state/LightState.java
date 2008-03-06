//
// $Id$

package com.threerings.opengl.renderer.state;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the light state.
 */
public class LightState extends RenderState
{
    /** A state that disables lighting (the default). */
    public static final LightState DISABLED = new LightState(null, Color4f.DARK_GRAY);

    /**
     * Creates a new light state.
     */
    public LightState (Light[] lights, Color4f globalAmbient)
    {
        _lights = lights;
        _globalAmbient.set(globalAmbient);
    }

    /**
     * Returns a reference to the array of lights.
     */
    public Light[] getLights ()
    {
        return _lights;
    }

    /**
     * Returns a reference to the global ambient intensity.
     */
    public Color4f getGlobalAmbient ()
    {
        return _globalAmbient;
    }

    @Override // documentation inherited
    public int getType ()
    {
        return LIGHT_STATE;
    }

    @Override // documentation inherited
    public void apply (Renderer renderer)
    {
        renderer.setLightState(_lights, _globalAmbient);
    }

    /** The states of the lights. */
    protected Light[] _lights;

    /** The global ambient intensity. */
    protected Color4f _globalAmbient = new Color4f();
}
