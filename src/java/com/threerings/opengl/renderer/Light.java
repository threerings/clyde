//
// $Id$

package com.threerings.opengl.renderer;

import com.threerings.math.Vector3f;
import com.threerings.math.Vector4f;

/**
 * Represents the state of a single light.
 */
public class Light
{
    /** The basic light types. */
    public enum Type { DIRECTIONAL, POINT, SPOT };

    /** The ambient light intensity. */
    public Color4f ambient = new Color4f(0f, 0f, 0f, 1f);

    /** The diffuse light intensity. */
    public Color4f diffuse = new Color4f(1f, 1f, 1f, 1f);

    /** The specular light intensity. */
    public Color4f specular = new Color4f(1f, 1f, 1f, 1f);

    /** The position or direction of the light. */
    public Vector4f position = new Vector4f(0f, 0f, 1f, 0f);

    /** The light's spot direction. */
    public Vector3f spotDirection = new Vector3f(0f, 0f, -1f);

    /** The light's spot exponent. */
    public float spotExponent;

    /** The light's spot cutoff. */
    public float spotCutoff = 180f;

    /** The light's constant attenuation. */
    public float constantAttenuation = 1f;

    /** The light's linear attenuation. */
    public float linearAttenuation;

    /** The light's quadratic attenuation. */
    public float quadraticAttenuation;

    /** Set when the light has changed and must be reapplied. */
    public boolean dirty;

    /** A hint as to whether or not this light should cast shadows. */
    public boolean castsShadows;

    /**
     * Determines whether this light is "compatible" with the specified other.  Lights are
     * compatible if they have the same type (directional, point, spot) and hints.
     */
    public boolean isCompatible (Light olight)
    {
        return getType() == olight.getType() && castsShadows == olight.castsShadows;
    }

    /**
     * Returns the basic type of this light.
     */
    public Type getType ()
    {
        return (position.w == 0f) ? Type.DIRECTIONAL :
            (spotCutoff == 180f ? Type.POINT : Type.SPOT);
    }
}
