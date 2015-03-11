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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.prefs.Preferences;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.samskivert.util.Interval;
import com.samskivert.util.RunQueue;

import com.threerings.math.Vector3f;

import com.threerings.config.Reference;
import com.threerings.config.tools.ConfigEditor;
import com.threerings.config.tools.ResourceEditor;
import com.threerings.editor.Editable;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.editor.util.EditorContext;
import com.threerings.swing.LogPanel;
import com.threerings.util.MessageBundle;
import com.threerings.util.ToolUtil;

import com.threerings.opengl.camera.CameraHandler;
import com.threerings.opengl.camera.MouseOrbiter;
import com.threerings.opengl.camera.OrbitCameraHandler;
import com.threerings.opengl.compositor.config.RenderSchemeConfig;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.util.Compass;
import com.threerings.opengl.util.DebugBounds;
import com.threerings.opengl.util.Grid;
import com.threerings.opengl.util.Stats;

/**
 * A base class for the OpenGL tool applications.
 */
public abstract class GlCanvasTool extends GlCanvasApp
    implements EditorContext, ActionListener
{
    /**
     * Creates a new tool application.
     *
     * @param msgs the name of the application message bundle.
     */
    public GlCanvasTool (String msgs)
    {
        // configure the log file
        ToolUtil.configureLog(msgs + ".log");

        // resolve the message bundle
        _msgs = _msgmgr.getBundle(msgs);

        // create and initialize the editable preferences
        _eprefs = createEditablePrefs();
        _eprefs.init(_rsrcmgr);

        // add the log status panel
        _frame.add(new LogPanel(_msgmgr, true), BorderLayout.SOUTH);

        // initialize the configuration manager now that we have configured the resource dir
        _cfgmgr.init();
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getActionCommand();
        if (action.equals("quit")) {
            shutdown();
        } else if (action.equals("configs")) {
            ConfigEditor.create(this).setVisible(true);
        } else if (action.equals("resources")) {
            new ResourceEditor(_msgmgr, _cfgmgr, _colorpos).setVisible(true);
        } else if (action.equals("preferences")) {
            if (_pdialog == null) {
                _pdialog = EditorPanel.createDialog(
                    _canvas, this, _msgs.get("t.preferences"), _eprefs);
            }
            _pdialog.setVisible(true);
        } else if (action.equals("refresh")) {
            _rsrcmgr.checkForModifications();
        } else if (action.equals("recenter")) {
            ((OrbitCameraHandler)_camhand).getTarget().set(Vector3f.ZERO);
        }
    }

    @Override
    public boolean shouldCheckTimestamps ()
    {
        return true;
    }

    @Override
    protected CameraHandler createCameraHandler ()
    {
        // add an orbiter to move the camera with the mouse
        OrbitCameraHandler camhand = new OrbitCameraHandler(this);
        new MouseOrbiter(camhand).addTo(_canvas);
        return camhand;
    }

    @Override
    protected void didInit ()
    {
        super.didInit();

        // notify the prefs
        _eprefs.didInit();

        // create the various renderables
        _grid = createGrid();
        _grid.getColor().set(0.2f, 0.2f, 0.2f, 1f);
        _bounds = createBounds();
        _compass = new Compass(this);
        _stats = new Stats(this);
    }

    @Override
    protected void compositeView ()
    {
        super.compositeView();

        // composite the various renderables
        if (_showGrid != null && _showGrid.isSelected()) {
            _grid.composite();
        }
        if (_bounds != null && _showBounds.isSelected()) {
            _bounds.composite();
        }
        if (_showCompass != null && _showCompass.isSelected()) {
            _compass.composite();
        }
        if (_showStats.isSelected()) {
            _stats.composite();
        }
    }

    /**
     * Creates and returns the editable preferences.
     */
    protected abstract CanvasToolPrefs createEditablePrefs ();

    /**
     * Creates the grid object.
     */
    protected Grid createGrid ()
    {
        return new Grid(this, 65, 1f);
    }

    /**
     * Creates the debug bounds object.
     */
    protected DebugBounds createBounds ()
    {
        return null;
    }

    /**
     * Creates a menu with the specified name and mnemonic.
     */
    protected JMenu createMenu (String name, int mnemonic)
    {
        return ToolUtil.createMenu(_msgs, name, mnemonic);
    }

    /**
     * Creates a menu item with the specified action, mnemonic, and (optional) accelerator.
     */
    protected JMenuItem createMenuItem (String action, int mnemonic, int accelerator)
    {
        return ToolUtil.createMenuItem(this, _msgs, action, mnemonic, accelerator);
    }

    /**
     * Creates a menu item with the specified action, mnemonic, and (optional) accelerator
     * key/modifiers.
     */
    protected JMenuItem createMenuItem (
        String action, int mnemonic, int accelerator, int modifiers)
    {
        return ToolUtil.createMenuItem(this, _msgs, action, mnemonic, accelerator, modifiers);
    }

    /**
     * Creates a check box menu item with the specified action, mnemonic, and (optional)
     * accelerator.
     */
    protected JCheckBoxMenuItem createCheckBoxMenuItem (
        String action, int mnemonic, int accelerator)
    {
        return ToolUtil.createCheckBoxMenuItem(this, _msgs, action, mnemonic, accelerator);
    }

    /**
     * Creates a check box menu item with the specified action, mnemonic, and (optional)
     * accelerator key/modifiers.
     */
    protected JCheckBoxMenuItem createCheckBoxMenuItem (
        String action, int mnemonic, int accelerator, int modifiers)
    {
        return ToolUtil.createCheckBoxMenuItem(
            this, _msgs, action, mnemonic, accelerator, modifiers);
    }

    /**
     * Creates an action with the specified command, mnemonic, and (optional) accelerator.
     */
    protected Action createAction (String command, int mnemonic, int accelerator)
    {
        return ToolUtil.createAction(this, _msgs, command, mnemonic, accelerator);
    }

    /**
     * Creates an action with the specified command, mnemonic, and (optional) accelerator
     * key/modifiers.
     */
    protected Action createAction (String command, int mnemonic, int accelerator, int modifiers)
    {
        return ToolUtil.createAction(this, _msgs, command, mnemonic, accelerator, modifiers);
    }

    /**
     * Creates a button with the specified action.
     */
    protected JButton createButton (String action)
    {
        return ToolUtil.createButton(this, _msgs, action);
    }

    /**
     * Creates a button with the specified action and translation key.
     */
    protected JButton createButton (String action, String key)
    {
        return ToolUtil.createButton(this, _msgs, action, key);
    }

    /**
     * The preferences for canvas tools.
     */
    protected class CanvasToolPrefs extends ToolUtil.EditablePrefs
    {
        public CanvasToolPrefs (Preferences prefs)
        {
            super(prefs);

            // initialize the refresh interval
            _refreshInterval = new Interval(RunQueue.AWT) {
                public void expired () {
                    _rsrcmgr.checkForModifications();
                }
            };
            rescheduleRefreshInterval();

            // set the background color
            Color4f color = getPref("background_color", Color4f.GRAY);
            _compositor.getDefaultBackgroundColor().set(color.r, color.g, color.b, 0f);

            // and the render scheme, compatibility mode, etc.
            _renderScheme = _prefs.get("render_scheme", null);
            _compatibilityMode = _prefs.getBoolean("compatibility_mode", false);
            _renderEffects = _prefs.getBoolean("render_effects", true);
        }

        /**
         * Called when the canvas has been initialized.
         */
        public void didInit ()
        {
            // initialize vertical synchronization
            ((GlCanvas)_canvas).setVSyncEnabled(getVSyncEnabled());
        }

        /**
         * Sets whether or not automatic file refresh is enabled.
         */
        @Editable(weight=1)
        public void setAutoRefreshEnabled (boolean enabled)
        {
            _prefs.putBoolean("auto_refresh_enabled", enabled);
            rescheduleRefreshInterval();
        }

        /**
         * Checks whether or not automatic file refresh is enabled.
         */
        @Editable
        public boolean isAutoRefreshEnabled ()
        {
            return _prefs.getBoolean("auto_refresh_enabled", false);
        }

        /**
         * Sets the refresh interval.
         */
        @Editable(weight=2, min=0, step=0.01)
        public void setRefreshInterval (float interval)
        {
            _prefs.putFloat("refresh_interval", interval);
            rescheduleRefreshInterval();
        }

        /**
         * Returns the refresh interval.
         */
        @Editable
        public float getRefreshInterval ()
        {
            return _prefs.getFloat("refresh_interval", 0.5f);
        }

        /**
         * Sets the background color.
         */
        @Editable(weight=3)
        public void setBackgroundColor (Color4f color)
        {
            _compositor.getDefaultBackgroundColor().set(color.r, color.g, color.b, 0f);
            putPref("background_color", color);
        }

        /**
         * Returns the background color.
         */
        @Editable
        public Color4f getBackgroundColor ()
        {
            Color4f color = _compositor.getDefaultBackgroundColor();
            return new Color4f(color.r, color.g, color.b, 1f);
        }

        /**
         * Enables or disables vertical synchronization.
         */
        @Editable(weight=4)
        public void setVSyncEnabled (boolean enabled)
        {
            ((GlCanvas)_canvas).setVSyncEnabled(enabled);
            _prefs.putBoolean("vsync_enabled", enabled);
        }

        /**
         * Checks whether vertical synchronization is enabled.
         */
        @Editable
        public boolean getVSyncEnabled ()
        {
            return _prefs.getBoolean("vsync_enabled", true);
        }

        /**
         * Sets the render scheme to use.
         */
        @Editable(nullable=true, weight=5)
        @Reference(RenderSchemeConfig.class)
        public void setRenderScheme (String scheme)
        {
            GlCanvasTool.this.setRenderScheme(scheme);
            if (scheme == null) {
                _prefs.remove("render_scheme");
            } else {
                _prefs.put("render_scheme", scheme);
            }
        }

        /**
         * Returns the active render scheme.
         */
        @Editable
        @Reference(RenderSchemeConfig.class)
        public String getRenderScheme ()
        {
            return _renderScheme;
        }

        /**
         * Sets whether or not to enable compatibility mode.
         */
        @Editable(weight=6)
        public void setCompatibilityMode (boolean enabled)
        {
            GlCanvasTool.this.setCompatibilityMode(enabled);
            _prefs.putBoolean("compatibility_mode", enabled);
        }

        /**
         * Returns the active compatibility mode setting.
         */
        @Editable
        public boolean getCompatibilityMode ()
        {
            return _compatibilityMode;
        }

        /**
         * Sets whether or not to enable render effects.
         */
        @Editable(weight=7)
        public void setRenderEffects (boolean enabled)
        {
            GlCanvasTool.this.setRenderEffects(enabled);
            _prefs.putBoolean("render_effects", enabled);
        }

        /**
         * Returns the active render effects setting.
         */
        @Editable
        public boolean getRenderEffects ()
        {
            return _renderEffects;
        }

        /**
         * Updates the schedule of the refresh interval.
         */
        protected void rescheduleRefreshInterval ()
        {
            float interval = getRefreshInterval();
            if (isAutoRefreshEnabled() && interval > 0f) {
                _refreshInterval.schedule((long)(interval * 1000f), true);
            } else {
                _refreshInterval.cancel();
            }
        }

        /** The interval we use to check for resource modifications. */
        protected Interval _refreshInterval;
    }

    /** The tool message bundle. */
    protected MessageBundle _msgs;

    /** The editable preferences. */
    protected CanvasToolPrefs _eprefs;

    /** Toggles for the various renderables. */
    protected JCheckBoxMenuItem _showBounds, _showCompass, _showGrid, _showStats;

    /** The preferences dialog. */
    protected JDialog _pdialog;

    /** The reference grid. */
    protected Grid _grid;

    /** The bounds display. */
    protected DebugBounds _bounds;

    /** The coordinate system compass. */
    protected Compass _compass;

    /** The render stats display. */
    protected Stats _stats;
}
