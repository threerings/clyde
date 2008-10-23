//
// $Id$

package com.threerings.opengl;

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

import com.threerings.math.Vector3f;

import com.threerings.config.tools.ConfigEditor;
import com.threerings.config.tools.ResourceEditor;
import com.threerings.editor.Editable;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.editor.util.EditorContext;
import com.threerings.util.MessageBundle;
import com.threerings.util.ToolUtil;

import com.threerings.opengl.camera.CameraHandler;
import com.threerings.opengl.camera.MouseOrbiter;
import com.threerings.opengl.camera.OrbitCameraHandler;
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
            new ConfigEditor(_msgmgr, _cfgmgr, _colorpos).setVisible(true);
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

    @Override // documentation inherited
    public boolean shouldCheckTimestamps ()
    {
        return true;
    }

    @Override // documentation inherited
    protected CameraHandler createCameraHandler ()
    {
        // add an orbiter to move the camera with the mouse
        OrbitCameraHandler camhand = new OrbitCameraHandler(this);
        new MouseOrbiter(camhand).addTo(_canvas);
        return camhand;
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        // create the various renderables
        _grid = createGrid();
        _grid.getColor().set(0.2f, 0.2f, 0.2f, 1f);
        _bounds = createBounds();
        _compass = new Compass(this);
        _stats = new Stats(this);

        // note that we've opened a window
        ToolUtil.windowAdded();
    }

    @Override // documentation inherited
    protected void enqueueView ()
    {
        super.enqueueView();

        // enqueue the various renderables
        // (TEMP: check for null refs until the old tools are gone)
        if (_showGrid == null || _showGrid.isSelected()) {
            _grid.enqueue();
        }
        if (_bounds != null && _showBounds.isSelected()) {
            _bounds.enqueue();
        }
        if (_showCompass.isSelected()) {
            _compass.enqueue();
        }
        if (_showStats.isSelected()) {
            _stats.enqueue();
        }
    }

    /**
     * Creates and returns the editable preferences.
     */
    protected abstract ToolUtil.EditablePrefs createEditablePrefs ();

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
            _refreshInterval = new Interval(GlCanvasTool.this) {
                public void expired () {
                    _rsrcmgr.checkForModifications();
                }
            };
            rescheduleRefreshInterval();

            // set the background color
            _compositor.getBackgroundColor().set(getPref("background_color", Color4f.GRAY));
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
            _compositor.getBackgroundColor().set(color);
            putPref("background_color", color);
        }

        /**
         * Returns the background color.
         */
        @Editable
        public Color4f getBackgroundColor ()
        {
            return _compositor.getBackgroundColor();
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
    protected ToolUtil.EditablePrefs _eprefs;

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
