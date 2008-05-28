//
// $Id$

package com.threerings.opengl.util;

import com.threerings.config.ConfigManager;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;

import com.threerings.opengl.renderer.Renderer;

/**
 * Provides access to the various components of the OpenGL rendering system.  Not to be confused
 * with LWJGL's {@link org.lwjgl.opengl.GLContext}.
 */
public interface GlContext
{
    /**
     * Returns a reference to the renderer.
     */
    public Renderer getRenderer ();

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
