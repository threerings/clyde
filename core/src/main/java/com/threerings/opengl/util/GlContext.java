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
import com.threerings.editor.util.EditorContext;
import com.threerings.expr.DynamicScope;
import com.threerings.media.image.ColorPository;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;

import com.threerings.openal.util.AlContext;
import com.threerings.opengl.GlApp;
import com.threerings.opengl.camera.CameraHandler;
import com.threerings.opengl.compositor.Compositor;
import com.threerings.opengl.renderer.Renderer;

/**
 * Provides access to the various components of the OpenGL rendering system.  Not to be confused
 * with LWJGL's {@link org.lwjgl.opengl.GLContext}.
 */
public interface GlContext extends AlContext, EditorContext
{
    /**
     * Returns a reference to the application object.
     */
    public GlApp getApp ();

    /**
     * Returns a reference to the scope.
     */
    public DynamicScope getScope ();

    /**
     * Makes the OpenGL context current.
     */
    public void makeCurrent ();

    /**
     * Sets the renderer reference.
     */
    public void setRenderer (Renderer renderer);

    /**
     * Returns a reference to the renderer.
     */
    public Renderer getRenderer ();

    /**
     * Returns a reference to the compositor.
     */
    public Compositor getCompositor ();

    /**
     * Sets the camera handler reference.
     */
    public void setCameraHandler (CameraHandler camhand);

    /**
     * Returns a reference to the camera handler.
     */
    public CameraHandler getCameraHandler ();

    /**
     * Returns a reference to the resource manager.
     */
    public ResourceManager getResourceManager ();

    /**
     * Returns a reference to the message manager.
     */
    public MessageManager getMessageManager ();

    /**
     * Returns a reference to the configuration manager.
     */
    public ConfigManager getConfigManager ();

    /**
     * Returns a reference to the color pository.
     */
    public ColorPository getColorPository ();

    /**
     * Returns a reference to the image cache.
     */
    public ImageCache getImageCache ();

    /**
     * Returns a reference to the shader cache.
     */
    public ShaderCache getShaderCache ();
}
