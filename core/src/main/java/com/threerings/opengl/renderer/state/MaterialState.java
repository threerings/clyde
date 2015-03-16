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

package com.threerings.opengl.renderer.state;

import org.lwjgl.opengl.GL11;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the material state.
 */
public class MaterialState extends RenderState
{
    /** The default material. */
    public static final MaterialState DEFAULT = new MaterialState(
        Color4f.DARK_GRAY, Color4f.GRAY, Color4f.BLACK, Color4f.BLACK, 0f,
        Color4f.DARK_GRAY, Color4f.GRAY, Color4f.BLACK, Color4f.BLACK, 0f,
        -1, GL11.GL_FRONT, false, false, false, false);

    /** A simple white material. */
    public static final MaterialState WHITE = new MaterialState(
        Color4f.WHITE, Color4f.WHITE, Color4f.BLACK, Color4f.BLACK, 0f,
        Color4f.WHITE, Color4f.WHITE, Color4f.BLACK, Color4f.BLACK, 0f,
        -1, GL11.GL_FRONT, false, false, false, false);

    /**
     * If there is a shared instance with the supplied parameters, returns a reference to it;
     * otherwise, returns a new state with the parameters.
     */
    public static MaterialState getInstance (
        Color4f ambient, Color4f diffuse, Color4f specular, Color4f emission, float shininess,
        int colorMaterialMode, boolean localViewer, boolean separateSpecular, boolean flatShading)
    {
        return getInstance(
            ambient, diffuse, specular, emission, shininess,
            ambient, diffuse, specular, emission, shininess,
            colorMaterialMode, GL11.GL_FRONT,
            false, localViewer, separateSpecular, flatShading);
    }

    /**
     * If there is a shared instance with the supplied parameters, returns a reference to it;
     * otherwise, returns a new state with the parameters.
     */
    public static MaterialState getInstance (
        Color4f frontAmbient, Color4f frontDiffuse, Color4f frontSpecular, Color4f frontEmission,
            float frontShininess,
        Color4f backAmbient, Color4f backDiffuse, Color4f backSpecular, Color4f backEmission,
            float backShininess,
        int colorMaterialMode, int colorMaterialFace,
        boolean localViewer, boolean separateSpecular, boolean flatShading)
    {
        return getInstance(
            frontAmbient, frontDiffuse, frontSpecular, frontEmission, frontShininess,
            backAmbient, backDiffuse, backSpecular, backEmission, backShininess,
            colorMaterialMode, colorMaterialFace,
            true, localViewer, separateSpecular, flatShading);
    }

    /**
     * If there is a shared instance with the supplied parameters, returns a reference to it;
     * otherwise, returns a new state with the parameters.
     */
    public static MaterialState getInstance (
        Color4f frontAmbient, Color4f frontDiffuse, Color4f frontSpecular, Color4f frontEmission,
            float frontShininess,
        Color4f backAmbient, Color4f backDiffuse, Color4f backSpecular, Color4f backEmission,
            float backShininess,
        int colorMaterialMode, int colorMaterialFace,
        boolean twoSide, boolean localViewer, boolean separateSpecular, boolean flatShading)
    {
        return getInstance(new MaterialState(
            frontAmbient, frontDiffuse, frontSpecular, frontEmission, frontShininess,
            backAmbient, backDiffuse, backSpecular, backEmission, backShininess,
            colorMaterialMode, colorMaterialFace,
            twoSide, localViewer, separateSpecular, flatShading));
    }

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
     * Creates a new one-sided material state.
     */
    public MaterialState (
        Color4f ambient, Color4f diffuse, Color4f specular, Color4f emission, float shininess,
        int colorMaterialMode, boolean localViewer, boolean separateSpecular, boolean flatShading)
    {
        this(ambient, diffuse, specular, emission, shininess,
            ambient, diffuse, specular, emission, shininess,
            colorMaterialMode, GL11.GL_FRONT,
            false, localViewer, separateSpecular, flatShading);
    }

    /**
     * Creates a new two-sided material state.
     */
    public MaterialState (
        Color4f frontAmbient, Color4f frontDiffuse, Color4f frontSpecular, Color4f frontEmission,
            float frontShininess,
        Color4f backAmbient, Color4f backDiffuse, Color4f backSpecular, Color4f backEmission,
            float backShininess,
        int colorMaterialMode, int colorMaterialFace,
        boolean localViewer, boolean separateSpecular, boolean flatShading)
    {
        this(frontAmbient, frontDiffuse, frontSpecular, frontEmission, frontShininess,
            backAmbient, backDiffuse, backSpecular, backEmission, backShininess,
            colorMaterialMode, colorMaterialFace,
            true, localViewer, separateSpecular, flatShading);
    }

    /**
     * Creates a new material state.
     */
    public MaterialState (
        Color4f frontAmbient, Color4f frontDiffuse, Color4f frontSpecular, Color4f frontEmission,
            float frontShininess,
        Color4f backAmbient, Color4f backDiffuse, Color4f backSpecular, Color4f backEmission,
            float backShininess,
        int colorMaterialMode, int colorMaterialFace,
        boolean twoSide, boolean localViewer, boolean separateSpecular, boolean flatShading)
    {
        _frontAmbient.set(frontAmbient);
        _frontDiffuse.set(frontDiffuse);
        _frontSpecular.set(frontSpecular);
        _frontEmission.set(frontEmission);
        _frontShininess = frontShininess;
        _backAmbient.set(backAmbient);
        _backDiffuse.set(backDiffuse);
        _backSpecular.set(backSpecular);
        _backEmission.set(backEmission);
        _backShininess = backShininess;
        _colorMaterialMode = colorMaterialMode;
        _colorMaterialFace = colorMaterialFace;
        _twoSide = twoSide;
        _localViewer = localViewer;
        _separateSpecular = separateSpecular;
        _flatShading = flatShading;
    }

    /**
     * Creates a new material state.
     */
    public MaterialState ()
    {
    }

    /**
     * Returns a reference to the front ambient color.
     */
    public Color4f getFrontAmbient ()
    {
        return _frontAmbient;
    }

    /**
     * Returns a reference to the front diffuse color.
     */
    public Color4f getFrontDiffuse ()
    {
        return _frontDiffuse;
    }

    /**
     * Returns a reference to the front specular color.
     */
    public Color4f getFrontSpecular ()
    {
        return _frontSpecular;
    }

    /**
     * Returns a reference to the front emissive color.
     */
    public Color4f getFrontEmission ()
    {
        return _frontEmission;
    }

    /**
     * Returns the front shininess.
     */
    public float getFrontShininess ()
    {
        return _frontShininess;
    }

    /**
     * Returns a reference to the back ambient color.
     */
    public Color4f getBackAmbient ()
    {
        return _backAmbient;
    }

    /**
     * Returns a reference to the back diffuse color.
     */
    public Color4f getBackDiffuse ()
    {
        return _backDiffuse;
    }

    /**
     * Returns a reference to the back specular color.
     */
    public Color4f getBackSpecular ()
    {
        return _backSpecular;
    }

    /**
     * Returns a reference to the back emissive color.
     */
    public Color4f getBackEmission ()
    {
        return _backEmission;
    }

    /**
     * Returns the back shininess.
     */
    public float getBackShininess ()
    {
        return _backShininess;
    }

    /**
     * Returns the color material mode (or -1 if disabled).
     */
    public int getColorMaterialMode ()
    {
        return _colorMaterialMode;
    }

    /**
     * Returns the color material face.
     */
    public int getColorMaterialFace ()
    {
        return _colorMaterialFace;
    }

    /**
     * Returns whether or not two sided lighting is enabled.
     */
    public boolean getTwoSide ()
    {
        return _twoSide;
    }

    /**
     * Returns whether or not local viewer mode is enabled.
     */
    public boolean getLocalViewer ()
    {
        return _localViewer;
    }

    /**
     * Returns whether or not separate specular is enabled.
     */
    public boolean getSeparateSpecular ()
    {
        return _separateSpecular;
    }

    /**
     * Returns whether or not flat shading is enabled.
     */
    public boolean getFlatShading ()
    {
        return _flatShading;
    }

    @Override
    public int getType ()
    {
        return MATERIAL_STATE;
    }

    @Override
    public void apply (Renderer renderer)
    {
        renderer.setMaterialState(
            _frontAmbient, _frontDiffuse, _frontSpecular, _frontEmission, _frontShininess,
            _backAmbient, _backDiffuse, _backSpecular, _backEmission, _backShininess,
            _colorMaterialMode, _colorMaterialFace,
            _twoSide, _localViewer, _separateSpecular, _flatShading);
    }

    @Override
    public boolean equals (Object other)
    {
        MaterialState ostate;
        return other instanceof MaterialState &&
            _frontAmbient.equals((ostate = (MaterialState)other)._frontAmbient) &&
            _frontDiffuse.equals(ostate._frontDiffuse) &&
            _frontSpecular.equals(ostate._frontSpecular) &&
            _frontEmission.equals(ostate._frontEmission) &&
            _frontShininess == ostate._frontShininess &&
            _backAmbient.equals(ostate._backAmbient) &&
            _backDiffuse.equals(ostate._backDiffuse) &&
            _backSpecular.equals(ostate._backSpecular) &&
            _backEmission.equals(ostate._backEmission) &&
            _backShininess == ostate._backShininess &&
            _colorMaterialMode == ostate._colorMaterialMode &&
            _colorMaterialFace == ostate._colorMaterialFace &&
            _twoSide == ostate._twoSide && _localViewer == ostate._localViewer &&
            _separateSpecular == ostate._separateSpecular && _flatShading == ostate._flatShading;
    }

    @Override
    public int hashCode ()
    {
        int result = _frontAmbient != null ? _frontAmbient.hashCode() : 0;
        result = 31 * result + (_frontDiffuse != null ? _frontDiffuse.hashCode() : 0);
        result = 31 * result + (_frontSpecular != null ? _frontSpecular.hashCode() : 0);
        result = 31 * result + (_frontEmission != null ? _frontEmission.hashCode() : 0);
        result = 31 * result + Float.floatToIntBits(_frontShininess);
        result = 31 * result + (_backAmbient != null ? _backAmbient.hashCode() : 0);
        result = 31 * result + (_backDiffuse != null ? _backDiffuse.hashCode() : 0);
        result = 31 * result + (_backSpecular != null ? _backSpecular.hashCode() : 0);
        result = 31 * result + (_backEmission != null ? _backEmission.hashCode() : 0);
        result = 31 * result + Float.floatToIntBits(_backShininess);
        result = 31 * result + _colorMaterialMode;
        result = 31 * result + _colorMaterialFace;
        result = 31 * result + (_twoSide ? 1 : 0);
        result = 31 * result + (_localViewer ? 1 : 0);
        result = 31 * result + (_separateSpecular ? 1 : 0);
        result = 31 * result + (_flatShading ? 1 : 0);
        return result;
    }

    /** The front ambient color. */
    protected Color4f _frontAmbient = new Color4f();

    /** The front diffuse color. */
    protected Color4f _frontDiffuse = new Color4f();

    /** The front specular color. */
    protected Color4f _frontSpecular = new Color4f(Color4f.BLACK);

    /** The front emissive color. */
    protected Color4f _frontEmission = new Color4f(Color4f.BLACK);

    /** The front shininess. */
    protected float _frontShininess;

    /** The back ambient color. */
    protected Color4f _backAmbient = new Color4f();

    /** The back diffuse color. */
    protected Color4f _backDiffuse = new Color4f();

    /** The back specular color. */
    protected Color4f _backSpecular = new Color4f(Color4f.BLACK);

    /** The back emissive color. */
    protected Color4f _backEmission = new Color4f(Color4f.BLACK);

    /** The back shininess. */
    protected float _backShininess;

    /** The color material mode (or -1 if disabled). */
    protected int _colorMaterialMode = -1;

    /** The color material face. */
    protected int _colorMaterialFace = GL11.GL_FRONT;

    /** Whether or not to use two-sided lighting. */
    protected boolean _twoSide;

    /** The local viewer flag. */
    protected boolean _localViewer;

    /** The separate specular flag. */
    protected boolean _separateSpecular;

    /** The flat shading flag. */
    protected boolean _flatShading;
}
