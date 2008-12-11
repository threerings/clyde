//
// $Id$

package com.threerings.editor.swing.editors;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;

import java.lang.reflect.Array;

import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import com.samskivert.swing.GroupLayout;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ClassUtil;
import com.samskivert.util.IntTuple;
import com.samskivert.util.ListUtil;

import com.threerings.util.DeepUtil;
import com.threerings.util.MessageBundle;

import com.threerings.editor.Introspector;
import com.threerings.editor.Property;
import com.threerings.editor.swing.ObjectPanel;

/**
 * An editor for objects or lists of objects or primitives.  Uses a table.
 */
public class TableArrayListEditor extends ArrayListEditor
    implements TableModel, ListSelectionListener, ChangeListener
{
    // documentation inherited from interface TableModel
    public int getRowCount ()
    {
        return getLength();
    }

    // documentation inherited from interface TableModel
    public int getColumnCount ()
    {
        return _columns.length;
    }

    // documentation inherited from interface TableModel
    public String getColumnName (int column)
    {
        return _columns[column].getName();
    }

    // documentation inherited from interface TableModel
    public Class<?> getColumnClass (int column)
    {
        return _columns[column].getColumnClass();
    }

    // documentation inherited from interface TableModel
    public boolean isCellEditable (int row, int column)
    {
        return _columns[column].isEditable();
    }

    // documentation inherited from interface TableModel
    public Object getValueAt (int row, int column)
    {
        return _columns[column].getColumnValue(row);
    }

    // documentation inherited from interface TableModel
    public void setValueAt (Object value, int row, int column)
    {
        _columns[column].setColumnValue(row, value);
        fireTableChanged(row, row, column, TableModelEvent.UPDATE);
        fireStateChanged(true);
    }

    // documentation inherited from interface TableModel
    public void addTableModelListener (TableModelListener listener)
    {
        listenerList.add(TableModelListener.class, listener);
    }

    // documentation inherited from interface TableModel
    public void removeTableModelListener (TableModelListener listener)
    {
        listenerList.add(TableModelListener.class, listener);
    }

    // documentation inherited from interface ListSelectionListener
    public void valueChanged (ListSelectionEvent event)
    {
        updateSelected();
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        int row = _table.getSelectedRow();
        setValue(row, _opanel.getValue());
        fireTableChanged(row, row, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
        fireStateChanged(true);
    }

    @Override // documentation inherited
    public void actionPerformed (ActionEvent event)
    {
        Object source = event.getSource();
        if (source == _add && is2DArray()) {
            // create a new row of the required type, populated with default instances
            Class cctype = _property.getComponentType().getComponentType();
            Object value = Array.newInstance(cctype, _columns.length);
            for (int ii = 0; ii < _columns.length; ii++) {
                Array.set(value, ii, getDefaultInstance(cctype, _object));
            }
            addValue(value);

        } else if (source == _addColumn) {
            addColumn();

        } else if (source == _copy) {
            IntTuple selection = getSelection();
            if (selection.right == -1) {
                copyValue(selection.left);
            } else {
                copyColumn(selection.right);
            }
        } else if (source == _delete) {
            IntTuple selection = getSelection();
            if (selection.right == -1) {
                removeValue(selection.left);
            } else {
                removeColumn(selection.right);
            }
        } else {
            super.actionPerformed(event);
        }
    }

    @Override // documentation inherited
    public void update ()
    {
        int min = 0, max = Integer.MAX_VALUE;
        if (is2DArray()) {
            createArrayColumns();
            min = max = TableModelEvent.HEADER_ROW;
        }
        fireTableChanged(min, max, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
        if (min == TableModelEvent.HEADER_ROW) {
            updateColumnWidths();
        }
        updateSelected();
    }

    @Override // documentation inherited
    public void makeVisible (int idx)
    {
        setSelection(idx, -1);
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        // determine the column model
        final Class ctype = _property.getComponentType();
        boolean showHeader = true;
        if (is2DArray()) {
            _columns = new Column[0]; // actual columns will be created on update

        } else if (isTableCellType(ctype)) {
            _columns = new Column[] { new Column() {
                public String getName () {
                    return null;
                }
                public String getPathComponent () {
                    return "";
                }
                public Class getColumnClass () {
                    return ClassUtil.objectEquivalentOf(ctype);
                }
                public Object getColumnValue (int row) {
                    return getValue(row);
                }
                public void setColumnValue (int row, Object value) {
                    setValue(row, value);
                }
                public int getWidth () {
                    return _property.getAnnotation().width();
                }
            }};
            showHeader = false;

        } else {
            Property[] properties = Introspector.getProperties(ctype);
            if (!_property.getAnnotation().nullable()) {
                ArrayList<Column> columns = new ArrayList<Column>();
                final MessageBundle msgs = _msgmgr.getBundle(Introspector.getMessageBundle(ctype));
                for (int ii = 0; ii < properties.length; ii++) {
                    final Property property = properties[ii];
                    if (!property.getAnnotation().column()) {
                        continue;
                    }
                    columns.add(new Column() {
                        public String getName () {
                            return getLabel(property.getName(), msgs);
                        }
                        public String getPathComponent () {
                            return "." + property.getName();
                        }
                        public Class getColumnClass () {
                            return ClassUtil.objectEquivalentOf(property.getType());
                        }
                        public Object getColumnValue (int row) {
                            return property.get(getValue(row));
                        }
                        public void setColumnValue (int row, Object value) {
                            property.set(getValue(row), value);
                        }
                        public int getWidth () {
                            return property.getAnnotation().width();
                        }
                    });
                }
                _columns = columns.toArray(new Column[columns.size()]);
            }
            int ncols = (_columns == null) ? 0 : _columns.length;
            if (ncols == 0) {
                _columns = new Column[] { new Column() {
                    public String getName () {
                        return "";
                    }
                    public String getPathComponent () {
                        return "";
                    }
                    public Class getColumnClass () {
                        return String.class;
                    }
                    public boolean isEditable () {
                        return false;
                    }
                    public Object getColumnValue (int row) {
                        Object value = getValue(row);
                        return getLabel(value == null ? null : value.getClass());
                    }
                    public void setColumnValue (int row, Object value) {
                        // no-op
                    }
                    public int getWidth () {
                        return 20;
                    }
                }};
                showHeader = false;
            }
            Class[] types = _property.getComponentSubtypes();
            _opanel = new ObjectPanel(
                _ctx, _property.getComponentTypeLabel(), types, _lineage, _object, ncols > 0);
            _opanel.addChangeListener(this);
        }

        JPanel outer = new JPanel();
        outer.setBackground(null);
        add(outer);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(null);
        outer.add(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        _table = new JTable(this);
        if (showHeader) {
            _table.getTableHeader().setReorderingAllowed(false);
            panel.add(_table.getTableHeader(), BorderLayout.NORTH);
        }
        updateColumnWidths();
        panel.add(_table, BorderLayout.CENTER);
        if (is2DArray()) {
            _table.setColumnSelectionAllowed(true);
            _table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        } else {
            _table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }
        _table.getSelectionModel().addListSelectionListener(this);
        _table.getColumnModel().getSelectionModel().addListSelectionListener(this);

        // hacky transferable lets us move rows around in the array
        _table.setDragEnabled(true);
        final DataFlavor cflavor = new DataFlavor(IntTuple.class, null);
        _table.setTransferHandler(new TransferHandler() {
            public int getSourceActions (JComponent comp) {
                return MOVE;
            }
            public boolean canImport (JComponent comp, DataFlavor[] flavors) {
                return ListUtil.containsRef(flavors, cflavor);
            }
            public boolean importData (JComponent comp, Transferable t) {
                try {
                    IntTuple selection = (IntTuple)t.getTransferData(cflavor);
                    if (selection.left == -1) {
                        moveColumn(selection.right);
                    } else if (selection.right == -1) {
                        moveValue(selection.left);
                    } else {
                        moveCell(selection.left, selection.right);
                    }
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
            protected Transferable createTransferable (JComponent c) {
                final IntTuple selection = getSelection();
                if (selection == null) {
                    return null;
                }
                // set the selection mode depending on the selection type
                if (is2DArray()) {
                    if (selection.left == -1) {
                        _table.setRowSelectionAllowed(false);
                    } else if (selection.right == -1) {
                        _table.setColumnSelectionAllowed(false);
                    }
                }
                return new Transferable() {
                    public Object getTransferData (DataFlavor flavor) {
                        return selection;
                    }
                    public DataFlavor[] getTransferDataFlavors () {
                        return new DataFlavor[] { cflavor };
                    }
                    public boolean isDataFlavorSupported (DataFlavor flavor) {
                        return flavor == cflavor;
                    }
                };
            }
            protected void exportDone (JComponent source, Transferable data, int action) {
                // restore the selection mode
                if (is2DArray()) {
                    _table.setCellSelectionEnabled(true);
                }
            }
        });

        JPanel bpanel = new JPanel();
        bpanel.setBackground(null);
        add(bpanel);
        bpanel.add(_add = new JButton(is2DArray() ?
            getActionLabel("new", "row") : _msgs.get("m.new")));
        _add.addActionListener(this);
        if (is2DArray()) {
            bpanel.add(_addColumn = new JButton(getActionLabel("new", "column")));
            _addColumn.addActionListener(this);
        }
        bpanel.add(_copy = new JButton(_msgs.get("m.copy")));
        _copy.addActionListener(this);
        bpanel.add(_delete = new JButton(_msgs.get("m.delete")));
        _delete.addActionListener(this);

        if (_opanel != null) {
            add(_opanel);
        }
    }

    @Override // documentation inherited
    protected String getMousePath (Point pt)
    {
        if (getComponentAt(pt) == _opanel) {
            return "[" + _table.getSelectedRow() + "]" + _opanel.getMousePath();
        }
        pt = SwingUtilities.convertPoint(this, pt, _table);
        int row = _table.rowAtPoint(pt);
        int col = _table.columnAtPoint(pt);
        return ((row == -1 || col == -1) ? "" :
            ("[" + row + "]" + _columns[col].getPathComponent()));
    }

    @Override // documentation inherited
    protected void addValue (Object value)
    {
        super.addValue(value);
        int row = getLength() - 1;
        fireTableChanged(row, row, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
        if (_columns.length > 0) {
            setSelection(row, -1);
        }
    }

    @Override // documentation inherited
    protected void copyValue (int idx)
    {
        super.copyValue(idx);
        int row = idx + 1;
        fireTableChanged(row, row, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
        if (_columns.length > 0) {
            setSelection(row, -1);
        }
    }

    @Override // documentation inherited
    protected void removeValue (int idx)
    {
        super.removeValue(idx);
        fireTableChanged(idx, idx, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE);
        setSelection(Math.min(idx, getLength() - 1), -1);
    }

    /**
     * Adds a new column.
     */
    protected void addColumn ()
    {
        // update the column model
        _columns = ArrayUtil.append(_columns, createArrayColumn(_columns.length));

        // expand all rows to include the new column
        Class cctype = _property.getComponentType().getComponentType();
        for (int ii = 0, nn = getLength(); ii < nn; ii++) {
            Object ovalue = getValue(ii);
            Object nvalue = Array.newInstance(cctype, _columns.length);
            System.arraycopy(ovalue, 0, nvalue, 0, _columns.length - 1);
            Array.set(nvalue, _columns.length - 1, getDefaultInstance(cctype, _object));
            setValue(ii, nvalue);
        }

        // fire notification events, update selection
        fireStateChanged(true);
        fireTableChanged(
            TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW,
            _columns.length - 1, TableModelEvent.INSERT);
        updateColumnWidths();
        if (getLength() > 0) {
            setSelection(-1, _columns.length - 1);
        }
    }

    /**
     * Copies the column at the specified index.
     */
    protected void copyColumn (int column)
    {
        // update the column model
        _columns = ArrayUtil.append(_columns, createArrayColumn(_columns.length));

        // expand all rows to include the new column
        int idx = column + 1;
        Class cctype = _property.getComponentType().getComponentType();
        for (int ii = 0, nn = getLength(); ii < nn; ii++) {
            Object ovalue = getValue(ii);
            Object nvalue = Array.newInstance(cctype, _columns.length);
            System.arraycopy(ovalue, 0, nvalue, 0, idx);
            Array.set(nvalue, idx, DeepUtil.copy(Array.get(ovalue, column)));
            System.arraycopy(ovalue, idx, nvalue, idx + 1, _columns.length - idx - 1);
            setValue(ii, nvalue);
        }

        // fire notification events, update selection
        fireStateChanged(true);
        fireTableChanged(
            TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW,
            idx, TableModelEvent.INSERT);
        updateColumnWidths();
        if (getLength() > 0) {
            setSelection(-1, idx);
        }
    }

    /**
     * Deletes the column at the specified index.
     */
    protected void removeColumn (int column)
    {
        // update the column model
        _columns = ArrayUtil.splice(_columns, _columns.length - 1);

        // remove the column from all rows
        Class cctype = _property.getComponentType().getComponentType();
        for (int ii = 0, nn = getLength(); ii < nn; ii++) {
            Object ovalue = getValue(ii);
            Object nvalue = Array.newInstance(cctype, _columns.length);
            System.arraycopy(ovalue, 0, nvalue, 0, column);
            System.arraycopy(ovalue, column + 1, nvalue, column, _columns.length - column);
            setValue(ii, nvalue);
        }

        // fire notification events, update selection
        fireStateChanged(true);
        fireTableChanged(
            TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW,
            TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
        updateColumnWidths();
        setSelection(-1, Math.min(column, _columns.length - 1));
    }

    /**
     * Updates the preferred widths of the columns.
     */
    protected void updateColumnWidths ()
    {
        for (int ii = 0; ii < _columns.length; ii++) {
            // the default width is in characters, so fudge it a bit for pixels
            _table.getColumnModel().getColumn(ii).setPreferredWidth(
                _columns[ii].getWidth() * 10);
        }
    }

    /**
     * Determines whether the property is a 2D array.
     */
    protected boolean is2DArray ()
    {
        Class ctype = _property.getComponentType();
        return ctype.isArray() && isTableCellType(ctype.getComponentType());
    }

    /**
     * (Re)creates the columns for a 2D array property.
     */
    protected void createArrayColumns ()
    {
        Object element = (getLength() == 0) ? null : getValue(0);
        _columns = new Column[element == null ? 0 : Array.getLength(element)];
        for (int ii = 0; ii < _columns.length; ii++) {
            _columns[ii] = createArrayColumn(ii);
        }
    }

    /**
     * Creates and returns an array column.
     */
    protected Column createArrayColumn (final int column)
    {
        final Class cctype = _property.getComponentType().getComponentType();
        return new Column() {
            public String getName () {
                return Integer.toString(column);
            }
            public String getPathComponent () {
                return "[" + column + "]";
            }
            public Class getColumnClass () {
                return ClassUtil.objectEquivalentOf(cctype);
            }
            public Object getColumnValue (int row) {
                return Array.get(getValue(row), column);
            }
            public void setColumnValue (int row, Object value) {
                Array.set(getValue(row), column, value);
            }
            public int getWidth () {
                return _property.getAnnotation().width();
            }
        };
    }

    /**
     * Moves the specified row to the selected row.
     */
    protected void moveValue (int row)
    {
        int selected = _table.getSelectedRow();
        if (selected == row) {
            return;
        }
        // store the value at the original row and shift the intermediate values up/down
        Object value = getValue(row);
        int dir = (selected < row) ? -1 : +1;
        for (int ii = row; ii != selected; ii += dir) {
            setValue(ii, getValue(ii + dir));
        }
        setValue(selected, value);
        fireTableChanged(
            Math.min(selected, row), Math.max(selected, row),
            TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
        fireStateChanged(true);
        setSelection(selected, -1);
        updateSelected();
    }

    /**
     * Moves a column to the selected column.
     */
    protected void moveColumn (int column)
    {
        int selected = _table.getSelectedColumn();
        if (selected == column) {
            return;
        }
        for (int ii = 0, nn = getLength(); ii < nn; ii++) {
            moveWithinArray(getValue(ii), column, selected);
        }
        fireTableChanged(
            0, Integer.MAX_VALUE, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
        fireStateChanged(true);
        setSelection(-1, selected);
        updateSelected();
    }

    /**
     * Moves a single cell to the selected cell.
     */
    protected void moveCell (int row, int col)
    {
        int srow = _table.getSelectedRow();
        int scol = _table.getSelectedColumn();
        if (!(srow == row ^ scol == col)) {
            return; // must move within same column or same row
        }
        if (srow == row) {
            moveWithinArray(getValue(row), col, scol);
            fireTableChanged(row, row, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
        } else { // scol == col
            Column column = _columns[col];
            Object value = column.getColumnValue(row);
            int dir = (srow < row) ? -1 : +1;
            for (int ii = row; ii != srow; ii += dir) {
                column.setColumnValue(ii, column.getColumnValue(ii + dir));
            }
            column.setColumnValue(srow, value);
            fireTableChanged(
                Math.min(srow, row), Math.max(srow, row), col, TableModelEvent.UPDATE);
        }
        fireStateChanged(true);
        updateSelected();
    }

    /**
     * Moves the value at <code>source</code> to <code>dest</code>, shifting values left
     * or right to make room.
     */
    protected void moveWithinArray (Object array, int source, int dest)
    {
        Object value = Array.get(array, source);
        if (dest < source) {
            System.arraycopy(array, dest, array, dest + 1, source - dest);
        } else {
            System.arraycopy(array, source + 1, array, source, dest - source);
        }
        Array.set(array, dest, value);
    }

    /**
     * Updates based on the selection state.
     */
    protected void updateSelected ()
    {
        IntTuple selection = getSelection();
        boolean row = false, column = false;
        if (selection != null) {
            row = (selection.right == -1);
            column = (selection.left == -1);
        }
        _delete.setEnabled(column || row && getLength() > _min);
        _copy.setEnabled(column || row && getLength() < _max);
        if (_opanel != null) {
            if (selection == null) {
                _opanel.setVisible(false);
            } else {
                _opanel.setVisible(true);
                _opanel.setValue(getValue(selection.left));
            }
        }
    }

    /**
     * Returns the selection as a (row, column) pair.  If an entire row is selected, column
     * will be -1.  If an entire column is selected, row will be -1.  If both numbers are
     * valid, a single cell at that location is selected.  Otherwise, the method returns
     * <code>null</code> to indicate that there is no usable selection.
     */
    protected IntTuple getSelection ()
    {
        if (!_table.getColumnSelectionAllowed()) {
            int row = _table.getSelectedRow();
            return (row == -1) ? null : new IntTuple(row, -1);
        } else if (!_table.getRowSelectionAllowed()) {
            int column = _table.getSelectedColumn();
            return (column == -1) ? null : new IntTuple(-1, column);
        }
        int[] rows = _table.getSelectedRows();
        int[] cols = _table.getSelectedColumns();
        if (rows.length == 1) {
            if (cols.length == 1) {
                return new IntTuple(rows[0], cols[0]);
            } else if (cols.length == _columns.length) {
                return new IntTuple(rows[0], -1);
            }
        } else if (cols.length == 1 && rows.length == getLength()) {
            return new IntTuple(-1, cols[0]);
        }
        return null;
    }

    /**
     * Sets the selection in using the convention of {@link #getSelection}.
     */
    protected void setSelection (int row, int column)
    {
        if (row == -1 && column == -1) {
            _table.clearSelection();
            return;
        }
        if (row == -1) {
            _table.setRowSelectionInterval(0, getLength() - 1);
        } else {
            _table.setRowSelectionInterval(row, row);
        }
        if (!is2DArray()) {
            return;
        }
        if (column == -1) {
            _table.setColumnSelectionInterval(0, _columns.length - 1);
        } else {
            _table.setColumnSelectionInterval(column, column);
        }
    }

    /**
     * Fires a {@link TableModelEvent}.
     */
    protected void fireTableChanged (int firstRow, int lastRow, int column, int type)
    {
        Object[] listeners = listenerList.getListenerList();
        TableModelEvent event = null;
        for (int ii = listeners.length - 2; ii >= 0; ii -= 2) {
            if (listeners[ii] == TableModelListener.class) {
                if (event == null) {
                    event = new TableModelEvent(this, firstRow, lastRow, column, type);
                }
                ((TableModelListener)listeners[ii + 1]).tableChanged(event);
            }
        }
    }

    /**
     * Represents a column in the table.
     */
    protected abstract class Column
    {
        /**
         * Returns the name of this column.
         */
        public abstract String getName ();

        /**
         * Returns the path component for this column.
         */
        public abstract String getPathComponent ();

        /**
         * Returns the class of this column.
         */
        public abstract Class getColumnClass ();

        /**
         * Determines whether cells in this column are editable.
         */
        public boolean isEditable ()
        {
            return true;
        }

        /**
         * Returns the value of this column at the specified row.
         */
        public abstract Object getColumnValue (int row);

        /**
         * Sets the value at the specified row.
         */
        public abstract void setColumnValue (int row, Object value);

        /**
         * Returns the preferred width of the column.
         */
        public abstract int getWidth ();
    }

    /** The column info. */
    protected Column[] _columns;

    /** The table containing the array data. */
    protected JTable _table;

    /** The add column button. */
    protected JButton _addColumn;

    /** The copy and delete buttons. */
    protected JButton _copy, _delete;

    /** The object panel used to edit the non-inline properties. */
    protected ObjectPanel _opanel;
}
