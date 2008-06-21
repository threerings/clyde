//
// $Id$

package com.threerings.opengl.material.config;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.config.AlphaStateConfig;
import com.threerings.opengl.renderer.config.ColorMaskStateConfig;
import com.threerings.opengl.renderer.config.ColorStateConfig;
import com.threerings.opengl.renderer.config.CullStateConfig;
import com.threerings.opengl.renderer.config.DepthStateConfig;
import com.threerings.opengl.renderer.config.FogStateConfig;
import com.threerings.opengl.renderer.config.LightStateConfig;
import com.threerings.opengl.renderer.config.LineStateConfig;
import com.threerings.opengl.renderer.config.MaterialStateConfig;
import com.threerings.opengl.renderer.config.PointStateConfig;
import com.threerings.opengl.renderer.config.PolygonStateConfig;
import com.threerings.opengl.renderer.config.ShaderStateConfig;
import com.threerings.opengl.renderer.config.StencilStateConfig;
import com.threerings.opengl.renderer.config.TextureStateConfig;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.util.GlContext;

/**
 * Represents a single material pass.
 */
public class PassConfig extends DeepObject
    implements Exportable
{
    /** The alpha state to use in this pass. */
    @Editable(nullable=false)
    public AlphaStateConfig alphaState = new AlphaStateConfig();

    /** The color state to use in this pass. */
    @Editable
    public ColorStateConfig colorState = new ColorStateConfig();

    /** The color mask state to use in this pass. */
    @Editable(nullable=false)
    public ColorMaskStateConfig colorMaskState = new ColorMaskStateConfig();

    /** The cull state to use in this pass. */
    @Editable(nullable=false)
    public CullStateConfig cullState = new CullStateConfig();

    /** The depth state to use in this pass. */
    @Editable(nullable=false)
    public DepthStateConfig depthState = new DepthStateConfig();

    /** The fog state to use in this pass (overriding the default). */
    @Editable(types={ FogStateConfig.Disabled.class, FogStateConfig.Linear.class,
        FogStateConfig.Exponential.class }, nullable=true)
    public FogStateConfig fogStateOverride;

    /** The light state to use in this pass (overriding the default). */
    @Editable(types={ LightStateConfig.Disabled.class, LightStateConfig.Enabled.class },
        nullable=true)
    public LightStateConfig lightStateOverride;

    /** The line state. */
    @Editable
    public LineStateConfig lineState;

    /** The material state to use in this pass. */
    @Editable(types={ MaterialStateConfig.OneSided.class, MaterialStateConfig.TwoSided.class },
        nullable=true)
    public MaterialStateConfig materialState = new MaterialStateConfig.OneSided();

    /** The polygon state to use in this pass. */
    @Editable(nullable=false)
    public PolygonStateConfig polygonState = new PolygonStateConfig();

    /** The point state. */
    @Editable
    public PointStateConfig pointState;

    /** The shader state to use in this pass. */
    @Editable
    public ShaderStateConfig shaderState;

    /** The stencil state to use in this pass. */
    @Editable(nullable=false)
    public StencilStateConfig stencilState = new StencilStateConfig();

    /** The texture state to use in this pass. */
    @Editable(nullable=false)
    public TextureStateConfig textureState = new TextureStateConfig();

    /**
     * Creates the set of states for this pass.
     */
    public RenderState[] createStates (GlContext ctx)
    {
        RenderState[] states = RenderState.createEmptySet();
        states[RenderState.ALPHA_STATE] = alphaState.getState();
        states[RenderState.COLOR_STATE] = (colorState == null) ? null : colorState.getState();
        states[RenderState.COLOR_MASK_STATE] = colorMaskState.getState();
        states[RenderState.CULL_STATE] = cullState.getState();
        states[RenderState.DEPTH_STATE] = depthState.getState();
        states[RenderState.FOG_STATE] =
            (fogStateOverride == null) ? null : fogStateOverride.getState();
        states[RenderState.LIGHT_STATE] =
            (lightStateOverride == null) ? null : lightStateOverride.getState();
        states[RenderState.LINE_STATE] = (lineState == null) ? null : lineState.getState();
        states[RenderState.MATERIAL_STATE] =
            (materialState == null) ? null : materialState.getState();
        states[RenderState.POLYGON_STATE] = polygonState.getState();
        states[RenderState.POINT_STATE] = (pointState == null) ? null : pointState.getState();
        states[RenderState.SHADER_STATE] = shaderState.getState(ctx);
        states[RenderState.STENCIL_STATE] = stencilState.getState();
        states[RenderState.TEXTURE_STATE] = textureState.getState(ctx);
        return states;
    }
}
