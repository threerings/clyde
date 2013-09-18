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

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBTextureCubeMap;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * An OpenGL frame buffer object.
 */
public class Framebuffer
{
    /**
     * Creates a frame buffer object for the specified renderer.
     */
    public Framebuffer (Renderer renderer)
    {
        _renderer = renderer;
        IntBuffer idbuf = BufferUtils.createIntBuffer(1);
        EXTFramebufferObject.glGenFramebuffersEXT(idbuf);
        _id = idbuf.get(0);
    }

    /**
     * Returns this frame buffer's OpenGL identifier.
     */
    public final int getId ()
    {
        return _id;
    }

    /**
     * Attaches a texture to the color target of this frame buffer.
     */
    public void setColorAttachment (Texture texture)
    {
        setColorAttachment(texture, 0, 0);
    }

    /**
     * Attaches a texture to the color target of this frame buffer.
     *
     * @param level the mipmap level.
     * @param param for depth textures, the z offset; for cube map textures, the face index.
     */
    public void setColorAttachment (Texture texture, int level, int param)
    {
        if (_colorAttachment != texture || _colorLevel != level || _colorParam != param) {
            setAttachment(
                EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, texture,
                _colorLevel = level, _colorParam = param);
            _colorAttachment = texture;
        }
    }

    /**
     * Attaches a render buffer to the color target of this frame buffer.
     */
    public void setColorAttachment (Renderbuffer renderbuffer)
    {
        if (_colorAttachment != renderbuffer) {
            setAttachment(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, renderbuffer);
            _colorAttachment = renderbuffer;
        }
    }

    /**
     * Returns a reference to the color target of this frame buffer (either a {@link Texture} or
     * a {@link Renderbuffer}, or <code>null</code>).
     */
    public Object getColorAttachment ()
    {
        return _colorAttachment;
    }

    /**
     * Attaches a texture to the depth target of this frame buffer.
     */
    public void setDepthAttachment (Texture texture)
    {
        setDepthAttachment(texture, 0, 0);
    }

    /**
     * Attaches a texture to the depth target of this frame buffer.
     *
     * @param level the mipmap level.
     * @param param for depth textures, the z offset; for cube map textures, the face index.
     */
    public void setDepthAttachment (Texture texture, int level, int param)
    {
        if (_depthAttachment != texture || _depthLevel != level || _depthParam != param) {
            setAttachment(
                EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, texture,
                _depthLevel = level, _depthParam = param);
            _depthAttachment = texture;
        }
    }

    /**
     * Attaches a render buffer to the depth target of this frame buffer.
     */
    public void setDepthAttachment (Renderbuffer renderbuffer)
    {
        if (_depthAttachment != renderbuffer) {
            setAttachment(EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, renderbuffer);
            _depthAttachment = renderbuffer;
        }
    }

    /**
     * Returns a reference to the depth target of this frame buffer (either a {@link Texture} or
     * a {@link Renderbuffer}, or <code>null</code>).
     */
    public Object getDepthAttachment ()
    {
        return _depthAttachment;
    }

    /**
     * Attaches a texture to the stencil target of this frame buffer.
     */
    public void setStencilAttachment (Texture texture)
    {
        setStencilAttachment(texture, 0, 0);
    }

    /**
     * Attaches a texture to the stencil target of this frame buffer.
     *
     * @param level the mipmap level.
     * @param param for depth textures, the z offset; for cube map textures, the face index.
     */
    public void setStencilAttachment (Texture texture, int level, int param)
    {
        if (_stencilAttachment != texture || _stencilLevel != level || _stencilParam != param) {
            setAttachment(
                EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT, texture,
                _stencilLevel = level, _stencilParam = param);
            _stencilAttachment = texture;
        }
    }

    /**
     * Attaches a render buffer to the stencil target of this frame buffer.
     */
    public void setStencilAttachment (Renderbuffer renderbuffer)
    {
        if (_stencilAttachment != renderbuffer) {
            setAttachment(EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT, renderbuffer);
            _stencilAttachment = renderbuffer;
        }
    }

    /**
     * Returns a reference to the stencil target of this frame buffer (either a {@link Texture} or
     * a {@link Renderbuffer}, or <code>null</code>).
     */
    public Object getStencilAttachment ()
    {
        return _stencilAttachment;
    }

    /**
     * Checks whether the frame buffer is "complete."
     */
    public boolean isComplete ()
    {
        return checkStatus() == EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT;
    }

    /**
     * Checks the status of the frame buffer.
     */
    public int checkStatus ()
    {
        Framebuffer obuffer = _renderer.getFramebuffer();
        _renderer.setFramebuffer(this);
        int status = EXTFramebufferObject.glCheckFramebufferStatusEXT(
            EXTFramebufferObject.GL_FRAMEBUFFER_EXT);
        _renderer.setFramebuffer(obuffer);
        return status;
    }

    /**
     * Deletes this frame buffer, rendering it unusable.
     */
    public void delete ()
    {
        IntBuffer idbuf = BufferUtils.createIntBuffer(1);
        idbuf.put(_id).rewind();
        EXTFramebufferObject.glDeleteFramebuffersEXT(idbuf);
        _id = 0;
    }

    /**
     * Attaches a texture to this frame buffer at the specified attachment point.
     *
     * @param level the mipmap level.
     * @param param depending on the texture type, either the 3D texture z offset or the
     * cube map face index.
     */
    protected void setAttachment (int attachment, Texture texture, int level, int param)
    {
        Framebuffer obuffer = _renderer.getFramebuffer();
        _renderer.setFramebuffer(this);
        int target = texture.getTarget();
        if (target == GL11.GL_TEXTURE_1D) {
            EXTFramebufferObject.glFramebufferTexture1DEXT(
                EXTFramebufferObject.GL_FRAMEBUFFER_EXT, attachment,
                GL11.GL_TEXTURE_1D, texture.getId(), level);
        } else if (target == GL12.GL_TEXTURE_3D) {
            EXTFramebufferObject.glFramebufferTexture3DEXT(
                EXTFramebufferObject.GL_FRAMEBUFFER_EXT, attachment,
                GL12.GL_TEXTURE_3D, texture.getId(), level, param);
        } else { // GL_TEXTURE_2D, GL_TEXTURE_RECTANGLE_ARB, GL_TEXTURE_CUBE_MAP_ARB
            EXTFramebufferObject.glFramebufferTexture2DEXT(
                EXTFramebufferObject.GL_FRAMEBUFFER_EXT, attachment,
                (target == ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB) ?
                    TextureCubeMap.FACE_TARGETS[param] : target,
                texture.getId(), level);
        }
        _renderer.setFramebuffer(obuffer);
    }

    /**
     * Attaches a render buffer to this frame buffer at the specified attachment point.
     */
    protected void setAttachment (int attachment, Renderbuffer renderbuffer)
    {
        Framebuffer obuffer = _renderer.getFramebuffer();
        _renderer.setFramebuffer(this);
        EXTFramebufferObject.glFramebufferRenderbufferEXT(
            EXTFramebufferObject.GL_FRAMEBUFFER_EXT, attachment,
            EXTFramebufferObject.GL_RENDERBUFFER_EXT, renderbuffer.getId());
        _renderer.setFramebuffer(obuffer);
    }

    @Override
    protected void finalize ()
        throws Throwable
    {
        super.finalize();
        if (_id > 0) {
            _renderer.framebufferFinalized(_id);
        }
    }

    /** The renderer responsible for this frame buffer. */
    protected Renderer _renderer;

    /** The OpenGL identifier for the frame buffer. */
    protected int _id;

    /** The texture or render buffer attached to the color target. */
    protected Object _colorAttachment;

    /** The mipmap level and parameter of the texture attached to the color target. */
    protected int _colorLevel, _colorParam;

    /** The texture or render buffer attached to the depth target. */
    protected Object _depthAttachment;

    /** The mipmap level and parameter of the texture attached to the depth target. */
    protected int _depthLevel, _depthParam;

    /** The texture or render buffer attached to the stencil target. */
    protected Object _stencilAttachment;

    /** The mipmap level and parameter of the texture attached to the stencil target. */
    protected int _stencilLevel, _stencilParam;
}
