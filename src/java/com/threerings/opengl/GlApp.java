//
// $Id$

package com.threerings.opengl;

import org.lwjgl.opengl.PixelFormat;

import com.samskivert.util.ObjectUtil;
import com.samskivert.util.RunQueue;

import com.threerings.config.ConfigManager;
import com.threerings.editor.util.EditorContext;
import com.threerings.expr.DynamicScope;
import com.threerings.expr.MutableLong;
import com.threerings.expr.Scoped;
import com.threerings.math.FloatMath;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.media.image.ColorPository;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;

import com.threerings.openal.ClipProvider;
import com.threerings.openal.Listener;
import com.threerings.openal.ResourceClipProvider;
import com.threerings.openal.SoundManager;
import com.threerings.opengl.camera.CameraHandler;
import com.threerings.opengl.camera.OrbitCameraHandler;
import com.threerings.opengl.compositor.Compositor;
import com.threerings.opengl.gui.Root;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.ImageCache;
import com.threerings.opengl.util.MaterialCache;
import com.threerings.opengl.util.ModelCache;
import com.threerings.opengl.util.Renderable;
import com.threerings.opengl.util.ShaderCache;
import com.threerings.opengl.util.TextureCache;

/**
 * A base class for OpenGL-based applications.
 */
public abstract class GlApp extends DynamicScope
    implements GlContext, EditorContext
{
    public GlApp ()
    {
        super("app");
        _renderer = new Renderer();
        _compositor = new Compositor(this);
        _rsrcmgr = new ResourceManager("rsrc/");
        _msgmgr = new MessageManager("rsrc.i18n");
        _cfgmgr = new ConfigManager(_rsrcmgr, "config/");
        _colorpos = ColorPository.loadColorPository(_rsrcmgr);
        _soundmgr = SoundManager.createSoundManager(getRunQueue());
        _clipprov = new ResourceClipProvider(_rsrcmgr);
        _imgcache = new ImageCache(this, shouldCheckTimestamps());
        _texcache = new TextureCache(this);
        _shadcache = new ShaderCache(this, shouldCheckTimestamps());
        _matcache = new MaterialCache(this);
        _modcache = new ModelCache(this);

        // initialize our scoped fields
        _viewTransform = _compositor.getCamera().getViewTransform();
    }

    /**
     * Returns a reference to the application's run queue.
     */
    public abstract RunQueue getRunQueue ();

    /**
     * Returns a reference to the application's camera handler.
     */
    public CameraHandler getCameraHandler ()
    {
        return _camhand;
    }

    /**
     * Sets the render scheme.
     */
    public void setRenderScheme (String scheme)
    {
        if (!ObjectUtil.equals(_renderScheme, scheme)) {
            _renderScheme = scheme;
            wasUpdated();
        }
    }

    /**
     * Returns the name of the configured render scheme.
     */
    public String getRenderScheme ()
    {
        return _renderScheme;
    }

    /**
     * Creates a user interface root appropriate for this application.
     */
    public abstract Root createRoot ();

    /**
     * Starts up the application.
     */
    public abstract void startup ();

    /**
     * Shuts down the application.
     */
    public abstract void shutdown ();

    // documentation inherited from interface AlContext, GlContext
    public DynamicScope getScope ()
    {
        return this;
    }

    // documentation inherited from interfaces AlContext, GlContext, EditorContext
    public ResourceManager getResourceManager ()
    {
        return _rsrcmgr;
    }

    // documentation inherited from interfaces AlContext, GlContext, EditorContext
    public ConfigManager getConfigManager ()
    {
        return _cfgmgr;
    }

    // documentation inherited from interface AlContext
    public SoundManager getSoundManager ()
    {
        return _soundmgr;
    }

    // documentation inherited from interface AlContext
    public ClipProvider getClipProvider ()
    {
        return _clipprov;
    }

    // documentation inherited from interface GlContext
    public void setRenderer (Renderer renderer)
    {
        _renderer = renderer;
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
    public MessageManager getMessageManager ()
    {
        return _msgmgr;
    }

    // documentation inherited from interface GlContext, EditorContext
    public ColorPository getColorPository ()
    {
        return _colorpos;
    }

    // documentation inherited from interface GlContext
    public ImageCache getImageCache ()
    {
        return _imgcache;
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

    /**
     * Determines whether or not we should check resource file timestamps when we load them from
     * the cache (in other words, whether we expect the files to be modified externally).
     */
    protected boolean shouldCheckTimestamps ()
    {
        return false;
    }

    /**
     * Initializes the view once the OpenGL context is available.
     */
    protected void init ()
    {
        initRenderer();
        _camhand = createCameraHandler();

        // add a root to call the enqueue method
        _compositor.addRoot(new Renderable() {
            public void enqueue () {
                GlApp.this.enqueueView();
            }
        });

        // give subclasses a chance to init
        didInit();
    }

    /**
     * Initializes the renderer.
     */
    protected abstract void initRenderer ();

    /**
     * Creates and returns the camera handler.
     */
    protected CameraHandler createCameraHandler ()
    {
        return new OrbitCameraHandler(this);
    }

    /**
     * Override to perform custom initialization after the render context is valid.
     */
    protected void didInit ()
    {
    }

    /**
     * Override to perform cleanup before the application exits.
     */
    protected void willShutdown ()
    {
    }

    /**
     * Performs any updates that are necessary even when not rendering.
     */
    protected void updateView ()
    {
        long nnow = System.currentTimeMillis();
        float elapsed = (nnow - _now.value) / 1000f;
        _now.value = nnow;

        updateView(elapsed);
    }

    /**
     * Performs any updates that are necessary even when not rendering.
     *
     * @param elapsed the elapsed time since the last update, in seconds.
     */
    protected void updateView (float elapsed)
    {
        // update the camera position
        _camhand.updatePosition();

        // update the listener position and orientation
        Listener listener = _soundmgr.getListener();
        Transform3D transform = _compositor.getCamera().getWorldTransform();
        Vector3f translation = transform.getTranslation();
        Quaternion rotation = transform.getRotation();
        rotation.transformUnitY(_up);
        rotation.transformUnitZ(_at).negateLocal();
        listener.setPosition(translation.x, translation.y, translation.z);
        listener.setOrientation(_at.x, _at.y, _at.z, _up.x, _up.y, _up.z);

        // update the sound manager streams
        _soundmgr.updateStreams(elapsed);
    }

    /**
     * Renders the entire view.
     */
    protected void renderView ()
    {
        _compositor.renderView();
    }

    /**
     * Gives the application a chance to enqueue anything it might want rendered.
     */
    protected void enqueueView ()
    {
        // update the view transform state
        _viewTransformState.getModelview().set(_viewTransform);
        _viewTransformState.setDirty(true);

        // update the shared axial billboard rotation (this assumes that the camera doesn't
        // "roll")
        Quaternion viewRotation = _viewTransform.getRotation();
        float angle = FloatMath.HALF_PI + 2f*FloatMath.atan2(viewRotation.x, viewRotation.w);
        _billboardRotation.fromAngleAxis(angle, Vector3f.UNIT_X);
    }

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

    /** The image cache. */
    protected ImageCache _imgcache;

    /** The texture cache. */
    protected TextureCache _texcache;

    /** The shader cache. */
    protected ShaderCache _shadcache;

    /** The material cache. */
    protected MaterialCache _matcache;

    /** The model cache. */
    protected ModelCache _modcache;

    /** The sound manager. */
    protected SoundManager _soundmgr;

    /** The clip provider. */
    protected ClipProvider _clipprov;

    /** A container for the current time as sampled at the beginning of the frame. */
    @Scoped
    protected MutableLong _now = new MutableLong(System.currentTimeMillis());

    /** A container for the application epoch. */
    @Scoped
    protected MutableLong _epoch = new MutableLong(System.currentTimeMillis());

    /** The base render scheme (used to select material techniques). */
    @Scoped
    protected String _renderScheme;

    /** A scoped reference to the camera's view transform. */
    @Scoped
    protected Transform3D _viewTransform;

    /** A scoped reference to the root world transform. */
    @Scoped
    protected Transform3D _worldTransform = new Transform3D();

    /** A transform state containing the camera's view transform. */
    @Scoped
    protected TransformState _viewTransformState = new TransformState();

    /** The view rotation shared by all billboards aligned with the z axis and the view vector. */
    @Scoped
    protected Quaternion _billboardRotation = new Quaternion();

    /** Used to compute listener orientation. */
    protected Vector3f _at = new Vector3f(), _up = new Vector3f();

    /** Our supported pixel formats in order of preference. */
    protected static final PixelFormat[] PIXEL_FORMATS = {
        new PixelFormat(8, 16, 8), new PixelFormat(1, 16, 8),
        new PixelFormat(0, 16, 8), new PixelFormat(0, 8, 0) };
}
