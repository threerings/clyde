//
// $Id$

package com.threerings.opengl.compositor.config;

import org.lwjgl.opengl.GL11;

import com.threerings.config.ConfigReference;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.gui.util.Rectangle;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

/**
 * The configuration of a render queue.
 */
public class RenderQueueConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the render queue.
     */
    @EditorTypes({ Normal.class, Ortho.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Creates the render queue corresponding to this configuration.
         */
        public abstract RenderQueue createRenderQueue (GlContext ctx);
    }

    /**
     * Base class of the original implementations.
     */
    public static abstract class Original extends Implementation
    {
        /** The priority of the queue. */
        @Editable
        public int priority;
    }

    /**
     * A normal queue.
     */
    public static class Normal extends Original
    {
        @Override // documentation inherited
        public RenderQueue createRenderQueue (GlContext ctx)
        {
            return new RenderQueue(priority);
        }
    }

    /**
     * An orthographic queue.
     */
    public static class Ortho extends Original
    {
        @Override // documentation inherited
        public RenderQueue createRenderQueue (GlContext ctx)
        {
            return new RenderQueue(priority) {
                public void render (Renderer renderer) {
                    // make sure we have something to render
                    if (size() == 0) {
                        return;
                    }

                    // save the projection matrix and replace it with the ortho matrix
                    renderer.setMatrixMode(GL11.GL_PROJECTION);
                    GL11.glPushMatrix();
                    GL11.glLoadIdentity();
                    Rectangle viewport = renderer.getViewport();
                    GL11.glOrtho(0f, viewport.width, 0f, viewport.height, -1f, +1f);

                    // render the queue contents
                    super.render(renderer);

                    // restore the projection matrix
                    renderer.setMatrixMode(GL11.GL_PROJECTION);
                    GL11.glPopMatrix();
                }
            };
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The render queue reference. */
        @Editable(nullable=true)
        public ConfigReference<RenderQueueConfig> renderQueue;

        @Override // documentation inherited
        public RenderQueue createRenderQueue (GlContext ctx)
        {
            if (renderQueue == null) {
                return null;
            }
            RenderQueueConfig config = ctx.getConfigManager().getConfig(
                RenderQueueConfig.class, renderQueue);
            return (config == null) ? null : config.createRenderQueue(ctx);
        }
    }

    /** The actual render queue implementation. */
    @Editable
    public Implementation implementation = new Normal();

    /**
     * Creates the render queue corresponding to this configuration.
     */
    public RenderQueue createRenderQueue (GlContext ctx)
    {
        return implementation.createRenderQueue(ctx);
    }
}
