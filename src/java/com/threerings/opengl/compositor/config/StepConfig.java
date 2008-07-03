//
// $Id$

package com.threerings.opengl.compositor.config;

import org.lwjgl.opengl.GL11;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.state.ColorMaskState;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.StencilState;

/**
 * Represents a single step in the process of updating a target.
 */
@EditorTypes({
    StepConfig.Clear.class,
    StepConfig.RenderScene.class,
    StepConfig.RenderQuad.class })
public abstract class StepConfig extends DeepObject
    implements Exportable
{
    /**
     * Clears some or all of the buffers.
     */
    public static class Clear extends StepConfig
    {
        /**
         * Color clear parameters.
         */
        public static class Color extends DeepObject
            implements Exportable
        {
            /** Whether or not to clear the color buffer. */
            @Editable(hgroup="c")
            public boolean clear = true;

            /** The color clear value. */
            @Editable(mode="alpha", hgroup="c")
            public Color4f value = new Color4f(0f, 0f, 0f, 0f);
        }

        /**
         * Depth clear parameters.
         */
        public static class Depth extends DeepObject
            implements Exportable
        {
            /** Whether or not to clear the depth buffer. */
            @Editable(hgroup="c")
            public boolean clear = true;

            /** The depth clear value. */
            @Editable(min=0, max=1, step=0.01, hgroup="c")
            public float value = 1f;
        }

        /**
         * Stencil clear parameters.
         */
        public static class Stencil extends DeepObject
            implements Exportable
        {
            /** Whether or not to clear the stencil buffer. */
            @Editable(hgroup="c")
            public boolean clear = true;

            /** The stencil clear value. */
            @Editable(min=0, hgroup="c")
            public int value;
        }

        /** Color buffer clear parameters. */
        @Editable
        public Color color = new Color();

        /** Depth buffer clear parameters. */
        @Editable
        public Depth depth = new Depth();

        /** Stencil buffer clear parameters. */
        @Editable
        public Stencil stencil = new Stencil();

        /**
         * Performs the clear operation.
         */
        public void execute (Renderer renderer)
        {
            int bits = 0;
            if (color.clear) {
                bits |= GL11.GL_COLOR_BUFFER_BIT;
                renderer.setClearColor(color.value);
                renderer.setState(ColorMaskState.ALL);
            }
            if (depth.clear) {
                bits |= GL11.GL_DEPTH_BUFFER_BIT;
                renderer.setClearDepth(depth.value);
                renderer.setState(DepthState.TEST_WRITE);
            }
            if (stencil.clear) {
                bits |= GL11.GL_STENCIL_BUFFER_BIT;
                renderer.setClearStencil(stencil.value);
                renderer.setState(StencilState.DISABLED);
            }
            if (bits != 0) {
                GL11.glClear(bits);
            }
        }
    }

    /**
     * Renders the scene.
     */
    public static class RenderScene extends StepConfig
    {
    }

    /**
     * Renders a full-screen quad.
     */
    public static class RenderQuad extends StepConfig
    {
        /** The material to use when rendering the quad. */
        @Editable(nullable=true)
        public ConfigReference<MaterialConfig> material;

        /** The level of tesselation in the x direction. */
        @Editable(min=1, hgroup="d")
        public int divisionsX = 1;

        /** The level of tesselation in the y direction. */
        @Editable(min=1, hgroup="d")
        public int divisionsY = 1;
    }
}
