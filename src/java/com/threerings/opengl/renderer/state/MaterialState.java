//
// $Id$

package com.threerings.opengl.renderer.state;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the material state.
 */
public class MaterialState extends RenderState
{
    /** The default material. */
    public static final MaterialState DEFAULT = new MaterialState(
        Color4f.DARK_GRAY, Color4f.GRAY, Color4f.BLACK, Color4f.BLACK, 0f);

    /** A simple white material. */
    public static final MaterialState WHITE = new MaterialState(
        Color4f.WHITE, Color4f.WHITE, Color4f.BLACK, Color4f.BLACK, 0f);

    /**
     * If there is a shared equivalent to the specified state, this method will return the shared
     * state; otherwise, it will simply return the parameter.
     */
    public static MaterialState getInstance (MaterialState state)
    {
        if (state.equals(DEFAULT)) {
            return DEFAULT;
        } else if (state.equals(WHITE)) {
            return WHITE;
        } else {
            return state;
        }
    }

    /**
     * Creates a new material state.
     */
    public MaterialState (
        Color4f ambient, Color4f diffuse, Color4f specular, Color4f emission, float shininess)
    {
        _ambient.set(ambient);
        _diffuse.set(diffuse);
        _specular.set(specular);
        _emission.set(emission);
        _shininess = shininess;
    }

    /**
     * Creates a new material state.
     */
    public MaterialState ()
    {
    }

    /**
     * Returns a reference to the material ambient color.
     */
    public Color4f getAmbient ()
    {
        return _ambient;
    }

    /**
     * Returns a reference to the material diffuse color.
     */
    public Color4f getDiffuse ()
    {
        return _diffuse;
    }

    /**
     * Returns a reference to the material specular color.
     */
    public Color4f getSpecular ()
    {
        return _specular;
    }

    /**
     * Returns a reference to the material emissive color.
     */
    public Color4f getEmission ()
    {
        return _emission;
    }

    /**
     * Returns the material shininess.
     */
    public float getShininess ()
    {
        return _shininess;
    }

    @Override // documentation inherited
    public int getType ()
    {
        return MATERIAL_STATE;
    }

    @Override // documentation inherited
    public void apply (Renderer renderer)
    {
        renderer.setMaterialState(_ambient, _diffuse, _specular, _emission, _shininess);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        MaterialState ostate;
        return other instanceof MaterialState &&
            _ambient.equals((ostate = (MaterialState)other)._ambient) &&
            _diffuse.equals(ostate._diffuse) && _specular.equals(ostate._specular) &&
            _emission.equals(ostate._emission) && _shininess == ostate._shininess;
    }

    /** The material ambient color. */
    protected Color4f _ambient = new Color4f();

    /** The material diffuse color. */
    protected Color4f _diffuse = new Color4f();

    /** The material specular color. */
    protected Color4f _specular = new Color4f(Color4f.BLACK);

    /** The material emissive color. */
    protected Color4f _emission = new Color4f(Color4f.BLACK);

    /** The material shininess. */
    protected float _shininess;
}
