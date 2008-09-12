//
// $Id$

package com.threerings.tudey.tools;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.prefs.Preferences;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.filechooser.FileFilter;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.media.image.ImageUtil;

import com.threerings.config.tools.ConfigEditor;
import com.threerings.export.BinaryExporter;
import com.threerings.export.BinaryImporter;
import com.threerings.export.XMLExporter;
import com.threerings.export.XMLImporter;
import com.threerings.util.ToolUtil;

import com.threerings.opengl.GlCanvasTool;
import com.threerings.opengl.util.DebugBounds;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.data.TudeySceneModel;

import static com.threerings.tudey.Log.*;

/**
 * The scene editor application.
 */
public class SceneEditor extends GlCanvasTool
{
    /**
     * A tool to use in the scene editor.
     */
    public static abstract class Tool extends JPanel
    {
        public Tool ()
        {
            super(new VGroupLayout(GroupLayout.STRETCH, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        }

        /**
         * Notes that the tool has been activated.
         */
        public void activate ()
        {
            // nothing by default
        }

        /**
         * Notes that the tool has been deactivated.
         */
        public void deactivate ()
        {
            // nothing by default
        }

        /**
         * Notes that the scene object has changed.
         */
        public void sceneChanged (TudeySceneModel scene)
        {
            // nothing by default
        }
    }

    /**
     * The program entry point.
     */
    public static void main (String[] args)
    {
        new SceneEditor(args.length > 0 ? args[0] : null).startup();
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
        file.add(_importSelection = createMenuItem("import_selection", KeyEvent.VK_M, -1));
        file.add(_exportSelection = createMenuItem("export_selection", KeyEvent.VK_X, -1));
        file.addSeparator();
        file.add(createMenuItem("quit", KeyEvent.VK_Q, KeyEvent.VK_Q));

        JMenu edit = createMenu("edit", KeyEvent.VK_E);
        menubar.add(edit);
        edit.add(new JMenuItem(_cut = createAction("cut", KeyEvent.VK_T, KeyEvent.VK_X)));
        edit.add(new JMenuItem(_copy = createAction("copy", KeyEvent.VK_C, KeyEvent.VK_C)));
        edit.add(new JMenuItem(_paste = createAction("paste", KeyEvent.VK_P, KeyEvent.VK_V)));
        edit.add(new JMenuItem(
            _delete = createAction("delete", KeyEvent.VK_D, KeyEvent.VK_DELETE, 0)));
        edit.addSeparator();
        edit.add(_rotateCW = createMenuItem("rotate_cw", KeyEvent.VK_R, -1));
        edit.add(_rotateCCW = createMenuItem("rotate_ccw", KeyEvent.VK_O, -1));
        edit.addSeparator();
        edit.add(_raise = createMenuItem("raise", KeyEvent.VK_A, -1));
        edit.add(_lower = createMenuItem("lower", KeyEvent.VK_L, -1));
        edit.addSeparator();
        edit.add(createMenuItem("configs", KeyEvent.VK_N, KeyEvent.VK_G));
        edit.add(createMenuItem("resources", KeyEvent.VK_S, KeyEvent.VK_E));
        edit.add(createMenuItem("preferences", KeyEvent.VK_F, KeyEvent.VK_P));

        JMenu view = createMenu("view", KeyEvent.VK_V);
        menubar.add(view);
        view.add(_showBounds = createCheckBoxMenuItem("bounds", KeyEvent.VK_B, KeyEvent.VK_B));
        view.add(_showCompass = createCheckBoxMenuItem("compass", KeyEvent.VK_O, KeyEvent.VK_M));
        _showCompass.setSelected(true);
        view.add(_showStats = createCheckBoxMenuItem("stats", KeyEvent.VK_S, KeyEvent.VK_T));
        view.addSeparator();
        view.add(createMenuItem("refresh", KeyEvent.VK_F, KeyEvent.VK_F));
        view.addSeparator();
        view.add(createMenuItem("raise_grid", KeyEvent.VK_R, KeyEvent.VK_UP, 0));
        view.add(createMenuItem("lower_grid", KeyEvent.VK_L, KeyEvent.VK_DOWN, 0));
        view.addSeparator();
        view.add(createMenuItem("reorient", KeyEvent.VK_I, KeyEvent.VK_I));
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

        // configure the edit panel
        _epanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        _epanel.setPreferredSize(new Dimension(350, 1));

        // create the tool box
        JPanel outer = new JPanel();
        _epanel.add(outer, GroupLayout.FIXED);
        JPanel tpanel = new JPanel(new GridLayout(1, 1));
        outer.add(tpanel);
        GlobalEditor gedit = new GlobalEditor(this);
        addTool(tpanel, "global_editor", gedit);

        // create the option panel
        _opanel = GroupLayout.makeVStretchBox(5);
        _epanel.add(_opanel);

        // activate the global editor tool
        setActiveTool(gedit);
    }

    /**
     * Returns a reference to the scene view.
     */
    public TudeySceneView getView ()
    {
        return _view;
    }

    @Override // documentation inherited
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getActionCommand();
        Tool tool = _tools.get(action);
        if (tool != null) {
            setActiveTool(tool);
            return;
        }
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
            new ConfigEditor(_msgmgr, _scene.getConfigManager(), _colorpos).setVisible(true);
        } else {
            super.actionPerformed(event);
        }
    }

    @Override // documentation inherited
    protected JComponent createCanvasContainer ()
    {
        JSplitPane pane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, true, _canvas, _epanel = GroupLayout.makeVStretchBox(5));
        _canvas.setMinimumSize(new Dimension(1, 1));
        pane.setResizeWeight(1.0);
        pane.setOneTouchExpandable(true);
        return pane;
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
        return new CanvasToolPrefs(_prefs);
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        // create the scene view
        _view = new TudeySceneView(this);
        _view.wasAdded();

        // attempt to load the scene file specified on the command line if any
        // (otherwise, create an empty scene)
        if (_initScene != null) {
            open(_initScene);
        } else {
            newScene();
        }
    }

    @Override // documentation inherited
    protected void updateView (float elapsed)
    {
        super.updateView(elapsed);
        _view.tick(elapsed);
    }

    @Override // documentation inherited
    protected void enqueueView ()
    {
        super.enqueueView();
        _view.enqueue();
    }

    /**
     * Adds a tool to the tool panel.
     */
    protected void addTool (JPanel tpanel, String name, Tool tool)
    {
        tpanel.add(createIconButton(name));
        _tools.put(name, tool);
    }

    /**
     * Creates a button with the named icon.
     */
    protected JButton createIconButton (String name)
    {
        BufferedImage image;
        try {
            image = _rsrcmgr.getImageResource("media/tudey/" + name + ".png");
        } catch (IOException e) {
            log.warning("Error loading image.", "name", name, e);
            image = ImageUtil.createErrorImage(16, 16);
        }
        JButton button = new JButton(new ImageIcon(image));
        button.setPreferredSize(ICON_BUTTON_SIZE);
        button.setActionCommand(name);
        button.addActionListener(this);
        return button;
    }

    /**
     * Sets the active tool.
     */
    protected void setActiveTool (Tool tool)
    {
        if (_activeTool != null) {
            _activeTool.deactivate();
            _opanel.remove(_activeTool);
        }
        if ((_activeTool = tool) != null) {
            _opanel.add(_activeTool);
            _activeTool.activate();
        }
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
        _scene = scene;
        _scene.init(_cfgmgr);

        // update the view
        _view.setSceneModel(_scene);

        // notify the tools
        for (Tool tool : _tools.values()) {
            tool.sceneChanged(scene);
        }
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

    /** The file to attempt to load on initialization, if any. */
    protected File _initScene;

    /** The revert menu item. */
    protected JMenuItem _revert;

    /** The selection import and export menu items. */
    protected JMenuItem _importSelection, _exportSelection;

    /** The edit menu actions. */
    protected Action _cut, _copy, _paste, _delete;

    /** The rotate menu items. */
    protected JMenuItem _rotateCW, _rotateCCW;

    /** The raise/lower menu items. */
    protected JMenuItem _raise, _lower;

    /** The file chooser for opening and saving scene files. */
    protected JFileChooser _chooser;

    /** The file chooser for importing and exporting scene files. */
    protected JFileChooser _exportChooser;

    /** The panel that holds the editor bits. */
    protected JPanel _epanel;

    /** The panel that holds the tool options. */
    protected JPanel _opanel;

    /** Tools mapped by name. */
    protected HashMap<String, Tool> _tools = new HashMap<String, Tool>();

    /** The active tool. */
    protected Tool _activeTool;

    /** The loaded scene file. */
    protected File _file;

    /** The scene being edited. */
    protected TudeySceneModel _scene;

    /** The scene view. */
    protected TudeySceneView _view;

    /** The application preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(SceneEditor.class);

    /** The size of the icon buttons. */
    protected static final Dimension ICON_BUTTON_SIZE = new Dimension(20, 20);
}
