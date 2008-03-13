//
// $Id$

package com.threerings.opengl.effect.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.lwjgl.opengl.GL11;

import com.samskivert.swing.GroupLayout;
import com.samskivert.util.ListUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.math.Vector3f;

import com.threerings.editor.Editable;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.export.BinaryExporter;
import com.threerings.export.BinaryImporter;
import com.threerings.export.Exportable;
import com.threerings.export.XMLExporter;
import com.threerings.export.XMLImporter;

import com.threerings.opengl.GlCanvasApp;
import com.threerings.opengl.camera.CameraHandler;
import com.threerings.opengl.camera.MouseOrbiter;
import com.threerings.opengl.camera.OrbitCameraHandler;
import com.threerings.opengl.effect.ParticleSystem;
import com.threerings.opengl.effect.ParticleSystem.Layer;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.util.Compass;
import com.threerings.opengl.util.DebugBounds;
import com.threerings.opengl.util.Grid;
import com.threerings.opengl.util.SimpleRenderable;

import static java.util.logging.Level.*;
import static com.threerings.opengl.Log.*;

/**
 * The particle editor application.
 */
public class ParticleEditor extends GlCanvasApp
    implements ActionListener, ListSelectionListener
{
    /**
     * An editable preferences object (implements {@link Exportable} to make sure that Proguard
     * doesn't remove its methods).
     */
    public class PreferencesObject
        implements Exportable
    {
        public PreferencesObject ()
        {
            String bgcolor = _prefs.get("background_color", "0.8, 0.8, 0.8, 1");
            try {
                _renderer.setClearColor(new Color4f(StringUtil.parseFloatArray(bgcolor)));
            } catch (Exception e) {
                e.printStackTrace();
            }
            _texturePath = new File(_prefs.get("texture_path", System.getProperty("user.dir")));
        }

        /**
         * Sets the background color.
         */
        @Editable(weight=0)
        public void setBackgroundColor (Color4f color)
        {
            _renderer.setClearColor(color);
            _prefs.put("background_color",
                color.r + ", " + color.g + ", " + color.b + ", " + color.a);
        }

        /**
         * Returns the background color.
         */
        @Editable
        public Color4f getBackgroundColor ()
        {
            return _renderer.getClearColor();
        }

        /**
         * Sets the path to use when resolving textures.
         */
        @Editable(weight=1, mode="directory", nullable=false)
        public void setTexturePath (File path)
        {
            _texturePath = path;
            String pstr = path.toString();
            _prefs.put("texture_path", pstr);
            _particles.setPath(pstr);
        }

        /**
         * Returns the texture path.
         */
        @Editable
        public File getTexturePath ()
        {
            return _texturePath;
        }

        /** The path to use when resolving textures. */
        protected File _texturePath;
    }

    /**
     * The program entry point.
     */
    public static void main (String[] args)
    {
        new ParticleEditor(args.length > 0 ? args[0] : null).start();
    }

    /**
     * Creates the particle editor with (optionally) the path to a particle system to load.
     */
    public ParticleEditor (String particles)
    {
        _msgs = _msgmgr.getBundle("particle");
        _initParticles = (particles == null) ? null : new File(particles);

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
        edit.add(createMenuItem("preferences", KeyEvent.VK_P, KeyEvent.VK_P));

        JMenu view = createMenu("view", KeyEvent.VK_V);
        menubar.add(view);
        view.add(createMenuItem("toggle_ground", KeyEvent.VK_G, KeyEvent.VK_G));
        view.add(createMenuItem("toggle_bounds", KeyEvent.VK_B, KeyEvent.VK_B));
        view.add(createMenuItem("toggle_compass", KeyEvent.VK_O, KeyEvent.VK_M));
        view.add(createMenuItem("toggle_stats", KeyEvent.VK_S, KeyEvent.VK_T));
        view.addSeparator();
        view.add(createMenuItem("reset", KeyEvent.VK_R, KeyEvent.VK_R, 0));
        view.add(createMenuItem("recenter", KeyEvent.VK_C, KeyEvent.VK_C));

        // create the file chooser
        _chooser = new JFileChooser(_prefs.get("particle_dir", null));
        _chooser.setFileFilter(new FileFilter() {
            public boolean accept (File file) {
                return file.isDirectory() || file.toString().toLowerCase().endsWith(".dat");
            }
            public String getDescription () {
                return _msgs.get("m.particle_files");
            }
        });

        // and the export chooser
        _exportChooser = new JFileChooser(_prefs.get("particle_export_dir", null));
        _exportChooser.setFileFilter(new FileFilter() {
            public boolean accept (File file) {
                return file.isDirectory() || file.toString().toLowerCase().endsWith(".xml");
            }
            public String getDescription () {
                return _msgs.get("m.xml_files");
            }
        });

        // create the edit panel
        JPanel epanel = GroupLayout.makeVStretchBox(5);
        _frame.add(epanel, BorderLayout.EAST);
        epanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        epanel.setPreferredSize(new Dimension(250, 1));
        epanel.setMaximumSize(new Dimension(250, Integer.MAX_VALUE));

        // create the layer table
        JPanel lpanel = GroupLayout.makeVStretchBox(5);
        epanel.add(lpanel, GroupLayout.FIXED);
        lpanel.add(new JScrollPane(
            _ltable = new JTable() {
                public void changeSelection (
                    int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                    if (columnIndex != 1) {
                        super.changeSelection(rowIndex, columnIndex, toggle, extend);
                    }
                }
            },
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        _ltable.setPreferredScrollableViewportSize(new Dimension(1, 85));
        _ltable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        _ltable.getColumnModel().getSelectionModel().setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION);
        _ltable.getSelectionModel().addListSelectionListener(this);
        _ltable.setDragEnabled(true);
        _ltable.setCellSelectionEnabled(true);
        _ltable.getTableHeader().setReorderingAllowed(false);
        _ltable.getTableHeader().setResizingAllowed(false);
        Component comp =
            ((DefaultCellEditor)_ltable.getDefaultEditor(Boolean.class)).getComponent();
        comp.setBackground(Color.white);
        comp.setFocusable(false);
        final DataFlavor lflavor = new DataFlavor(ParticleSystem.Layer.class, null);
        _ltable.setTransferHandler(new TransferHandler() {
            public int getSourceActions (JComponent comp) {
                return MOVE;
            }
            public boolean canImport (JComponent comp, DataFlavor[] flavors) {
                return ListUtil.containsRef(flavors, lflavor);
            }
            public boolean importData (JComponent comp, Transferable t) {
                try {
                    int row = (Integer)t.getTransferData(lflavor);
                    ((LayerTableModel)_ltable.getModel()).moveLayer(row);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
            protected Transferable createTransferable (JComponent c) {
                final int row = _ltable.getSelectedRow();
                return new Transferable() {
                    public Object getTransferData (DataFlavor flavor) {
                        return row;
                    }
                    public DataFlavor[] getTransferDataFlavors () {
                        return new DataFlavor[] { lflavor };
                    }
                    public boolean isDataFlavorSupported (DataFlavor flavor) {
                        return flavor == lflavor;
                    }
                };
            }
        });
        JPanel bpanel = new JPanel();
        lpanel.add(bpanel, GroupLayout.FIXED);
        bpanel.add(createButton("new_layer", "m.new"));
        bpanel.add(_cloneLayer = createButton("clone_layer", "m.clone"));
        _cloneLayer.setEnabled(false);
        bpanel.add(_deleteLayer = createButton("delete_layer", "m.delete"));
        _deleteLayer.setEnabled(false);

        // create the editor panel
        JPanel ipanel = GroupLayout.makeVStretchBox(5);
        epanel.add(ipanel);
        ipanel.add(_editor = new EditorPanel(_msgs, EditorPanel.CategoryMode.CHOOSER, null));
        _editor.setVisible(false);

        // create the reset button
        bpanel = new JPanel();
        epanel.add(bpanel, GroupLayout.FIXED);
        bpanel.add(createButton("reset"));
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getActionCommand();
        if (action.equals("new")) {
            newParticles();
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
            importParticles();
        } else if (action.equals("export")) {
            exportParticles();
        } else if (action.equals("quit")) {
            System.exit(0);
        } else if (action.equals("preferences")) {
            showPreferences();
        } else if (action.equals("toggle_ground")) {
            _ground = (_ground == null) ? createGround() : null;
        } else if (action.equals("toggle_bounds")) {
            _bounds = (_bounds == null) ? createBounds() : null;
        } else if (action.equals("toggle_compass")) {
            _compass = (_compass == null) ? new Compass(this) : null;
        } else if (action.equals("toggle_stats")) {
            _renderer.setShowStats(!_renderer.getShowStats());
        } else if (action.equals("reset")) {
            _particles.reset();
        } else if (action.equals("recenter")) {
            ((OrbitCameraHandler)_camhand).getTarget().set(Vector3f.ZERO);
        } else if (action.equals("new_layer")) {
            ((LayerTableModel)_ltable.getModel()).newLayer();
        } else if (action.equals("clone_layer")) {
            ((LayerTableModel)_ltable.getModel()).cloneLayer();
        } else if (action.equals("delete_layer")) {
            ((LayerTableModel)_ltable.getModel()).deleteLayer();
        }
    }

    // documentation inherited from interface ListSelectionListener
    public void valueChanged (ListSelectionEvent event)
    {
        int idx = _ltable.getSelectedRow();
        boolean enabled = (idx != -1);
        _cloneLayer.setEnabled(enabled);
        _deleteLayer.setEnabled(enabled);
        _editor.setVisible(enabled);
        if (enabled) {
            _editor.setObject(_particles.getLayers().get(idx));
        }
    }

    /**
     * Creates a menu with the specified name and mnemonic.
     */
    protected JMenu createMenu (String name, int mnemonic)
    {
        JMenu menu = new JMenu(_msgs.get("m." + name));
        menu.setMnemonic(mnemonic);
        return menu;
    }

    /**
     * Creates a menu item with the specified action, mnemonic, and (optional) accelerator.
     */
    protected JMenuItem createMenuItem (String action, int mnemonic, int accelerator)
    {
        return createMenuItem(action, mnemonic, accelerator, KeyEvent.CTRL_MASK);
    }

    /**
     * Creates a menu item with the specified action, mnemonic, and (optional) accelerator
     * key/modifiers.
     */
    protected JMenuItem createMenuItem (
        String action, int mnemonic, int accelerator, int modifiers)
    {
        JMenuItem item = new JMenuItem(_msgs.get("m." + action), mnemonic);
        item.setActionCommand(action);
        item.addActionListener(this);
        if (accelerator != -1) {
            item.setAccelerator(KeyStroke.getKeyStroke(accelerator, modifiers));
        }
        return item;
    }

    /**
     * Creates a button with the specified action.
     */
    protected JButton createButton (String action)
    {
        return createButton(action, "m." + action);
    }

    /**
     * Creates a button with the specified action and translation key.
     */
    protected JButton createButton (String action, String key)
    {
        JButton button = new JButton(_msgs.get(key));
        button.setActionCommand(action);
        button.addActionListener(this);
        return button;
    }

    /**
     * (Re)creates the ground plane.
     */
    protected SimpleRenderable createGround ()
    {
        return new SimpleRenderable(this) {
            protected RenderState[] createStates () {
                RenderState[] states = super.createStates();
                states[RenderState.COLOR_STATE] = new ColorState(
                    new Color4f(0.4f, 0.4f, 0.4f, 1f));
                return states;
            }
            protected void draw () {
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glVertex3f(-32f, -32f, -0.01f);
                GL11.glVertex3f(+32f, -32f, -0.01f);
                GL11.glVertex3f(+32f, +32f, -0.01f);
                GL11.glVertex3f(-32f, +32f, -0.01f);
                GL11.glEnd();
            }
        };
    }

    /**
     * (Re)creates the debug bounds renderer.
     */
    protected DebugBounds createBounds ()
    {
        return new DebugBounds(this) {
            protected void draw () {
                _particles.drawBounds();
            }
        };
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
        // create the preferences object
        _prefsobj = new PreferencesObject();

        // create the reference grid
        _grid = new Grid(this, 65, 1f);
        _grid.getColor().set(0.2f, 0.2f, 0.2f, 1f);

        // initialize the table
        _ltable.setModel(new LayerTableModel());
        _ltable.getColumnModel().getColumn(1).setMaxWidth(60);

        // attempt to load the particle file specified on the command line if any
        // (otherwise, create an empty particle system)
        if (_initParticles != null) {
            open(_initParticles);
        } else {
            newParticles();
        }
    }

    @Override // documentation inherited
    protected void updateScene ()
    {
        long time = System.currentTimeMillis();
        float elapsed = (_lastTick == 0L) ? 0f : (time - _lastTick) / 1000f;
        _particles.tick(elapsed);
        _lastTick = time;
    }

    @Override // documentation inherited
    protected void renderScene ()
    {
        // queue up the ground
        if (_ground != null) {
            _ground.enqueue();
        }

        // and the grid
        _grid.enqueue();

        // and the particles
        _particles.enqueue();

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
     * Creates a new particle system.
     */
    protected void newParticles ()
    {
        _particles = new ParticleSystem();
        initParticles(_chooser.getCurrentDirectory().toString());
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
        _prefs.put("particle_dir", _chooser.getCurrentDirectory().toString());
    }

    /**
     * Attempts to open the specified particle file.
     */
    protected void open (File file)
    {
        try {
            BinaryImporter in = new BinaryImporter(new FileInputStream(file));
            _particles = (ParticleSystem)in.readObject();
            initParticles(file.getParent());
            in.close();
            setFile(file);
        } catch (IOException e) {
            log.log(WARNING, "Failed to open particles [file=" + file + "].", e);
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
        _prefs.put("particle_dir", _chooser.getCurrentDirectory().toString());
    }

    /**
     * Attempts to save to the specified file.
     */
    protected void save (File file)
    {
        try {
            BinaryExporter out = new BinaryExporter(new FileOutputStream(file));
            out.writeObject(_particles);
            out.close();
            setFile(file);
        } catch (IOException e) {
            log.log(WARNING, "Failed to save particles [file=" + file + "].", e);
        }
    }

    /**
     * Brings up the import dialog.
     */
    protected void importParticles ()
    {
        if (_exportChooser.showOpenDialog(_frame) == JFileChooser.APPROVE_OPTION) {
            File file = _exportChooser.getSelectedFile();
            try {
                XMLImporter in = new XMLImporter(new FileInputStream(file));
                _particles = (ParticleSystem)in.readObject();
                initParticles(file.getParent());
                in.close();
            } catch (IOException e) {
                log.log(WARNING, "Failed to import particles [file=" + file +"].", e);
            }
        }
        _prefs.put("particle_export_dir", _exportChooser.getCurrentDirectory().toString());
    }

    /**
     * Initializes the particles with their context and path.
     */
    protected void initParticles (String path)
    {
        _particles.init(this, _prefsobj.getTexturePath().toString());
        ((LayerTableModel)_ltable.getModel()).fireTableDataChanged();
    }

    /**
     * Brings up the export dialog.
     */
    protected void exportParticles ()
    {
        if (_exportChooser.showSaveDialog(_frame) == JFileChooser.APPROVE_OPTION) {
            File file = _exportChooser.getSelectedFile();
            try {
                XMLExporter out = new XMLExporter(new FileOutputStream(file));
                out.writeObject(_particles);
                out.close();
            } catch (IOException e) {
                log.log(WARNING, "Failed to export particles [file=" + file + "].", e);
            }
        }
        _prefs.put("particle_export_dir", _exportChooser.getCurrentDirectory().toString());
    }

    /**
     * Brings up the preferences dialog.
     */
    protected void showPreferences ()
    {
        if (_pdialog == null) {
            _pdialog = new JDialog(_frame, _msgs.get("t.preferences"));
            EditorPanel epanel = new EditorPanel(_msgs);
            _pdialog.add(epanel, BorderLayout.CENTER);
            epanel.setObject(_prefsobj);
            JPanel bpanel = new JPanel();
            _pdialog.add(bpanel, BorderLayout.SOUTH);
            JButton ok = new JButton(_msgs.get("m.ok"));
            bpanel.add(ok);
            ok.addActionListener(new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    _pdialog.setVisible(false);
                }
            });
            _pdialog.setSize(300, 200);
        }
        showCentered(_pdialog);
    }

    /**
     * Shows a dialog centered on the canvas.
     */
    protected void showCentered (JDialog dialog)
    {
        Point pt = _canvas.getLocationOnScreen();
        dialog.setLocation(
            pt.x + (_canvas.getWidth() - dialog.getWidth()) / 2,
            pt.y + (_canvas.getHeight() - dialog.getHeight()) / 2);
        dialog.setVisible(true);
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
     * A table model for the particle system layers.
     */
    protected class LayerTableModel extends AbstractTableModel
    {
        /**
         * Creates a new layer.
         */
        public void newLayer ()
        {
            // find the highest named layer
            ArrayList<Layer> layers = _particles.getLayers();
            int max = 0;
            String prefix = _msgs.get("m.layer");
            for (Layer layer : layers) {
                String name = layer.name;
                if (!name.startsWith(prefix)) {
                    continue;
                }
                try {
                    max = Math.max(max, Integer.parseInt(name.substring(prefix.length()).trim()));
                } catch (NumberFormatException e) { }
            }
            int idx = layers.size();
            Layer layer = _particles.createLayer(prefix + " " + (max + 1));
            layer.init();
            layers.add(layer);
            fireTableRowsInserted(idx, idx);
            _ltable.changeSelection(idx, 0, false, false);
        }

        /**
         * Clones the currently selected layer.
         */
        public void cloneLayer ()
        {
            int idx = _ltable.getSelectedRow();
            ArrayList<Layer> layers = _particles.getLayers();
            Layer layer = (Layer)layers.get(idx).clone();
            layer.name = layer.name + " " + _msgs.get("m.clone");
            layers.add(++idx, layer);
            fireTableRowsInserted(idx, idx);
            _ltable.changeSelection(idx, 0, false, false);
        }

        /**
         * Deletes the currently selected layer.
         */
        public void deleteLayer ()
        {
            int idx = _ltable.getSelectedRow();
            ArrayList<Layer> layers = _particles.getLayers();
            Layer layer = layers.remove(idx);
            fireTableRowsDeleted(idx, idx);
            int size = layers.size();
            if (idx < size) {
                _ltable.changeSelection(idx, 0, false, false);
            } else if (size > 0) {
                _ltable.changeSelection(size - 1, 0, false, false);
            }
        }

        /**
         * Moves a layer from the specified position to the currently selected index.
         */
        public void moveLayer (int fromIdx)
        {
            int toIdx = _ltable.getSelectedRow();
            if (fromIdx == toIdx) {
                return;
            }
            ArrayList<Layer> layers = _particles.getLayers();
            Layer layer = layers.remove(fromIdx);
            layers.add(toIdx, layer);
            fireTableRowsUpdated(Math.min(fromIdx, toIdx), Math.max(fromIdx, toIdx));
            _editor.setObject(layer);
        }

        // documentation inherited from interface TableModel
        public int getRowCount ()
        {
            return (_particles == null) ? 0 : _particles.getLayers().size();
        }

        // documentation inherited from interface TableModel
        public int getColumnCount ()
        {
            return 2;
        }

        // documentation inherited from interface TableModel
        public Object getValueAt (int row, int column)
        {
            Layer layer = _particles.getLayers().get(row);
            return (column == 0) ? layer.name : layer.visible;
        }

        @Override // documentation inherited
        public String getColumnName (int column)
        {
            return _msgs.get(column == 0 ? "m.layer" : "m.visible");
        }

        @Override // documentation inherited
        public Class<?> getColumnClass (int column)
        {
            return (column == 0) ? String.class : Boolean.class;
        }

        @Override // documentation inherited
        public boolean isCellEditable (int row, int column)
        {
            return true;
        }

        @Override // documentation inherited
        public void setValueAt (Object value, int row, int column)
        {
            Layer layer = _particles.getLayers().get(row);
            if (column == 0) {
                layer.name = (String)value;
            } else {
                layer.visible = (Boolean)value;
            }
        }
    }

    /** The viewer message bundle. */
    protected MessageBundle _msgs;

    /** The file to attempt to load on initialization, if any. */
    protected File _initParticles;

    /** The revert menu item. */
    protected JMenuItem _revert;

    /** The file chooser for opening and saving particle files. */
    protected JFileChooser _chooser;

    /** The file chooser for importing and exporting particle files. */
    protected JFileChooser _exportChooser;

    /** The preferences dialog. */
    protected JDialog _pdialog;

    /** The layer table. */
    protected JTable _ltable;

    /** The clone and delete layer buttons. */
    protected JButton _cloneLayer, _deleteLayer;

    /** The option category combo box. */
    protected JComboBox _categories;

    /** The layer editor panel. */
    protected EditorPanel _editor;

    /** The ground plane. */
    protected SimpleRenderable _ground;

    /** The reference grid. */
    protected Grid _grid;

    /** The bounds display. */
    protected DebugBounds _bounds;

    /** The coordinate system compass. */
    protected Compass _compass;

    /** The preferences object. */
    protected PreferencesObject _prefsobj;

    /** The loaded particle file. */
    protected File _file;

    /** The particle system being edited. */
    protected ParticleSystem _particles;

    /** The time of the last tick. */
    protected long _lastTick;

    /** The application preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(ParticleEditor.class);
}
