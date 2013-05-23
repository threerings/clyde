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

package com.threerings.opengl.gui.tools;

import java.awt.Dimension;
import java.awt.event.KeyEvent;

import java.io.File;

import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.lwjgl.opengl.GL11;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.Spacer;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.editor.swing.DraggableSpinner;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.editor.swing.ObjectPanel;
import com.threerings.editor.swing.editors.ObjectEditor;
import com.threerings.expr.Scoped;
import com.threerings.util.ChangeBlock;

import com.threerings.opengl.GlCanvasTool;
import com.threerings.opengl.gui.CanvasRoot;
import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.Root;
import com.threerings.opengl.gui.StretchWindow;
import com.threerings.opengl.gui.UserInterface;
import com.threerings.opengl.gui.config.ComponentConfig;
import com.threerings.opengl.gui.config.UserInterfaceConfig;
import com.threerings.opengl.renderer.Color4f;

import static com.threerings.opengl.gui.Log.log;

/**
 * Tool for testing user interfaces.
 */
public class InterfaceTester extends GlCanvasTool
    implements ChangeListener, ConfigUpdateListener<UserInterfaceConfig>
{
    /**
     * The program entry point.
     */
    public static void main (String[] args)
    {
        new InterfaceTester(args.length > 0 ? args[0] : null).startup();
    }

    /**
     * Creates the interface tester with (optionally) the path to an interface to load.
     */
    public InterfaceTester (String userInterface)
    {
        super("interface");

        // set the title
        _frame.setTitle(_msgs.get("m.title"));

        // populate the menu bar
        JMenuBar menubar = new JMenuBar();
        _frame.setJMenuBar(menubar);

        JMenu file = createMenu("file", KeyEvent.VK_F);
        menubar.add(file);
        file.add(createMenuItem("quit", KeyEvent.VK_Q, KeyEvent.VK_Q));

        JMenu edit = createMenu("edit", KeyEvent.VK_E);
        menubar.add(edit);
        edit.add(createMenuItem("configs", KeyEvent.VK_C, KeyEvent.VK_G));
        edit.add(createMenuItem("resources", KeyEvent.VK_R, KeyEvent.VK_R));
        edit.add(createMenuItem("preferences", KeyEvent.VK_P, KeyEvent.VK_P));

        JMenu view = createMenu("view", KeyEvent.VK_V);
        menubar.add(view);
        view.add(_showStats = createCheckBoxMenuItem("stats", KeyEvent.VK_S, KeyEvent.VK_T));
        view.addSeparator();
        view.add(createMenuItem("refresh", KeyEvent.VK_F, KeyEvent.VK_F));

        // configure the side panel
        _cpanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        _cpanel.setPreferredSize(new Dimension(350, 1));

        // add the config editor
        _cpanel.add(_epanel = new EditorPanel(this));

        // add the controls
        JPanel controls = new JPanel();
        _cpanel.add(controls, GroupLayout.FIXED);
        controls.add(new JLabel(_msgs.get("m.width")));
        controls.add(_width = new DraggableSpinner(-1, -1, Integer.MAX_VALUE, 1));
        _width.setMinimumSize(_width.getPreferredSize());
        _width.setMaximumSize(_width.getPreferredSize());
        _width.addChangeListener(this);
        controls.add(new Spacer(10, 1));
        controls.add(new JLabel(_msgs.get("m.height")));
        controls.add(_height = new DraggableSpinner(-1, -1, Integer.MAX_VALUE, 1));
        _height.setMinimumSize(_height.getPreferredSize());
        _height.setMaximumSize(_height.getPreferredSize());
        _height.addChangeListener(this);

        // configure the config editor
        UserInterfaceConfig.Derived impl = new UserInterfaceConfig.Derived();
        if (userInterface != null) {
            String path = _rsrcmgr.getResourcePath(new File(userInterface));
            if (path != null) {
                impl.userInterface = new ConfigReference<UserInterfaceConfig>(path);
            }
        }
        _epanel.setObject(impl);
        _epanel.addChangeListener(this);
    }

    @Override
    public Root createRoot ()
    {
        if (_canvasRoot == null) {
            _canvasRoot = new CanvasRoot(this, _canvas) {
                @Override
                protected void draw ()
                {
                    super.draw();

                    if (_highlights.isEmpty()) {
                        return;
                    }
                    _ctx.getRenderer().setTextureState(null);

                    GL11.glLineWidth(2f);
                    GL11.glEnable(GL11.GL_COLOR_LOGIC_OP);
                    GL11.glLogicOp(GL11.GL_XOR);

                    // draw a frickin box for highlighted components
                    for (Component comp : _highlights) {
                        if (!comp.isAdded() || !comp.isVisible()) {
                            continue;
                        }
                        int x = comp.getAbsoluteX();
                        int y = comp.getAbsoluteY();
                        int w = comp.getWidth();
                        int h = comp.getHeight();

                        GL11.glBegin(GL11.GL_LINE_LOOP);
                        GL11.glVertex2f(x, y);
                        GL11.glVertex2f(x + w, y);
                        GL11.glVertex2f(x + w, y + h);
                        GL11.glVertex2f(x, y + h);
                        GL11.glEnd();
                    }

                    GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
                }
            };
        }
        return _canvasRoot;
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        if (event.getSource() == _epanel) {
            // let the config know that it was updated
            if (!_block.enter()) {
                return;
            }
            try {
                _userInterface.getConfig().wasUpdated();
            } finally {
                _block.leave();
            }
        } else {
            _userInterface.setPreferredSize(_width.getIntValue(), _height.getIntValue());
        }
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<UserInterfaceConfig> event)
    {
        // update the editor panel
        if (!_block.enter()) {
            return;
        }
        try {
            _epanel.update();
            _epanel.validate();
        } finally {
            _block.leave();
        }
    }

    @Override
    protected JComponent createCanvasContainer ()
    {
        JSplitPane pane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, true, _canvas, _cpanel = GroupLayout.makeVStretchBox(5));
        _canvas.setMinimumSize(new Dimension(1, 1));
        pane.setResizeWeight(1.0);
        pane.setOneTouchExpandable(true);
        return pane;
    }

    @Override
    protected CanvasToolPrefs createEditablePrefs ()
    {
        return new CanvasToolPrefs(_prefs);
    }

    @Override
    protected void didInit ()
    {
        super.didInit();

        // create the ui root
        _root = createRoot();
        _root.setModalShade(new Color4f(0f, 0f, 0f, 0.5f));

        // and the window
        StretchWindow window = new StretchWindow(
            this, new com.threerings.opengl.gui.layout.HGroupLayout());
        _root.addWindow(window);

        // set up the ui
        UserInterfaceConfig config = new UserInterfaceConfig();
        config.init(_cfgmgr);
        config.implementation = (UserInterfaceConfig.Derived)_epanel.getObject();
        config.addListener(this);
        window.add(_userInterface = new UserInterface(this, config, this));
    }

    @Override
    protected void updateView (float elapsed)
    {
        super.updateView(elapsed);
        _root.tick(elapsed);
    }

    @Override
    protected void compositeView ()
    {
        super.compositeView();
        _root.composite();
    }

    /**
     * Tracks components by their originating ComponentConfig.
     */
    @Scoped
    protected void editorHighlight (Component comp, boolean highlight)
    {
        if (highlight) {
            _highlights.add(comp);
        } else {
            _highlights.remove(comp);
        }
    }

    /** The panel that holds the control bits. */
    protected JPanel _cpanel;

    /** The editor panel we use to edit the interface configuration. */
    protected EditorPanel _epanel;

    /** The width and height controls. */
    protected DraggableSpinner _width, _height;

    /** Indicates that we should ignore any changes, because we're the one effecting them. */
    protected ChangeBlock _block = new ChangeBlock();

    /** The user interface root. */
    protected Root _root;

    /** The user interface component. */
    protected UserInterface _userInterface;

    /** Components that should be drawn highlighted. */
    protected Set<Component> _highlights = Sets.newSetFromMap(
        new MapMaker().concurrencyLevel(1).weakKeys().<Component, Boolean>makeMap());

    /** The application preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(InterfaceTester.class);
}
