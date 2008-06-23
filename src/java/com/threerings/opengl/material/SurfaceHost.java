//
// $Id$

package com.threerings.opengl.material;

import com.threerings.math.Transform3D;

import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.renderer.state.MaterialState;
import com.threerings.opengl.renderer.state.TransformState;

/**
 * Implemented by objects that contain surfaces to provide access to shared parameters.
 */
public interface SurfaceHost
{
    /**
     * Returns a reference to the modelview transform.
     */
    public Transform3D getModelview ();

    /**
     * Returns a reference to the shared color state.
     */
    public ColorState getColorState ();

    /**
     * Returns a reference to the shared fog state.
     */
    public FogState getFogState ();

    /**
     * Returns a reference to the shared light state.
     */
    public LightState getLightState ();

    /**
     * Returns a reference to the shared material state.
     */
    public MaterialState getMaterialState ();

    /**
     * Returns a reference to the shared transform state.
     */
    public TransformState getTransformState ();
}
