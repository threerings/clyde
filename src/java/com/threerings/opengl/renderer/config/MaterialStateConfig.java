//
// $Id$

package com.threerings.opengl.renderer.config;

import org.lwjgl.opengl.GL11;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.MaterialState;

/**
 * Configurable material state.
 */
public abstract class MaterialStateConfig extends DeepObject
    implements Exportable
{
    /** Color material mode constants. */
    public enum ColorMaterialMode
    {
        DISABLED(-1),
        AMBIENT(GL11.GL_AMBIENT),
        DIFFUSE(GL11.GL_DIFFUSE),
        AMBIENT_AND_DIFFUSE(GL11.GL_AMBIENT_AND_DIFFUSE),
        SPECULAR(GL11.GL_SPECULAR),
        EMISSION(GL11.GL_EMISSION);

        public int getConstant ()
        {
            return _constant;
        }

        ColorMaterialMode (int constant)
        {
            _constant = constant;
        }

        protected int _constant;
    }

    /** Color material face constants. */
    public enum ColorMaterialFace
    {
        FRONT(GL11.GL_FRONT),
        BACK(GL11.GL_BACK),
        FRONT_AND_BACK(GL11.GL_FRONT_AND_BACK);

        public int getConstant ()
        {
            return _constant;
        }

        ColorMaterialFace (int constant)
        {
            _constant = constant;
        }

        protected int _constant;
    }

    /**
     * A one-sided material configuration.
     */
    public static class OneSided extends MaterialStateConfig
    {
        /** The ambient reflectivity. */
        @Editable(mode="alpha", hgroup="m1")
        public Color4f ambient = new Color4f(Color4f.WHITE);

        /** The specular reflectivity. */
        @Editable(mode="alpha", hgroup="m1")
        public Color4f specular = new Color4f(Color4f.BLACK);

        /** The diffuse reflectivity. */
        @Editable(mode="alpha", hgroup="m2")
        public Color4f diffuse = new Color4f(Color4f.WHITE);

        /** The emissive color. */
        @Editable(mode="alpha", hgroup="m2")
        public Color4f emission = new Color4f(Color4f.BLACK);

        /** The specular exponent. */
        @Editable(min=0, max=128, step=0.1, hgroup="cm")
        public float shininess;

        public OneSided (TwoSided other)
        {
            super(other);
            ambient.set(other.front.ambient);
            diffuse.set(other.front.diffuse);
            specular.set(other.front.specular);
            emission.set(other.front.emission);
            shininess = other.front.shininess;
        }

        public OneSided ()
        {
        }

        @Override // documentation inherited
        public MaterialState getState ()
        {
            return MaterialState.getInstance(
                ambient, diffuse, specular, emission, shininess,
                colorMaterialMode.getConstant(), localViewer, separateSpecular, flatShading);
        }
    }

    /**
     * A two-sided material configuration.
     */
    public static class TwoSided extends MaterialStateConfig
    {
        /** The front side. */
        @Editable(nullable=false, hgroup="s")
        public Side front = new Side();

        /** The back side. */
        @Editable(nullable=false, hgroup="s")
        public Side back = new Side();

        /** The color material face. */
        @Editable(weight=1, hgroup="cm")
        public ColorMaterialFace colorMaterialFace = ColorMaterialFace.FRONT_AND_BACK;

        public TwoSided (OneSided other)
        {
            super(other);
            front.set(other);
            back.set(other);
        }

        public TwoSided ()
        {
        }

        @Override // documentation inherited
        public MaterialState getState ()
        {
            return MaterialState.getInstance(
                front.ambient, front.diffuse, front.specular, front.emission, front.shininess,
                back.ambient, back.diffuse, back.specular, back.emission, back.shininess,
                colorMaterialMode.getConstant(), colorMaterialFace.getConstant(),
                localViewer, separateSpecular, flatShading);
        }
    }

    /**
     * The parameters of one side.
     */
    public static class Side extends DeepObject
        implements Exportable
    {
        /** The ambient reflectivity. */
        @Editable(mode="alpha")
        public Color4f ambient = new Color4f(Color4f.WHITE);

        /** The diffuse reflectivity. */
        @Editable(mode="alpha")
        public Color4f diffuse = new Color4f(Color4f.WHITE);

        /** The specular reflectivity. */
        @Editable(mode="alpha")
        public Color4f specular = new Color4f(Color4f.BLACK);

        /** The emissive color. */
        @Editable(mode="alpha")
        public Color4f emission = new Color4f(Color4f.BLACK);

        /** The specular exponent. */
        @Editable(min=0, max=128, step=0.1)
        public float shininess;

        /**
         * Sets all of the parameters to those in the supplied config.
         */
        public void set (OneSided other)
        {
            ambient.set(other.ambient);
            diffuse.set(other.diffuse);
            specular.set(other.specular);
            emission.set(other.emission);
            shininess = other.shininess;
        }
    }

    /** The color material mode. */
    @Editable(weight=1, hgroup="cm")
    public ColorMaterialMode colorMaterialMode = ColorMaterialMode.DISABLED;

    /** The local viewer flag. */
    @Editable(hgroup="m3", weight=2)
    public boolean localViewer;

    /** The separate specular flag. */
    @Editable(hgroup="m3", weight=2)
    public boolean separateSpecular;

    /** The flat shading flag. */
    @Editable(hgroup="m3", weight=2)
    public boolean flatShading;

    /**
     * Returns the subclasses available for selection in the editor.
     */
    public static Class[] getEditorTypes ()
    {
        return new Class[] { OneSided.class, TwoSided.class };
    }

    public MaterialStateConfig (MaterialStateConfig other)
    {
        colorMaterialMode = other.colorMaterialMode;
        localViewer = other.localViewer;
        separateSpecular = other.separateSpecular;
        flatShading = other.flatShading;
    }

    public MaterialStateConfig ()
    {
    }

    /**
     * Returns the corresponding material state.
     */
    public abstract MaterialState getState ();
}
