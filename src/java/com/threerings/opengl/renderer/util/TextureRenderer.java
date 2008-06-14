//
// $Id$

package com.threerings.opengl.renderer.util;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ARBDepthTexture;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.opengl.RenderTexture;

import com.threerings.opengl.renderer.Framebuffer;
import com.threerings.opengl.renderer.Renderbuffer;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.Texture2D;

import static com.threerings.opengl.Log.*;

/**
 * Provides render-to-texture functionality using various methods according to the abilities
 * of the driver.
 */
public class TextureRenderer
{
    /** The type of texture to render. */
    public enum Type { RGB, RGBA, DEPTH };

    /**
     * Creates a new texture renderer with the specified width, height, pixel format, and texture
     * type.
     */
    public TextureRenderer (
        Renderer renderer, int width, int height, PixelFormat pformat, Type type)
    {
        _renderer = renderer;
        _texture = new Texture2D(_renderer);

        // first try fbos
        if (GLContext.getCapabilities().GL_EXT_framebuffer_object) {
            _framebuffer = new Framebuffer(renderer);

            // attach the texture
            int dbits = pformat.getDepthBits();
            if (type == Type.DEPTH) {
                _texture.setImage(getDepthFormat(dbits), width, height, false, false);
                _framebuffer.setDepthAttachment(_texture);
            } else {
                _texture.setImage(
                    type == Type.RGB ? GL11.GL_RGB : GL11.GL_RGBA, width, height, false, false);
                _framebuffer.setColorAttachment(_texture);
            }

            // attach a depth buffer if desired
            if (type != Type.DEPTH && dbits > 0) {
                Renderbuffer depth = new Renderbuffer(renderer);
                depth.setStorage(getDepthFormat(dbits), width, height);
                _framebuffer.setDepthAttachment(depth);
            }

            // and a stencil buffer
            int sbits = pformat.getStencilBits();
            if (sbits > 0) {
                Renderbuffer stencil = new Renderbuffer(renderer);
                stencil.setStorage(getStencilFormat(sbits), width, height);
                _framebuffer.setStencilAttachment(stencil);
            }
            return;
        }

        // then try pbuffers with or without rtt
        int pcaps = Pbuffer.getCapabilities();
        if ((pcaps & Pbuffer.PBUFFER_SUPPORTED) != 0) {
            RenderTexture rtex = null;
            if ((pcaps & Pbuffer.RENDER_TEXTURE_SUPPORTED) != 0 &&
                !(type == Type.DEPTH && (pcaps & Pbuffer.RENDER_DEPTH_TEXTURE_SUPPORTED) == 0)) {
                rtex = new RenderTexture(
                    type == Type.RGB, type == Type.RGBA, type == Type.DEPTH,
                    false, GL11.GL_TEXTURE_2D, 0);
            }
            try {
                _pbuffer = new Pbuffer(width, height, pformat, rtex, renderer.getDrawable());
                _renderer = new Renderer();
                _renderer.init(_pbuffer, width, height);

            } catch (LWJGLException e) {
                log.warning("Failed to create pbuffer.", e);
            }
        }
    }

    /**
     * Returns a reference to the texture object.
     */
    public Texture2D getTexture ()
    {
        return _texture;
    }

    /**
     * Returns a reference to the renderer to use.
     */
    public Renderer getRenderer ()
    {
        return _renderer;
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

    /** The texture to which we render. */
    protected Texture2D _texture;

    /** The renderer with which we render. */
    protected Renderer _renderer;

    /** The frame buffer object, if supported. */
    protected Framebuffer _framebuffer;

    /** The pbuffer object, if supported. */
    protected Pbuffer _pbuffer;

    /** Whether or not we are rendering to texture, if using a pbuffer. */
    protected boolean _rtt;
}
