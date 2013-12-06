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

package com.threerings.opengl.renderer;

import java.lang.ref.WeakReference;

import java.util.Map;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ARBDepthTexture;
import org.lwjgl.opengl.ARBTextureCubeMap;
import org.lwjgl.opengl.ARBTextureRectangle;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.opengl.RenderTexture;

import com.threerings.util.CacheUtil;
import com.threerings.opengl.camera.Camera;
import com.threerings.opengl.gui.util.Rectangle;
import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.Log.log;

/**
 * Provides render-to-texture functionality using various methods according to the abilities
 * of the driver.
 */
public class TextureRenderer
{
    /**
     * Retrieves the shared texture renderer instance for the supplied textures.
     */
    public static TextureRenderer getInstance (
        GlContext ctx, Texture color, Texture depth, PixelFormat pformat)
    {
        return getInstance(ctx, color, depth, -1, -1, pformat);
    }

    /**
     * Retrieves the shared texture renderer instance for the supplied textures.
     *
     * @param width the width of the render surface, or -1 to match the texture dimensions.
     * @param height the height of the render surface, or -1.
     */
    public static TextureRenderer getInstance (
        GlContext ctx, Texture color, Texture depth, int width, int height, PixelFormat pformat)
    {
        InstanceKey key = new InstanceKey(color, depth);
        TextureRenderer instance = _instances.get(key);
        if (instance == null) {
            _instances.put(key,
                instance = new TextureRenderer(ctx, color, depth, width, height, pformat));
        }
        return instance;
    }

    /**
     * Creates a new texture renderer to render into the specified texture(s).
     *
     * @param width the width of the render surface, or -1 to match the texture dimensions.
     * @param height the height of the render surface, or -1.
     */
    public TextureRenderer (
        GlContext ctx, Texture color, Texture depth, int width, int height, PixelFormat pformat)
    {
        _ctx = ctx;
        _renderer = ctx.getRenderer();
        _color = color;
        _depth = depth;
        _pformat = pformat;
        Texture tex = (color == null) ? depth : color;
        int twidth = tex.getWidth(), theight = tex.getHeight();

        // match the texture dimensions if unspecified
        if (width == -1) {
            _width = twidth;
            _height = theight;
            _matchTextureDimensions = true;
        } else {
            _width = width;
            _height = height;
        }

        // first try fbos (temporarily disabled)
        if (false && GLContext.getCapabilities().GL_EXT_framebuffer_object) {
            Framebuffer obuffer = _renderer.getFramebuffer();
            _renderer.setFramebuffer(_framebuffer = new Framebuffer(_renderer));

            // attach the color texture
            if (color != null) {
                _framebuffer.setColorAttachment(color);
            }

            // attach the depth texture or render buffer
            if (depth != null) {
                _framebuffer.setDepthAttachment(depth);
            } else {
                int dbits = pformat.getDepthBits();
                if (dbits > 0) {
                    Renderbuffer dbuf = new Renderbuffer(_renderer);
                    dbuf.setStorage(GL11.GL_DEPTH_COMPONENT, twidth, theight);
                    _framebuffer.setDepthAttachment(dbuf);
                }
            }

            // add a stencil buffer if requested
            int sbits = pformat.getStencilBits();
            if (sbits > 0) {
                Renderbuffer sbuf = new Renderbuffer(_renderer);
                sbuf.setStorage(GL11.GL_STENCIL_INDEX, twidth, theight);
                _framebuffer.setStencilAttachment(sbuf);
            }

            // if we have no color buffer, disable draw and read
            if (color == null) {
                GL11.glDrawBuffer(GL11.GL_NONE);
                GL11.glReadBuffer(GL11.GL_NONE);
            }

            // get the status
            int status = EXTFramebufferObject.glCheckFramebufferStatusEXT(
                EXTFramebufferObject.GL_FRAMEBUFFER_EXT);

            // restore the old frame buffer
            _renderer.setFramebuffer(obuffer);

            // process the status
            if (status == EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT) {
                return; // success!
            } else if (status != EXTFramebufferObject.GL_FRAMEBUFFER_UNSUPPORTED_EXT) {
                log.warning("Framebuffer incomplete.", "status", status);
            }
            deleteFramebuffer(_framebuffer);
            _framebuffer = null;
        }

        // then try pbuffers with or without rtt (temporarily disabled)
        // even calling Pbuffer.getCapabilities has side effects on Windows with AWTGLCanvas: it
        // seems to put us in a glBegin segment, which causes a subsequent call to glViewport to
        // fail with GL_INVALID_OPERATION
        int pcaps = 0; // Pbuffer.getCapabilities();
        if ((pcaps & Pbuffer.PBUFFER_SUPPORTED) != 0) {
            int target = getRenderTextureTarget(tex.getTarget());
            boolean rectangle = (target == RenderTexture.RENDER_TEXTURE_RECTANGLE);
            _pwidth = _width;
            _pheight = _height;
            if ((pcaps & Pbuffer.RENDER_TEXTURE_SUPPORTED) != 0 &&
                    (!rectangle || (pcaps & Pbuffer.RENDER_TEXTURE_RECTANGLE_SUPPORTED) != 0) &&
                    (depth == null || (pcaps & Pbuffer.RENDER_DEPTH_TEXTURE_SUPPORTED) != 0)) {
                boolean rgb = false, rgba = false;
                if (color != null) {
                    rgb = !(rgba = color.hasAlpha());
                }
                _rtex = new RenderTexture(rgb, rgba, depth != null, rectangle, target, 0);
                _pwidth = twidth;
                _pheight = theight;
            }
            initPbuffer();
        }

        // the final option is to render to back buffer, then copy to texture
    }

    /**
     * Returns the configured width of the renderer.
     */
    public int getWidth ()
    {
        return _width;
    }

    /**
     * Returns the configured height of the renderer.
     */
    public int getHeight ()
    {
        return _height;
    }

    /**
     * Returns a reference to the color texture (if any).
     */
    public Texture getColor ()
    {
        return _color;
    }

    /**
     * Returns a reference to the depth texture (if any).
     */
    public Texture getDepth ()
    {
        return _depth;
    }

    /**
     * Starts rendering to the texture.
     */
    public void startRender ()
    {
        startRender(0, 0);
    }

    /**
     * Starts rendering to the texture.
     *
     * @param level the mipmap level.
     * @param param the cube map face index or z offset, as appropriate.
     */
    public void startRender (int level, int param)
    {
        if (_matchTextureDimensions) {
            Texture tex = (_color == null) ? _depth : _color;
            int twidth = tex.getWidth(), theight = tex.getHeight();
            if (_width != twidth || _height != theight) {
                resize(twidth, theight);
            }
        }
        _level = level;
        _param = param;
        if (_framebuffer != null) {
            _obuffer = _renderer.getFramebuffer();
            _renderer.setFramebuffer(_framebuffer);

        } else if (_pbuffer != null) {
            if (_pbuffer.isBufferLost()) {
                _pbuffer.destroy();
                initPbuffer();
            }
            if (_rtex != null) {
                releaseTextures();
            }
            try {
                _pbuffer.makeCurrent();
                _orenderer = _ctx.getRenderer();
                _ctx.setRenderer(_renderer);
            } catch (LWJGLException e) {
                log.warning("Failed to make pbuffer context current.", e);
            }
        }
        if (_color == null) {
            _odraw = _renderer.getDrawBuffer();
            _oread = _renderer.getReadBuffer();
            _renderer.setBuffers(GL11.GL_NONE, GL11.GL_NONE);
        }
        Camera camera = _ctx.getCompositor().getCamera();
        _oviewport.set(camera.getViewport());
        camera.getViewport().set(0, 0, _width, _height);
        camera.apply(_renderer);
    }

    /**
     * Stops rendering to the texture (and makes it available for use).
     */
    public void commitRender ()
    {
        Camera camera = _ctx.getCompositor().getCamera();
        camera.getViewport().set(_oviewport);
        camera.apply(_renderer);
        if (_color == null) {
            _renderer.setBuffers(_odraw, _oread);
        }
        if (_framebuffer != null) {
            _renderer.setFramebuffer(_obuffer);
            _obuffer = null;

        } else if (_pbuffer != null) {
            if (_rtex == null) {
                copyTextures();
            }
            _ctx.setRenderer(_orenderer);
            _ctx.makeCurrent();
            if (_rtex != null) {
                bindTextures();
            }
        } else {
            copyTextures();
        }
    }

    /**
     * Disposes of this texture renderer, rendering it unusable.
     */
    public void dispose ()
    {
        if (_framebuffer != null) {
            deleteFramebuffer(_framebuffer);
            _framebuffer = null;
        }
        if (_pbuffer != null) {
            _pbuffer.destroy();
            _pbuffer = null;
        }
    }

    @Override
    protected void finalize ()
        throws Throwable
    {
        super.finalize();
        if (_pbuffer != null) {
            _renderer.pbufferFinalized(_pbuffer);
        }
    }

    /**
     * Resizes the texture render state to match the supplied dimensions.
     */
    protected void resize (int width, int height)
    {
        _width = width;
        _height = height;

        if (_framebuffer != null) {
            // resize the depth and/or stencil render buffers
            if (_depth == null && _pformat.getDepthBits() > 0) {
                Renderbuffer dbuf = new Renderbuffer(_renderer);
                dbuf.setStorage(GL11.GL_DEPTH_COMPONENT, width, height);
                _framebuffer.setDepthAttachment(dbuf);
            }
            if (_pformat.getStencilBits() > 0) {
                Renderbuffer sbuf = new Renderbuffer(_renderer);
                sbuf.setStorage(GL11.GL_STENCIL_INDEX, width, height);
                _framebuffer.setStencilAttachment(sbuf);
            }
        } else if (_pbuffer != null) {
            _pwidth = width;
            _pheight = height;
            initPbuffer();
        }
    }

    /**
     * (Re)initializes the pbuffer.
     */
    protected void initPbuffer ()
    {
        try {
            _pbuffer = new Pbuffer(
                _pwidth, _pheight, _pformat, _rtex, _ctx.getRenderer().getDrawable());
            _pbuffer.makeCurrent();
            _renderer = new Renderer();
            _renderer.init(_pbuffer, _pwidth, _pheight);
            _ctx.makeCurrent();
            if (_rtex != null) {
                bindTextures();
            }
        } catch (LWJGLException e) {
            log.warning("Failed to create pbuffer.", e);
            if (_pbuffer != null) {
                _pbuffer.destroy();
                _pbuffer = null;
            }
        }
    }

    /**
     * Binds the texture(s) to the pbuffer.
     */
    protected void bindTextures ()
    {
        if (_color != null) {
            _ctx.getRenderer().setTexture(_color);
            _pbuffer.bindTexImage(Pbuffer.FRONT_LEFT_BUFFER);
        }
        if (_depth != null) {
            _ctx.getRenderer().setTexture(_depth);
            _pbuffer.bindTexImage(Pbuffer.DEPTH_BUFFER);
        }
    }

    /**
     * Releases the texture(s) from the pbuffer.
     */
    protected void releaseTextures ()
    {
        if (_color != null) {
            _pbuffer.releaseTexImage(Pbuffer.FRONT_LEFT_BUFFER);
        }
        if (_depth != null) {
            _pbuffer.releaseTexImage(Pbuffer.DEPTH_BUFFER);
        }
    }

    /**
     * Copies the textures from the buffer.
     */
    protected void copyTextures ()
    {
        if (_color != null) {
            copyTexture(_color);
        }
        if (_depth != null) {
            copyTexture(_depth);
        }
    }

    /**
     * Copies a single texture from the buffer.
     */
    protected void copyTexture (Texture texture)
    {
        int target;
        if (texture instanceof Texture2D) {
            target = texture.getTarget();
        } else if (texture instanceof TextureCubeMap) {
            target = TextureCubeMap.FACE_TARGETS[_param];
        } else {
            return;
        }
        _renderer.setTexture(texture);
        GL11.glCopyTexSubImage2D(target, _level, 0, 0, 0, 0, _width, _height);
    }

    /**
     * Returns the depth format with the specified number of bits.
     */
    protected static int getDepthFormat (int bits)
    {
        switch (bits) {
            default: case 16: return ARBDepthTexture.GL_DEPTH_COMPONENT16_ARB;
            case 24: return ARBDepthTexture.GL_DEPTH_COMPONENT24_ARB;
            case 32: return ARBDepthTexture.GL_DEPTH_COMPONENT32_ARB;
        }
    }

    /**
     * Returns the stencil format with the specified number of bits.
     */
    protected static int getStencilFormat (int bits)
    {
        switch (bits) {
            default: case 1: return EXTFramebufferObject.GL_STENCIL_INDEX1_EXT;
            case 4: return EXTFramebufferObject.GL_STENCIL_INDEX4_EXT;
            case 8: return EXTFramebufferObject.GL_STENCIL_INDEX8_EXT;
            case 16: return EXTFramebufferObject.GL_STENCIL_INDEX16_EXT;
        }
    }

    /**
     * Returns the render texture target corresponding to the specified texture target.
     */
    protected static int getRenderTextureTarget (int target)
    {
        switch (target) {
            case GL11.GL_TEXTURE_1D:
                return RenderTexture.RENDER_TEXTURE_1D;
            default: case GL11.GL_TEXTURE_2D:
                return RenderTexture.RENDER_TEXTURE_2D;
            case ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB:
                return RenderTexture.RENDER_TEXTURE_CUBE_MAP;
            case ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB:
                return RenderTexture.RENDER_TEXTURE_RECTANGLE;
        }
    }

    /**
     * Deletes the specified frame buffer and its render buffer attachments.
     */
    protected static void deleteFramebuffer (Framebuffer framebuffer)
    {
        maybeDeleteAttachment(framebuffer.getColorAttachment());
        maybeDeleteAttachment(framebuffer.getDepthAttachment());
        maybeDeleteAttachment(framebuffer.getStencilAttachment());
        framebuffer.delete();
    }

    /**
     * Deletes the supplied attachment if it is a {@link Renderbuffer}.
     */
    protected static void maybeDeleteAttachment (Object attachment)
    {
        if (attachment instanceof Renderbuffer) {
            ((Renderbuffer)attachment).delete();
        }
    }

    /**
     * Identifies a shared texture renderer instance.
     */
    protected static class InstanceKey
    {
        public InstanceKey (Texture color, Texture depth)
        {
            _color = new WeakReference<Texture>(color);
            _depth = new WeakReference<Texture>(depth);
        }

        @Override
        public int hashCode ()
        {
            return System.identityHashCode(_color.get()) ^
                System.identityHashCode(_depth.get());
        }

        @Override
        public boolean equals (Object other)
        {
            InstanceKey okey = (InstanceKey)other;
            return _color.get() == okey._color.get() && _depth.get() == okey._depth.get();
        }

        /** The color and depth textures. */
        protected WeakReference<Texture> _color, _depth;
    }

    /** The renderer context. */
    protected GlContext _ctx;

    /** The renderer with which we render. */
    protected Renderer _renderer;

    /** The width and height of the render surface. */
    protected int _width, _height;

    /** If true, change the render surface dimensions to match the texture dimensions. */
    protected boolean _matchTextureDimensions;

    /** The requested pixel format. */
    protected PixelFormat _pformat;

    /** The color and/or depth textures to which we render. */
    protected Texture _color, _depth;

    /** The frame buffer object, if supported. */
    protected Framebuffer _framebuffer;

    /** The dimensions of the Pbuffer. */
    protected int _pwidth, _pheight;

    /** The render-to-texture configuration. */
    protected RenderTexture _rtex;

    /** The pbuffer object, if supported. */
    protected Pbuffer _pbuffer;

    /** The mipmap level. */
    protected int _level;

    /** The cube map face index or z offset, as appropriate. */
    protected int _param;

    /** The original viewport. */
    protected Rectangle _oviewport = new Rectangle();

    /** The originally bound frame buffer. */
    protected Framebuffer _obuffer;

    /** The original context renderer. */
    protected Renderer _orenderer;

    /** The original draw and read buffers. */
    protected int _odraw, _oread;

    /** The shared texture renderer instances. */
    protected static Map<InstanceKey, TextureRenderer> _instances = CacheUtil.softValues();
}
