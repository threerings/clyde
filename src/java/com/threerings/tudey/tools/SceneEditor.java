//
// $Id$

package com.threerings.tudey.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.Spacer;
import com.samskivert.swing.util.SwingUtil;
import com.samskivert.util.Predicate;

import com.threerings.media.image.ImageUtil;
import com.threerings.util.KeyboardManager.KeyObserver;

import com.threerings.config.ConfigManager;
import com.threerings.config.tools.ConfigEditor;
import com.threerings.export.BinaryExporter;
import com.threerings.export.BinaryImporter;
import com.threerings.export.XMLExporter;
import com.threerings.export.XMLImporter;
import com.threerings.expr.Scoped;
import com.threerings.math.Ray3D;
import com.threerings.math.Vector3f;
import com.threerings.util.ToolUtil;

import com.threerings.opengl.GlCanvasTool;
import com.threerings.opengl.camera.CameraHandler;
import com.threerings.opengl.camera.OrbitCameraHandler;
import com.threerings.opengl.camera.MouseOrbiter;
import com.threerings.opengl.util.DebugBounds;
import com.threerings.opengl.util.Grid;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.sprite.EntrySprite;
import com.threerings.tudey.client.sprite.PlaceableSprite;
import com.threerings.tudey.client.sprite.Sprite;
import com.threerings.tudey.client.sprite.TileSprite;
import com.threerings.tudey.client.util.ShapeSceneElement;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.GlobalEntry;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.util.TudeySceneMetrics;

import static com.threerings.tudey.Log.*;

/**
 * The scene editor application.
 */
public class SceneEditor extends GlCanvasTool
    implements KeyObserver, MouseListener
{
    /** Allows only tile sprites. */
    public static final Predicate<Sprite> TILE_SPRITE_FILTER =
        new Predicate.InstanceOf<Sprite>(TileSprite.class);

    /** Allows only placeable sprites. */
    public static final Predicate<Sprite> PLACEABLE_SPRITE_FILTER =
        new Predicate.InstanceOf<Sprite>(PlaceableSprite.class);

    /** Allows tile and placeable sprites. */
    @SuppressWarnings("unchecked")
    public static final Predicate<Sprite> DEFAULT_SPRITE_FILTER =
        new Predicate.Or<Sprite>(TILE_SPRITE_FILTER, PLACEABLE_SPRITE_FILTER);

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
        file.add(createMenuItem("import_selection", KeyEvent.VK_M, -1));
        file.add(_exportSelection = createMenuItem("export_selection", KeyEvent.VK_X, -1));
        _exportSelection.setEnabled(false);
        file.addSeparator();
        file.add(createMenuItem("quit", KeyEvent.VK_Q, KeyEvent.VK_Q));

        JMenu edit = createMenu("edit", KeyEvent.VK_E);
        menubar.add(edit);
        edit.add(new JMenuItem(_cut = createAction("cut", KeyEvent.VK_T, KeyEvent.VK_X)));
        _cut.setEnabled(false);
        edit.add(new JMenuItem(_copy = createAction("copy", KeyEvent.VK_C, KeyEvent.VK_C)));
        _copy.setEnabled(false);
        edit.add(new JMenuItem(_paste = createAction("paste", KeyEvent.VK_P, KeyEvent.VK_V)));
        _paste.setEnabled(false);
        edit.add(new JMenuItem(
            _delete = createAction("delete", KeyEvent.VK_D, KeyEvent.VK_DELETE, 0)));
        _delete.setEnabled(false);
        edit.addSeparator();
        edit.add(_rotateCW = createMenuItem("rotate_cw", KeyEvent.VK_R, -1));
        _rotateCW.setEnabled(false);
        edit.add(_rotateCCW = createMenuItem("rotate_ccw", KeyEvent.VK_O, -1));
        _rotateCCW.setEnabled(false);
        edit.addSeparator();
        edit.add(_raise = createMenuItem("raise", KeyEvent.VK_A, -1));
        _raise.setEnabled(false);
        edit.add(_lower = createMenuItem("lower", KeyEvent.VK_L, -1));
        _lower.setEnabled(false);
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

        // populate the tool bar
        _toolbar.setLayout(new HGroupLayout(GroupLayout.STRETCH));
        _toolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        _toolbar.setFloatable(false);
        _toolbar.setRollover(true);
        _toolbar.add(createIconButton("save"), GroupLayout.FIXED);
        _toolbar.add(new Spacer(80, 1), GroupLayout.FIXED);
        _toolbar.add(_markers = createToggleButton("markers"), GroupLayout.FIXED);
        _toolbar.add(_light = createToggleButton("light"), GroupLayout.FIXED);
        _toolbar.add(_fog = createToggleButton("fog"), GroupLayout.FIXED);
        _toolbar.add(_sound = createToggleButton("sound"), GroupLayout.FIXED);
        _toolbar.add(new Spacer(1, 1));
        _toolbar.add(createIconButton("raise_grid"), GroupLayout.FIXED);
        _toolbar.add(createIconButton("lower_grid"), GroupLayout.FIXED);

        // configure the edit panel
        _epanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        _epanel.setPreferredSize(new Dimension(350, 1));

        // create the tool box
        JPanel outer = new JPanel();
        _epanel.add(outer, GroupLayout.FIXED);
        ButtonGroup tgroup = new ButtonGroup();
        JPanel tpanel = new JPanel(new GridLayout(0, 7, 5, 5));
        outer.add(tpanel);
        addTool(tpanel, tgroup, "arrow", _arrow = new Arrow(this));
        addTool(tpanel, tgroup, "selector", new Selector(this));
        addTool(tpanel, tgroup, "mover", new Mover(this));
        addTool(tpanel, tgroup, "placer", new Placer(this));
        addTool(tpanel, tgroup, "path_definer", new PathDefiner(this));
        addTool(tpanel, tgroup, "area_definer", new AreaDefiner(this));
        addTool(tpanel, tgroup, "global_editor", _globalEditor = new GlobalEditor(this));
        addTool(tpanel, tgroup, "tile_brush", new TileBrush(this));
        addTool(tpanel, tgroup, "ground_brush", new GroundBrush(this));
        addTool(tpanel, tgroup, "wall_brush", new WallBrush(this));
        addTool(tpanel, tgroup, "eraser", new Eraser(this));

        // create the option panel
        _opanel = GroupLayout.makeVStretchBox(5);
        _epanel.add(_opanel);

        // activate the arrow tool
        setActiveTool(_arrow);

        // add ourself as a key observer
        _keymgr.registerKeyObserver(this);

        // and as a mouse listener
        _canvas.addMouseListener(this);
    }

    /**
     * Returns a reference to the scene view.
     */
    public TudeySceneView getView ()
    {
        return _view;
    }

    /**
     * Returns a reference to the editor grid.
     */
    public EditorGrid getGrid ()
    {
        return _grid;
    }

    /**
     * Checks whether the shift key is being held down.
     */
    public boolean isShiftDown ()
    {
        return _shiftDown;
    }

    /**
     * Checks whether the control key is being held down.
     */
    public boolean isControlDown ()
    {
        return _controlDown;
    }

    /**
     * Checks whether the alt key is being held down.
     */
    public boolean isAltDown ()
    {
        return _altDown;
    }

    /**
     * Checks whether the first mouse button is being held down on the canvas.
     */
    public boolean isFirstButtonDown ()
    {
        return _firstButtonDown;
    }

    /**
     * Checks whether the second mouse button is being held down on the canvas.
     */
    public boolean isSecondButtonDown ()
    {
        return _secondButtonDown;
    }

    /**
     * Checks whether the third mouse button is being held down on the canvas.
     */
    public boolean isThirdButtonDown ()
    {
        return _thirdButtonDown;
    }

    /**
     * Sets the selection shape.
     */
    public void setSelection (Shape selection)
    {
        _selection.setShape(selection);

        // update the ui bits
        boolean enable = (selection != null);
        _exportSelection.setEnabled(enable);
        _cut.setEnabled(enable);
        _copy.setEnabled(enable);
        _delete.setEnabled(enable);
        _rotateCW.setEnabled(enable);
        _rotateCCW.setEnabled(enable);
        _raise.setEnabled(enable);
        _lower.setEnabled(enable);
    }

    /**
     * Returns a reference to the selection shape.
     */
    public Shape getSelection ()
    {
        return _selection.getShape();
    }

    /**
     * Attempts to edit the object under the mouse cursor.
     */
    public void editMouseObject ()
    {
        if (!getMouseRay(_pick)) {
            return;
        }
        Sprite sprite = _view.getIntersection(_pick, _pt, DEFAULT_SPRITE_FILTER);
        if (sprite instanceof EntrySprite) {
            Entry entry = ((EntrySprite)sprite).getEntry();
            if (entry instanceof GlobalEntry) {
                setActiveTool(_globalEditor);
                _globalEditor.edit((GlobalEntry)entry);
            } else {
                setActiveTool(_arrow);
                _arrow.edit(entry);
            }
        }
    }

    /**
     * Attempts to delete the object under the mouse cursor.
     */
    public void deleteMouseObject ()
    {
        deleteMouseObject(DEFAULT_SPRITE_FILTER);
    }

    /**
     * Attempts to delete the object under the mouse cursor.
     */
    public void deleteMouseObject (Predicate<Sprite> filter)
    {
        if (!getMouseRay(_pick)) {
            return;
        }
        Sprite sprite = _view.getIntersection(_pick, _pt, filter);
        if (sprite instanceof EntrySprite) {
            Entry entry = ((EntrySprite)sprite).getEntry();
            _scene.removeEntry(entry.getKey());
        }
    }

    // documentation inherited from interface KeyObserver
    public void handleKeyEvent (int id, int keyCode, long timestamp)
    {
        boolean pressed = (id == KeyEvent.KEY_PRESSED);
        switch (keyCode) {
            case KeyEvent.VK_SHIFT:
                _shiftDown = pressed;
                break;
            case KeyEvent.VK_CONTROL:
                _controlDown = pressed;
                break;
            case KeyEvent.VK_ALT:
                _altDown = pressed;
                break;
        }
    }

    // documentation inherited from interface MouseListener
    public void mouseClicked (MouseEvent event)
    {
        if (mouseCameraEnabled() && event.getClickCount() == 2) {
            editMouseObject();
        }
    }

    // documentation inherited from interface MouseListener
    public void mousePressed (MouseEvent event)
    {
        switch (event.getButton()) {
            case MouseEvent.BUTTON1:
                _firstButtonDown = true;
                break;
            case MouseEvent.BUTTON2:
                _secondButtonDown = true;
                break;
            case MouseEvent.BUTTON3:
                _thirdButtonDown = true;
                break;
        }
    }

    // documentation inherited from interface MouseListener
    public void mouseReleased (MouseEvent event)
    {
        switch (event.getButton()) {
            case MouseEvent.BUTTON1:
                _firstButtonDown = false;
                break;
            case MouseEvent.BUTTON2:
                _secondButtonDown = false;
                break;
            case MouseEvent.BUTTON3:
                _thirdButtonDown = false;
                break;
        }
    }

    // documentation inherited from interface MouseListener
    public void mouseEntered (MouseEvent event)
    {
        // no-op
    }

    // documentation inherited from interface MouseListener
    public void mouseExited (MouseEvent event)
    {
        // no-op
    }

    @Override // documentation inherited
    public ConfigManager getConfigManager ()
    {
        return (_scene == null) ? _cfgmgr : _scene.getConfigManager();
    }

    @Override // documentation inherited
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getActionCommand();
        EditorTool tool = _tools.get(action);
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
        } else if (action.equals("cut")) {

        } else if (action.equals("copy")) {

        } else if (action.equals("delete")) {
            deleteSelection();
        } else if (action.equals("raise")) {
            raiseSelection(+1);
        } else if (action.equals("lower")) {
            raiseSelection(-1);
        } else if (action.equals("configs")) {
            new ConfigEditor(_msgmgr, _scene.getConfigManager(), _colorpos).setVisible(true);
        } else if (action.equals("raise_grid")) {
            _grid.setElevation(_grid.getElevation() + 1);
        } else if (action.equals("lower_grid")) {
            _grid.setElevation(_grid.getElevation() - 1);
        } else if (action.equals("markers")) {
            _markersVisible = !_markers.isSelected();
            wasUpdated();
        } else if (action.equals("light")) {
            _lightingEnabled = !_light.isSelected();
            wasUpdated();
        } else if (action.equals("fog")) {
            _fogEnabled = !_fog.isSelected();
            wasUpdated();
        } else if (action.equals("sound")) {
            _soundEnabled = !_sound.isSelected();
            wasUpdated();
        } else {
            super.actionPerformed(event);
        }
    }

    @Override // documentation inherited
    protected JComponent createCanvasContainer ()
    {
        JPanel ccont = new JPanel(new BorderLayout());
        ccont.add(_toolbar = new JToolBar(), BorderLayout.NORTH);
        ccont.add(_canvas, BorderLayout.CENTER);
        JSplitPane pane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, true, ccont, _epanel = GroupLayout.makeVStretchBox(5));
        _canvas.setMinimumSize(new Dimension(1, 1));
        pane.setResizeWeight(1.0);
        pane.setOneTouchExpandable(true);
        bindAction(ccont, KeyEvent.VK_UP, 0, "raise_grid");
        bindAction(ccont, KeyEvent.VK_DOWN, 0, "lower_grid");
        return pane;
    }

    @Override // documentation inherited
    protected Grid createGrid ()
    {
        return (_grid = new EditorGrid(this));
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
    protected CameraHandler createCameraHandler ()
    {
        // camera target elevation matches grid elevation
        OrbitCameraHandler camhand = new OrbitCameraHandler(this) {
            public void updatePosition () {
                _target.z = _grid.getZ();
                super.updatePosition();
            }
        };
        // mouse movement is enabled when the tool allows it or control is held down
        new MouseOrbiter(camhand, true) {
            public void mouseDragged (MouseEvent event) {
                if (mouseCameraEnabled()) {
                    super.mouseDragged(event);
                } else {
                    super.mouseMoved(event);
                }
            }
            public void mouseWheelMoved (MouseWheelEvent event) {
                if (mouseCameraEnabled()) {
                    super.mouseWheelMoved(event);
                }
            }
        }.addTo(_canvas);
        return camhand;
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        // create the scene view
        _view = new TudeySceneView(this, this);

        // initialize the selection
        _selection = new ShapeSceneElement(this, true);

        // initialize the tools
        for (EditorTool tool : _tools.values()) {
            tool.init();
        }

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
        _activeTool.tick(elapsed);
        _grid.tick(elapsed);

        // update the selection transform
        _selection.getTransform().getTranslation().z =
            TudeySceneMetrics.getTileZ(_grid.getElevation());
    }

    @Override // documentation inherited
    protected void enqueueView ()
    {
        super.enqueueView();
        _view.enqueue();
        _selection.enqueue();
        _activeTool.enqueue();
    }

    /**
     * Adds a tool to the tool panel.
     */
    protected void addTool (JPanel tpanel, ButtonGroup tgroup, String name, EditorTool tool)
    {
        JToggleButton button = createToggleButton(name);
        tpanel.add(button);
        tgroup.add(button);

        _tools.put(name, tool);
        tool.setButton(button);
    }

    /**
     * Creates an icon button with the specified name.
     */
    protected JButton createIconButton (String name)
    {
        JButton button = new JButton(createIcon(name));
        button.setMinimumSize(TOOL_BUTTON_SIZE);
        button.setMaximumSize(TOOL_BUTTON_SIZE);
        button.setPreferredSize(TOOL_BUTTON_SIZE);
        button.setActionCommand(name);
        button.addActionListener(this);
        return button;
    }

    /**
     * Creates a toggle button with different icons for the unselected and selected states.
     */
    protected JToggleButton createToggleButton (String name)
    {
        JToggleButton button = new JToggleButton(createIcon(name));
        button.setSelectedIcon(createIcon(name + "_select"));
        button.setMinimumSize(TOOL_BUTTON_SIZE);
        button.setMaximumSize(TOOL_BUTTON_SIZE);
        button.setPreferredSize(TOOL_BUTTON_SIZE);
        button.setActionCommand(name);
        button.addActionListener(this);
        return button;
    }

    /**
     * Creates the named icon.
     */
    protected ImageIcon createIcon (String name)
    {
        BufferedImage image;
        try {
            image = _rsrcmgr.getImageResource("media/tudey/" + name + ".png");
        } catch (IOException e) {
            log.warning("Error loading image.", "name", name, e);
            image = ImageUtil.createErrorImage(24, 24);
        }
        return new ImageIcon(image);
    }

    /**
     * Sets the active tool.
     */
    protected void setActiveTool (EditorTool tool)
    {
        if (_activeTool == tool) {
            return;
        }
        if (_activeTool != null) {
            _activeTool.deactivate();
            _opanel.remove(_activeTool);
        }
        if ((_activeTool = tool) != null) {
            _opanel.add(_activeTool);
            _activeTool.activate();
        }
        SwingUtil.refresh(_opanel);
    }

    /**
     * Binds a keystroke to an action on the specified component.
     */
    protected void bindAction (
        final JComponent comp, int keyCode, int modifiers, final String action)
    {
        comp.getInputMap().put(KeyStroke.getKeyStroke(keyCode, modifiers), action);
        comp.getActionMap().put(action, new AbstractAction(action) {
            public void actionPerformed (ActionEvent event) {
                SceneEditor.this.actionPerformed(new ActionEvent(
                    comp, ActionEvent.ACTION_PERFORMED, action));
            }
        });
    }

    /**
     * Determines whether mouse camera control is enabled.
     */
    protected boolean mouseCameraEnabled ()
    {
        return _activeTool.allowsMouseCamera() || isControlDown();
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
                setFile(null);
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
        for (EditorTool tool : _tools.values()) {
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

    /**
     * Deletes the entries under the selected region.
     */
    protected void deleteSelection ()
    {
        for (Entry entry : getSelectedEntries()) {
            _scene.removeEntry(entry.getKey());
        }
        setSelection(null);
    }

    /**
     * Raises or lowers the entries under the selection region by the specified amount.
     */
    protected void raiseSelection (int amount)
    {
        for (Entry entry : getSelectedEntries()) {
            Entry centry = (Entry)entry.clone();
            centry.raise(amount);
            if (!centry.equals(entry)) {
                _scene.updateEntry(centry);
            }
        }
    }

    /**
     * Returns a new list containing the selected entries.
     */
    protected ArrayList<Entry> getSelectedEntries ()
    {
        ArrayList<Entry> entries = new ArrayList<Entry>();
        _scene.getEntries(_selection.getShape(), entries);
        return entries;
    }

    /** The file to attempt to load on initialization, if any. */
    protected File _initScene;

    /** The revert menu item. */
    protected JMenuItem _revert;

    /** The selection export menu item. */
    protected JMenuItem _exportSelection;

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

    /** The tool bar. */
    protected JToolBar _toolbar;

    /** Toggle buttons. */
    protected JToggleButton _markers, _light, _fog, _sound;

    /** The panel that holds the editor bits. */
    protected JPanel _epanel;

    /** The panel that holds the tool options. */
    protected JPanel _opanel;

    /** Tools mapped by name. */
    protected HashMap<String, EditorTool> _tools = new HashMap<String, EditorTool>();

    /** The global editor tool. */
    protected GlobalEditor _globalEditor;

    /** The arrow tool. */
    protected Arrow _arrow;

    /** The active tool. */
    protected EditorTool _activeTool;

    /** The loaded scene file. */
    protected File _file;

    /** The scene being edited. */
    protected TudeySceneModel _scene;

    /** The scene view. */
    protected TudeySceneView _view;

    /** Whether or not markers are visible. */
    @Scoped
    protected boolean _markersVisible = true;

    /** Whether or not lighting is enabled. */
    @Scoped
    protected boolean _lightingEnabled = true;

    /** Whether or not fog is enabled. */
    @Scoped
    protected boolean _fogEnabled = true;

    /** Whether or not sound is enabled. */
    @Scoped
    protected boolean _soundEnabled = true;

    /** A casted reference to the editor grid. */
    protected EditorGrid _grid;

    /** Whether or not the shift, control, and/or alt keys are being held down. */
    protected boolean _shiftDown, _controlDown, _altDown;

    /** Whether or not each of the mouse buttons are being held down on the canvas. */
    protected boolean _firstButtonDown, _secondButtonDown, _thirdButtonDown;

    /** The selection element. */
    protected ShapeSceneElement _selection;

    /** Used for picking. */
    protected Ray3D _pick = new Ray3D();

    /** Holds the location of the pick result. */
    protected Vector3f _pt = new Vector3f();

    /** The application preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(SceneEditor.class);

    /** The size of the tool buttons. */
    protected static final Dimension TOOL_BUTTON_SIZE = new Dimension(28, 28);
}
