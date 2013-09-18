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

package com.threerings.opengl.util;

import com.threerings.config.ConfigManager;
import com.threerings.expr.DynamicScope;
import com.threerings.media.image.ColorPository;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;

import com.threerings.openal.ClipProvider;
import com.threerings.openal.SoundManager;
import com.threerings.opengl.GlApp;
import com.threerings.opengl.camera.CameraHandler;
import com.threerings.opengl.compositor.Compositor;
import com.threerings.opengl.renderer.Renderer;

/**
 * Wraps another {@link GlContext}, allowing subclasses to override methods selectively.
 */
public abstract class GlContextWrapper
    implements GlContext
{
    /**
     * Unwraps the provided context layer by layer until the base is found.
     */
    public static GlContext getBase (GlContext ctx)
    {
        return (ctx instanceof GlContextWrapper) ?
            getBase(((GlContextWrapper)ctx).getWrapped()) : ctx;
    }

    /**
     * Creates a new wrapper to wrap the specified context.
     */
    public GlContextWrapper (GlContext wrapped)
    {
        _wrapped = wrapped;
    }

    /**
     * Returns a reference to the wrapped context.
     */
    public GlContext getWrapped ()
    {
        return _wrapped;
    }

    // documentation inherited from interface GlContext
    public GlApp getApp ()
    {
        return _wrapped.getApp();
    }

    // documentation inherited from interface AlContext, GlContext
    public DynamicScope getScope ()
    {
        return _wrapped.getScope();
    }

    // documentation inherited from interfaces AlContext, GlContext, EditorContext
    public ConfigManager getConfigManager ()
    {
        return _wrapped.getConfigManager();
    }

    // documentation inherited from interface AlContext
    public SoundManager getSoundManager ()
    {
        return _wrapped.getSoundManager();
    }

    // documentation inherited from interface AlContext
    public ClipProvider getClipProvider ()
    {
        return _wrapped.getClipProvider();
    }

    // documentation inherited from interface GlContext
    public void makeCurrent ()
    {
        _wrapped.makeCurrent();
    }

    // documentation inherited from interface GlContext
    public void setRenderer (Renderer renderer)
    {
        _wrapped.setRenderer(renderer);
    }

    // documentation inherited from interface GlContext
    public Renderer getRenderer ()
    {
        return _wrapped.getRenderer();
    }

    // documentation inherited from interface GlContext
    public Compositor getCompositor ()
    {
        return _wrapped.getCompositor();
    }

    // documentation inherited from interface GlContext
    public void setCameraHandler (CameraHandler camhand)
    {
        _wrapped.setCameraHandler(camhand);
    }

    // documentation inherited from interface GlContext
    public CameraHandler getCameraHandler ()
    {
        return _wrapped.getCameraHandler();
    }

    // documentation inherited from interface GlContext
    public ResourceManager getResourceManager ()
    {
        return _wrapped.getResourceManager();
    }

    // documentation inherited from interface GlContext
    public MessageManager getMessageManager ()
    {
        return _wrapped.getMessageManager();
    }

    // documentation inherited from interface GlContext
    public ColorPository getColorPository ()
    {
        return _wrapped.getColorPository();
    }

    // documentation inherited from interface GlContext
    public ImageCache getImageCache ()
    {
        return _wrapped.getImageCache();
    }

    // documentation inherited from interface GlContext
    public ShaderCache getShaderCache ()
    {
        return _wrapped.getShaderCache();
    }

    /** The wrapped context. */
    protected GlContext _wrapped;
}
