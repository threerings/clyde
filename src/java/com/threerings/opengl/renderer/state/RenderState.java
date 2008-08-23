//
// $Id$

package com.threerings.opengl.renderer.state;

import com.threerings.opengl.renderer.Renderer;

/**
 * The superclass for all render state classes.  Each render state class represents a
 * separate aspect of the OpenGL state.  Because the renderer relies on referential
 * equality while rendering, the states must not change while rendering is taking place.
 */
public abstract class RenderState
{
    /** The alpha testing/blending state. */
    public static final int ALPHA_STATE = 0;

    /** The geometry array state. */
    public static final int ARRAY_STATE = 1;

    /** The draw color state. */
    public static final int COLOR_STATE = 2;

    /** The color mask state. */
    public static final int COLOR_MASK_STATE = 3;

    /** The back-face culling state. */
    public static final int CULL_STATE = 4;

    /** The depth testing/writing state. */
    public static final int DEPTH_STATE = 5;

    /** The fog state. */
    public static final int FOG_STATE = 6;

    /** The light state. */
    public static final int LIGHT_STATE = 7;

    /** The line state. */
    public static final int LINE_STATE = 8;

    /** The material state. */
    public static final int MATERIAL_STATE = 9;

    /** The point state. */
    public static final int POINT_STATE = 10;

    /** The polygon state. */
    public static final int POLYGON_STATE = 11;

    /** The GLSL shader state. */
    public static final int SHADER_STATE = 12;

    /** The stencil state. */
    public static final int STENCIL_STATE = 13;

    /** The texture state. */
    public static final int TEXTURE_STATE = 14;

    /** The transform state. */
    public static final int TRANSFORM_STATE = 15;

    /** The total number of state types. */
    public static final int STATE_COUNT = 16;

    /**
     * Creates and returns a new, empty render state set.
     */
    public static RenderState[] createEmptySet ()
    {
        return new RenderState[STATE_COUNT];
    }

    /**
     * Returns a new render state set containing the default states.
     */
    public static RenderState[] createDefaultSet ()
    {
        return getDefaults().clone();
    }

    /**
     * Returns a reference to the shared default state array.
     */
    public static RenderState[] getDefaults ()
    {
        if (_defaults == null) {
            _defaults = new RenderState[] {
                AlphaState.OPAQUE, ArrayState.DISABLED, ColorState.WHITE, ColorMaskState.ALL,
                CullState.DISABLED, DepthState.WRITE, FogState.DISABLED, LightState.DISABLED,
                LineState.DEFAULT, MaterialState.DEFAULT, PointState.DEFAULT, PolygonState.DEFAULT,
                ShaderState.DISABLED, StencilState.DISABLED, TextureState.DISABLED,
                TransformState.IDENTITY };
        }
        return _defaults;
    }

    /**
     * Copies any non-null states in <code>s1</code> into <code>s2</code>.
     */
    public static void copy (RenderState[] s1, RenderState[] s2)
    {
        for (int ii = 0; ii < STATE_COUNT; ii++) {
            RenderState state = s1[ii];
            if (state != null) {
                s2[ii] = state;
            }
        }
    }

    /**
     * Returns the type of this state.
     */
    public abstract int getType ();

    /**
     * Applies this state.
     */
    public abstract void apply (Renderer renderer);

    /**
     * Sets the dirty flag.
     */
    public final void setDirty (boolean dirty)
    {
        _dirty = dirty;
    }

    /**
     * Checks the dirty flag.
     */
    public final boolean isDirty ()
    {
        return _dirty;
    }

    /** Set when the state has changed and must be reapplied. */
    protected boolean _dirty;

    /** The default states. */
    protected static RenderState[] _defaults;
}
