//
// $Id$

package com.threerings.opengl.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import com.samskivert.util.ListUtil;

import com.threerings.util.ToolUtil;

import com.threerings.opengl.gui.icon.Icon;

/**
 * A handler for local object references.
 */
public class LocalTransferHandler<T> extends TransferHandler
{
    /**
     * Creates a new transfer handler for the specified class.
     */
    public LocalTransferHandler (Class<T> clazz)
    {
        _clazz = clazz;
        _flavor = ToolUtil.createLocalFlavor(clazz);
    }

    @Override // documentation inherited
    public Icon getVisualRepresentation (Transferable data)
    {
        try {
            return getVisualRepresentation(_clazz.cast(data.getTransferData(_flavor)));
        } catch (Exception e) {
            return null;
        }
    }

    @Override // documentation inherited
    public boolean canImport (Component comp, DataFlavor[] transferFlavors)
    {
        return ListUtil.contains(transferFlavors, _flavor) && canImport(comp);
    }

    @Override // documentation inherited
    public boolean importData (Component comp, Transferable data)
    {
        try {
            return importObject(comp, _clazz.cast(data.getTransferData(_flavor)));
        } catch (Exception e) {
            return false;
        }
    }

    @Override // documentation inherited
    protected Transferable createTransferable (Component comp)
    {
        final T object = getObject(comp);
        return new Transferable() {
            public DataFlavor[] getTransferDataFlavors () {
                return new DataFlavor[] { _flavor };
            }
            public boolean isDataFlavorSupported (DataFlavor flavor) {
                return flavor.equals(_flavor);
            }
            public Object getTransferData (DataFlavor flavor)
                throws UnsupportedFlavorException {
                if (flavor.equals(_flavor)) {
                    return object;
                } else {
                    throw new UnsupportedFlavorException(flavor);
                }
            }
        };
    }

    @Override // documentation inherited
    protected void exportDone (Component source, Transferable data, int action)
    {
        try {
            exportDone(source, _clazz.cast(data.getTransferData(_flavor)), action);
        } catch (Exception e) {
            // shouldn't happen
        }
    }

    /**
     * Returns the visual representation for the specified object, or null to use the default.
     */
    protected Icon getVisualRepresentation (T object)
    {
        return null;
    }

    /**
     * Determines whether the component can import an object.
     */
    protected boolean canImport (Component comp)
    {
        return false;
    }

    /**
     * Attempts to import the specified object.
     */
    protected boolean importObject (Component comp, T object)
    {
        return false;
    }

    /**
     * Returns the object associated with the component.
     */
    protected T getObject (Component comp)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Called when the export action has completed.
     */
    protected void exportDone (Component source, T object, int action)
    {
        // nothing by default
    }

    /** The class that we handle. */
    protected Class<T> _clazz;

    /** The corresponding data flavor. */
    protected DataFlavor _flavor;
}
