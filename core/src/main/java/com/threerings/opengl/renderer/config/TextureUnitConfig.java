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

import java.util.List;

import com.threerings.util.DeepObject;

import com.threerings.math.Transform3D;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.expr.Updater;

import com.threerings.opengl.compositor.Dependency;
import com.threerings.opengl.renderer.TextureUnit;
import com.threerings.opengl.renderer.state.TextureState;
import com.threerings.opengl.util.GlContext;

/**
 * Contains the configuration of a single texture unit.
 */
public class TextureUnitConfig extends DeepObject
    implements Exportable
{
    /** A reference to the texture to bind to the unit. */
    @Editable(nullable=true)
    public ConfigReference<TextureConfig> texture;

    /** The texture environment. */
    @Editable
    public TextureEnvironmentConfig environment = new TextureEnvironmentConfig.Modulate();

    /** The texture coordinate set to use. */
    @Editable(min=0)
    public int coordSet;

    /** The texture coordinate generation function for the s coordinate. */
    @Editable(nullable=true)
    public TextureCoordGenConfig coordGenS;

    /** The texture coordinate generation function for the t coordinate. */
    @Editable(nullable=true)
    public TextureCoordGenConfig coordGenT;

    /** The texture coordinate generation function for the r coordinate. */
    @Editable(nullable=true)
    @EditorTypes({
        TextureCoordGenConfig.ObjectLinear.class, TextureCoordGenConfig.EyeLinear.class,
        TextureCoordGenConfig.NormalMap.class, TextureCoordGenConfig.ReflectionMap.class })
    public TextureCoordGenConfig coordGenR;

    /** The texture coordinate generation function for the q coordinate. */
    @Editable(nullable=true)
    @EditorTypes({
        TextureCoordGenConfig.ObjectLinear.class, TextureCoordGenConfig.EyeLinear.class })
    public TextureCoordGenConfig coordGenQ;

    /** The texture transform. */
    @Editable(step=0.01)
    public Transform3D transform = new Transform3D();

    /**
     * Checks whether the unit configuration is supported.
     */
    public boolean isSupported (GlContext ctx, boolean fallback)
    {
        if (!(environment.isSupported(fallback) &&
            (coordGenS == null || coordGenS.isSupported(fallback)) &&
            (coordGenT == null || coordGenT.isSupported(fallback)) &&
            (coordGenR == null || coordGenR.isSupported(fallback)) &&
            (coordGenQ == null || coordGenQ.isSupported(fallback)))) {
            return false;
        }
        TextureConfig config = getTextureConfig(ctx);
        return config == null || config.isSupported(ctx, fallback);
    }

    /**
     * Creates the texture unit corresponding to this configuration.
     */
    public TextureUnit createUnit (
        GlContext ctx, TextureState state, Scope scope,
        List<Dependency.Adder> adders, List<Updater> updaters)
    {
        TextureUnit unit = new TextureUnit();
        TextureConfig config = getTextureConfig(ctx);
        if (config != null) {
            unit.texture = config.getTexture(ctx, state, unit, scope, adders, updaters);
        }
        environment.configure(unit);
        if (coordGenS != null) {
            unit.genModeS = coordGenS.getModeAndPlane(unit.genPlaneS);
        }
        if (coordGenT != null) {
            unit.genModeT = coordGenT.getModeAndPlane(unit.genPlaneT);
        }
        if (coordGenR != null) {
            unit.genModeR = coordGenR.getModeAndPlane(unit.genPlaneR);
        }
        if (coordGenQ != null) {
            unit.genModeQ = coordGenQ.getModeAndPlane(unit.genPlaneQ);
        }
        unit.transform.set(transform);
        return unit;
    }

    /**
     * Returns the configuration of the unit texture.
     */
    protected TextureConfig getTextureConfig (GlContext ctx)
    {
        return (texture == null) ?
            null : ctx.getConfigManager().getConfig(TextureConfig.class, texture);
    }
}
