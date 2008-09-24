//
// $Id$

package com.threerings.opengl.renderer;

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

import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.Log.*;

/**
 * Provides render-to-texture functionality using various methods according to the abilities
 * of the driver.
 */
public class TextureRenderer
{
    /**
     * Creates a new texture renderer to render into the specified texture(s).
     */
    public TextureRenderer (
        GlContext ctx, int width, int height, Texture color, Texture depth, PixelFormat pformat)
    {
        _ctx = ctx;
        _renderer = ctx.getRenderer();
        _width = width;
        _height = height;
        _color = color;
        _depth = depth;
        int twidth = color.getWidth(), theight = color.getHeight();

        // first try fbos
        if (GLContext.getCapabilities().GL_EXT_framebuffer_object) {
            _framebuffer = new Framebuffer(_renderer);

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
                    dbuf.setStorage(getDepthFormat(dbits), twidth, theight);
                    _framebuffer.setDepthAttachment(dbuf);
                }
            }

            // add a stencil buffer if requested
            int sbits = pformat.getStencilBits();
            if (sbits > 0) {
                Renderbuffer sbuf = new Renderbuffer(_renderer);
                sbuf.setStorage(getStencilFormat(sbits), twidth, theight);
                _framebuffer.setStencilAttachment(sbuf);
            }
            return;
        }

        // then try pbuffers with or without rtt
        int pcaps = Pbuffer.getCapabilities();
        if ((pcaps & Pbuffer.PBUFFER_SUPPORTED) != 0) {
            int target = getRenderTextureTarget(
                color == null ? depth.getTarget() : color.getTarget());
            boolean rectangle = (target == RenderTexture.RENDER_TEXTURE_RECTANGLE);
            _pwidth = width;
            _pheight = height;
            _pformat = pformat;
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
     * Starts rendering to the texture.
     */
    public void startRender ()
    {
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
    }

    /**
     * Stops rendering to the texture (and makes it available for use).
     */
    public void commitRender ()
    {
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
        _renderer.setTexture(texture);
        if (texture instanceof Texture2D) {
            GL11.glCopyTexSubImage2D(texture.getTarget(), 0, 0, 0, 0, 0, _width, _height);
        }
    }

    @Override // documentation inherited
    protected void finalize ()
        throws Throwable
    {
        super.finalize();
        if (_pbuffer != null) {
            _renderer.pbufferFinalized(_pbuffer);
        }
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

    /** The renderer context. */
    protected GlContext _ctx;

    /** The renderer with which we render. */
    protected Renderer _renderer;

    /** The width and height of the render surface. */
    protected int _width, _height;

    /** The color and/or depth textures to which we render. */
    protected Texture _color, _depth;

    /** The frame buffer object, if supported. */
    protected Framebuffer _framebuffer;

    /** The dimensions of the Pbuffer. */
    protected int _pwidth, _pheight;

    /** The format of the Pbuffer. */
    protected PixelFormat _pformat;

    /** The render-to-texture configuration. */
    protected RenderTexture _rtex;

    /** The pbuffer object, if supported. */
    protected Pbuffer _pbuffer;

    /** The originally bound frame buffer. */
    protected Framebuffer _obuffer;

    /** The original context renderer. */
    protected Renderer _orenderer;

    /** The original draw and read buffers. */
    protected int _odraw, _oread;
}
