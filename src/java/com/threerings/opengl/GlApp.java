//
// $Id$

package com.threerings.opengl;

import com.samskivert.util.RunQueue;

import com.threerings.config.ConfigManager;
import com.threerings.editor.util.EditorContext;
import com.threerings.expr.DynamicScope;
import com.threerings.expr.MutableLong;
import com.threerings.expr.Scoped;
import com.threerings.media.image.ColorPository;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;

import com.threerings.opengl.camera.CameraHandler;
import com.threerings.opengl.compositor.Compositor;
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
    implements GlContext, EditorContext, RunQueue
{
    public GlApp ()
    {
        _scope = new DynamicScope(this, "app");
        _renderer = new Renderer();
        _compositor = new Compositor(this);
        _rsrcmgr = new ResourceManager("rsrc/");
        _msgmgr = new MessageManager("rsrc.i18n");
        _cfgmgr = new ConfigManager(_rsrcmgr, "config/");
        _colorpos = ColorPository.loadColorPository(_rsrcmgr);
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
    public DynamicScope getScope ()
    {
        return _scope;
    }
    
    // documentation inherited from interface GlContext
    public Renderer getRenderer ()
    {
        return _renderer;
    }

    // documentation inherited from interface GlContext
    public Compositor getCompositor ()
    {
        return _compositor;
    }

    // documentation inherited from interfaces GlContext, EditorContext
    public ResourceManager getResourceManager ()
    {
        return _rsrcmgr;
    }

    // documentation inherited from interfaces GlContext, EditorContext
    public MessageManager getMessageManager ()
    {
        return _msgmgr;
    }

    // documentation inherited from interfaces GlContext, EditorContext
    public ConfigManager getConfigManager ()
    {
        return _cfgmgr;
    }

    // documentation inherited from interface GlContext, EditorContext
    public ColorPository getColorPository ()
    {
        return _colorpos;
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

    /** The expression scope. */
    protected DynamicScope _scope;
    
    /** The OpenGL renderer. */
    protected Renderer _renderer;

    /** The view compositor. */
    protected Compositor _compositor;

    /** The camera handler. */
    protected CameraHandler _camhand;

    /** The resource manager. */
    protected ResourceManager _rsrcmgr;

    /** The message manager. */
    protected MessageManager _msgmgr;

    /** The configuration manager. */
    protected ConfigManager _cfgmgr;

    /** The color pository. */
    protected ColorPository _colorpos;

    /** The texture cache. */
    protected TextureCache _texcache;

    /** The shader cache. */
    protected ShaderCache _shadcache;

    /** The material cache. */
    protected MaterialCache _matcache;

    /** The model cache. */
    protected ModelCache _modcache;
    
    /** A container for the current time as sampled at the beginning of the frame. */
    @Scoped
    protected MutableLong _now = new MutableLong(System.currentTimeMillis());
    
    /** A container for the application epoch. */
    @Scoped
    protected MutableLong _epoch = new MutableLong(System.currentTimeMillis());
}
