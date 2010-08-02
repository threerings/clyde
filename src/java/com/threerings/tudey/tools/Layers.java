//
// $Id$

package com.threerings.tudey.tools;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

import com.samskivert.swing.GroupLayout;

import com.threerings.opengl.model.Model;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.sprite.EntrySprite;
import com.threerings.tudey.data.TudeySceneModel;

/**
 * The Layer display widget.
 */
public class Layers extends EditorTool
{
    /**
     * Create the layer display tool.
     */
    public Layers (SceneEditor editor)
    {
        super(editor);
        ((GroupLayout) getLayout()).setGap(0);
        _tableModel = new LayerTableModel(editor);
        _table = _tableModel.getTable();
        _table.setPreferredScrollableViewportSize(new Dimension(100, 64));
        _table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        _table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged (ListSelectionEvent event) {
                int layer = getSelectedLayer();
                // if no layer is selected, select layer 0 (avoid triggering this unless
                // we're in the process of adjusting)
                if (layer == -1 && !event.getValueIsAdjusting() && (event.getFirstIndex() != -1)) {
                    layer = 0;
                    setSelectedLayer(layer);
                }
                _removeLayerAction.setEnabled(layer != 0);
            }
        });
        _tableModel.addTableModelListener(new TableModelListener() {
            public void tableChanged (TableModelEvent event) {
                _mergeVisibleLayersAction.setEnabled(_tableModel.getVisibleLayers().size() >= 2);
            }
        });
        _removeLayerAction.setEnabled(false);
        _mergeVisibleLayersAction.setEnabled(false);

        add(GroupLayout.makeButtonBox(new JLabel("Layers"),
            new JButton(_addLayerAction), new JButton(_removeLayerAction),
            new JButton(_mergeVisibleLayersAction)), GroupLayout.FIXED);
        add(new JScrollPane(_table));
    }

    /**
     * Get the currently selected layer.
     */
    public int getSelectedLayer ()
    {
        return _table.getSelectedRow();
    }

    /**
     * Set the currently selected layer.
     */
    public void setSelectedLayer (int layer)
    {
        _table.setRowSelectionInterval(layer, layer);
        // and scroll the selected row to be visible
        int rowHeight = _table.getRowHeight();
        _table.scrollRectToVisible(new Rectangle(0, rowHeight * layer, 1, rowHeight));
    }

    /**
     * Select the next or previous layer, and update the visibility of all
     * layers so that only the selected layer (and the base (0)) are visible.
     */
    public void selectLayer (boolean next)
    {
        int layerCount = _table.getRowCount();
        int layer = (getSelectedLayer() + (next ? 1 : layerCount - 1)) % layerCount;
        _tableModel.setVisibilities(0, layer);
        setSelectedLayer(layer);
    }

    @Override
    public void sceneChanged (TudeySceneModel scene)
    {
        super.sceneChanged(scene);
        _tableModel.setScene(scene);
        setSelectedLayer(0);
    }

    protected void mergeVisible ()
    {
        TudeySceneView view = _editor.getView();
        List<Integer> visible = _tableModel.getVisibleLayers();
        // remove the first layer and call that the "mergeTo" layer
        int mergeTo = visible.remove(0);

        // move all entries in the merged layers to the mergeTo layer
        for (TudeySceneModel.Entry entry : _scene.getEntries()) {
            Object key = entry.getKey();
            if (visible.contains(_scene.getLayer(key))) {
                _scene.setLayer(key, mergeTo);
            }
        }

        // kill the layers, highest to lowest
        for (int ii = visible.size() - 1; ii >= 0; ii--) {
            _tableModel.removeLayer(visible.get(ii));
        }
    }

    /**
     * Utility to ask for confirmation of a layer operation.
     */
    protected boolean confirm (String title, String message)
    {
        return JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
            this, message, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
    }

    /** The table. */
    protected JTable _table;

    /** The table model. */
    protected LayerTableModel _tableModel;

    /** An action for adding a new layer. */
    protected Action _addLayerAction = new AbstractAction("+") {
        public void actionPerformed (ActionEvent e) {
            int newLayer = _scene.getLayers().size();
            _tableModel.addLayer("Layer " + newLayer);
            // immediately select it
            setSelectedLayer(newLayer);
        }
        { // initializer
            putValue(Action.SHORT_DESCRIPTION, "Add a new layer");
        }
    };

    /** An action for removing the currently selected layer. */
    protected Action _removeLayerAction = new AbstractAction("-") {
        public void actionPerformed (ActionEvent e) {
            int layer = getSelectedLayer();
            if (_scene.isLayerEmpty(layer) ||
                    confirm("Layer is not empty!", "This layer has stuff on it.\n" +
                        "Continue anyway and move the stuff to the base layer?")) {
                _tableModel.removeLayer(layer);
            }
        }
        { // initializer
            putValue(Action.SHORT_DESCRIPTION, "Remove the selected layer");
        }
    };

    /** An action for merging the visible layers. */
    protected Action _mergeVisibleLayersAction = new AbstractAction("Merge visible") {
        public void actionPerformed (ActionEvent e) {
            if (confirm("Merge layers...", "This cannot be un-done.\n" +
                "Are you sure you want to merge the visible layers?")) {
                mergeVisible();
            }
        }
        { // initializer
            putValue(Action.SHORT_DESCRIPTION, "Merge visible layers into the lowest visible");
        }
    };
}

/**
 * A table model for the table in the layer tool.
 */
class LayerTableModel extends AbstractTableModel
    implements TudeySceneModel.LayerObserver
{
    public LayerTableModel (SceneEditor editor)
    {
        _editor = editor;
    }

    public JTable getTable ()
    {
        JTable table = new JTable(this);
        // configure the cell editor for visible to not change the selection
        JCheckBox box = new JCheckBox();
        box.setHorizontalAlignment(JCheckBox.CENTER);
        table.setDefaultEditor(Boolean.class, new DefaultCellEditor(box) {
            public boolean shouldSelectCell (EventObject anEvent) {
                return false;
            }
        });
        TableColumnModel columnModel = table.getColumnModel();
        TableColumn numCol = columnModel.getColumn(0);
        numCol.setResizable(false);
        numCol.setMaxWidth(20);
        TableColumn visCol = columnModel.getColumn(2);
        visCol.setResizable(false);
        visCol.setMaxWidth(10);
        return table;
    }

    public void setScene (TudeySceneModel scene)
    {
        _scene = scene;
        _scene.addObserver(this);
        _vis = Lists.newArrayList(Collections.nCopies(getRowCount(), Boolean.TRUE));
        fireTableDataChanged();
    }

    public void addLayer (String name)
    {
        int newLayer = _scene.addLayer(name);
        _vis.add(Boolean.TRUE);
        fireTableRowsInserted(newLayer, newLayer);
    }

    public void removeLayer (int layer)
    {
        _scene.removeLayer(layer);
        _vis.remove(layer);
        fireTableRowsDeleted(layer, layer);
    }

    public List<Integer> getVisibleLayers ()
    {
        List<Integer> visLayers = Lists.newArrayList();
        for (int layer = 0, nn = _vis.size(); layer < nn; layer++) {
            if (_vis.get(layer)) {
                visLayers.add(layer);
            }
        }
        return visLayers;
    }

    // from TableModel
    public int getRowCount ()
    {
        return (_scene == null) ? 0 : _scene.getLayers().size();
    }

    // from TableModel
    public int getColumnCount ()
    {
        return 3;
    }

    // from TableModel
    public Object getValueAt (int row, int column)
    {
        switch (column) {
        case 0: return row;
        default: return _scene.getLayers().get(row);
        case 2: return _vis.get(row);
        }
    }

    @Override
    public Class<?> getColumnClass (int column)
    {
        switch (column) {
        case 0: return Integer.class;
        default: return String.class;
        case 2: return Boolean.class;
        }
    }

    @Override
    public String getColumnName (int column)
    {
        switch (column) {
        case 0: return "#";
        default: return "Layer";
        case 2: return "\u0298"; // sort of an eye-looking glyph
        }
    }

    @Override
    public boolean isCellEditable (int row, int column)
    {
        switch (column) {
        case 0: return false;
        default: return (row > 0);
        case 2: return true;
        }
    }

    @Override
    public void setValueAt (Object value, int row, int column)
    {
        Preconditions.checkArgument(isCellEditable(row, column));
        switch (column) {
        default:
            _scene.renameLayer(row, String.valueOf(value));
            break;

        case 2:
            Boolean visible = (Boolean)value;
            _vis.set(row, visible);
            updateVisibility(row, visible);
            break;
        }
        fireTableRowsUpdated(row, row);
    }

    /**
     * Update the visibilities of each layer, en masse.
     */
    public void setVisibilities (int... visibleLayers)
    {
        List<Integer> viz = Ints.asList(visibleLayers);
        for (int ii = 0, nn = _vis.size(); ii < nn; ii++) {
            _vis.set(ii, viz.contains(ii));
        }
        fireTableDataChanged();
        // update every damn entry
        for (TudeySceneModel.Entry entry : _scene.getEntries()) {
            Object key = entry.getKey();
            setVisibility(key, _vis.get(_scene.getLayer(key)));
        }
    }

    protected void updateVisibility (int layer, boolean visible)
    {
        // just enumerate all entries, since there is otherwise no list of things on layer 0
        for (TudeySceneModel.Entry entry : _scene.getEntries()) {
            Object key = entry.getKey();
            if (layer == _scene.getLayer(key)) {
                setVisibility(key, visible);
            }
        }
    }

    // from Observer (not part of public API)
    public void entryAdded (TudeySceneModel.Entry entry)
    {
        // nada
    }

    // from Observer (not part of public API)
    public void entryUpdated (TudeySceneModel.Entry oentry, TudeySceneModel.Entry nentry)
    {
        // nada
    }

    // from Observer (not part of public API)
    public void entryRemoved (TudeySceneModel.Entry oentry)
    {
        // nada
    }

    // from LayerObserver (not part of public API)
    public void entryLayerWasSet (Object key, int layer)
    {
        setVisibility(key, _vis.get(layer));
    }

    protected void setVisibility (Object key, boolean visible)
    {
        EntrySprite sprite = _editor.getView().getEntrySprite(key);
        if (sprite != null) {
            sprite.setVisible(visible);
        }
    }

    protected SceneEditor _editor;

    protected List<Boolean> _vis;

    protected TudeySceneModel _scene;
}
