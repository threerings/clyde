//
// $Id$

package com.threerings.tudey.tools;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.samskivert.swing.GroupLayout;

import com.google.common.collect.Lists;

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
        _tableModel = new LayerTableModel(editor);
        _table = _tableModel.getTable();
        _table.setPreferredScrollableViewportSize(new Dimension(100, 64));
        _table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        _table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged (ListSelectionEvent event) {
                int layer = getSelectedLayer();
                if (layer == -1) {
                    layer = 0;
                    setSelectedLayer(layer);
                }
                _removeLayerAction.setEnabled(layer != 0);
            }
        });
        _removeLayerAction.setEnabled(false);

        add(GroupLayout.makeButtonBox(new JLabel("Layers"),
            new JButton(_addLayerAction), new JButton(_removeLayerAction)), GroupLayout.FIXED);
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
    }

    @Override
    public void sceneChanged (TudeySceneModel scene)
    {
        super.sceneChanged(scene);
        _tableModel.setScene(scene);
        setSelectedLayer(0);
    }

    /** The table. */
    protected JTable _table;

    /** The table model. */
    protected LayerTableModel _tableModel;

    /** An action for adding a new layer. */
    protected Action _addLayerAction = new AbstractAction("+") {
        public void actionPerformed (ActionEvent e) {
            _tableModel.addLayer("Layer " + (1 + _scene.getLayers().size()));
        }
    };

    /** An action for removing the currently selected layer. */
    protected Action _removeLayerAction = new AbstractAction("-") {
        public void actionPerformed (ActionEvent e) {
            int layer = getSelectedLayer();
            if (_scene.isLayerEmpty(layer) ||
                (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
                    Layers.this,
                    "This layer has stuff on it.\n" +
                        "Continue anyway and move the stuff to the base layer?",
                    "Layer is not empty!",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE))) {
                _tableModel.removeLayer(layer);
            }
        }
    };
}

/**
 * A table model for the table in the layer tool.
 */
class LayerTableModel extends AbstractTableModel
{
    public LayerTableModel (SceneEditor editor)
    {
        _editor = editor;
    }

    public JTable getTable ()
    {
        JTable table = new JTable(this);
        TableColumnModel columnModel = table.getColumnModel();
        TableColumn visCol = columnModel.getColumn(1);
        visCol.setResizable(false);
        visCol.setMaxWidth(10);
        return table;
    }

    public void setScene (TudeySceneModel scene)
    {
        _scene = scene;
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
        // update the visibilty of all objects on that layer to match layer 0's vis
        updateVisibility(layer, _vis.get(0));
        // remove it
        _scene.removeLayer(layer);
        _vis.remove(layer);
        fireTableRowsDeleted(layer, layer);
    }

    // from TableModel
    public int getRowCount ()
    {
        return (_scene == null) ? 0 : _scene.getLayers().size();
    }

    // from TableModel
    public int getColumnCount ()
    {
        return 2;
    }

    // from TableModel
    public Object getValueAt (int row, int column)
    {
        switch (column) {
        default: return _scene.getLayers().get(row);
        case 1: return _vis.get(row);
        }
    }

    @Override
    public Class<?> getColumnClass (int column)
    {
        switch (column) {
        default: return String.class;
        case 1: return Boolean.class;
        }
    }

    @Override
    public String getColumnName (int column)
    {
        switch (column) {
        default: return "Layers";
        case 1: return "i"; // TODO
        }
    }

    @Override
    public boolean isCellEditable (int row, int column)
    {
        switch (column) {
        default: return (row > 0);
        case 1: return true;
        }
    }

    @Override
    public void setValueAt (Object value, int row, int column)
    {
        switch (column) {
        default:
            _scene.renameLayer(row, String.valueOf(value));
            break;

        case 1:
            Boolean visible = (Boolean)value;
            _vis.set(row, visible);
            updateVisibility(row, visible);
            break;
        }
        fireTableRowsUpdated(row, row);
    }

    protected void updateVisibility (int layer, boolean visible)
    {
        TudeySceneView view = _editor.getView();
        // just enumerate all entries, since there is otherwise no list of things on layer 0
        for (TudeySceneModel.Entry entry : _scene.getEntries()) {
            Object key = entry.getKey();
            if (layer == _scene.getLayer(key)) {
                EntrySprite sprite = view.getEntrySprite(key);
                if (sprite == null) {
                    System.err.println("WTF: " + key);
                } else {
                    sprite.getModel().setVisible(visible);
                }
            }
        }
    }

    protected SceneEditor _editor;

    protected List<Boolean> _vis;

    protected TudeySceneModel _scene;
}
