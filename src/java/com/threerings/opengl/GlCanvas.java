//
// $Id$

package com.threerings.opengl;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.util.logging.Level;
import javax.swing.JPopupMenu;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.AWTGLCanvas;
import org.lwjgl.opengl.PixelFormat;

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
            log.log(Level.WARNING, "Failed to make context current.", e);
        }
    }

    @Override // documentation inherited
    public void swapBuffers ()
    {
        try {
            super.swapBuffers();
        } catch (LWJGLException e) {
            log.log(Level.WARNING, "Error swapping buffers.", e);
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
        updateScene();
        if (isShowing()) {
            renderScene();
            swapBuffers();
        }
    }

    /**
     * Override to perform any updates that are required even if not rendering.
     */
    protected void updateScene ()
    {
    }

    /**
     * Override to render the canvas scene.
     */
    protected void renderScene ()
    {
    }

    /** The interval that updates the frame. */
    protected Interval _updater;
}
