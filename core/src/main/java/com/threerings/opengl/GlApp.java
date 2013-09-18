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

package com.threerings.opengl;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;

import com.google.common.base.Objects;

import com.samskivert.util.RunQueue;

import com.threerings.config.ConfigManager;
import com.threerings.editor.util.EditorContext;
import com.threerings.expr.DynamicScope;
import com.threerings.expr.MutableFloat;
import com.threerings.expr.MutableLong;
import com.threerings.expr.Scoped;
import com.threerings.math.FloatMath;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.media.image.ColorPository;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;
import com.threerings.util.ToolUtil;

import com.threerings.openal.ClipProvider;
import com.threerings.openal.Listener;
import com.threerings.openal.ResourceClipProvider;
import com.threerings.openal.SoundManager;
import com.threerings.opengl.camera.CameraHandler;
import com.threerings.opengl.camera.OrbitCameraHandler;
import com.threerings.opengl.compositor.Compositable;
import com.threerings.opengl.compositor.Compositor;
import com.threerings.opengl.compositor.Enqueueable;
import com.threerings.opengl.gui.Root;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.ImageCache;
import com.threerings.opengl.util.ShaderCache;

/**
 * A base class for OpenGL-based applications.
 */
public abstract class GlApp extends DynamicScope
    implements GlContext, EditorContext, Enqueueable
{
    public GlApp ()
    {
        super("app");
        _renderer = new Renderer();
        _compositor = new Compositor(this);
        _msgmgr = new MessageManager("rsrc.i18n");
        initSharedManagers();
        _soundmgr = SoundManager.createSoundManager(getRunQueue());
        _clipprov = new ResourceClipProvider(_rsrcmgr);
        _imgcache = new ImageCache(this, shouldCheckTimestamps());
        _shadcache = new ShaderCache(this, shouldCheckTimestamps());

        // initialize our scoped fields
        _viewTransform = _viewTransformState.getModelview();
    }

    /**
     * Returns a reference to the application's run queue.
     */
    public abstract RunQueue getRunQueue ();

    /**
     * Sets the render scheme.
     */
    public void setRenderScheme (String scheme)
    {
        if (!Objects.equal(_renderScheme, scheme)) {
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
     * Enables or disables compatibility mode, which disables certain features for maximum
     * compatibility.
     */
    public void setCompatibilityMode (boolean enabled)
    {
        if (_compatibilityMode != enabled) {
            _compatibilityMode = enabled;
            wasUpdated();
        }
    }

    /**
     * Checks whether compatibility mode is enabled.
     */
    public boolean getCompatibilityMode ()
    {
        return _compatibilityMode;
    }

    /**
     * Enables or disables render effects.
     */
    public void setRenderEffects (boolean enabled)
    {
        if (_renderEffects != enabled) {
            _renderEffects = enabled;
            wasUpdated();
        }
    }

    /**
     * Checks whether render effects are enabled.
     */
    public boolean getRenderEffects ()
    {
        return _renderEffects;
    }

    /**
     * Returns a reference to the stream gain.
     */
    public MutableFloat getStreamGain ()
    {
        return _streamGain;
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

    /**
     * Convenience method for translation.
     */
    public String xlate (String bundle, String msg)
    {
        return _msgmgr.getBundle(bundle).xlate(msg);
    }

    /**
     * Creates and returns a snapshot image of the current frame.
     */
    public BufferedImage createSnapshot ()
    {
        return createSnapshot(false);
    }

    /**
     * Creates and returns a snapshot image of the current frame.
     */
    public BufferedImage createSnapshot (boolean alpha)
    {
        // read the contents of the frame buffer
        int width = _renderer.getWidth(), height = _renderer.getHeight();
        int comps = alpha ? 4 : 3;
        ByteBuffer buf = BufferUtils.createByteBuffer(comps * width * height);
        GL11.glReadPixels(0, 0, width, height, alpha ? GL11.GL_RGBA : GL11.GL_RGB,
            GL11.GL_UNSIGNED_BYTE, buf);

        // create a buffered image to match the format
        ComponentColorModel cmodel = new ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_sRGB), alpha, false,
            alpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        BufferedImage image = new BufferedImage(
            cmodel, Raster.createInterleavedRaster(
                DataBuffer.TYPE_BYTE, width, height, comps, null),
            false, null);

        // retrieve and populate the image data buffer
        byte[] data = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
        for (int yy = height - 1; yy >= 0; yy--) {
            buf.get(data, yy*width*comps, width*comps);
        }
        return image;
    }

    // documentation inherited from interface GlContext
    public GlApp getApp ()
    {
        return this;
    }

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

    // documentation inherited from interface GlContext
    public void setCameraHandler (CameraHandler camhand)
    {
        if (_camhand != null) {
            _camhand.wasRemoved();
        }
        if ((_camhand = camhand) != null) {
            _camhand.wasAdded();
        }
    }

    // documentation inherited from interface GlContext
    public CameraHandler getCameraHandler ()
    {
        return _camhand;
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
    public ShaderCache getShaderCache ()
    {
        return _shadcache;
    }

    // documentation inherited from interface Enqueueable
    public void enqueue ()
    {
        // update the view transform state
        _viewTransform.set(_compositor.getCamera().getViewTransform());
        _viewTransformState.setDirty(true);

        // update the shared axial billboard rotation (this assumes that the camera doesn't
        // "roll")
        _viewTransform.extractRotation(_billboardRotation);
        float angle = FloatMath.HALF_PI +
            2f*FloatMath.atan2(_billboardRotation.x, _billboardRotation.w);
        _billboardRotation.fromAngleAxis(angle, Vector3f.UNIT_X);
    }

    /**
     * Initializes the references to the resource manager, config manager, and color pository.  By
     * default this creates new managers, but it may be overridden to copy references to existing
     * ones.
     */
    protected void initSharedManagers ()
    {
        _rsrcmgr = new ResourceManager("rsrc/");
        _rsrcmgr.activateResourceProtocol();
        _cfgmgr = new ConfigManager(_rsrcmgr, _msgmgr, "config/");
        _colorpos = ColorPository.loadColorPository(_rsrcmgr);
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
        setCameraHandler(createCameraHandler());

        // add a root to call the composite method
        _compositor.addRoot(new Compositable() {
            public void composite () {
                GlApp.this.compositeView();
            }
        });

        // note that we've opened a window
        ToolUtil.windowAdded();

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
        _soundmgr.shutdown();
    }

    /**
     * Performs any updates that are necessary even when not rendering.
     */
    protected void updateView ()
    {
        long nnow = System.currentTimeMillis();
        float elapsed = Math.max(0f, (nnow - _now.value) / 1000f);
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
        if (!_soundmgr.isInitialized()) {
            return;
        }
        Vector3f translation = _camhand.getViewerTranslation();
        Quaternion rotation = _camhand.getViewerRotation();
        rotation.transformUnitY(_up);
        rotation.transformUnitZ(_at).negateLocal();
        Listener listener = _soundmgr.getListener();
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
     * Gives the application a chance to composite anything it might want rendered.
     */
    protected void compositeView ()
    {
        // the app's enqueue method prepares root state
        _compositor.addEnqueueable(this);
    }

    /**
     * Returns the pixel formats to use in attempting to create the display, in order of
     * preference.
     */
    protected PixelFormat[] getPixelFormats ()
    {
        return getPixelFormats(getAntialiasingLevel());
    }

    /**
     * Returns the antialiasing level desired.
     */
    protected int getAntialiasingLevel ()
    {
        return 0;
    }

    /**
     * Returns the pixel formats to use in attempting to create the display, in order of
     * preference.
     *
     * @param antialiasingLevel the antialiasing level desired.
     */
    protected static PixelFormat[] getPixelFormats (int antialiasingLevel)
    {
        if (antialiasingLevel == 0) {
            return DEFAULT_PIXEL_FORMATS;
        }
        // keep dividing the number of samples by two until we reach one
        int levels = antialiasingLevel + 1;
        PixelFormat[] formats = new PixelFormat[DEFAULT_PIXEL_FORMATS.length * levels];
        for (int ii = 0; ii < levels; ii++, antialiasingLevel--) {
            for (int jj = 0; jj < DEFAULT_PIXEL_FORMATS.length; jj++) {
                formats[ii * DEFAULT_PIXEL_FORMATS.length + jj] =
                    DEFAULT_PIXEL_FORMATS[jj].withSamples(
                        antialiasingLevel == 0 ? 0 : 1 << antialiasingLevel);
            }
        }
        return formats;
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

    /** The shader cache. */
    protected ShaderCache _shadcache;

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

    /** Controls whether certain features are disabled for maximum compatibility. */
    @Scoped
    protected boolean _compatibilityMode;

    /** Controls whether render effects are enabled. */
    @Scoped
    protected boolean _renderEffects = true;

    /** A scoped reference to the root view transform. */
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

    /** A container for the global stream gain. */
    @Scoped
    protected MutableFloat _streamGain = new MutableFloat(1f);

    /** Used to compute listener orientation. */
    protected Vector3f _at = new Vector3f(), _up = new Vector3f();

    /** Our default supported pixel formats in order of preference. */
    protected static final PixelFormat[] DEFAULT_PIXEL_FORMATS = {
        new PixelFormat(8, 16, 8), new PixelFormat(1, 16, 8),
        new PixelFormat(0, 16, 8), new PixelFormat(0, 8, 0) };
}
