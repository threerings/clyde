//
// $Id$

package com.threerings.opengl;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;

import com.samskivert.util.Queue;

import com.threerings.opengl.gui.Root;

import static com.threerings.opengl.Log.*;

/**
 * A base class for applications that use LWJGL's {@link Display} class.
 */
public abstract class GlDisplayApp extends GlApp
{
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

    // documentation inherited from interface RunQueue
    public void postRunnable (Runnable run)
    {
        _queue.append(run);
    }

    // documentation inherited from interface RunQueue
    public boolean isDispatchThread ()
    {
        return Thread.currentThread() == _dispatchThread;
    }

    @Override // documentation inherited
    public Root createRoot ()
    {
        return null;
    }

    @Override // documentation inherited
    public void startup ()
    {
        if (!createDisplay()) {
            return;
        }
        // set up
        _dispatchThread = Thread.currentThread();
        _shutdownRequested = false;
        init();

        // run main loop
        while (!(Display.isCloseRequested() || _shutdownRequested)) {
            try {
                Runnable runnable;
                while ((runnable = _queue.getNonBlocking()) != null) {
                    runnable.run();
                }
                updateView();
                if (Display.isVisible()) {
                    renderView();
                }
                Display.update();

            } catch (Exception e) {
                log.warning("Caught exception in frame loop.", e);
            }
        }

        // clean up
        willShutdown();
        Display.destroy();
        _dispatchThread = null;
    }

    @Override // documentation inherited
    public void shutdown ()
    {
        _shutdownRequested = true;
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
        return false;
    }

    /** The dispatch thread. */
    protected Thread _dispatchThread;

    /** The queue of things to run. */
    protected Queue<Runnable> _queue = new Queue<Runnable>();

    /** Set when shutdown is desired. */
    protected boolean _shutdownRequested;
}
