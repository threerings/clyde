//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

import java.awt.EventQueue;
import java.awt.image.BufferedImage;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Controllers;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;

import com.samskivert.util.RunQueue;

import com.threerings.opengl.gui.DisplayRoot;
import com.threerings.opengl.gui.Root;

import static com.threerings.opengl.Log.*;

/**
 * A base class for applications that use LWJGL's {@link Display} class.
 */
public abstract class GlDisplayApp extends GlApp
{
    public GlDisplayApp ()
    {
        // enable vsync unless configured otherwise
        Display.setVSyncEnabled(!Boolean.getBoolean("no_vsync"));
    }

    /**
     * Returns an array containing the available display modes.
     */
    public DisplayMode[] getAvailableDisplayModes ()
    {
        try {
            return Display.getAvailableDisplayModes();
        } catch (LWJGLException e) {
            log.warning("Failed to retrieve available display modes.", e);
            return new DisplayMode[] { Display.getDisplayMode() };
        }
    }

    /**
     * Sets the display mode and updates the viewport if the display is created.
     */
    public void setDisplayMode (DisplayMode mode)
    {
        if (Display.getDisplayMode().equals(mode)) {
            return;
        }
        try {
            Display.setDisplayMode(mode);
            if (Display.isCreated()) {
                _renderer.setSize(mode.getWidth(), mode.getHeight());
            }
        } catch (LWJGLException e) {
            log.warning("Failed to set display mode.", "mode", mode, e);
        }
    }

    /**
     * Sets the display icon.
     *
     * @param paths the resource paths of the icons to set.
     */
    public void setIcon (String... paths)
    {
        ByteBuffer[] icons = new ByteBuffer[paths.length];
        for (int ii = 0; ii < paths.length; ii++) {
            BufferedImage image = _imgcache.getBufferedImage(paths[ii]);
            int[] argb = image.getRGB(
                0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
            ByteBuffer buf = BufferUtils.createByteBuffer(argb.length * 4);
            for (int pixel : argb) {
                buf.put((byte)((pixel >> 16) & 0xFF));
                buf.put((byte)((pixel >> 8) & 0xFF));
                buf.put((byte)(pixel & 0xFF));
                buf.put((byte)((pixel >> 24) & 0xFF));
            }
            buf.rewind();
            icons[ii] = buf;
        }
        Display.setIcon(icons);
    }

    /**
     * Sets the fullscreen mode.
     */
    public void setFullscreen (boolean fullscreen)
    {
        try {
            Display.setFullscreen(fullscreen);
        } catch (LWJGLException e) {
            log.warning("Failed to set fullscreen mode.", "fullscreen", fullscreen, e);
        }
    }

    // documentation inherited from interface GlContext
    public void makeCurrent ()
    {
        try {
            Display.makeCurrent();
        } catch (LWJGLException e) {
            log.warning("Failed to make context current.", e);
        }
    }

    @Override // documentation inherited
    public RunQueue getRunQueue ()
    {
        return RunQueue.AWT;
    }

    @Override // documentation inherited
    public Root createRoot ()
    {
        if (_displayRoot == null) {
            _displayRoot = new DisplayRoot(this);
        }
        return _displayRoot;
    }

    @Override // documentation inherited
    public void startup ()
    {
        // all the work happens in the AWT thread
        EventQueue.invokeLater(new Runnable() {
            public void run () {
                init();
            }
        });
    }

    @Override // documentation inherited
    public void shutdown ()
    {
        willShutdown();
        Keyboard.destroy();
        Mouse.destroy();
        Controllers.destroy();
        Display.destroy();
        System.exit(0);
    }

    @Override // documentation inherited
    protected void init ()
    {
        if (!createDisplay()) {
            return;
        }
        super.init();

        // create the input devices
        try {
            Keyboard.create();
        } catch (LWJGLException e) {
            log.warning("Failed to create keyboard.", e);
        }
        try {
            Mouse.create();
        } catch (LWJGLException e) {
            log.warning("Failed to create mouse.", e);
        }
        try {
            Controllers.create();
        } catch (LWJGLException e) {
            log.warning("Failed to create controllers.", e);
        }

        // start the updater
        final Runnable updater = new Runnable() {
            public void run () {
                if (Display.isCloseRequested()) {
                    shutdown();
                } else {
                    makeCurrent();
                    updateFrame();
                    EventQueue.invokeLater(this);
                }
            }
        };
        EventQueue.invokeLater(updater);
    }

    @Override // documentation inherited
    protected void willShutdown ()
    {
        if (_displayRoot != null) {
            _displayRoot.dispose();
            _displayRoot = null;
        }
        super.willShutdown();
    }

    @Override // documentation inherited
    protected void initRenderer ()
    {
        DisplayMode mode = Display.getDisplayMode();
        _renderer.init(Display.getDrawable(), mode.getWidth(), mode.getHeight());
    }

    /**
     * Creates the display with one of the supported pixel formats.
     *
     * @return true if successful, false if we couldn't find a valid pixel format.
     */
    protected boolean createDisplay ()
    {
        for (PixelFormat format : PIXEL_FORMATS) {
            try {
                Display.create(format);
                return true;
            } catch (LWJGLException e) {
                // proceed to next format
            }
        }
        log.warning("Couldn't find valid pixel format.");

        // if we're in fullscreen mode, try switching to windowed mode and trying again
        if (Display.isFullscreen()) {
            try {
                Display.setFullscreen(false);
                log.info("Trying again in windowed mode.");
                return createDisplay();

            } catch (LWJGLException e) {
                log.warning("Failed to disable fullscreen mode.", e);
            }
        }

        return false;
    }

    /**
     * Updates and renders a single frame.
     */
    protected void updateFrame ()
    {
        try {
            updateView();
            if (Display.isVisible()) {
                renderView();
            }
            Display.update();

        } catch (Exception e) {
            log.warning("Caught exception in frame loop.", e);
        }
    }

    /** Our root. */
    protected Root _displayRoot;
}
