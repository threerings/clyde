//
// $Id$

package com.threerings.tudey.tools;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;

import com.threerings.config.tools.ConfigEditor;
import com.threerings.export.BinaryExporter;
import com.threerings.export.BinaryImporter;
import com.threerings.export.XMLExporter;
import com.threerings.export.XMLImporter;
import com.threerings.util.ToolUtil;

import com.threerings.opengl.GlCanvasTool;
import com.threerings.opengl.util.DebugBounds;

import com.threerings.tudey.data.TudeySceneModel;

import static com.threerings.tudey.Log.*;

/**
 * The scene editor application.
 */
public class SceneEditor extends GlCanvasTool
{
    /**
     * The program entry point.
     */
    public static void main (String[] args)
    {
        new SceneEditor(args.length > 0 ? args[0] : null).start();
    }

    /**
     * Creates the scene editor with (optionally) the path to a scene to load.
     */
    public SceneEditor (String scene)
    {
        super("scene");
        _initScene = (scene == null) ? null : new File(scene);

        // set the title
        updateTitle();

        // populate the menu bar
        JMenuBar menubar = new JMenuBar();
        _frame.setJMenuBar(menubar);

        JMenu file = createMenu("file", KeyEvent.VK_F);
        menubar.add(file);
        file.add(createMenuItem("new", KeyEvent.VK_N, KeyEvent.VK_N));
        file.add(createMenuItem("open", KeyEvent.VK_O, KeyEvent.VK_O));
        file.addSeparator();
        file.add(createMenuItem("save", KeyEvent.VK_S, KeyEvent.VK_S));
        file.add(createMenuItem("save_as", KeyEvent.VK_A, KeyEvent.VK_A));
        file.add(_revert = createMenuItem("revert", KeyEvent.VK_R, KeyEvent.VK_R));
        _revert.setEnabled(false);
        file.addSeparator();
        file.add(createMenuItem("import", KeyEvent.VK_I, -1));
        file.add(createMenuItem("export", KeyEvent.VK_E, -1));
        file.addSeparator();
        file.add(createMenuItem("quit", KeyEvent.VK_Q, KeyEvent.VK_Q));

        JMenu edit = createMenu("edit", KeyEvent.VK_E);
        menubar.add(edit);
        edit.add(createMenuItem("configs", KeyEvent.VK_C, KeyEvent.VK_G));
        edit.add(createMenuItem("preferences", KeyEvent.VK_P, KeyEvent.VK_P));

        JMenu view = createMenu("view", KeyEvent.VK_V);
        menubar.add(view);
        view.add(createMenuItem("toggle_bounds", KeyEvent.VK_B, KeyEvent.VK_B));
        view.add(createMenuItem("toggle_compass", KeyEvent.VK_O, KeyEvent.VK_M));
        view.add(createMenuItem("toggle_stats", KeyEvent.VK_S, KeyEvent.VK_T));
        view.addSeparator();
        view.add(createMenuItem("recenter", KeyEvent.VK_C, KeyEvent.VK_C));

        // create the file chooser
        _chooser = new JFileChooser(_prefs.get("scene_dir", null));
        _chooser.setFileFilter(new FileFilter() {
            public boolean accept (File file) {
                return file.isDirectory() || file.toString().toLowerCase().endsWith(".dat");
            }
            public String getDescription () {
                return _msgs.get("m.scene_files");
            }
        });

        // and the export chooser
        _exportChooser = new JFileChooser(_prefs.get("scene_export_dir", null));
        _exportChooser.setFileFilter(new FileFilter() {
            public boolean accept (File file) {
                return file.isDirectory() || file.toString().toLowerCase().endsWith(".xml");
            }
            public String getDescription () {
                return _msgs.get("m.xml_files");
            }
        });
    }

    @Override // documentation inherited
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getActionCommand();
        if (action.equals("new")) {
            newScene();
        } else if (action.equals("open")) {
            open();
        } else if (action.equals("save")) {
            if (_file != null) {
                save(_file);
            } else {
                save();
            }
        } else if (action.equals("save_as")) {
            save();
        } else if (action.equals("revert")) {
            open(_file);
        } else if (action.equals("import")) {
            importScene();
        } else if (action.equals("export")) {
            exportScene();
        } else if (action.equals("configs")) {
            if (_configEditor == null) {
                _configEditor = new ConfigEditor(_msgmgr, _scene.getConfigManager());
            }
            _configEditor.setVisible(true);
        } else {
            super.actionPerformed(event);
        }
    }

    @Override // documentation inherited
    protected DebugBounds createBounds ()
    {
        return new DebugBounds(this) {
            protected void draw () {
                // ...
            }
        };
    }

    @Override // documentation inherited
    protected ToolUtil.EditablePrefs createEditablePrefs ()
    {
        return new SceneEditorPrefs(_prefs);
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        // attempt to load the scene file specified on the command line if any
        // (otherwise, create an empty scene)
        if (_initScene != null) {
            open(_initScene);
        } else {
            newScene();
        }

        // initialize the clock
        _lastTick = System.currentTimeMillis();
    }

    @Override // documentation inherited
    protected void updateScene ()
    {
        long time = System.currentTimeMillis();
        float elapsed = (time - _lastTick) / 1000f;
        _lastTick = time;
    }

    @Override // documentation inherited
    protected void renderScene ()
    {
        // clear the previous frame
        _renderer.clearFrame();

        // and the grid
        _grid.enqueue();

        // and maybe the bounding box(es)
        if (_bounds != null) {
            _bounds.enqueue();
        }

        // and maybe the compass
        if (_compass != null) {
            _compass.enqueue();
        }

        // render the contents of the queues
        _renderer.renderFrame();
    }

    /**
     * Creates a new scene.
     */
    protected void newScene ()
    {
        setScene(new TudeySceneModel());
        setFile(null);
    }

    /**
     * Brings up the open dialog.
     */
    protected void open ()
    {
        if (_chooser.showOpenDialog(_frame) == JFileChooser.APPROVE_OPTION) {
            open(_chooser.getSelectedFile());
        }
        _prefs.put("scene_dir", _chooser.getCurrentDirectory().toString());
    }

    /**
     * Attempts to open the specified scene file.
     */
    protected void open (File file)
    {
        try {
            BinaryImporter in = new BinaryImporter(new FileInputStream(file));
            setScene((TudeySceneModel)in.readObject());
            in.close();
            setFile(file);
        } catch (IOException e) {
            log.warning("Failed to open scene [file=" + file + "].", e);
        }
    }

    /**
     * Brings up the save dialog.
     */
    protected void save ()
    {
        if (_chooser.showSaveDialog(_frame) == JFileChooser.APPROVE_OPTION) {
            save(_chooser.getSelectedFile());
        }
        _prefs.put("scene_dir", _chooser.getCurrentDirectory().toString());
    }

    /**
     * Attempts to save to the specified file.
     */
    protected void save (File file)
    {
        try {
            BinaryExporter out = new BinaryExporter(new FileOutputStream(file));
            out.writeObject(_scene);
            out.close();
            setFile(file);
        } catch (IOException e) {
            log.warning("Failed to save scene [file=" + file + "].", e);
        }
    }

    /**
     * Brings up the import dialog.
     */
    protected void importScene ()
    {
        if (_exportChooser.showOpenDialog(_frame) == JFileChooser.APPROVE_OPTION) {
            File file = _exportChooser.getSelectedFile();
            try {
                XMLImporter in = new XMLImporter(new FileInputStream(file));
                setScene((TudeySceneModel)in.readObject());
                in.close();
            } catch (IOException e) {
                log.warning("Failed to import scene [file=" + file +"].", e);
            }
        }
        _prefs.put("scene_export_dir", _exportChooser.getCurrentDirectory().toString());
    }

    /**
     * Initializes the scene.
     */
    protected void setScene (TudeySceneModel scene)
    {
        if (_configEditor != null) {
            _configEditor.setVisible(false);
            _configEditor = null;
        }
        _scene = scene;
        _scene.init(_cfgmgr);
    }

    /**
     * Brings up the export dialog.
     */
    protected void exportScene ()
    {
        if (_exportChooser.showSaveDialog(_frame) == JFileChooser.APPROVE_OPTION) {
            File file = _exportChooser.getSelectedFile();
            try {
                XMLExporter out = new XMLExporter(new FileOutputStream(file));
                out.writeObject(_scene);
                out.close();
            } catch (IOException e) {
                log.warning("Failed to export scene [file=" + file + "].", e);
            }
        }
        _prefs.put("scene_export_dir", _exportChooser.getCurrentDirectory().toString());
    }

    /**
     * Sets the file and updates the revert item and title bar.
     */
    protected void setFile (File file)
    {
        _file = file;
        _revert.setEnabled(file != null);
        updateTitle();
    }

    /**
     * Updates the title based on the file.
     */
    protected void updateTitle ()
    {
        String title = _msgs.get("m.title");
        if (_file != null) {
            title = title + ": " + _file;
        }
        _frame.setTitle(title);
    }

    /**
     * Scene editor preferences.
     */
    protected class SceneEditorPrefs extends CanvasToolPrefs
    {
        public SceneEditorPrefs (Preferences prefs)
        {
            super(prefs);
        }
    }

    /** The file to attempt to load on initialization, if any. */
    protected File _initScene;

    /** The revert menu item. */
    protected JMenuItem _revert;

    /** The file chooser for opening and saving scene files. */
    protected JFileChooser _chooser;

    /** The file chooser for importing and exporting scene files. */
    protected JFileChooser _exportChooser;

    /** The configuration editor. */
    protected ConfigEditor _configEditor;

    /** The loaded scene file. */
    protected File _file;

    /** The scene being edited. */
    protected TudeySceneModel _scene;

    /** The time of the last tick. */
    protected long _lastTick;

    /** The application preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(SceneEditor.class);
}
