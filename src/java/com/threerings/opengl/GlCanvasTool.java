//
// $Id$

package com.threerings.opengl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

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
 * A base class for the OpenGL tool applications, such as the model viewer and the particle editor.
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
        } else if (action.equals("recenter")) {
            ((OrbitCameraHandler)_camhand).getTarget().set(Vector3f.ZERO);
        }
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
        _grid = new Grid(this, 65, 1f);
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
        _grid.enqueue();
        if (_showBounds.isSelected()) {
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
     * Creates the debug bounds object.
     */
    protected abstract DebugBounds createBounds ();

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
            _compositor.getBackgroundColor().set(getPref("background_color", Color4f.GRAY));
        }

        /**
         * Sets the background color.
         */
        @Editable(weight=1)
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
    }

    /** The tool message bundle. */
    protected MessageBundle _msgs;

    /** The editable preferences. */
    protected ToolUtil.EditablePrefs _eprefs;

    /** Toggles for the various renderables. */
    protected JCheckBoxMenuItem _showBounds, _showCompass, _showStats;

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
