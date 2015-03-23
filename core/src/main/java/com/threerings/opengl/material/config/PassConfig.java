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

package com.threerings.opengl.material.config;

import java.util.List;

import com.google.common.collect.Lists;

import com.threerings.config.ConfigReferenceSet;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.expr.ExpressionBinding;
import com.threerings.expr.Scope;
import com.threerings.expr.Updater;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.util.DeepObject;

import com.threerings.opengl.compositor.Dependency;
import com.threerings.opengl.geometry.config.PassDescriptor;
import com.threerings.opengl.renderer.config.AlphaStateConfig;
import com.threerings.opengl.renderer.config.ColorMaskStateConfig;
import com.threerings.opengl.renderer.config.ColorStateConfig;
import com.threerings.opengl.renderer.config.CullStateConfig;
import com.threerings.opengl.renderer.config.DepthStateConfig;
import com.threerings.opengl.renderer.config.FogStateConfig;
import com.threerings.opengl.renderer.config.LightStateConfig;
import com.threerings.opengl.renderer.config.LineStateConfig;
import com.threerings.opengl.renderer.config.MaterialStateConfig;
import com.threerings.opengl.renderer.config.MaterialStateConfig.ColorMaterialMode;
import com.threerings.opengl.renderer.config.PointStateConfig;
import com.threerings.opengl.renderer.config.PolygonStateConfig;
import com.threerings.opengl.renderer.config.ShaderStateConfig;
import com.threerings.opengl.renderer.config.StencilStateConfig;
import com.threerings.opengl.renderer.config.TextureStateConfig;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Preloadable;

/**
 * Represents a single material pass.
 */
public class PassConfig extends DeepObject
    implements Exportable, Preloadable.LoadableConfig
{
    /** The alpha state to use in this pass. */
    @Editable
    public AlphaStateConfig alphaState = new AlphaStateConfig();

    /** The color state to use in this pass (overriding the default). */
    @Editable(nullable=true)
    public ColorStateConfig colorStateOverride;

    /** The color mask state to use in this pass. */
    @Editable
    public ColorMaskStateConfig colorMaskState = new ColorMaskStateConfig();

    /** The cull state to use in this pass. */
    @Editable
    public CullStateConfig cullState = new CullStateConfig();

    /** The depth state to use in this pass. */
    @Editable
    public DepthStateConfig depthState = new DepthStateConfig();

    /** The fog state to use in this pass (overriding the default). */
    @Editable(nullable=true)
    public FogStateConfig fogStateOverride;

    /** The light state to use in this pass (overriding the default). */
    @Editable(nullable=true)
    public LightStateConfig lightStateOverride;

    /** The line state. */
    @Editable(nullable=true)
    public LineStateConfig lineState;

    /** The material state to use in this pass. */
    @Editable(nullable=true)
    public MaterialStateConfig materialState = new MaterialStateConfig.OneSided();

    /** The point state. */
    @Editable(nullable=true)
    public PointStateConfig pointState;

    /** The polygon state to use in this pass. */
    @Editable
    public PolygonStateConfig polygonState = new PolygonStateConfig();

    /** The shader state to use in this pass. */
    @Editable
    public ShaderStateConfig shaderState = new ShaderStateConfig.Disabled();

    /** The stencil state to use in this pass. */
    @Editable
    public StencilStateConfig stencilState = new StencilStateConfig();

    /** The texture state to use in this pass. */
    @Editable
    public TextureStateConfig textureState = new TextureStateConfig();

    /** The static expression bindings for this pass. */
    @Editable
    public ExpressionBinding[] staticBindings = ExpressionBinding.EMPTY_ARRAY;

    /** The dynamic expression bindings for this pass. */
    @Editable
    public ExpressionBinding[] dynamicBindings = ExpressionBinding.EMPTY_ARRAY;

    /** Whether or not to ignore the vertex colors. */
    @Editable
    public boolean ignoreVertexColors;

    @Override
    public void preload (GlContext ctx)
    {
        // Do nothing for now. TODO: Figure out what needs to be brought in.
        materialState.getState();
        for (ExpressionBinding binding : staticBindings) {
            binding.preload(ctx);
        }
        for (ExpressionBinding binding : dynamicBindings) {
            binding.preload(ctx);
        }
    }

    @Deprecated
    public void getUpdateReferences (ConfigReferenceSet refs)
    {
    }

    /**
     * Determines whether this pass is supported.
     */
    public boolean isSupported (GlContext ctx, boolean fallback)
    {
        return (materialState == null || materialState.isSupported(fallback)) &&
            shaderState.isSupported(ctx, fallback) &&
            textureState.isSupported(ctx, fallback);
    }

    /**
     * Returns a descriptor for this pass that can be used to configure a geometry instance.
     */
    public PassDescriptor createDescriptor (GlContext ctx)
    {
        PassDescriptor desc = new PassDescriptor();
        shaderState.populateDescriptor(ctx, desc);
        textureState.populateDescriptor(desc);
        if (ignoreVertexColors) {
            desc.colors = false;
        } else {
            desc.colors |= !(lightStateOverride instanceof LightStateConfig.Enabled) ||
                materialState == null ||
                materialState.colorMaterialMode != ColorMaterialMode.DISABLED;
        }
        desc.normals |= !(lightStateOverride instanceof LightStateConfig.Disabled);
        return desc;
    }

    /**
     * Creates the set of states for this pass.
     *
     * @param adders holds adders to run on composite.
     * @param updaters holds updaters to run on enqueue.
     */
    public RenderState[] createStates (
        GlContext ctx, Scope scope, List<Dependency.Adder> adders, List<Updater> updaters)
    {
        RenderState[] states = RenderState.createEmptySet();
        states[RenderState.ALPHA_STATE] = alphaState.getState();
        states[RenderState.COLOR_STATE] = (colorStateOverride == null) ?
            ScopeUtil.resolve(scope, "colorState", ColorState.WHITE, ColorState.class) :
                colorStateOverride.getState();
        states[RenderState.COLOR_MASK_STATE] = colorMaskState.getState();
        states[RenderState.CULL_STATE] = cullState.getState();
        states[RenderState.DEPTH_STATE] = depthState.getState();
        states[RenderState.FOG_STATE] = (fogStateOverride == null) ?
            ScopeUtil.resolve(scope, "fogState", FogState.DISABLED, FogState.class) :
                fogStateOverride.getState();
        states[RenderState.LIGHT_STATE] = (lightStateOverride == null) ?
            ScopeUtil.resolve(scope, "lightState", LightState.DISABLED, LightState.class) :
                lightStateOverride.getState(ctx, scope, updaters);
        states[RenderState.LINE_STATE] = (lineState == null) ? null : lineState.getState();
        states[RenderState.MATERIAL_STATE] = (materialState == null) ?
            null : materialState.getState();
        states[RenderState.POINT_STATE] = (pointState == null) ? null : pointState.getState();
        states[RenderState.POLYGON_STATE] = polygonState.getState();
        states[RenderState.STENCIL_STATE] = stencilState.getState();
        states[RenderState.TEXTURE_STATE] = textureState.getState(ctx, scope, adders, updaters);

        // we create the shader state last because it may depend on the other states
        states[RenderState.SHADER_STATE] = shaderState.getState(ctx, scope, states, updaters);
        return states;
    }

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        if (materialState != null) {
            materialState.invalidate();
        }
        textureState.invalidate();
        for (ExpressionBinding binding : staticBindings) {
            binding.invalidate();
        }
        for (ExpressionBinding binding : dynamicBindings) {
            binding.invalidate();
        }
    }
}
