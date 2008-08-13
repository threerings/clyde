//
// $Id$

package com.threerings.opengl.mod.tools;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;

import java.io.File;

import java.util.prefs.Preferences;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.GroupLayout;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Property;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.util.ToolUtil;

import com.threerings.opengl.GlCanvasTool;
import com.threerings.opengl.mod.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.util.DebugBounds;

/**
 * A simple model viewer application.
 */
public class ModelViewer extends GlCanvasTool
    implements ChangeListener
{
    /**
     * The program entry point.
     */
    public static void main (String[] args)
    {
        new ModelViewer(args.length > 0 ? args[0] : null).startup();
    }

    /**
     * Creates the model viewer with (optionally) the path to a model to load.
     */
    public ModelViewer (String model)
    {
        super("viewer");

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
        view.add(_showBounds = createCheckBoxMenuItem("bounds", KeyEvent.VK_B, KeyEvent.VK_B));
        view.add(_showCompass = createCheckBoxMenuItem("compass", KeyEvent.VK_C, KeyEvent.VK_M));
        _showCompass.setSelected(true);
        view.add(_showStats = createCheckBoxMenuItem("stats", KeyEvent.VK_S, KeyEvent.VK_T));
        view.addSeparator();
        view.add(createMenuItem("recenter", KeyEvent.VK_R, KeyEvent.VK_C));

        JPanel bottom = GroupLayout.makeVBox(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH);
        _frame.add(bottom, BorderLayout.SOUTH);
        bottom.add(_epanel = new EditorPanel(
            this, EditorPanel.CategoryMode.PANELS, new Property[0]));

        ModelConfig.Derived impl = new ModelConfig.Derived();
        if (model != null) {
            String path = _rsrcmgr.getResourcePath(new File(model));
            if (path != null) {
                impl.model = new ConfigReference<ModelConfig>(path);
            }
        }
        _epanel.setObject(impl);
        _epanel.addChangeListener(this);
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        _model.getConfig().wasUpdated();
    }

    @Override // documentation inherited
    protected DebugBounds createBounds ()
    {
        return new DebugBounds(this) {
            protected void draw () {
//                    _model.updateWorldBounds();
//                    _model.drawBounds();
            }
        };
    }

    @Override // documentation inherited
    protected ToolUtil.EditablePrefs createEditablePrefs ()
    {
        return new CanvasToolPrefs(_prefs);
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        // set up the model
        ModelConfig config = new ModelConfig();
        config.implementation = (ModelConfig.Derived)_epanel.getObject();
        _model = new Model(this, config);
        _model.setParentScope(this);
    }

    @Override // documentation inherited
    protected void updateView ()
    {
        super.updateView();
        long time = System.currentTimeMillis();
        float elapsed = (_lastTick == 0L) ? 0f : (time - _lastTick) / 1000f;
        _model.tick(elapsed);
        _lastTick = time;
    }

    @Override // documentation inherited
    protected void enqueueView ()
    {
        super.enqueueView();
        _model.enqueue();
    }

    /** The editor panel we use to edit the model configuration. */
    protected EditorPanel _epanel;

    /** The model being viewed. */
    protected Model _model;

    /** The time of the last tick. */
    protected long _lastTick;

    /** The application preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(ModelViewer.class);
}
