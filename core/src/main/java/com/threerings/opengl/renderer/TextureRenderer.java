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

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL;

import com.threerings.util.CacheUtil;
import com.threerings.opengl.camera.Camera;
import com.threerings.opengl.gui.util.Rectangle;
import com.threerings.opengl.lwjgl2.PixelFormat;
import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.Log.log;

/**
 * Provides render-to-texture functionality using FBOs.
 * In LWJGL 3, Pbuffer and RenderTexture are no longer available;
 * FBO-based rendering is used exclusively.
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

    if (width == -1) {
      _width = twidth;
      _height = theight;
      _matchTextureDimensions = true;
    } else {
      _width = width;
      _height = height;
    }

    // Use FBO-based rendering (the only option in LWJGL 3)
    initFramebuffer(twidth, theight);
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
   * Stops rendering to the texture.
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
    } else {
      copyTextures();
    }
  }

  /**
   * Disposes of this texture renderer.
   */
  public void dispose ()
  {
    if (_framebuffer != null) {
      deleteFramebuffer(_framebuffer);
      _framebuffer = null;
    }
  }

  /**
   * Initializes the FBO for rendering.
   */
  protected void initFramebuffer (int twidth, int theight)
  {
    Framebuffer obuffer = _renderer.getFramebuffer();
    _renderer.setFramebuffer(_framebuffer = new Framebuffer(_renderer));

    if (_color != null) {
      _framebuffer.setColorAttachment(_color);
    }
    if (_depth != null) {
      _framebuffer.setDepthAttachment(_depth);
    } else {
      int dbits = _pformat.depthBits;
      if (dbits > 0) {
        Renderbuffer dbuf = new Renderbuffer(_renderer);
        dbuf.setStorage(GL11.GL_DEPTH_COMPONENT, twidth, theight);
        _framebuffer.setDepthAttachment(dbuf);
      }
    }
    int sbits = _pformat.stencilBits;
    if (sbits > 0) {
      Renderbuffer sbuf = new Renderbuffer(_renderer);
      sbuf.setStorage(GL11.GL_STENCIL_INDEX, twidth, theight);
      _framebuffer.setStencilAttachment(sbuf);
    }
    if (_color == null) {
      GL11.glDrawBuffer(GL11.GL_NONE);
      GL11.glReadBuffer(GL11.GL_NONE);
    }

    int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
    _renderer.setFramebuffer(obuffer);

    if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
      log.warning("Framebuffer incomplete.", "status", status);
      deleteFramebuffer(_framebuffer);
      _framebuffer = null;
    }
  }

  /**
   * Resizes the texture render state.
   */
  protected void resize (int width, int height)
  {
    _width = width;
    _height = height;
    if (_framebuffer != null) {
      if (_depth == null && _pformat.depthBits > 0) {
        Renderbuffer dbuf = new Renderbuffer(_renderer);
        dbuf.setStorage(GL11.GL_DEPTH_COMPONENT, width, height);
        _framebuffer.setDepthAttachment(dbuf);
      }
      if (_pformat.stencilBits > 0) {
        Renderbuffer sbuf = new Renderbuffer(_renderer);
        sbuf.setStorage(GL11.GL_STENCIL_INDEX, width, height);
        _framebuffer.setStencilAttachment(sbuf);
      }
    }
  }

  /**
   * Copies the textures from the buffer (fallback when FBO is not available).
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
      default: case 16: return GL14.GL_DEPTH_COMPONENT16;
      case 24: return GL14.GL_DEPTH_COMPONENT24;
      case 32: return GL14.GL_DEPTH_COMPONENT32;
    }
  }

  /**
   * Returns the stencil format with the specified number of bits.
   */
  protected static int getStencilFormat (int bits)
  {
    switch (bits) {
      default: case 1: return GL30.GL_STENCIL_INDEX1;
      case 4: return GL30.GL_STENCIL_INDEX4;
      case 8: return GL30.GL_STENCIL_INDEX8;
      case 16: return GL30.GL_STENCIL_INDEX16;
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

    protected WeakReference<Texture> _color, _depth;
  }

  protected GlContext _ctx;
  protected Renderer _renderer;
  protected int _width, _height;
  protected boolean _matchTextureDimensions;
  protected PixelFormat _pformat;
  protected Texture _color, _depth;
  protected Framebuffer _framebuffer;
  protected int _level;
  protected int _param;
  protected Rectangle _oviewport = new Rectangle();
  protected Framebuffer _obuffer;
  protected int _odraw, _oread;

  protected static Map<InstanceKey, TextureRenderer> _instances = CacheUtil.softValues();
}
