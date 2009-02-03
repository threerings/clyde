//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import javax.swing.JPopupMenu;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.AWTGLCanvas;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.opengl.Util;

import com.samskivert.util.Interval;
import com.samskivert.util.RunQueue;

import static com.threerings.opengl.Log.*;

/**
 * A canvas for OpenGL rendering.
 */
public class GlCanvas extends AWTGLCanvas
{
    /**
     * Creates a canvas with the supplied pixel format.
     */
    public GlCanvas (PixelFormat pformat)
        throws LWJGLException
    {
        super(pformat);

        // make popups heavyweight so that we can see them over the canvas
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    }

    /**
     * Creates a canvas on the specified device with the supplied pixel format.
     */
    public GlCanvas (GraphicsDevice device, PixelFormat pformat)
        throws LWJGLException
    {
        super(device, pformat);
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    }

    @Override // documentation inherited
    public void makeCurrent ()
    {
        try {
            super.makeCurrent();
        } catch (LWJGLException e) {
            log.warning("Failed to make context current.", e);
        }
    }

    @Override // documentation inherited
    public void swapBuffers ()
    {
        try {
            super.swapBuffers();
        } catch (LWJGLException e) {
            log.warning("Error swapping buffers.", e);
        }
    }

    @Override // documentation inherited
    public void removeNotify ()
    {
        super.removeNotify();
        stopUpdating();
    }

    @Override // documentation inherited
    protected void initGL ()
    {
        // initialize outside of LWJGL's context window
        EventQueue.invokeLater(new Runnable() {
            public void run () {
                init();
            }
        });
    }

    /**
     * Called once the canvas has a valid OpenGL context.
     */
    protected void init ()
    {
        // make the context current once and for all
        makeCurrent();

        // now that we're initialized, make sure we don't call LWJGL's paint method
        disableEvents(AWTEvent.PAINT_EVENT_MASK);
        setIgnoreRepaint(true);

        // let subclasses do their own initialization
        didInit();

        // start rendering frames
        startUpdating();
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
    }

    /**
     * Starts calling {@link #updateFrame} regularly.
     */
    protected void startUpdating ()
    {
        (_updater = new Interval(RunQueue.AWT) {
            public void expired () {
                updateFrame();
                schedule(1L);
            }
        }).schedule(1L);
    }

    /**
     * Stops calling {@link #updateFrame}.
     */
    protected void stopUpdating ()
    {
        if (_updater != null) {
            _updater.cancel();
            _updater = null;
        }
    }

    /**
     * Updates and, if the canvas is showing, renders the scene and swaps the buffers.
     */
    protected void updateFrame ()
    {
        try {
            updateView();
            if (isShowing()) {
                renderView();
                swapBuffers();
            }
            Util.checkGLError();

        } catch (Exception e) {
            log.warning("Caught exception in frame loop.", e);
        }
    }

    /**
     * Override to perform any updates that are required even if not rendering.
     */
    protected void updateView ()
    {
    }

    /**
     * Override to render the contents of the canvas.
     */
    protected void renderView ()
    {
    }

    /** The interval that updates the frame. */
    protected Interval _updater;
}
