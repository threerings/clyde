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

    @Override
    public Icon getVisualRepresentation (Transferable data)
    {
        try {
            return getVisualRepresentation(_clazz.cast(data.getTransferData(_flavor)));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean canImport (Component comp, DataFlavor[] transferFlavors)
    {
        return ListUtil.contains(transferFlavors, _flavor) && canImport(comp);
    }

    @Override
    public boolean importData (Component comp, Transferable data)
    {
        try {
            return importObject(comp, _clazz.cast(data.getTransferData(_flavor)));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
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

    @Override
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
