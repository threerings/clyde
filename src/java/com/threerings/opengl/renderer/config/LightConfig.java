//
// $Id$

package com.threerings.opengl.renderer.config;

import com.threerings.util.DeepObject;

import com.threerings.math.Vector3f;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Light;

/**
 * Represents the state of a single light.
 */
public abstract class LightConfig extends DeepObject
    implements Exportable
{
    /**
     * A directional light.
     */
    public static class Directional extends LightConfig
    {
        /** The direction of the light. */
        @Editable(mode="normalized", hgroup="p")
        public Vector3f direction = new Vector3f(0f, 0f, 1f);

        public Directional (LightConfig other)
        {
            super(other);
        }

        public Directional ()
        {
        }

        @Override // documentation inherited
        public Light createLight ()
        {
            Light light = super.createLight();
            light.position.set(direction.x, direction.y, direction.z, 0f);
            return light;
        }
    }

    /**
     * A point light.
     */
    public static class Point extends LightConfig
    {
        /** The location of the light. */
        @Editable(hgroup="p")
        public Vector3f position = new Vector3f();

        /** The light's attenuation parameters. */
        @Editable(hgroup="p")
        public Attenuation attenuation = new Attenuation();

        public Point (Point other)
        {
            super(other);
            position.set(other.position);
            attenuation.constant = other.attenuation.constant;
            attenuation.linear = other.attenuation.linear;
            attenuation.quadratic = other.attenuation.quadratic;
        }

        public Point (LightConfig other)
        {
            super(other);
        }

        public Point ()
        {
        }

        @Override // documentation inherited
        public Light createLight ()
        {
            Light light = super.createLight();
            light.position.set(position.x, position.y, position.z, 0f);
            light.constantAttenuation = attenuation.constant;
            light.linearAttenuation = attenuation.linear;
            light.quadraticAttenuation = attenuation.quadratic;
            return light;
        }
    }

    /**
     * A spot light.
     */
    public static class Spot extends Point
    {
        /** The spot direction. */
        @Editable(mode="normalized", hgroup="d")
        public Vector3f direction = new Vector3f(0f, 0f, -1f);

        /** The falloff parameters. */
        @Editable(hgroup="d")
        public Falloff falloff = new Falloff();

        public Spot (Point other)
        {
            super(other);
        }

        public Spot (LightConfig other)
        {
            super(other);
        }

        public Spot ()
        {
        }

        @Override // documentation inherited
        public Light createLight ()
        {
            Light light = super.createLight();
            light.spotDirection.set(direction);
            light.spotExponent = falloff.exponent;
            light.spotCutoff = falloff.cutoff;
            return light;
        }
    }

    /**
     * Represents the colors of the light.
     */
    public static class Colors extends DeepObject
        implements Exportable
    {
        /** The ambient light color. */
        @Editable
        public Color4f ambient = new Color4f(0f, 0f, 0f, 1f);

        /** The diffuse light color. */
        @Editable
        public Color4f diffuse = new Color4f(1f, 1f, 1f, 1f);

        /** The specular light color. */
        @Editable
        public Color4f specular = new Color4f(1f, 1f, 1f, 1f);
    }

    /**
     * Represents the light's attenuation coefficients.
     */
    public static class Attenuation extends DeepObject
        implements Exportable
    {
        /** The constant attenutation. */
        @Editable(min=0, step=0.01)
        public float constant = 1f;

        /** The linear attenuation. */
        @Editable(min=0, step=0.01)
        public float linear;

        /** The quadratic attenutation. */
        @Editable(min=0, step=0.01)
        public float quadratic;
    }

    /**
     * Represents the spot light falloff.
     */
    public static class Falloff extends DeepObject
        implements Exportable
    {
        /** The falloff exponent. */
        @Editable(min=0, max=128)
        public float exponent;

        /** The falloff cutoff. */
        @Editable(min=0, max=90)
        public float cutoff;
    }

    /**
     * Returns the subclasses available for selection in the editor.
     */
    public static Class[] getEditorTypes ()
    {
        return new Class[] { Directional.class, Point.class, Spot.class };
    }

    /** The color of the light. */
    @Editable(hgroup="p")
    public Colors colors = new Colors();

    public LightConfig (LightConfig other)
    {
        colors.ambient.set(other.colors.ambient);
        colors.diffuse.set(other.colors.diffuse);
        colors.specular.set(other.colors.specular);
    }

    public LightConfig ()
    {
    }

    /**
     * Creates a light object corresponding to this configuration.
     */
    public Light createLight ()
    {
        Light light = new Light();
        light.ambient.set(colors.ambient);
        light.diffuse.set(colors.diffuse);
        light.specular.set(colors.specular);
        return light;
    }
}
