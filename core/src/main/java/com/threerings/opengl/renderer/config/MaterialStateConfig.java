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

package com.threerings.opengl.renderer.config;

import java.lang.ref.SoftReference;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.MaterialState;

/**
 * Configurable material state.
 */
@EditorTypes({ MaterialStateConfig.OneSided.class, MaterialStateConfig.TwoSided.class })
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

        protected final int _constant;
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

        protected final int _constant;
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
            ambient.set(other.front.ambient);
            diffuse.set(other.front.diffuse);
            specular.set(other.front.specular);
            emission.set(other.front.emission);
            shininess = other.front.shininess;
        }

        public OneSided ()
        {
        }

        @Override
        protected MaterialState createInstance ()
        {
            return new MaterialState(
                ambient, diffuse, specular, emission, shininess, colorMaterialMode.getConstant(),
                localViewer, separateSpecular && GLContext.getCapabilities().OpenGL12,
                flatShading);
        }
    }

    /**
     * A two-sided material configuration.
     */
    public static class TwoSided extends MaterialStateConfig
    {
        /** The front side. */
        @Editable(hgroup="s")
        public Side front = new Side();

        /** The back side. */
        @Editable(hgroup="s")
        public Side back = new Side();

        /** The color material face. */
        @Editable(weight=1, hgroup="cm")
        public ColorMaterialFace colorMaterialFace = ColorMaterialFace.FRONT_AND_BACK;

        public TwoSided (OneSided other)
        {
            front.set(other);
            back.set(other);
        }

        public TwoSided ()
        {
        }

        @Override
        protected MaterialState createInstance ()
        {
            return new MaterialState(
                front.ambient, front.diffuse, front.specular, front.emission, front.shininess,
                back.ambient, back.diffuse, back.specular, back.emission, back.shininess,
                colorMaterialMode.getConstant(), colorMaterialFace.getConstant(), localViewer,
                separateSpecular && GLContext.getCapabilities().OpenGL12, flatShading);
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
    public ColorMaterialMode colorMaterialMode = ColorMaterialMode.AMBIENT_AND_DIFFUSE;

    /** The local viewer flag. */
    @Editable(hgroup="m3", weight=2)
    public boolean localViewer;

    /** The separate specular flag. */
    @Editable(hgroup="m3", weight=2)
    public boolean separateSpecular;

    /** The flat shading flag. */
    @Editable(hgroup="m3", weight=2)
    public boolean flatShading;

    /** If true, do not use a shared instance. */
    @Editable(weight=2)
    public boolean uniqueInstance;

    /**
     * Determines whether this state is supported by the hardware.
     */
    public boolean isSupported (boolean fallback)
    {
        return !separateSpecular || GLContext.getCapabilities().OpenGL12 || fallback;
    }

    /**
     * Returns the corresponding material state.
     */
    public MaterialState getState ()
    {
        if (uniqueInstance) {
            return createInstance();
        }
        MaterialState instance = (_instance == null) ? null : _instance.get();
        if (instance == null) {
            _instance = new SoftReference<MaterialState>(
                instance = MaterialState.getInstance(createInstance()));
        }
        return instance;
    }

    /**
     * Invalidates the config's cached data.
     */
    public void invalidate ()
    {
        _instance = null;
    }

    /**
     * Creates a material state instance corresponding to this config.
     */
    protected abstract MaterialState createInstance ();

    /** Cached state instance. */
    @DeepOmit
    protected transient SoftReference<MaterialState> _instance;
}
