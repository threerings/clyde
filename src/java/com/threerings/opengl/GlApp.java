//
// $Id$

package com.threerings.opengl;

import com.samskivert.util.RunQueue;

import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;

import com.threerings.opengl.camera.CameraHandler;
import com.threerings.opengl.gui.Root;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.MaterialCache;
import com.threerings.opengl.util.ModelCache;
import com.threerings.opengl.util.ShaderCache;
import com.threerings.opengl.util.TextureCache;

/**
 * A base class for OpenGL-based applications.
 */
public abstract class GlApp
    implements GlContext, RunQueue
{
    public GlApp ()
    {
        _renderer = new Renderer();
        _rsrcmgr = new ResourceManager("rsrc/");
        _msgmgr = new MessageManager("rsrc.i18n");
        _texcache = new TextureCache(this);
        _shadcache = new ShaderCache(this);
        _matcache = new MaterialCache(this);
        _modcache = new ModelCache(this);
    }

    /**
     * Creates a user interface root appropriate for this application.
     */
    public abstract Root createRoot ();

    /**
     * Returns a reference to the application's camera handler.
     */
    public CameraHandler getCameraHandler ()
    {
        return _camhand;
    }

    // documentation inherited from interface GlContext
    public Renderer getRenderer ()
    {
        return _renderer;
    }

    // documentation inherited from interface GlContext
    public ResourceManager getResourceManager ()
    {
        return _rsrcmgr;
    }

    // documentation inherited from interface GlContext
    public MessageManager getMessageManager ()
    {
        return _msgmgr;
    }

    // documentation inherited from interface GlContext
    public TextureCache getTextureCache ()
    {
        return _texcache;
    }

    // documentation inherited from interface GlContext
    public ShaderCache getShaderCache ()
    {
        return _shadcache;
    }

    // documentation inherited from interface GlContext
    public MaterialCache getMaterialCache ()
    {
        return _matcache;
    }

    // documentation inherited from interface GlContext
    public ModelCache getModelCache ()
    {
        return _modcache;
    }

    /** The OpenGL renderer. */
    protected Renderer _renderer;

    /** The camera handler. */
    protected CameraHandler _camhand;

    /** The resource manager. */
    protected ResourceManager _rsrcmgr;

    /** The message manager. */
    protected MessageManager _msgmgr;

    /** The texture cache. */
    protected TextureCache _texcache;

    /** The shader cache. */
    protected ShaderCache _shadcache;

    /** The material cache. */
    protected MaterialCache _matcache;

    /** The model cache. */
    protected ModelCache _modcache;
}
