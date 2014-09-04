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

package com.threerings.opengl.effect.tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;

import org.lwjgl.opengl.GL11;

import com.samskivert.swing.GroupLayout;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ListUtil;

import com.threerings.config.ConfigManager;
import com.threerings.config.tools.ConfigEditor;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.export.BinaryExporter;
import com.threerings.export.BinaryImporter;
import com.threerings.export.XMLExporter;
import com.threerings.export.XMLImporter;

import com.threerings.opengl.effect.config.ParticleSystemConfig;
import com.threerings.opengl.effect.config.ParticleSystemConfig.Layer;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.tools.ModelTool;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.util.SimpleTransformable;

import static com.threerings.opengl.Log.log;

/**
 * The particle editor application.
 */
public class ParticleEditor extends ModelTool
    implements ListSelectionListener, ChangeListener
{
    /**
     * The program entry point.
     */
    public static void main (String[] args)
    {
        new ParticleEditor(args.length > 0 ? args[0] : null).startup();
    }

    /**
     * Creates the particle editor with (optionally) the path to a particle system to load.
     */
    public ParticleEditor (String particles)
    {
        super("particle");
        _initParticles = (particles == null) ? null : new File(particles);

        // set the title
        updateTitle();

        // populate the menu bar
        JMenuBar menubar = new JMenuBar();
        _frame.setJMenuBar(menubar);

        JMenu file = createMenu("file", KeyEvent.VK_F);
        menubar.add(file);
        createFileMenuItems(file);

        JMenu edit = createMenu("edit", KeyEvent.VK_E);
        menubar.add(edit);
        edit.add(createMenuItem("configs", KeyEvent.VK_C, KeyEvent.VK_G));
        edit.add(createMenuItem("resources", KeyEvent.VK_R, KeyEvent.VK_U));
        edit.add(createMenuItem("preferences", KeyEvent.VK_P, KeyEvent.VK_P));

        JMenu view = createMenu("view", KeyEvent.VK_V);
        menubar.add(view);
        view.add(_autoReset = createCheckBoxMenuItem("auto_reset", KeyEvent.VK_A, KeyEvent.VK_E));
        view.addSeparator();
        view.add(_showEnvironment =
            createCheckBoxMenuItem("environment", KeyEvent.VK_E, KeyEvent.VK_V));
        _showEnvironment.setSelected(true);
        view.add(_showGround = createCheckBoxMenuItem("ground", KeyEvent.VK_G, KeyEvent.VK_D));
        view.add(_showGrid = createCheckBoxMenuItem("grid", KeyEvent.VK_R, KeyEvent.VK_I));
        _showGrid.setSelected(true);
        view.add(_showBounds = createCheckBoxMenuItem("bounds", KeyEvent.VK_B, KeyEvent.VK_B));
        view.add(_showCompass = createCheckBoxMenuItem("compass", KeyEvent.VK_O, KeyEvent.VK_M));
        view.add(_showStats = createCheckBoxMenuItem("stats", KeyEvent.VK_S, KeyEvent.VK_T));
        view.addSeparator();
        view.add(createMenuItem("refresh", KeyEvent.VK_F, KeyEvent.VK_F));
        view.addSeparator();
        view.add(createMenuItem("recenter", KeyEvent.VK_C, KeyEvent.VK_C));
        view.add(createMenuItem("reset", KeyEvent.VK_R, KeyEvent.VK_R, 0));

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

        // configure the edit panel
        _epanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        _epanel.setPreferredSize(new Dimension(350, 1));

        // create the layer table
        JPanel lpanel = GroupLayout.makeVStretchBox(5);
        _epanel.add(lpanel, GroupLayout.FIXED);
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
        final DataFlavor lflavor = new DataFlavor(Layer.class, null);
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
        bpanel.add(_cloneLayer = createButton("clone_layer", "m.copy"));
        _cloneLayer.setEnabled(false);
        bpanel.add(_deleteLayer = createButton("delete_layer", "m.delete"));
        _deleteLayer.setEnabled(false);

        // create the editor panel
        JPanel ipanel = GroupLayout.makeVStretchBox(5);
        _epanel.add(ipanel);
        ipanel.add(_editor = new EditorPanel(this, EditorPanel.CategoryMode.CHOOSER, null, true));
        _editor.addChangeListener(this);
        _editor.setVisible(false);

        // create the reset button
        bpanel = new JPanel();
        _epanel.add(bpanel, GroupLayout.FIXED);
        bpanel.add(createButton("reset"));
    }

    @Override
    public ConfigManager getConfigManager ()
    {
        return (_model != null)
            ? _model.getConfig().getConfigManager()
            : super.getConfigManager();
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
            _editor.setObject(getLayers()[idx]);
        }
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        _model.getConfig().wasUpdated();
    }

    @Override
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
            if (showCantUndo()) {
                open(_file);
            }
        } else if (action.equals("import")) {
            importParticles();
        } else if (action.equals("export")) {
            exportParticles();
        } else if (action.equals("import_layers")) {
            importLayers();
        } else if (action.equals("configs")) {
            ConfigEditor.create(this).setVisible(true);
        } else if (action.equals("new_layer")) {
            ((LayerTableModel)_ltable.getModel()).newLayer();
        } else if (action.equals("clone_layer")) {
            ((LayerTableModel)_ltable.getModel()).cloneLayer();
        } else if (action.equals("delete_layer")) {
            ((LayerTableModel)_ltable.getModel()).deleteLayer();
        } else {
            super.actionPerformed(event);
        }
    }

    protected void createFileMenuItems (JMenu file)
    {
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
        file.add(createMenuItem("import_layers", KeyEvent.VK_L, KeyEvent.VK_L));
        file.addSeparator();
        file.add(createMenuItem("quit", KeyEvent.VK_Q, KeyEvent.VK_Q));
    }

    @Override
    protected JComponent createCanvasContainer ()
    {
        JSplitPane pane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, true, _canvas, _epanel = GroupLayout.makeVStretchBox(5));
        _canvas.setMinimumSize(new Dimension(1, 1));
        pane.setResizeWeight(1.0);
        pane.setOneTouchExpandable(true);
        return pane;
    }

    @Override
    protected CanvasToolPrefs createEditablePrefs ()
    {
        return new ModelToolPrefs(_prefs);
    }

    @Override
    protected void didInit ()
    {
        super.didInit();

        // create the ground plane
        _ground = new SimpleTransformable(this) {
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

        // create the model
        ModelConfig config = new ModelConfig();
        config.implementation = new ParticleSystemConfig();
        config.init(_cfgmgr);
        _scene.add(_model = new Model(this, config));

        // initialize the table
        _ltable.setModel(new LayerTableModel());
        _ltable.getColumnModel().getColumn(1).setMaxWidth(60);

        // attempt to load the particle file specified on the command line if any
        if (_initParticles != null) {
            open(_initParticles);
        }
    }

    @Override
    protected void updateView (float elapsed)
    {
        super.updateView(elapsed);
        if (_autoReset.isSelected() && _model.hasCompleted()) {
            _model.reset();
        }
    }

    @Override
    protected void compositeView ()
    {
        super.compositeView();
        if (_showGround.isSelected()) {
            _ground.composite();
        }
    }

    /**
     * Creates a new particle system.
     */
    protected void newParticles ()
    {
        ModelConfig config = new ModelConfig();
        config.implementation = new ParticleSystemConfig();
        setConfig(config, null);
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
            setConfig((ModelConfig)in.readObject(), file);
            in.close();
        } catch (Exception e) { // IOException, ClassCastException
            log.warning("Failed to open particles.", "file", file, e);
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
            out.writeObject(_model.getConfig());
            out.close();
            setFile(file);
        } catch (IOException e) {
            log.warning("Failed to save particles.", "file", file, e);
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
                setConfig((ModelConfig)in.readObject(), null);
                in.close();
            } catch (Exception e) { // IOException, ClassCastException
                log.warning("Failed to import particles.", "file", file, e);
            }
        }
        _prefs.put("particle_export_dir", _exportChooser.getCurrentDirectory().toString());
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
                out.writeObject(_model.getConfig());
                out.close();
            } catch (IOException e) {
                log.warning("Failed to export particles.", "file", file, e);
            }
        }
        _prefs.put("particle_export_dir", _exportChooser.getCurrentDirectory().toString());
    }

    /**
     * Brings up the import layers dialog.
     */
    protected void importLayers ()
    {
        if (_chooser.showOpenDialog(_frame) == JFileChooser.APPROVE_OPTION) {
            File file = _chooser.getSelectedFile();
            try {
                BinaryImporter in = new BinaryImporter(new FileInputStream(file));
                ModelConfig config = (ModelConfig)in.readObject();
                ((LayerTableModel)_ltable.getModel()).insertLayers(
                    ((ParticleSystemConfig)config.implementation).layers);
                in.close();
            } catch (Exception e) { // IOException, ClassCastException, NullPointerException
                log.warning("Failed to import layers.", "file", file, e);
            }
        }
        _prefs.put("particle_dir", _chooser.getCurrentDirectory().toString());
    }

    /**
     * Sets the configuration of the particle system.
     */
    protected void setConfig (ModelConfig config, File file)
    {
        if (!(config.implementation instanceof ParticleSystemConfig)) {
            throw new ClassCastException(config.getClass().getName());
        }
        config.init(_cfgmgr);
        _model.setConfig(config);
        ((LayerTableModel)_ltable.getModel()).fireTableDataChanged();
        setFile(file);
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
     * Sets the array of layers and notes that the config was updated.
     */
    protected void setLayers (Layer[] layers)
    {
        getParticleSystemConfig().layers = layers;
        _model.getConfig().wasUpdated();
    }

    /**
     * Returns a reference to the array of layers.
     */
    protected Layer[] getLayers ()
    {
        return getParticleSystemConfig().layers;
    }

    /**
     * Returns a reference to the particle system configuration.
     */
    public ParticleSystemConfig getParticleSystemConfig ()
    {
        return (ParticleSystemConfig)_model.getConfig().implementation;
    }

    /**
     * Shows a confirm dialog.
     */
    protected boolean showCantUndo ()
    {
        return JOptionPane.showConfirmDialog(_frame, _msgs.get("m.cant_undo"),
                _msgs.get("t.cant_undo"), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) == 0;
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
            Layer[] olayers = getLayers();
            int max = 0;
            String prefix = _msgs.get("m.layer");
            for (Layer layer : olayers) {
                String name = layer.name;
                if (!name.startsWith(prefix)) {
                    continue;
                }
                try {
                    max = Math.max(max, Integer.parseInt(name.substring(prefix.length()).trim()));
                } catch (NumberFormatException e) { }
            }
            Layer nlayer = new Layer();
            nlayer.name = prefix + " " + (max + 1);
            setLayers(ArrayUtil.append(olayers, nlayer));
            fireTableRowsInserted(olayers.length, olayers.length);
            _ltable.changeSelection(olayers.length, 0, false, false);
        }

        /**
         * Inserts copies of the specified layers.
         */
        public void insertLayers (Layer[] nlayers)
        {
            if (nlayers.length == 0) {
                return;
            }
            Layer[] olayers = getLayers();
            setLayers(ArrayUtil.concatenate(olayers, nlayers));
            fireTableRowsInserted(olayers.length, olayers.length + nlayers.length - 1);
            _ltable.changeSelection(olayers.length, 0, false, false);
        }

        /**
         * Clones the currently selected layer.
         */
        public void cloneLayer ()
        {
            int idx = _ltable.getSelectedRow();
            Layer[] olayers = getLayers();
            Layer nlayer = (Layer)olayers[idx].clone();
            nlayer.name = nlayer.name + " " + _msgs.get("m.clone");
            setLayers(ArrayUtil.insert(olayers, nlayer, ++idx));
            fireTableRowsInserted(idx, idx);
            _ltable.changeSelection(idx, 0, false, false);
        }

        /**
         * Deletes the currently selected layer.
         */
        public void deleteLayer ()
        {
            int idx = _ltable.getSelectedRow();
            Layer[] layers = ArrayUtil.splice(getLayers(), idx, 1);
            setLayers(layers);
            fireTableRowsDeleted(idx, idx);
            if (idx < layers.length) {
                _ltable.changeSelection(idx, 0, false, false);
            } else if (layers.length > 0) {
                _ltable.changeSelection(layers.length - 1, 0, false, false);
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
            Layer[] layers = getLayers();
            Layer layer = layers[fromIdx];
            if (toIdx < fromIdx) {
                System.arraycopy(layers, toIdx, layers, toIdx + 1, fromIdx - toIdx);
            } else {
                System.arraycopy(layers, fromIdx + 1, layers, fromIdx, toIdx - fromIdx);
            }
            layers[toIdx] = layer;
            _model.getConfig().wasUpdated();
            fireTableRowsUpdated(Math.min(fromIdx, toIdx), Math.max(fromIdx, toIdx));
            _editor.setObject(layer);
        }

        // documentation inherited from interface TableModel
        public int getRowCount ()
        {
            return getLayers().length;
        }

        // documentation inherited from interface TableModel
        public int getColumnCount ()
        {
            return 2;
        }

        // documentation inherited from interface TableModel
        public Object getValueAt (int row, int column)
        {
            Layer layer = getLayers()[row];
            return (column == 0) ? layer.name : layer.visible;
        }

        @Override
        public String getColumnName (int column)
        {
            return _msgs.get(column == 0 ? "m.layer" : "m.visible");
        }

        @Override
        public Class<?> getColumnClass (int column)
        {
            return (column == 0) ? String.class : Boolean.class;
        }

        @Override
        public boolean isCellEditable (int row, int column)
        {
            return true;
        }

        @Override
        public void setValueAt (Object value, int row, int column)
        {
            Layer layer = getLayers()[row];
            if (column == 0) {
                layer.name = (String)value;
            } else {
                layer.visible = (Boolean)value;
            }
            _model.getConfig().wasUpdated();
        }
    }

    /** The file to attempt to load on initialization, if any. */
    protected File _initParticles;

    /** The revert menu item. */
    protected JMenuItem _revert;

    /** The toggle for automatic reset. */
    protected JCheckBoxMenuItem _autoReset;

    /** The toggle for the ground view. */
    protected JCheckBoxMenuItem _showGround;

    /** The file chooser for opening and saving particle files. */
    protected JFileChooser _chooser;

    /** The file chooser for importing and exporting particle files. */
    protected JFileChooser _exportChooser;

    /** The panel that holds the editor bits. */
    protected JPanel _epanel;

    /** The layer table. */
    protected JTable _ltable;

    /** The clone and delete layer buttons. */
    protected JButton _cloneLayer, _deleteLayer;

    /** The layer editor panel. */
    protected EditorPanel _editor;

    /** The ground plane. */
    protected SimpleTransformable _ground;

    /** The loaded particle file. */
    protected File _file;

    /** The application preferences. */
    protected static Preferences _prefs = Preferences.userNodeForPackage(ParticleEditor.class);
}

