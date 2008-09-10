//
// $Id$

package com.threerings.opengl.util;

import com.threerings.config.ConfigManager;
import com.threerings.expr.DynamicScope;
import com.threerings.media.image.ColorPository;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;

import com.threerings.openal.util.AlContext;
import com.threerings.opengl.compositor.Compositor;
import com.threerings.opengl.renderer.Renderer;

/**
 * Provides access to the various components of the OpenGL rendering system.  Not to be confused
 * with LWJGL's {@link org.lwjgl.opengl.GLContext}.
 */
public interface GlContext extends AlContext
{
    /**
     * Returns a reference to the scope.
     */
    public DynamicScope getScope ();

    /**
     * Returns a reference to the renderer.
     */
    public Renderer getRenderer ();

    /**
     * Returns a reference to the compositor.
     */
    public Compositor getCompositor ();

    /**
     * Returns a reference to the resource manager.
     */
    public ResourceManager getResourceManager ();

    /**
     * Returns a reference to the message manager.
     */
    public MessageManager getMessageManager ();

    /**
     * Returns a reference to the configuration manager.
     */
    public ConfigManager getConfigManager ();

    /**
     * Returns a reference to the color pository.
     */
    public ColorPository getColorPository ();

    /**
     * Returns a reference to the image cache.
     */
    public ImageCache getImageCache ();

    /**
     * Returns a reference to the texture cache.
     */
    public TextureCache getTextureCache ();

    /**
     * Returns a reference to the shader cache.
     */
    public ShaderCache getShaderCache ();

    /**
     * Returns a reference to the material cache.
     */
    public MaterialCache getMaterialCache ();

    /**
     * Returns a reference to the model cache.
     */
    public ModelCache getModelCache ();
}
