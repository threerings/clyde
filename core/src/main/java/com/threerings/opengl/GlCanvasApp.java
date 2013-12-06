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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.PixelFormat;

import com.samskivert.swing.util.SwingUtil;
import com.samskivert.util.RunAnywhere;
import com.samskivert.util.RunQueue;

import com.threerings.util.KeyboardManager;
import com.threerings.util.KeyTranslatorImpl;

import com.threerings.math.Ray3D;

import com.threerings.opengl.gui.CanvasRoot;
import com.threerings.opengl.gui.Root;
import static com.threerings.opengl.Log.log;

/**
 * A base class for applications centered around an OpenGL canvas.
 */
public abstract class GlCanvasApp extends GlApp
{
    public GlCanvasApp ()
    {
        _frame = new JFrame();
        _frame.setSize(1024, 768);
        SwingUtil.centerWindow(_frame);

        // shutdown the application when the window is closed
        _frame.addWindowListener(new WindowAdapter() {
            public void windowClosing (WindowEvent event) {
                shutdown();
            }
        });

        // add the canvas inside a container so that we can use KeyboardManager
        if ((_canvas = createCanvas()) == null) {
            return;
        }
        JComponent cont = createCanvasContainer();
        _frame.add(cont, BorderLayout.CENTER);

        // create the keyboard manager
        _keymgr = new KeyboardManager();
        _keymgr.setTarget(cont, new KeyTranslatorImpl());

        // as a hack, add a focus listener that disables the keyboard manager when a text
        // component is in focus so that we get the native key repeat
        PropertyChangeListener pcl = new PropertyChangeListener() {
            public void propertyChange (PropertyChangeEvent event) {
                _keymgr.setEnabled(!(event.getNewValue() instanceof JTextComponent));
            }
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(
            "focusOwner", pcl);
    }

    /**
     * Returns a reference to the containing frame.
     */
    public JFrame getFrame ()
    {
        return _frame;
    }

    /**
     * Returns a reference to the canvas.
     */
    public Component getCanvas ()
    {
        return _canvas;
    }

    /**
     * Gets the ray through the canvas's mouse position.
     *
     * @return true if the mouse cursor is on the canvas (in which case the result will be
     * populated), false if it is not on the canvas.
     */
    public boolean getMouseRay (Ray3D result)
    {
        Point pt = _canvas.getMousePosition();
        if (pt == null) {
            return false;
        }
        getPickRay(pt.x, pt.y, result);
        return true;
    }

    /**
     * Finds the ray through the specified canvas coordinates.
     */
    public void getPickRay (int x, int y, Ray3D result)
    {
        // flip vertically to convert to viewport coordinates
        _compositor.getCamera().getPickRay(x, _canvas.getHeight() - y - 1, result);
    }

    // documentation inherited from interface GlContext
    public void makeCurrent ()
    {
        ((GlCanvas)_canvas).makeCurrent();
    }

    @Override
    public RunQueue getRunQueue ()
    {
        return RunQueue.AWT;
    }

    @Override
    public Root createRoot ()
    {
        if (_canvasRoot == null) {
            _canvasRoot = new CanvasRoot(this, _canvas);
        }
        return _canvasRoot;
    }

    @Override
    public void startup ()
    {
        _frame.setVisible(true);
    }

    @Override
    public void shutdown ()
    {
        willShutdown();
        ((GlCanvas)_canvas).shutdown();
        System.exit(0);
    }

    @Override
    protected void initRenderer ()
    {
        _renderer.init(((GlCanvas)_canvas).getDrawable(), _canvas.getWidth(), _canvas.getHeight());
    }

    @Override
    protected void didInit ()
    {
        // enable vsync unless configured otherwise
        ((GlCanvas)_canvas).setVSyncEnabled(!Boolean.getBoolean("no_vsync"));

        // notify the renderer on resize
        _canvas.addComponentListener(new ComponentAdapter() {
            public void componentResized (ComponentEvent event) {
                _renderer.setSize(_canvas.getWidth(), _canvas.getHeight());
            }
        });

        // request focus for the canvas
        _canvas.requestFocusInWindow();

        // enable the keyboard manager
        _keymgr.setEnabled(true);
    }

    @Override
    protected void willShutdown ()
    {
        if (_canvasRoot != null) {
            _canvasRoot.dispose();
            _canvasRoot = null;
        }
        super.willShutdown();
    }

    /**
     * Creates a canvas using one of our supported pixel formats.
     */
    protected Component createCanvas ()
    {
        // at least as of Ubuntu 9.10 (Karmic Koala), using the AWTCanvas on Linux results
        // in frequent crashes.  using it on Windows with the latest Nvidia drivers causes
        // the window to stop refreshing
        if (RunAnywhere.isLinux() || RunAnywhere.isWindows()) {
            return new DisplayCanvas(getAntialiasingLevel()) {
                @Override protected void didInit () {
                    GlCanvasApp.this.init();
                }
                @Override protected void updateView () {
                    GlCanvasApp.this.updateView();
                }
                @Override protected void renderView () {
                    GlCanvasApp.this.renderView();
                }
            };
        }
        for (PixelFormat format : getPixelFormats()) {
            try {
                return new AWTCanvas(format) {
                    @Override protected void didInit () {
                        GlCanvasApp.this.init();
                    }
                    @Override protected void updateView () {
                        GlCanvasApp.this.updateView();
                    }
                    @Override protected void renderView () {
                        GlCanvasApp.this.renderView();
                    }
                };
            } catch (LWJGLException e) {
                // proceed to next format
            }
        }
        log.warning("Couldn't find valid pixel format.");
        return null;
    }

    /**
     * Creates and returns the component that contains the canvas (after the canvas has been
     * created).
     */
    protected JComponent createCanvasContainer ()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(_canvas, BorderLayout.CENTER);
        return panel;
    }

    /** The frame containing the canvas. */
    protected JFrame _frame;

    /** The render canvas. */
    protected Component _canvas;

    /** The root. */
    protected Root _canvasRoot;

    /** The keyboard manager for the canvas. */
    protected KeyboardManager _keymgr;
}
