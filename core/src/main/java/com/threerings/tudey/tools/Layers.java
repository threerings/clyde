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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import java.util.Arrays;
import java.util.Collection;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.samskivert.swing.GroupLayout;

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
                if (!event.getValueIsAdjusting()) {
                    layerWasSelected(getSelectedLayer());
                }
            }
        });
        _tableModel.addTableModelListener(new TableModelListener() {
            public void tableChanged (TableModelEvent event) {
                _mergeVisibleLayersAction.setEnabled(
                    2 <= Collections.frequency(getLayerVisibility(), true));
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
     * Add a ChangeListener.
     *
     * A ChangeEvent will be emitted whenever a layer is added, removed, or selected.
     */
    public void addChangeListener (ChangeListener listener)
    {
        listenerList.add(ChangeListener.class, listener);
    }

    /**
     * Remove a ChangeListener.
     */
    public void removeChangeListener (ChangeListener listener)
    {
        listenerList.remove(ChangeListener.class, listener);
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
        setVisibleLayers(Arrays.asList(0, layer));
        setSelectedLayer(layer);
    }

    /**
     * Set the visible layers.
     * If a layer index is not in the collection, it will be invisible.
     */
    public void setVisibleLayers (Collection<Integer> layersToMakeVisible)
    {
        int selected = getSelectedLayer();
        _tableModel.setVisibilities(layersToMakeVisible);
        setSelectedLayer(selected);
    }

    /**
     * Get a <b>view</b> of layer visibility.
     *
     * @return a view of layer visibility, with each element corresponding to the layer
     * at the same index.
     */
    public List<Boolean> getLayerVisibility ()
    {
        return _tableModel.getLayerVisibility();
    }

    /**
     * Get a new, mutable list of the currently visible layer indexes.
     */
    public List<Integer> getVisibleLayers ()
    {
        List<Integer> visible = Lists.newArrayList();
        List<Boolean> visibility = getLayerVisibility();
        for (int layer = 0, nn = visibility.size(); layer < nn; layer++) {
            if (visibility.get(layer)) {
                visible.add(layer);
            }
        }
        return visible;
    }

    @Override
    public void sceneChanged (TudeySceneModel scene)
    {
        super.sceneChanged(scene);
        _tableModel.setScene(scene);
        setSelectedLayer(0);
    }

    /**
     * Callback when a layer is selected, by the user or programmatically.
     */
    protected void layerWasSelected (int layer)
    {
        _removeLayerAction.setEnabled(layer != 0);
        fireStateChanged();
    }

    /**
     * Fire off a ChangeEvent.
     */
    protected void fireStateChanged ()
    {
        Object[] listeners = listenerList.getListenerList();
        ChangeEvent event = null;
        for (int ii = listeners.length - 2; ii >= 0; ii -= 2) {
            if (listeners[ii] == ChangeListener.class) {
                if (event == null) {
                    event = new ChangeEvent(this);
                }
                ((ChangeListener)listeners[ii + 1]).stateChanged(event);
            }
        }
    }

    /**
     * Merge the currently visible layers.
     */
    protected void mergeVisible ()
    {
        List<Integer> visible = getVisibleLayers();
        // remove the first layer and call that the "mergeTo" layer
        int mergeTo = visible.remove(0); // remove first value, not value 0!

        for (TudeySceneModel.Entry entry : _scene.getEntries()) {
            Object key = entry.getKey();
            if (visible.contains(_scene.getLayer(key))) {
                _scene.setLayer(key, mergeTo);
            }
        }

        // kill the layers, highest to lowest
        for (Integer layer : Lists.reverse(visible)) {
            _tableModel.removeLayer(layer);
        }
        fireStateChanged();
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
            setSelectedLayer(_tableModel.addLayer("Layer " + newLayer, newLayer));
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
                // if it's currently selected, select the layer below it (we can never remove 0)
                if (getSelectedLayer() == layer) {
                    setSelectedLayer(layer - 1);
                }
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
        _scene.addObserverLast(this); // we need to be last!
        _vis = Lists.newArrayList(Collections.nCopies(getRowCount(), Boolean.TRUE));
        fireTableDataChanged();
    }

    public int addLayer (String name, int position)
    {
        int newLayer = _scene.addLayer(name, position);
        _vis.add(newLayer, Boolean.TRUE);
        fireTableRowsInserted(newLayer, newLayer);
        return newLayer;
    }

    public void removeLayer (int layer)
    {
        _scene.removeLayer(layer);
        _vis.remove(layer);
        fireTableRowsDeleted(layer, layer);
    }

    /**
     * Get <b>view</b> of the layer visibilities.
     */
    public List<Boolean> getLayerVisibility ()
    {
        return Collections.unmodifiableList(_vis);
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
    public void setVisibilities (Collection<Integer> visibleLayers)
    {
        for (int ii = 0, nn = _vis.size(); ii < nn; ii++) {
            _vis.set(ii, visibleLayers.contains(ii));
        }
        fireTableDataChanged();
        // update every damn entry
        for (TudeySceneModel.Entry entry : _scene.getEntries()) {
            setVisibility(entry);
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
        setVisibility(entry);
    }

    // from Observer (not part of public API)
    public void entryUpdated (TudeySceneModel.Entry oentry, TudeySceneModel.Entry nentry)
    {
        setVisibility(nentry);
    }

    // from Observer (not part of public API)
    public void entryRemoved (TudeySceneModel.Entry oentry)
    {
        // nada
    }

    // from LayerObserver (not part of public API)
    public void entryLayerWasSet (Object key, int layer)
    {
        setVisibility(key, layer);
    }

    protected void setVisibility (TudeySceneModel.Entry entry)
    {
        Object key = entry.getKey();
        setVisibility(key, _scene.getLayer(key));
    }

    protected void setVisibility (Object key, int layer)
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
