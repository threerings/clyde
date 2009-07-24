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
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.PixelFormat;

import com.samskivert.swing.util.SwingUtil;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.RunQueue;

import com.threerings.media.ManagedJFrame;
import com.threerings.util.KeyboardManager;
import com.threerings.util.KeyTranslatorImpl;

import com.threerings.math.Ray3D;

import com.threerings.opengl.gui.CanvasRoot;
import com.threerings.opengl.gui.Root;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.Renderable;

import static com.threerings.opengl.Log.*;

/**
 * A base class for applications centered around an OpenGL canvas.
 */
public abstract class GlCanvasApp extends GlDisplayApp
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
        _canvas = createCanvas();
        JComponent cont = createCanvasContainer();
        _frame.add(cont, BorderLayout.CENTER);

        // make popups heavyweight so that we can see them over the canvas
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

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
    public Canvas getCanvas ()
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

    @Override // documentation inherited
    public Root createRoot ()
    {
        return new CanvasRoot(this, _canvas);
    }

    @Override // documentation inherited
    public void startup ()
    {
        _frame.setVisible(true);
        super.startup();
    }

    @Override // documentation inherited
    protected boolean createDisplay ()
    {
        try {
            Display.setParent(_canvas);
        } catch (LWJGLException e) {
            log.warning("Failed to parent Display to canvas.", e);
            return false;
        }
        return super.createDisplay();
    }

    @Override // documentation inherited
    protected void initRenderer ()
    {
        _renderer.init(Display.getDrawable(), _canvas.getWidth(), _canvas.getHeight());
    }

    @Override // documentation inherited
    protected void createInputDevices ()
    {
        // handled in AWT
    }

    @Override // documentation inherited
    protected void destroyInputDevices ()
    {
        // handled in AWT
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        // notify the renderer on resize
        _canvas.addComponentListener(new ComponentAdapter() {
            public void componentResized (ComponentEvent event) {
                _renderer.setSize(_canvas.getWidth(), _canvas.getHeight());
            }
        });

        // listen to all key/mouse events in order to keep modifiers up to date
        _canvas.getToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched (AWTEvent event) {
                _modifiers = ((InputEvent)event).getModifiersEx();
            }
        }, AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);

        // request focus for the canvas
        _canvas.requestFocusInWindow();

        // enable the keyboard manager
        _keymgr.setEnabled(true);
    }

    @Override // documentation inherited
    protected void updateView (float elapsed)
    {
        // check for mouse movement
        Point pt;
        if (_dragging) {
            pt = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(pt, _canvas);
        } else {
            pt = _canvas.getMousePosition();
        }
        if (!ObjectUtil.equals(_lastPoint, pt)) {
            if ((_lastPoint = pt) != null) {
                int id = _dragging ? MouseEvent.MOUSE_DRAGGED : MouseEvent.MOUSE_MOVED;
                _canvas.dispatchEvent(new MouseEvent(_canvas, id,
                    System.currentTimeMillis(), _modifiers, pt.x, pt.y, 0, false));
            }
        }
        super.updateView(elapsed);
    }

    /**
     * Creates our canvas component.
     */
    protected Canvas createCanvas ()
    {
        return new Canvas() { {
                setIgnoreRepaint(true);
                disableEvents(AWTEvent.PAINT_EVENT_MASK);
                enableEvents(AWTEvent.MOUSE_EVENT_MASK);
            }
            @Override public void addFocusListener (FocusListener listener) {
                // no-op; prevent LWJGL from stealing focus
            }
            @Override public void paint (Graphics g) {
                // no-op
            }
            @Override public void update (Graphics g) {
                // no-op
            }
            @Override protected void processMouseEvent (MouseEvent event) {
                super.processMouseEvent(event);
                int id = event.getID();
                int bit = 1 << event.getButton();
                if (id == MouseEvent.MOUSE_PRESSED) {
                    _buttons |= bit;
                } else if (id == MouseEvent.MOUSE_RELEASED) {
                    _buttons &= ~bit;
                } else {
                    return;
                }
                _dragging = (_buttons != 0);
            }
            protected int _buttons;
        };
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
    protected Canvas _canvas;

    /** The keyboard manager for the canvas. */
    protected KeyboardManager _keymgr;

    /** The last recorded mouse location. */
    protected Point _lastPoint;

    /** The current set of modifiers. */
    protected int _modifiers;

    /** Whether or not we're currently dragging the mouse. */
    protected boolean _dragging;

    /** Combines all three button down masks. */
    protected static final int BUTTON_DOWN_MASK =
        MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK;
}
