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

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.lang.reflect.Field;

import javax.swing.JPopupMenu;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.AWTGLCanvas;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.opengl.Util;

import static com.threerings.opengl.Log.log;

/**
 * A canvas that extends {@link AWTGLCanvas}.
 */
public class AWTCanvas extends AWTGLCanvas
    implements GlCanvas
{
    /**
     * Creates a canvas with the supplied pixel format.
     */
    public AWTCanvas (PixelFormat pformat)
        throws LWJGLException
    {
        super(pformat);

        // make popups heavyweight so that we can see them over the canvas
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    }

    // documentation inherited from interface GlCanvas
    public Drawable getDrawable ()
    {
        return this;
    }

    // documentation inherited from interface GlCanvas
    public void shutdown ()
    {
        // no-op
    }

    @Override
    public void makeCurrent ()
    {
        try {
            super.makeCurrent();
        } catch (LWJGLException e) {
            log.warning("Failed to make context current.", e);
        }
    }

    @Override
    public void swapBuffers ()
    {
        try {
            super.swapBuffers();
        } catch (LWJGLException e) {
            log.warning("Error swapping buffers.", e);
        }
    }

    @Override
    public void removeNotify ()
    {
        super.removeNotify();
        stopUpdating();
    }

    @Override
    protected void initGL ()
    {
        // hackery: increment the reentry count so that the context is never released
        try {
            Field field = AWTGLCanvas.class.getDeclaredField("reentry_count");
            field.setAccessible(true);
            field.setInt(this, 2);
        } catch (Exception e) {
            log.warning("Failed to access field.", e);
        }

        // now that we're initialized, make sure AWT doesn't call our paint method
        disableEvents(AWTEvent.PAINT_EVENT_MASK);
        setIgnoreRepaint(true);

        // let subclasses do their own initialization
        didInit();

        // start rendering frames
        startUpdating();
    }

    @Override
    protected void paintGL ()
    {
        renderView();
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
        _updater = new Runnable() {
            public void run () {
                if (_updater != null) {
                    updateFrame();
                    EventQueue.invokeLater(this);
                }
            }
        };
        EventQueue.invokeLater(_updater);
    }

    /**
     * Stops calling {@link #updateFrame}.
     */
    protected void stopUpdating ()
    {
        _updater = null;
    }

    /**
     * Updates and, if the canvas is showing, renders the scene and swaps the buffers.
     */
    protected void updateFrame ()
    {
        try {
            updateView();
            if (isShowing()) {
                // LWJGL's paint method obtains a lock on the AWT surface; without it, we crash
                // due to failed synchronization-related assertions on Linux
                paint(null);
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

    /** The runnable that updates the frame. */
    protected Runnable _updater;
}
