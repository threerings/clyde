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
        @Editable(mode="normalized")
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
        @Editable
        public Vector3f position = new Vector3f();

        /** The constant attenutation. */
        @Editable(min=0)
        public float constantAttenuation = 1f;

        /** The linear attenuation. */
        @Editable(min=0)
        public float linearAttenuation;

        /** The quadratic attenutation. */
        @Editable(min=0)
        public float quadraticAttenuation;

        public Point (Point other)
        {
            super(other);
            position.set(other.position);
            constantAttenuation = other.constantAttenuation;
            linearAttenuation = other.linearAttenuation;
            quadraticAttenuation = other.quadraticAttenuation;
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
            light.constantAttenuation = constantAttenuation;
            light.linearAttenuation = linearAttenuation;
            light.quadraticAttenuation = quadraticAttenuation;
            return light;
        }
    }

    /**
     * A spot light.
     */
    public static class Spot extends Point
    {
        /** The spot direction. */
        @Editable(mode="normalized")
        public Vector3f direction = new Vector3f(0f, 0f, -1f);

        /** The spot exponent. */
        @Editable(min=0, max=128)
        public float exponent;

        /** The spot cutoff. */
        @Editable(min=0, max=90)
        public float cutoff;

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
            light.spotExponent = exponent;
            light.spotCutoff = cutoff;
            return light;
        }
    }

    /**
     * Returns the subclasses available for selection in the editor.
     */
    public static Class[] getEditorTypes ()
    {
        return new Class[] { Directional.class, Point.class, Spot.class };
    }

    /** The ambient light intensity. */
    @Editable
    public Color4f ambient = new Color4f(0f, 0f, 0f, 1f);

    /** The diffuse light intensity. */
    @Editable
    public Color4f diffuse = new Color4f(1f, 1f, 1f, 1f);

    /** The specular light intensity. */
    @Editable
    public Color4f specular = new Color4f(1f, 1f, 1f, 1f);

    public LightConfig (LightConfig other)
    {
        ambient.set(other.ambient);
        diffuse.set(other.diffuse);
        specular.set(other.specular);
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
        light.ambient.set(ambient);
        light.diffuse.set(diffuse);
        light.specular.set(specular);
        return light;
    }
}
