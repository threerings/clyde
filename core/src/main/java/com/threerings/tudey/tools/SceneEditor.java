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

package com.threerings.tudey.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;

import org.lwjgl.opengl.GL11;

import com.apple.eawt.Application;
import com.apple.eawt.AppEvent;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.Spacer;
import com.samskivert.swing.util.SwingUtil;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.CollectionUtil;
import com.samskivert.util.RunAnywhere;

import com.threerings.crowd.client.PlaceView;
import com.threerings.media.image.ImageUtil;
import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ManagedConfig;
import com.threerings.config.tools.ConfigEditor;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorMessageBundle;
import com.threerings.editor.tools.BatchValidateDialog;
import com.threerings.editor.util.Validator;
import com.threerings.export.BinaryExporter;
import com.threerings.export.BinaryImporter;
import com.threerings.export.XMLExporter;
import com.threerings.export.XMLImporter;
import com.threerings.expr.Scoped;
import com.threerings.math.FloatMath;
import com.threerings.math.Ray3D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;
import com.threerings.swing.PrintStreamDialog;
import com.threerings.util.ToolUtil;

import com.threerings.opengl.camera.CameraHandler;
import com.threerings.opengl.camera.OrbitCameraHandler;
import com.threerings.opengl.camera.MouseOrbiter;
import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.gui.util.Rectangle;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.util.Grid;
import com.threerings.opengl.util.SimpleTransformable;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.sprite.EntrySprite;
import com.threerings.tudey.client.sprite.Sprite;
import com.threerings.tudey.config.TileConfig;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.AreaEntry;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.GlobalEntry;
import com.threerings.tudey.data.TudeySceneModel.Paint;
import com.threerings.tudey.data.TudeySceneModel.PathEntry;
import com.threerings.tudey.data.TudeySceneModel.PlaceableEntry;
import com.threerings.tudey.data.TudeySceneModel.TileEntry;
import com.threerings.tudey.util.Coord;
import com.threerings.tudey.util.EntryManipulator;
import com.threerings.tudey.util.TudeySceneMetrics;

import static com.threerings.tudey.Log.log;

/**
 * The scene editor application.
 */
public class SceneEditor extends TudeyTool
    implements EntryManipulator, TudeySceneModel.Observer,
        KeyEventDispatcher, MouseListener, ClipboardOwner
{
    /** Allows only tile entries. */
    public static final Predicate<Object> TILE_ENTRY_FILTER =
        Predicates.instanceOf(TileEntry.class);

    /** Allows only placeable entries. */
    public static final Predicate<Object> PLACEABLE_ENTRY_FILTER =
        Predicates.instanceOf(PlaceableEntry.class);

    /** Allows all entries except globals. */
    public static final Predicate<Object> DEFAULT_ENTRY_FILTER =
        Predicates.and(Predicates.instanceOf(Entry.class),
            Predicates.not(Predicates.instanceOf(GlobalEntry.class)));

    /** A function that transforms an Entry to its key. */
    public static final Function<Entry, Object> ENTRY_TO_KEY = new Function<Entry, Object>() {
        public Object apply (Entry entry) {
            return entry.getKey();
        }
    };

    /**
     * The program entry point.
     */
    public static void main (String[] args)
        throws Exception
    {
        // start up the scene editor app
        new SceneEditor(args.length > 0 ? args[0] : null).startup();
    }

    /**
     * Creates the scene editor with (optionally) the path to a scene to load.
     */
    public SceneEditor (String scene)
    {
        super("scene");

        if (RunAnywhere.isMacOS()) {
            setupMacSupport();
        }

        // we override shutdown() and may want to abort a close
        _frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        _initScene = (scene == null) ? null : new File(scene);

        // set the title
        updateTitle();

        // create the undo apparatus
        _undoSupport = new UndoableEditSupport();
        _undoSupport.addUndoableEditListener(_undomgr = new UndoManager());
        _undomgr.setLimit(10000);
        _undoSupport.addUndoableEditListener(new UndoableEditListener() {
            public void undoableEditHappened (UndoableEditEvent event) {
                updateUndoActions();
            }
        });

        // populate the menu bar
        JMenuBar menubar = new JMenuBar();
        _frame.setJMenuBar(menubar);

        menubar.add(_fileMenu = createFileMenu());
        menubar.add(_editMenu = createEditMenu());
        menubar.add(_viewMenu = createViewMenu());
        menubar.add(_toolMenu = createToolMenu());

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

        // and the selection chooser
        _selectionChooser = new JFileChooser(_prefs.get("selection_dir", null));
        _selectionChooser.setFileFilter(new FileFilter() {
            public boolean accept (File file) {
                return file.isDirectory() || file.toString().toLowerCase().endsWith(".dat");
            }
            public String getDescription () {
                return _msgs.get("m.selection_files");
            }
        });

        // populate the tool bar
        _toolbar.setLayout(new HGroupLayout(GroupLayout.STRETCH));
        _toolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        _toolbar.setFloatable(false);
        _toolbar.setRollover(true);
        JButton save = createIconButton("save");
        _toolbar.add(save, GroupLayout.FIXED);
        save.setPressedIcon(createIcon("save_click"));
        _toolbar.add(new Spacer(80, 1), GroupLayout.FIXED);
        _toolbar.add(_markers = createToggleButton("markers"), GroupLayout.FIXED);
        _markers.setSelected(!_markersVisible);
        _toolbar.add(_light = createToggleButton("light"), GroupLayout.FIXED);
        _light.setSelected(!_lightingEnabled);
        _toolbar.add(_fog = createToggleButton("fog"), GroupLayout.FIXED);
        _fog.setSelected(!_fogEnabled);
        _toolbar.add(_sound = createToggleButton("sound"), GroupLayout.FIXED);
        _sound.setSelected(!_soundEnabled);
        _toolbar.add(_camera = createToggleButton("camera"), GroupLayout.FIXED);
        _camera.setSelected(!_cameraEnabled);
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
        addTool(tpanel, tgroup, "selector", _selector = new Selector(this));
        addTool(tpanel, tgroup, "mover", _mover = new Mover(this));
        addTool(tpanel, tgroup, "placer", _placer = new Placer(this));
        addTool(tpanel, tgroup, "path_definer", _pathDefiner = new PathDefiner(this));
        addTool(tpanel, tgroup, "area_definer", _areaDefiner = new AreaDefiner(this));
        addTool(tpanel, tgroup, "global_editor", _globalEditor = new GlobalEditor(this, _layers));
        addTool(tpanel, tgroup, "tile_brush", _tileBrush = new TileBrush(this));
        addTool(tpanel, tgroup, "ground_brush", _groundBrush = new GroundBrush(this));
        addTool(tpanel, tgroup, "wall_brush", _wallBrush = new WallBrush(this));
        addTool(tpanel, tgroup, "palette", _palette = new Palette(this));
        addTool(tpanel, tgroup, "eyedropper", new Eyedropper(this));
        addTool(tpanel, tgroup, "eraser", new Eraser(this));
        addTool(tpanel, tgroup, "notepad", new Notepad(this));

        // create the option panel
        _opanel = GroupLayout.makeVStretchBox(5);

        // set up the layer tool, which is special and added to the _tools map by hand
        _tools.put("layers", _layers);

        _epanel.add(_layerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, _layers, _opanel));
        _layerSplit.setBorder(BorderFactory.createEmptyBorder());

        // restore preferences about where things are
        restorePrefs();

        // activate the arrow tool
        setActiveTool(_arrow);

        // add ourself as a key dispatcher
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);

        // add ourself as a mouse listener
        _canvas.addMouseListener(this);
    }

    /**
     * Configure Mac support, if we're running on a Mac.
     */
    protected void setupMacSupport ()
    {
        Application app = Application.getApplication();
        if (app == null) {
            return;
        }

        // TODO: This was deprecated. I guess it doesn't really 
        // we do not handle the "About" menu item, so remove it.
//        app.removeAboutMenuItem();

        // add a listener to gracefully shut down
        app.setQuitHandler(new QuitHandler() {
            @Override public void handleQuitRequestWith (AppEvent.QuitEvent e, QuitResponse resp) {
                resp.cancelQuit();
                shutdown();
            }
        });
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
     * Checks whether either of the special modifiers (control or alt) is down.
     */
    public boolean isSpecialDown ()
    {
        return isControlDown() || isAltDown();
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
     * Clears the selection.
     */
    public void clearSelection ()
    {
        setSelection();
    }

    /**
     * Returns a Predicate that selects entries only on the current layer.
     */
    public Predicate<Entry> getLayerPredicate ()
    {
        return new Predicate<Entry>() {
            public boolean apply (Entry entry) {
                return (_layers.getSelectedLayer() == _scene.getLayer(entry.getKey()));
            }
        };
    }

    /**
     * Returns a Predicate that selects entries on any visible layer.
     */
    public Predicate<Entry> getVisiblePredicate ()
    {
        final List<Boolean> vis = _layers.getLayerVisibility(); // returns an ever-changing view
        return new Predicate<Entry>() {
            public boolean apply (Entry entry) {
                return vis.get(_scene.getLayer(entry.getKey()));
            }
        };
    }

    /**
     * Selects the specified entries and switches to either the selector tool or the arrow
     * tool, depending on whether multiple entries are selected.
     */
    public void select (Entry... selection)
    {
        if (selection.length > 1) {
            setActiveTool(_selector);
        } else if (selection.length > 0) {
            setActiveTool(_arrow);
            _arrow.edit(selection[0]);
            return; // the arrow sets the selection
        }
        setSelection(selection);
    }

    /**
     * Sets the selected elements.
     */
    public void setSelection (Entry... selection)
    {
        // update the sprites' selected states
        for (Entry entry : _selection) {
            EntrySprite sprite = _view.getEntrySprite(entry.getKey());
            if (sprite != null) {
                sprite.setSelected(false);
            } else {
                // this is fine; the sprite has already been deleted
            }
        }
        for (Entry entry : (_selection = selection)) {
            EntrySprite sprite = _view.getEntrySprite(entry.getKey());
            if (sprite != null) {
                sprite.setSelected(true);
            } else {
                log.warning("Missing sprite for selected entry.", "entry", entry);
            }
        }
        // clear the selection pivot for the next rotation
        _selectionPivot = null;

        // update the ui bits
        boolean enable = (selection.length > 0);
        _exportSelection.setEnabled(enable);
        _cut.setEnabled(enable);
        _copy.setEnabled(enable);
        _delete.setEnabled(enable);
        _rotateCW.setEnabled(enable);
        _rotateCCW.setEnabled(enable);
        _raise.setEnabled(enable);
        _lower.setEnabled(enable);
        _saveToPalette.setEnabled(enable);
        _snapGrid.setEnabled(enable);
        _centerSelected.setEnabled(enable);
    }

    /**
     * Set the selected entries as "recent" in their appropriate tools.
     * This is separated from setSelection because the Selector tool updates
     * selection every tick until the mouse is finally released.
     */
    public void setSelectionAsRecent ()
    {
        for (Entry entry : getSelection()) {
            ConfigTool<?> tool = getToolForEntry(entry);
            if (tool != null) {
                tool.addAsRecent((ConfigReference<?>)entry.getReference());
            }
        }
    }

    /**
     * Returns the selected elements.
     */
    public Entry[] getSelection ()
    {
        return _selection;
    }

    /**
     * Determines whether the specified entry is selected.
     */
    public boolean isSelected (Entry entry)
    {
        return getSelectionIndex(entry) != -1;
    }

    /**
     * Attempts to edit the entry under the mouse cursor on the currently selected layer.
     */
    public void editMouseEntry ()
    {
        Entry entry = getMouseEntry(DEFAULT_ENTRY_FILTER);
        if (entry != null) {
            setActiveTool(_arrow);
            _arrow.edit(entry);
        }
    }

    /**
     * Attempts to delete the entry under the mouse cursor on the currently selected layer.
     */
    public void deleteMouseEntry ()
    {
        deleteMouseEntry(DEFAULT_ENTRY_FILTER);
    }

    /**
     * Attempts to delete the entry under the mouse cursor on the currently selected layer.
     */
    public void deleteMouseEntry (Predicate<? super Entry> filter)
    {
        Entry entry = getMouseEntry(filter);
        if (entry != null) {
            removeEntries(entry.getKey());
        }
    }

    /**
     * Attempts to "use" the entry under the mouse cursor on the currently selected layer.
     */
    public void useMouseEntry ()
    {
        Entry entry = getMouseEntry();

        @SuppressWarnings("unchecked") // safe: ConfigTool is <T extends ManagedConfig>
        ConfigTool<ManagedConfig> tool = (ConfigTool<ManagedConfig>)getToolForEntry(entry);
        if (tool == null) {
            return;
        }

        // set it as active and jam the reference in
        setActiveTool(tool);
        @SuppressWarnings("unchecked") // safe: all ConfigReferences are <T extends ManagedConfig>
        // ... and we know that the entry type works with the tool
        ConfigReference<ManagedConfig> ref = (ConfigReference<ManagedConfig>)entry.getReference();
        tool.setReference(ref);

        // and we need to customize some stuff for a few tools
        if (entry instanceof PlaceableEntry) {
            _placer.setAngle(((PlaceableEntry)entry).transform
                    .update(Transform3D.RIGID)
                    .getRotation().getRotationZ());

        } else if (entry instanceof TileEntry) {
            _tileBrush.setRotation(((TileEntry)entry).rotation);
        }
    }

    /**
     * Returns a reference to the entry under the mouse cursor on the currently selected layer.
     */
    public Entry getMouseEntry ()
    {
        return getMouseEntry(DEFAULT_ENTRY_FILTER);
    }

    /**
     * Returns a reference to the entry under the mouse cursor on the currently selected layer.
     */
    public Entry getMouseEntry (Predicate<? super Entry> filter)
    {
        if (!getMouseRay(_pick)) {
            return null;
        }
        final Predicate<Entry> pred = Predicates.and(getLayerPredicate(), filter);
        EntrySprite sprite = (EntrySprite)_view.getIntersection(
            _pick, _pt, new Predicate<Sprite>() {
                public boolean apply (Sprite sprite) {
                    return (sprite instanceof EntrySprite) &&
                        pred.apply(((EntrySprite) sprite).getEntry());
                }
            });
        return (sprite == null) ? null : sprite.getEntry();
    }

    /**
     * Starts moving the current selection.
     */
    public void moveSelection ()
    {
        removeAndMove(_selection);
    }

    /**
     * Removes the specified entries, then activates them in the mover tool.
     */
    public void removeAndMove (Entry... entries)
    {
        incrementEditId();
        removeEntries(Arrays.asList(entries));
        move(entries);
    }

    /**
     * Activates the supplied entries in the mover tool.
     */
    public void move (Entry... entries)
    {
        setActiveTool(_mover);
        _mover.move(entries);
    }

    /**
     * Increments the edit id, ensuring that any further edits will not be merged with previous
     * ones.
     */
    public void incrementEditId ()
    {
        _editId++;
    }

    /**
     * Adds an entry, removing any conflicting entries if necessary.
     */
    public void overwriteEntries (Entry... entries)
    {
        Set<Object> toRemoveKeys = Sets.newHashSet();
        List<TileEntry> temp = Lists.newArrayList();
        for (Entry entry : entries) {
            if (entry instanceof TileEntry) {
                TileEntry tentry = (TileEntry)entry;
                TileConfig.Original config = tentry.getConfig(getConfigManager());
                Rectangle region = new Rectangle();
                tentry.getRegion(config, region);
                _scene.getTileEntries(region, temp);
                toRemoveKeys.addAll(Collections2.transform(temp, ENTRY_TO_KEY));
                temp.clear();
            }
        }
        removeEntries(toRemoveKeys.toArray());
        addEntries(entries);
    }

    // documentation inherited from interface EntryManipulator
    public void addEntries (Entry... entries)
    {
        if (entries.length == 0) {
            return;
        }
        for (Entry entry : entries) {
            if (entry instanceof TileEntry) {
                clearPaint((TileEntry)entry);
                _layers.setSelectedLayer(0);
            }
        }
        _undoSupport.postEdit(
            new EntryEdit(_scene, _editId, _layers.getSelectedLayer(),
                entries, new Entry[0], new Object[0]));
    }

    // documentation inherited from interface EntryManipulator
    public void updateEntries (Entry... entries)
    {
        if (entries.length == 0) {
            return;
        }
        for (Entry entry : entries) {
            if (entry instanceof TileEntry) {
                clearPaint((TileEntry)_scene.getEntry(entry.getKey()));
                clearPaint((TileEntry)entry);
                _layers.setSelectedLayer(0);
            }
        }
        _undoSupport.postEdit(
            new EntryEdit(_scene, _editId, _layers.getSelectedLayer(),
                new Entry[0], entries, new Object[0]));
    }

    // documentation inherited from interface EntryManipulator
    public void removeEntries (Object... keys)
    {
        if (keys.length == 0) {
            return;
        }
        for (Object key : keys) {
            if (key instanceof Coord) {
                clearPaint((TileEntry)_scene.getEntry(key));
                _layers.setSelectedLayer(0);
            }
        }
        _undoSupport.postEdit(
            new EntryEdit(_scene, _editId, _layers.getSelectedLayer(),
                new Entry[0], new Entry[0], keys));
    }

    // documentation inherited from interface EntryManipulator
    public void removeEntries (Collection<? extends Entry> coll)
    {
        removeEntries(Collections2.transform(coll, ENTRY_TO_KEY).toArray());
    }

    // documentation inherited from interface EntryManipulator
    public void setPaint (Rectangle region, Paint paint)
    {
        _undoSupport.postEdit(new EntryEdit(_scene, _editId, _layers.getSelectedLayer(),
            region, paint));
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryAdded (Entry entry)
    {
        // no-op
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryUpdated (Entry oentry, Entry nentry)
    {
        // update selection
        int idx = getSelectionIndex(oentry);
        if (idx != -1) {
            _selection[idx] = nentry;
        }
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryRemoved (Entry oentry)
    {
        // update selection
        int idx = getSelectionIndex(oentry);
        if (idx != -1) {
            setSelection(ArrayUtil.splice(_selection, idx, 1));
        }
    }

    // documentation inherited from interface KeyEventDispatcher
    public boolean dispatchKeyEvent (KeyEvent event)
    {
        boolean pressed;
        int id = event.getID();
        if (id == KeyEvent.KEY_PRESSED) {
            pressed = true;
        } else if (id == KeyEvent.KEY_RELEASED) {
            pressed = false;
        } else {
            return false;
        }
        switch (event.getKeyCode()) {
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
        return false;
    }

    // documentation inherited from interface MouseListener
    public void mouseClicked (MouseEvent event)
    {
        if (mouseCameraEnabled() && event.getButton() == MouseEvent.BUTTON1 &&
                event.getClickCount() == 2) {
            editMouseEntry();
        }
    }

    // documentation inherited from interface MouseListener
    public void mousePressed (MouseEvent event)
    {
        if ((event.getModifiersEx() & InputEvent.ALT_DOWN_MASK) != 0) {
            useMouseEntry();
        }
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
        incrementEditId();
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
        incrementEditId();
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

    // documentation inherited from interface ClipboardOwner
    public void lostOwnership (Clipboard clipboard, Transferable contents)
    {
        _paste.setEnabled(false);
    }

    @Override
    public ConfigManager getConfigManager ()
    {
        return (_scene != null)
            ? _scene.getConfigManager()
            : super.getConfigManager();
    }

    @Override
    public void setPlaceView (PlaceView view)
    {
        super.setPlaceView(view);
        _testing = true;

        // hide editor ui
        if (_activeTool != null) {
            _activeTool.deactivate();
        }
        _frame.getJMenuBar().setVisible(false);
        _divsize = _pane.getDividerSize();
        _gridEnabled = _showGrid.isSelected();
        _showGrid.setSelected(false);
        _compassEnabled = _showCompass.isSelected();
        _showCompass.setSelected(false);
        _pane.setDividerSize(0);
        _toolbar.setVisible(false);
        _epanel.setVisible(false);
        SwingUtil.refresh((JComponent)_frame.getContentPane());
    }

    @Override
    public void clearPlaceView (PlaceView view)
    {
        // switch back to the editor view
        setView(_view);
        _testing = false;

        // show editor ui
        if (_activeTool != null) {
            _activeTool.activate();
        }
        _frame.getJMenuBar().setVisible(true);
        _pane.setDividerSize(_divsize);
        _showGrid.setSelected(_gridEnabled);
        _showCompass.setSelected(_compassEnabled);
        _toolbar.setVisible(true);
        _epanel.setVisible(true);
        _pane.resetToPreferredSizes();
        SwingUtil.refresh((JComponent)_frame.getContentPane());
    }

    @Override
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getActionCommand();
        EditorTool tool = _tools.get(action);
        if (tool != null) {
            setActiveTool(tool);
            return;
        }
        if (action.equals("new")) {
            if (saveWarning("clear the scene")) {
                newScene();
            }
        } else if (action.equals("open")) {
            if (saveWarning("open a new scene")) {
                open();
            }
        } else if (action.equals("save")) {
            if (_file != null) {
                save(_file);
            } else {
                save();
            }
        } else if (action.equals("save_as")) {
            save();
        } else if (action.equals("revert")) {
            if (saveWarning("revert to the last saved version")) {
                open(_file);
            }
        } else if (action.equals("import")) {
            importScene();
        } else if (action.equals("export")) {
            exportScene();
        } else if (action.equals("import_selection")) {
            importSelection();
        } else if (action.equals("export_selection")) {
            exportSelection();
        } else if (action.equals("test")) {
            testScene();
        } else if (action.equals("undo")) {
            _undomgr.undo();
            updateUndoActions();
        } else if (action.equals("redo")) {
            _undomgr.redo();
            updateUndoActions();
        } else if (action.equals("cut")) {
            copySelection();
            deleteSelection();
        } else if (action.equals("copy")) {
            copySelection();
        } else if (action.equals("paste")) {
            Transferable contents = _frame.getToolkit().getSystemClipboard().getContents(this);
            Entry[] selection = (Entry[])ToolUtil.getWrappedTransferData(contents);
            if (selection != null) {
                move(selection);
            }
        } else if (action.equals("delete")) {
            deleteSelection();
        } else if (action.equals("rotate_ccw")) {
            rotateSelection(+1);
        } else if (action.equals("rotate_cw")) {
            rotateSelection(-1);
        } else if (action.equals("raise")) {
            raiseSelection(+1);
        } else if (action.equals("lower")) {
            raiseSelection(-1);
        } else if (action.equals("save_to_palette")) {
            setActiveTool(_palette);
            _palette.add(_selection);
        } else if (action.equals("validate_refs")) {
            validateReferences();
        } else if (action.equals("delete_errors")) {
            deleteErrors();
        } else if (action.equals("configs")) {
            ConfigEditor.create(this).setVisible(true);
        } else if (action.equals("raise_grid")) {
            _grid.setElevation(_grid.getElevation() + 1);
        } else if (action.equals("lower_grid")) {
            _grid.setElevation(_grid.getElevation() - 1);
        } else if (action.equals("snap_grid")) {
            if (_selection.length > 0) {
                int elevation = _selection[0].getElevation();
                if (elevation > Integer.MIN_VALUE) {
                    _grid.setElevation(elevation);
                }
            }
        } else if (action.equals("reorient")) {
            ((OrbitCameraHandler)_camhand).getCoords().set(
                TudeySceneMetrics.getDefaultCameraConfig().coords);
        } else if (action.equals("center_selected")) {
            if (_selection.length > 0) {
                Vector2f trans = _selection[0].getTranslation(_cfgmgr);
                ((OrbitCameraHandler)_camhand).getTarget().set(new Vector3f(
                        trans.x, trans.y,
                        TudeySceneMetrics.getTileZ(_selection[0].getElevation())));
            }
        } else if (action.equals("next_layer")) {
            _layers.selectLayer(true);
        } else if (action.equals("prev_layer")) {
            _layers.selectLayer(false);
        } else if (action.equals("markers")) {
            _prefs.putBoolean("markersVisible", _markersVisible = !_markers.isSelected());
            wasUpdated();
        } else if (action.equals("light")) {
            _prefs.putBoolean("lightingEnabled", _lightingEnabled = !_light.isSelected());
            wasUpdated();
        } else if (action.equals("fog")) {
            _prefs.putBoolean("fogEnabled", _fogEnabled = !_fog.isSelected());
            wasUpdated();
        } else if (action.equals("sound")) {
            _prefs.putBoolean("soundEnabled", _soundEnabled = !_sound.isSelected());
            wasUpdated();
        } else if (action.equals("camera")) {
            _prefs.putBoolean("cameraEnabled", _cameraEnabled = !_camera.isSelected());
            wasUpdated();
        } else if (action.equals("batch_validate")) {
            new BatchValidateDialog(this, _frame, _prefs) {
                @Override protected boolean validate (Validator validator, String path)
                        throws Exception {
                    TudeySceneModel model = (TudeySceneModel)new BinaryImporter(
                        new FileInputStream(_rsrcmgr.getResourceFile(path))).readObject();
                    model.getConfigManager().init("scene", _cfgmgr);
                    return model.validateReferences(validator);
                }
            }.setVisible(true);
        } else {
            super.actionPerformed(event);
        }
    }

    @Override
    public void shutdown ()
    {
        if (saveWarning("quit")) {
            super.shutdown();
        }
    }

    protected JMenu createFileMenu ()
    {
        JMenu file = createMenu("file", KeyEvent.VK_F);

        file.add(createMenuItem("new", KeyEvent.VK_N, KeyEvent.VK_N));
        file.add(createMenuItem("open", KeyEvent.VK_O, KeyEvent.VK_O));
        file.add(_recents = new JMenu(_msgs.get("m.recent")));
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
        file.add(createMenuItem("test", KeyEvent.VK_T, KeyEvent.VK_B));
        file.addSeparator();
        file.add(createMenuItem("quit", KeyEvent.VK_Q, KeyEvent.VK_Q));

        return file;
    }

    protected JMenu createEditMenu ()
    {
        JMenu edit = createMenu("edit", KeyEvent.VK_E);
        edit.add(_undo = createAction("undo", KeyEvent.VK_U, KeyEvent.VK_Z));
        _undo.setEnabled(false);
        edit.add(_redo = createAction("redo", KeyEvent.VK_R, KeyEvent.VK_Y));
        _redo.setEnabled(false);
        edit.addSeparator();
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
        edit.add(_rotateCW = createMenuItem("rotate_ccw", KeyEvent.VK_O, KeyEvent.VK_LEFT));
        _rotateCW.setEnabled(false);
        edit.add(_rotateCCW = createMenuItem("rotate_cw", KeyEvent.VK_E, KeyEvent.VK_RIGHT));
        _rotateCCW.setEnabled(false);
        edit.addSeparator();
        edit.add(_raise = createMenuItem("raise", KeyEvent.VK_A, KeyEvent.VK_UP));
        _raise.setEnabled(false);
        edit.add(_lower = createMenuItem("lower", KeyEvent.VK_L, KeyEvent.VK_DOWN));
        _lower.setEnabled(false);
        edit.addSeparator();
        edit.add(_saveToPalette = createMenuItem("save_to_palette", KeyEvent.VK_V, KeyEvent.VK_L));
        _saveToPalette.setEnabled(false);
        edit.addSeparator();
        edit.add(createMenuItem("validate_refs", KeyEvent.VK_I, -1));
        edit.add(createMenuItem("delete_errors", KeyEvent.VK_E, -1));
        edit.addSeparator();
        edit.add(createMenuItem("configs", KeyEvent.VK_N, KeyEvent.VK_G));
        edit.add(createMenuItem("resources", KeyEvent.VK_S, KeyEvent.VK_E));
        edit.add(createMenuItem("preferences", KeyEvent.VK_F, KeyEvent.VK_P));

        return edit;
    }

    protected JMenu createViewMenu ()
    {
        JMenu view = createMenu("view", KeyEvent.VK_V);

        view.add(_showGrid = createCheckBoxMenuItem("grid", KeyEvent.VK_G, KeyEvent.VK_D));
        _showGrid.setSelected(true);
        view.add(_showCompass = createCheckBoxMenuItem("compass", KeyEvent.VK_O, KeyEvent.VK_M));
        _showCompass.setSelected(true);
        view.add(_showStats = createCheckBoxMenuItem("stats", KeyEvent.VK_S, KeyEvent.VK_T));
        view.addSeparator();
        view.add(createMenuItem("refresh", KeyEvent.VK_F, KeyEvent.VK_F));
        view.addSeparator();
        view.add(createMenuItem("raise_grid", KeyEvent.VK_R, KeyEvent.VK_UP, 0));
        view.add(createMenuItem("lower_grid", KeyEvent.VK_L, KeyEvent.VK_DOWN, 0));
        view.add(_snapGrid = createMenuItem("snap_grid", KeyEvent.VK_A, KeyEvent.VK_RIGHT, 0));
        _snapGrid.setEnabled(false);
        view.addSeparator();
        view.add(createMenuItem("reorient", KeyEvent.VK_I, KeyEvent.VK_I));
        view.add(createMenuItem("recenter", KeyEvent.VK_C, -1));
        view.add(_centerSelected =
                createMenuItem("center_selected", KeyEvent.VK_E, KeyEvent.VK_LEFT, 0));
        _centerSelected.setEnabled(false);
        view.addSeparator();
        view.add(createMenuItem("prev_layer", KeyEvent.VK_P, KeyEvent.VK_UP, KeyEvent.ALT_MASK));
        view.add(createMenuItem("next_layer", KeyEvent.VK_N, KeyEvent.VK_DOWN, KeyEvent.ALT_MASK));

        return view;
    }

    protected JMenu createToolMenu ()
    {
        JMenu tools = createMenu("tools", KeyEvent.VK_T);
        tools.add(createMenuItem("batch_validate", KeyEvent.VK_B, -1));

        return tools;
    }

    @Override
    protected JComponent createCanvasContainer ()
    {
        JPanel ccont = new JPanel(new BorderLayout());
        ccont.add(_toolbar = new JToolBar(), BorderLayout.NORTH);
        ccont.add(_canvas, BorderLayout.CENTER);
        _pane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, true, ccont, _epanel = GroupLayout.makeVStretchBox(5));
        _canvas.setMinimumSize(new Dimension(1, 1));
        _canvas.setPreferredSize(new Dimension(1, 1));
        _pane.setResizeWeight(1.0);
        _pane.setOneTouchExpandable(true);
        JComponent bcomp = (_canvas instanceof JComponent) ? ((JComponent)_canvas) : ccont;
        bindAction(bcomp, KeyEvent.VK_UP, 0, "raise_grid");
        bindAction(bcomp, KeyEvent.VK_DOWN, 0, "lower_grid");
        bindAction(bcomp, KeyEvent.VK_RIGHT, 0, "snap_grid");
        bindAction(bcomp, KeyEvent.VK_LEFT, 0, "center_selected");
        return _pane;
    }

    @Override
    protected Grid createGrid ()
    {
        return (_grid = new EditorGrid(this));
    }

    @Override
    protected CanvasToolPrefs createEditablePrefs ()
    {
        return new SceneEditorPrefs(_prefs);
    }

    @Override
    protected CameraHandler createCameraHandler ()
    {
        // just a placeholder; the scene view has the real camera handler
        return new OrbitCameraHandler(this);
    }

    @Override
    protected void didInit ()
    {
        super.didInit();

        // create the scene view
        setView(_view = new TudeySceneView(this) {
            @Override public void wasRemoved () {
                // do not dispose of the sprites/scene
                _ctx.getRoot().removeWindow(_inputWindow);
                if (_loadingWindow != null) {
                    _ctx.getRoot().removeWindow(_loadingWindow);
                    _loadingWindow = null;
                }
                if (_ctrl != null) {
                    _ctrl.wasRemoved();
                }
                _scene.clearEffects();
            }
            @Override protected OrbitCameraHandler createCameraHandler () {
                // camera target elevation matches grid elevation
                OrbitCameraHandler camhand = new OrbitCameraHandler(_ctx) {
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
            @Override protected int getMergeGranularity () {
                return 0; // can't merge tiles since they must be individually selectable
            }
        });

        // initialize the tools
        for (EditorTool tool : _tools.values()) {
            tool.init();
        }

        // create the origin renderable
        _origin = new SimpleTransformable(this, RenderQueue.OPAQUE, 0, false, 2) {
            @Override protected void draw () {
                float z = _grid.getZ() + 0.01f;
                GL11.glBegin(GL11.GL_LINES);
                GL11.glVertex3f(-1f, 0f, z);
                GL11.glVertex3f(+1f, 0f, z);
                GL11.glVertex3f(0f, -1f, z);
                GL11.glVertex3f(0f, +1f, z);
                GL11.glEnd();
            }
        };
        _origin.getStates()[RenderState.COLOR_STATE] = ColorState.getInstance(Color4f.RED);

        // attempt to load the scene file specified on the command line if any
        // (otherwise, create an empty scene)
        if (_initScene != null) {
            open(_initScene);
        } else {
            newScene();
        }
    }

    @Override
    protected void updateView (float elapsed)
    {
        super.updateView(elapsed);
        if (!_testing) {
            _activeTool.tick(elapsed);
            _grid.tick(elapsed);
        }
    }

    @Override
    protected void compositeView ()
    {
        super.compositeView();
        if (_showGrid.isSelected()) {
            _origin.composite();
        }
        if (!_testing) {
            _activeTool.composite();
        }
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

        // update whether we are showing or hiding layers
        boolean hideLayers = (tool instanceof Notepad);
        if (hideLayers) {
            if (_layers.isVisible()) {
                _layerDividerPos = _layerSplit.getDividerLocation();
                _layers.setVisible(false);
            }
        } else {
            _layers.setVisible(true);
            if (_layerDividerPos != 0) {
                _layerSplit.setDividerLocation(_layerDividerPos);
            }
        }
        boolean forceBase = (tool == _tileBrush) || (tool == _groundBrush) || (tool == _wallBrush);
        if (forceBase) {
            _layers.setSelectedLayer(0);
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
        return !_testing && (_activeTool.allowsMouseCamera() || isControlDown());
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
            _scene.setDirty(false);

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
        if (_scene != null) {
            _scene.removeObserver(this);
        }
        (_scene = scene).addObserver(this);
        _scene.init(_cfgmgr);

        // update the view
        _view.setSceneModel(_scene);

        // notify the tools
        for (EditorTool tool : _tools.values()) {
            tool.sceneChanged(scene);
        }

        // clear the selection and undo manager
        clearSelection();
        _undomgr.discardAllEdits();
        updateUndoActions();
    }

    /**
     * Updates the enabled states of the undo and redo actions.
     */
    protected void updateUndoActions ()
    {
        _undo.setEnabled(_undomgr.canUndo());
        _redo.setEnabled(_undomgr.canRedo());
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
     * Brings up the selection import dialog.
     */
    protected void importSelection ()
    {
        if (_selectionChooser.showOpenDialog(_frame) == JFileChooser.APPROVE_OPTION) {
            File file = _selectionChooser.getSelectedFile();
            try {
                BinaryImporter in = new BinaryImporter(new FileInputStream(file));
                move((Entry[])in.readObject());
                in.close();
            } catch (IOException e) {
                log.warning("Failed to import selection [file=" + file +"].", e);
            }
        }
        _prefs.put("selection_dir", _selectionChooser.getCurrentDirectory().toString());
    }

    /**
     * Brings up the selection export dialog.
     */
    protected void exportSelection ()
    {
        if (_selectionChooser.showSaveDialog(_frame) == JFileChooser.APPROVE_OPTION) {
            File file = _selectionChooser.getSelectedFile();
            try {
                BinaryExporter out = new BinaryExporter(new FileOutputStream(file));
                out.writeObject(_selection);
                out.close();
            } catch (IOException e) {
                log.warning("Failed to export selection [file=" + file + "].", e);
            }
        }
        _prefs.put("selection_dir", _selectionChooser.getCurrentDirectory().toString());
    }

    /**
     * Sets the file and updates the revert item and title bar.
     */
    protected void setFile (File file)
    {
        _file = file;
        _revert.setEnabled(file != null);
        updateTitle();

        // update recents
        if (file != null) {
            String name = file.getPath();
            _recentFiles.remove(name);
            _recentFiles.add(0, name);
            CollectionUtil.limit(_recentFiles, MAX_RECENT_FILES);
            _prefs.put("recent_files", Joiner.on('\n').join(_recentFiles));
        }
        // always update the recent menu
        _recents.removeAll();
        for (String recent : _recentFiles) {
            final File recentFile = new File(recent);
            Action action = new AbstractAction(recentFile.getName()) {
                public void actionPerformed (ActionEvent e) {
                    open(recentFile);
                }
            };
            action.putValue(Action.SHORT_DESCRIPTION, recentFile.getPath());
            _recents.add(new JMenuItem(action));
        }
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
     * Enters the scene test mode.
     */
    protected void testScene ()
    {
        TudeySceneModel scene = createTestScene();

        // configure the scene repository with a copy of our scene
        _server.getSceneRepository().setSceneModel(scene);

        // request to enter
        _scenedir.moveTo(_sceneId);
    }

    protected TudeySceneModel createTestScene ()
    {
        TudeySceneModel scene = _scene.clone();
        // remove non-visibile layers
        scene.init(_scene.getConfigManager());
        scene.version = 1;
        List<Object> toRemove = Lists.newArrayList();
        List<Boolean> visibility = _layers.getLayerVisibility();
        for (Entry entry : scene.getEntries()) {
            Object key = entry.getKey();
            if (!visibility.get(scene.getLayer(key))) {
                toRemove.add(key);
            }
        }
        for (Object key : toRemove) {
            scene.removeEntry(key);
        }

        scene.sceneId = ++_sceneId;

        return scene;
    }

    /**
     * Deletes the entries under the selected region.
     */
    protected void deleteSelection ()
    {
        incrementEditId();
        removeEntries(Arrays.asList(_selection));
    }

    /**
     * Rotates the entries under the selection region by the specified amount.
     */
    protected void rotateSelection (int amount)
    {
        // find the pivot point if not yet computed
        if (_selectionPivot == null) {
            Rect bounds = new Rect(), ebounds = new Rect();
            boolean tiles = false;
            for (Entry entry : _selection) {
                tiles |= (entry instanceof TileEntry);
                entry.getBounds(getConfigManager(), ebounds);
                bounds.addLocal(ebounds);
            }
            Vector2f center = bounds.getCenter();
            if (tiles) {
                // choose the closer of the nearest intersection and the nearest middle point
                Vector2f ci = new Vector2f(Math.round(center.x), Math.round(center.y));
                Vector2f cm = new Vector2f(
                    FloatMath.floor(center.x) + 0.5f, FloatMath.floor(center.y) + 0.5f);
                center = center.distance(ci) < center.distance(cm) ? ci : cm;
            }
            _selectionPivot = center;
        }
        float rotation = FloatMath.HALF_PI * amount;
        Vector2f translation = _selectionPivot.subtract(_selectionPivot.rotate(rotation));
        Transform2D xform = new Transform2D(translation, rotation);

        // transform the entries (retaining the pivot)
        Vector2f opivot = _selectionPivot;
        transformSelection(new Transform3D(xform));
        _selectionPivot = opivot;
    }

    /**
     * Raises or lowers the entries under the selection region by the specified amount.
     */
    protected void raiseSelection (int amount)
    {
        Transform3D xform = new Transform3D(Transform3D.RIGID);
        xform.getTranslation().z = TudeySceneMetrics.getTileZ(amount);
        transformSelection(xform);
        _grid.setElevation(_grid.getElevation() + amount);
    }

    /**
     * Transforms the selection.
     */
    protected void transformSelection (Transform3D xform)
    {
        incrementEditId();
        List<Object> removes = Lists.newArrayList();
        List<Entry> updates = Lists.newArrayList();
        List<Entry> overwrites = Lists.newArrayList();
        Entry[] oselection = _selection;
        Entry[] nselection = new Entry[oselection.length];
        for (int ii = 0; ii < oselection.length; ii++) {
            Entry oentry = oselection[ii];
            Entry nentry = nselection[ii] = (Entry)oentry.clone();
            nentry.transform(getConfigManager(), xform);
            Object okey = oentry.getKey(), nkey = nentry.getKey();
            if (!okey.equals(nkey)) {
                removes.add(okey);
                overwrites.add(nentry);
            } else {
                updates.add(nentry);
            }
        }
        removeEntries(removes.toArray());
        updateEntries(updates.toArray(new Entry[updates.size()]));
        overwriteEntries(overwrites.toArray(new Entry[overwrites.size()]));
        setSelection(nselection);
    }

    /**
     * Copies the selected entries to the clipboard.
     */
    protected void copySelection ()
    {
        // create a cloned array
        Entry[] selection = new Entry[_selection.length];
        for (int ii = 0; ii < _selection.length; ii++) {
            selection[ii] = (Entry)_selection[ii].clone();
        }
        Clipboard clipboard = _frame.getToolkit().getSystemClipboard();
        clipboard.setContents(new ToolUtil.WrappedTransfer(selection), this);
        _paste.setEnabled(true);
    }

    /**
     * Validates the scene's references.
     */
    protected void validateReferences ()
    {
        PrintStreamDialog dialog = new PrintStreamDialog(
            _frame, _msgs.get("m.validate_refs"), _msgs.get("b.ok"));
        _scene.validateReferences(createValidator(dialog.getPrintStream()));
        dialog.maybeShow();
    }

    /**
     * Create the validator for use with validation.
     */
    protected Validator createValidator (PrintStream out)
    {
        return new Validator(out);
    }

    /**
     * Deletes all entries whose configurations are null or missing.
     */
    protected void deleteErrors ()
    {
        incrementEditId();
        Collection<Entry> toProcess =
            (_selection.length > 0) ? Arrays.asList(_selection) : _scene.getEntries();
        Predicate<Entry> isInvalid = new Predicate<Entry>() {
            public boolean apply (Entry entry) {
                return !entry.isValid(getConfigManager());
            }
        };
        removeEntries(Collections2.filter(toProcess, isInvalid));
    }

    /**
     * Returns the index of the specified entry within the selection, or -1 if it is not selected.
     */
    protected int getSelectionIndex (Entry entry)
    {
        Object key = entry.getKey();
        for (int ii = 0; ii < _selection.length; ii++) {
            if (_selection[ii].getKey().equals(key)) {
                return ii;
            }
        }
        return -1;
    }

    /**
     * Get the ConfigTool associated with the specified Entry, if any.
     */
    protected ConfigTool<?> getToolForEntry (Entry entry)
    {
        if (entry instanceof PlaceableEntry) {
            return _placer;

        } else if (entry instanceof TileEntry) {
            return _tileBrush;

        } else if (entry instanceof AreaEntry) {
            return _areaDefiner;

        } else if (entry instanceof PathEntry) {
            return _pathDefiner;

        } else {
            return null;
        }
    }

    /**
     * Clears any paint underneath the specified tile entry.
     */
    protected void clearPaint (TileEntry entry)
    {
        if (entry == null) {
            return;
        }
        TileConfig.Original config = entry.getConfig(getConfigManager());
        Rectangle region = new Rectangle();
        entry.getRegion(config, region);
        setPaint(region, null);
    }

    /**
     * Show a warning if the scene is unsaved. Return true if it's ok to proceed with
     * the operation.
     */
    protected boolean saveWarning (String message)
    {
        if ((_scene == null) || !_scene.isDirty()) {
            return true;
        }
        int option = JOptionPane.showOptionDialog(_frame,
            "Discard unsaved changes and " + message + "?", "Discard changes?",
            JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
            new Object[] { "Cancel", "Save First", "Discard Changes" }, "Cancel");
        switch (option) {
        default:
            return false;

        case 1:
            if (_file != null) {
                save(_file);
                return true;
            }
            save();
            return false;

        case 2:
            return true;
        }
    }

    /**
     * Restore UI preferences.
     */
    protected void restorePrefs ()
    {
        String p = "SceneEditor."; // TODO? getClass().getSimpleName() + "." ???

        // restore/bind window bounds
        _eprefs.bindWindowBounds(p, _frame);

        // restore/bind the location of the main divider
        _eprefs.bindDividerLocation(p + "div", _pane);

        // restore/bind the location of the layer divider
        _eprefs.bindDividerLocation(p + "layerDiv", _layerSplit);
    }

    /**
     * The preferences for the scene editor.
     */
    @EditorMessageBundle("editor.default")
    protected class SceneEditorPrefs extends CanvasToolPrefs
    {
        /**
         * Creates a new preferences object.
         */
        public SceneEditorPrefs (Preferences prefs)
        {
            super(prefs);
        }

        /**
         * Sets the refresh interval.
         */
        @Editable(weight=8)
        public void setDebugRegions (boolean debug)
        {
            _prefs.putBoolean("debug_regions", debug);
        }

        /**
         * Returns the refresh interval.
         */
        @Editable
        public boolean getDebugRegions ()
        {
            return _prefs.getBoolean("debug_regions", false);
        }
    }

    /** The file to attempt to load on initialization, if any. */
    protected File _initScene;

    /** The undo manager. */
    protected UndoManager _undomgr;

    /** The undoable edit support object. */
    protected UndoableEditSupport _undoSupport;

    /** The current edit id. */
    protected int _editId;

    /** The revert menu item. */
    protected JMenuItem _revert;

    /** The selection export menu item. */
    protected JMenuItem _exportSelection;

    /** The undo and redo actions. */
    protected Action _undo, _redo;

    /** The edit menu actions. */
    protected Action _cut, _copy, _paste, _delete;

    /** The file menu. */
    protected JMenu _fileMenu;

    /** The edit menu. */
    protected JMenu _editMenu;

    /** The view menu. */
    protected JMenu _viewMenu;

    /** The tool menu. */
    protected JMenu _toolMenu;

    /** The recently-opened files. */
    protected JMenu _recents;

    /** The rotate menu items. */
    protected JMenuItem _rotateCW, _rotateCCW;

    /** The raise/lower menu items. */
    protected JMenuItem _raise, _lower;

    /** The save-to-palette menu item. */
    protected JMenuItem _saveToPalette;

    /** The view menu items. */
    protected JMenuItem _snapGrid, _centerSelected;

    /** The file chooser for opening and saving scene files. */
    protected JFileChooser _chooser;

    /** The file chooser for importing and exporting scene files. */
    protected JFileChooser _exportChooser;

    /** The file chooser for importing and exporting selections. */
    protected JFileChooser _selectionChooser;

    /** The split pane containing the canvas, toolbar, etc. */
    protected JSplitPane _pane;

    /** The size of the divider. */
    protected int _divsize;

    /** Whether or not the compass/grid are enabled. */
    protected boolean _compassEnabled, _gridEnabled;

    /** Set when we are in test mode. */
    protected boolean _testing;

    /** The tool bar. */
    protected JToolBar _toolbar;

    /** Toggle buttons. */
    protected JToggleButton _markers, _light, _fog, _sound, _camera;

    /** The panel that holds the editor bits. */
    protected JPanel _epanel;

    /** The panel that holds the tool options. */
    protected JPanel _opanel;

    /** Tools mapped by name. */
    protected Map<String, EditorTool> _tools = Maps.newHashMap();

    /** The arrow tool. */
    protected Arrow _arrow;

    /** The selector tool. */
    protected Selector _selector;

    /** The mover tool. */
    protected Mover _mover;

    /** The placer tool. */
    protected Placer _placer;

    /** The path definer tool. */
    protected PathDefiner _pathDefiner;

    /** The area definer tool. */
    protected AreaDefiner _areaDefiner;

    /** The global editor tool. */
    protected GlobalEditor _globalEditor;

    /** The tile brush tool. */
    protected TileBrush _tileBrush;

    /** The ground brush tool. */
    protected GroundBrush _groundBrush;

    /** The wall brush tool. */
    protected WallBrush _wallBrush;

    /** The palette tool. */
    protected Palette _palette;

    /** The pane splitting layers from the other tools. */
    protected JSplitPane _layerSplit;

    /** The last position of the layer divider. */
    protected int _layerDividerPos;

    /** The layer display tool. */
    protected Layers _layers = new Layers(this);

    /** The active tool. */
    protected EditorTool _activeTool;

    /** The loaded scene file. */
    protected File _file;

    /** The scene being edited. */
    protected TudeySceneModel _scene;

    /** The scene view. */
    protected TudeySceneView _view;

    /** The scene id used for testing. */
    protected int _sceneId;

    /** Whether or not markers are visible. */
    @Scoped
    protected boolean _markersVisible = _prefs.getBoolean("markersVisible", true);

    /** Whether or not lighting is enabled. */
    @Scoped
    protected boolean _lightingEnabled = _prefs.getBoolean("lightingEnabled", true);

    /** Whether or not fog is enabled. */
    @Scoped
    protected boolean _fogEnabled = _prefs.getBoolean("fogEnabled", true);

    /** Whether or not sound is enabled. */
    @Scoped
    protected boolean _soundEnabled = _prefs.getBoolean("soundEnabled", true);

    /** Whether or not camera is enabled. */
    @Scoped
    protected boolean _cameraEnabled = _prefs.getBoolean("cameraEnabled", true);

    /** A casted reference to the editor grid. */
    protected EditorGrid _grid;

    /** Draws the coordinate system origin. */
    protected SimpleTransformable _origin;

    /** Whether or not the shift, control, and/or alt keys are being held down. */
    protected boolean _shiftDown, _controlDown, _altDown;

    /** Whether or not each of the mouse buttons are being held down on the canvas. */
    protected boolean _firstButtonDown, _secondButtonDown, _thirdButtonDown;

    /** The selected elements. */
    protected Entry[] _selection = new Entry[0];

    /** The center of rotation for the selection. */
    protected Vector2f _selectionPivot;

    /** Used for picking. */
    protected Ray3D _pick = new Ray3D();

    /** Holds the location of the pick result. */
    protected Vector3f _pt = new Vector3f();

    /** The application preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(SceneEditor.class);

    /** Recently opened files. */
    protected static List<String> _recentFiles = Lists.newArrayList(
        Splitter.on('\n').omitEmptyStrings().split(_prefs.get("recent_files", "")));

    /** The maximum number of recently opened files to show. */
    protected static final int MAX_RECENT_FILES = 6;

    /** The size of the tool buttons. */
    protected static final Dimension TOOL_BUTTON_SIZE = new Dimension(28, 28);
}
